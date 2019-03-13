package ru.majordomo.hms.personmgr.service.order.ssl;

import com.querydsl.core.BooleanBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import ru.majordomo.hms.personmgr.common.CSRGenerator;
import ru.majordomo.hms.personmgr.common.OrderState;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.exception.InternalApiException;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.manager.AccountHistoryManager;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.order.ssl.*;
import ru.majordomo.hms.personmgr.service.AccountHelper;
import ru.majordomo.hms.personmgr.service.AccountNotificationHelper;
import ru.majordomo.hms.personmgr.service.ChargeMessage;
import ru.majordomo.hms.personmgr.service.gogetssl.GoGetSSLConnection;
import ru.majordomo.hms.personmgr.service.gogetssl.GoGetSSLConnectionFactory;
import ru.majordomo.hms.personmgr.manager.EntityBuilder;
import ru.majordomo.hms.personmgr.service.order.OrderManager;

import java.io.IOException;
import java.net.IDN;
import java.time.LocalDate;
import java.util.*;

import static ru.majordomo.hms.personmgr.common.FileUtils.getZipFileBytes;
import static ru.majordomo.hms.personmgr.model.order.ssl.ExternalState.REJECTED;

@Slf4j
@Service
public class SSLOrderManager extends OrderManager<SslCertificateOrder> {

    private final GoGetSSLConnectionFactory connectionFactory;
    private final AccountNotificationHelper notificationHelper;
    private final PersonalAccountManager accountManager;
    private final EntityBuilder<SslCertificateOrder> builder;
    private final AccountHelper accountHelper;
    private final AccountHistoryManager history;

    @Autowired
    public SSLOrderManager(
            GoGetSSLConnectionFactory connectionFactory,
            AccountNotificationHelper notificationHelper,
            PersonalAccountManager accountManager,
            EntityBuilder<SslCertificateOrder> builder,
            AccountHelper accountHelper,
            AccountHistoryManager history
    ) {
        this.connectionFactory = connectionFactory;
        this.notificationHelper = notificationHelper;
        this.accountManager = accountManager;
        this.builder = builder;
        this.accountHelper = accountHelper;
        this.history = history;
    }

    @Override
    protected void onCreate(SslCertificateOrder order) {
        build(order);

        generatePrivateKeyAndCsr(order);

        PersonalAccount account = accountManager.findOne(order.getPersonalAccountId());

        SimpleServiceMessage blockResult = accountHelper.block(
                account, ChargeMessage.builder(order.getProduct().getService()).build()
        );

        order.setDocumentNumber((String) blockResult.getParam("documentNumber"));

        history.save(
                account,
                "Заказан SSL-сертификат " + order.getProduct().getName() + " для домена " + order.getDomainName(),
                order.getOperator()
        );
    }

    private ExternalState getOnlyExternalStateFromGoGetSsl(SslCertificateOrder order) {
        return ExternalState.creator(
                connectionFactory.getConnection().getOrderStatus(order.getExternalOrderId()).get("status").toString()
        );
    }

    @Override
    protected void onDecline(SslCertificateOrder order) {
        if (order.getExternalState().equals(ExternalState.ACTIVE)
                || (order.getChain() != null && !order.getChain().isEmpty())
        ) {
            throw new ParameterValidationException("Сертификат выпущен, заказ не может быть отменён");
        }

        ExternalState realExternalState = getOnlyExternalStateFromGoGetSsl(order);

        if (realExternalState.equals(ExternalState.ACTIVE)) {
            throw new ParameterValidationException("Сертификат выпущен, заказ не может быть отменён");
        } else if (
                Arrays.asList(ExternalState.CANCELED, REJECTED).contains(realExternalState)
        ) {
            if (order.getDocumentNumber() != null) {
                try {
                    accountHelper.unblock(order.getPersonalAccountId(), order.getDocumentNumber());
                } catch (Exception e) {
                    log.error("on unblock money for order {} e {} message {}", order, e.getClass(), e.getMessage());
                }
            }
        } else {
            connectionFactory.getConnection().cancelSSLOrder(
                    order.getExternalOrderId(), "Domain owner refused order"
            );
        }
    }

    @Override
    protected void onFinish(SslCertificateOrder order) {
        if (order.getDocumentNumber() != null) {
            accountHelper.chargeBlocked(order.getPersonalAccountId(), order.getDocumentNumber());
        }

        try {
            Map<String, byte[]> files = new HashMap<>();

            int count = 0;
            for (String s : order.getChain()) {
                count++;
                String name = count == 1 ? order.getDomainName() + ".crt" : "ca" + count + ".crt";
                files.put(name, s.getBytes());
            }

            Map<String, String> attachment = new HashMap<>();
            try {
                String body = Base64.getEncoder().encodeToString(getZipFileBytes(files));

                notificationHelper.attachment()
                        .body(body)
                        .fileName(order.getDomainName() + ".zip")
                        .mimeType("application/zip")
                        .build();

            } catch (IOException e) {
                e.printStackTrace();
            }

            String certificateAuthority = String.join("\n", order.getChain().subList(1, order.getChain().size()));

            PersonalAccount account = accountManager.findOne(order.getPersonalAccountId());

            notificationHelper.emailBuilder()
                    .account(account)
                    .apiName("MajordomoSSLCertOrderIssued")
                    .attachment(attachment)
                    .param("domain", order.getDomainName())
                    .param("ssl_crt_code", order.getChain().get(0))
                    .param("ssl_ca_code", certificateAuthority)
                    .send();
        } catch (Exception e) {
            history.save(
                    order.getPersonalAccountId(),
                    "Не удалось отправить письмо с сертификатом для " + order.getDomainName(),
                    order.getOperator()
            );
        }
    }

    public List<SslCertificateOrder> getPendingOrders() {
        QSslCertificateOrder qOrder = QSslCertificateOrder.sslCertificateOrder;

        List<SslCertificateOrder> all = findAll(
                new BooleanBuilder()
                        .and(qOrder.state.notIn(OrderState.DECLINED, OrderState.FINISHED))
                        .and(qOrder.externalState.notIn(ExternalState.CANCELED, ExternalState.ACTIVE))
        );

        all.forEach(this::build);

        return all;
    }

    public void process(List<SslCertificateOrder> orders) {
        if (orders.isEmpty()) {
            log.debug("have not pending orders");
            return;
        }

        GoGetSSLConnection connection = connectionFactory.getConnection();

        orders.forEach(order -> process(order, connection));
    }

    private void process(SslCertificateOrder order, GoGetSSLConnection connection) {
        try {
            if (order.getExternalOrderId() == null) {
                placeOrder(order, connection);

                if (order.getExternalOrderId() == null) {
                    throw new RuntimeException("Не создался заказ ssl в gogetssl");
                } else {
                    save(order);
                }
            } else {
                Map statusData = connection.getOrderStatus(order.getExternalOrderId());

                String status = (String) statusData.get("status");

                order.setExternalState(ExternalState.creator(status));

                switch (order.getExternalState()) {
                    case ACTIVE:
                        //делаем СА и CRT удобочитаемым (строками шириной 64 символа)
                        String caCode = (String) statusData.get("ca_code");
                        String crtCode = (String) statusData.get("crt_code");

                        try {
                            if (statusData.get("valid_from") != null) {
                                order.setValidFrom(LocalDate.parse((String) statusData.get("valid_from")));
                            }
                            if (statusData.get("valid_till") != null) {
                                order.setValidTo(LocalDate.parse((String) statusData.get("valid_till")));
                            }
                        } catch (Exception e) {
                            log.error("can't get valid dates from cert status e {} message {} order {} statusData {}",
                                    e.getClass(), e.getMessage(), order, statusData
                            );
                        }
                        //Разбиваем CA на два отдельных сертификата
//                        String caCode1 = caCode.substring(0, caCode.indexOf("-----BEGIN CERTIFICATE-----") - 1);
//                        String caCode2 = caCode.substring(caCode.indexOf("-----BEGIN CERTIFICATE-----") - 1);

                        order.setChain(
                                Arrays.asList(
                                        crtCode, caCode
                                )
                        );

                        finish(order, order.getOperator());

                        break;
                    case CANCELED:
                    case REJECTED:
                        decline(order, order.getOperator());
                        break;

                    default:
                        save(order);
                }
            }
        } catch (Exception e) {
            log.error("process ssl queue e {} message {} order {}", e.getClass(), e.getMessage(), order);
            e.printStackTrace();
        }



    }

    private void placeOrder(SslCertificateOrder order, GoGetSSLConnection connection) {
        MultiValueMap<String, String> orderData = prepareOrderData(order);

        //Если это заказ на продление сертификата, то отправляем соответствующий запрос
        Map response = null;
        switch (order.getOrderType()) {
            case NEW:
                response = connection.addSSLOrder(orderData);
                break;

            case RENEW:
                response = connection.addSSLRenewOrder(orderData);
                break;

            default:
                log.error("there is not processing order {}", order);
        }

        if (response == null) {
            throw new InternalApiException("Не удалось обработать заказ сертификата");
        } else if (response.get("order_id") != null) {
            order.setExternalOrderId((Integer) response.get("order_id"));
        }
    }

    private MultiValueMap<String, String> prepareOrderData(SslCertificateOrder order) {
        String phone = order.getPhone().replaceAll("[+\\s]", "");

        String fax = (order.getFax() == null ? order.getPhone() : order.getFax())
                .replaceAll("[+\\s]", "");

        String approverEmail = order.getApproverEmail().getName() + "@" + IDN.toASCII(order.getDomainName());

        MultiValueMap<String, String> orderData = new LinkedMultiValueMap<>();

        // Информация о сертификате
        orderData.add("product_id", order.getProduct().getExternalProductId());
        orderData.add("csr", order.getCsr());
        //todo period в месяцах
        orderData.add("period", "12");
        orderData.add("server_count", "-1");
        orderData.add("dcv_method", "email");
        orderData.add("approver_email", approverEmail);
        orderData.add("webserver_type", order.getServerType().getExternalWebServerId().toString());

        // Контактная информация
        orderData.add("admin_firstname", order.getFirstName());
        orderData.add("admin_lastname", order.getLastName());
        orderData.add("admin_phone", phone);
        orderData.add("admin_title", order.getTitle());
        orderData.add("admin_email", order.getEmail());
        orderData.add("admin_city", order.getCity());
        orderData.add("admin_country", order.getCountry().getValue());
        orderData.add("tech_firstname", order.getFirstName());
        orderData.add("tech_lastname", order.getLastName());
        orderData.add("tech_phone", phone);
        orderData.add("tech_title", order.getTitle());
        orderData.add("tech_email", order.getEmail());
        orderData.add("tech_city", order.getCity());
        orderData.add("tech_country", order.getCountry().getValue());

        // Для мультидоменных сертификатов
        /*if (order.getProduct().isMultidomain() && count(order["domains"]))
        {
            $domain_list = array();
            foreach (order["domains"] as $domain)
            $domain_list[] = $domain["raw_name"];

            order_data += array(
            orderData.add("dns_names", implode(",", $domain_list)
        );
        }*/

        // Для сертификатов, заказанных на имя организации
        if (order.getIsOrganization()) {
            orderData.add("admin_organization", order.getOrganizationName());
            orderData.add("admin_fax", fax);
            orderData.add("tech_fax", fax);
            orderData.add("tech_organization", order.getOrganizationName());
            orderData.add("org_name", order.getOrganizationName());
            orderData.add("org_division", order.getDivision());
            orderData.add("org_addressline1", order.getAddressLineFirst());
            orderData.add("org_postalcode", order.getPostalCode());
            orderData.add("org_phone", phone);
            orderData.add("org_fax", fax);
            orderData.add("org_region", order.getRegion());
            orderData.add("org_city", order.getCity());
            orderData.add("org_country", order.getCountry().getValue());
        }

        return orderData;
    }

    public void build(SslCertificateOrder order) {
        builder.build(order);
    }

    private void generatePrivateKeyAndCsr(SslCertificateOrder order) {
        try {
            String commonName = (order.getProduct().isWildcard() ? "*." : "") + IDN.toASCII(order.getDomainName());
            String organizationUnit = order.getIsOrganization() ? order.getDivision() : "none";
            String organizationName = order.getIsOrganization() ? order.getOrganizationName() : order.getFirstName() + " " + order.getLastName();
            String location = order.getCity();
            String state = order.getRegion();
            String country = order.getCountry().getValue();

            CSRGenerator csrGenerator = new CSRGenerator();
            order.setCsr(
                    csrGenerator.getCSR(
                            commonName, organizationUnit, organizationName, location, state, country
                    )
            );

            order.setKey(
                    csrGenerator.getPrivateKeyAsString()
            );

        } catch (Exception e) {
            log.error("in generatePrivateKeyAndCsr catch e {} with message {} and order {}", e.getClass(), e.getMessage(), order);
            e.printStackTrace();
        }
    }
}
