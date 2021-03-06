package ru.majordomo.hms.personmgr.service.order.ssl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

import static java.time.temporal.ChronoUnit.DAYS;
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

    public void perValidate(SslCertificateOrder order) {
        if (isNotCompletedOrderByDomainNameExist(order.getPersonalAccountId(), order.getDomainName())) {
            throw new ParameterValidationException("???????? ?????????????????????????? ?????????? ???? ?????????????? ????????????");
        }
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
                "?????????????? SSL-???????????????????? " + order.getProduct().getName() + " ?????? ???????????? " + order.getDomainName(),
                order.getOperator()
        );
    }

    private ExternalState getOnlyExternalStateFromGoGetSsl(SslCertificateOrder order) {
        Map orderStatus = connectionFactory.getConnection().getOrderStatus(order.getExternalOrderId());

        order.setLastResponse(
                jsonStringify(orderStatus)
        );

        return ExternalState.creator(
                orderStatus.get("status").toString()
        );
    }

    @Override
    protected void onDecline(SslCertificateOrder order) {
        if (order.getExternalState().equals(ExternalState.ACTIVE)
                || (order.getChain() != null && !order.getChain().isEmpty())
        ) {
            throw new ParameterValidationException("???????????????????? ??????????????, ?????????? ???? ?????????? ???????? ??????????????");
        }

        ExternalState realExternalState = getOnlyExternalStateFromGoGetSsl(order);

        if (realExternalState.equals(ExternalState.ACTIVE)) {
            throw new ParameterValidationException("???????????????????? ??????????????, ?????????? ???? ?????????? ???????? ??????????????");
        } else if (
                Arrays.asList(ExternalState.CANCELED, REJECTED).contains(realExternalState)
        ) {
            if (order.getDocumentNumber() != null) {
                unblockMoney(order, "?????????????????? ???????????? ?? gogetssl: " + realExternalState);
            }
        } else {
            order.setLastResponse(
                    jsonStringify(
                            connectionFactory.getConnection().cancelSSLOrder(
                                    order.getExternalOrderId(), "Domain owner refused order"
                    )
                )
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
                    "???? ?????????????? ?????????????????? ???????????? ?? ???????????????????????? ?????? " + order.getDomainName(),
                    order.getOperator()
            );
        }
    }

    @Override
    public Page<SslCertificateOrder> findByPersonalAccountId(String accountId, Pageable pageable) {
        Page<SslCertificateOrder> orders = super.findByPersonalAccountId(accountId, pageable);
        orders.getContent().forEach(this::build);
        return orders;
    }

    @Override
    public SslCertificateOrder findOneByIdAndPersonalAccountId(String id, String personalAccountId) {
        SslCertificateOrder order = super.findOneByIdAndPersonalAccountId(id, personalAccountId);
        this.build(order);
        return order;
    }

    @Override
    public Page<SslCertificateOrder> findAll(Predicate predicate, Pageable pageable) {
        Page<SslCertificateOrder> orders = super.findAll(predicate, pageable);
        orders.getContent().forEach(this::build);
        return orders;
    }

    @Override
    public List<SslCertificateOrder> findAll(Predicate predicate) {
        List<SslCertificateOrder> orders = super.findAll(predicate);
        orders.forEach(this::build);
        return orders;
    }

    @Override
    public Page<SslCertificateOrder> findAll(Pageable pageable) {
        Page<SslCertificateOrder> page = super.findAll(pageable);
        page.getContent().forEach(this::build);
        return page;
    }

    @Override
    public SslCertificateOrder findOne(String id) {
        SslCertificateOrder order = super.findOne(id);
        build(order);
        return order;
    }

    private Boolean isNotCompletedOrderByDomainNameExist(String id, String domainName) {
        List<SslCertificateOrder> orders = repository.findByPersonalAccountIdAndStateIn(
                id,
                Arrays.asList(OrderState.NEW, OrderState.IN_PROGRESS)
        );
        SslCertificateOrder order = orders.stream()
                .filter(item -> item.getDomainName().equals(domainName))
                .findAny()
                .orElse(null);
        return order != null;
    }

    public List<SslCertificateOrder> getPendingOrders() {
        QSslCertificateOrder qOrder = QSslCertificateOrder.sslCertificateOrder;

        return findAll(
                new BooleanBuilder()
                        .and(qOrder.state.notIn(OrderState.DECLINED, OrderState.FINISHED))
                        .and(qOrder.externalState.notIn(ExternalState.CANCELED, ExternalState.ACTIVE))
        );
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
                    throw new RuntimeException("???? ???????????????? ?????????? ssl ?? gogetssl");
                } else {
                    save(order);
                }
            } else {
                Map statusData = connection.getOrderStatus(order.getExternalOrderId());

                order.setLastResponse(
                        jsonStringify(statusData)
                );

                String status = (String) statusData.get("status");

                order.setExternalState(ExternalState.creator(status));

                switch (order.getExternalState()) {
                    case ACTIVE:
                        //todo ???????????? ???? ?? CRT ?????????????????????????? (???????????????? ?????????????? 64 ??????????????),
                        // ?????????????????? CA ???? ?????? ?????????????????? ??????????????????????
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


                        order.setChain(
                                Arrays.asList(
                                        crtCode, caCode
                                )
                        );

                        finish(order, order.getOperator());

                        break;
                    case PROCESSING:
                        LocalDate dateForCheck = order.getLastNotifyDate() != null
                                ? order.getLastNotifyDate()
                                : order.getCreated().toLocalDate();

                        long daysBetween = DAYS.between(dateForCheck, LocalDate.now());
                        if (daysBetween != 0 && daysBetween % 5 == 0) {

                            notificationHelper.emailBuilder()
                                    .account(accountManager.findOne(order.getPersonalAccountId()))
                                    .apiName("HmsMajordomoSslOrderConfirmYourEmail")
                                    .param("approver_email", order.getApproverEmail().getName() + "@" + order.getDomainName())
                                    .param("domain", order.getDomainName())
                                    .send();

                            order.setLastNotifyDate(LocalDate.now());
                        }
                        save(order);

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
            throw new InternalApiException("???? ?????????????? ???????????????????? ?????????? ??????????????????????");
        } else if (response.get("order_id") != null) {
            order.setExternalOrderId((Integer) response.get("order_id"));
            order.setLastResponse(
                    jsonStringify(response)
            );
        }
    }

    private MultiValueMap<String, String> prepareOrderData(SslCertificateOrder order) {
        String phone = order.getPhone().replaceAll("[+\\s]", "");

        String fax = (order.getFax() == null ? order.getPhone() : order.getFax())
                .replaceAll("[+\\s]", "");

        String approverEmail = order.getApproverEmail().getName() + "@" + IDN.toASCII(order.getDomainName());

        MultiValueMap<String, String> orderData = new LinkedMultiValueMap<>();

        // ???????????????????? ?? ??????????????????????
        orderData.add("product_id", order.getProduct().getExternalProductId());
        orderData.add("csr", order.getCsr());
        //todo period ?? ??????????????
        orderData.add("period", "12");
        orderData.add("server_count", "-1");
        orderData.add("dcv_method", "email");
        orderData.add("approver_email", approverEmail);
        orderData.add("webserver_type", order.getServerType().getExternalWebServerId().toString());

        // ???????????????????? ????????????????????
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

        // ?????? ???????????????????????????? ????????????????????????
        /*if (order.getProduct().isMultidomain() && count(order["domains"]))
        {
            $domain_list = array();
            foreach (order["domains"] as $domain)
            $domain_list[] = $domain["raw_name"];

            order_data += array(
            orderData.add("dns_names", implode(",", $domain_list)
        );
        }*/

        // ?????? ????????????????????????, ???????????????????? ???? ?????? ??????????????????????
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

    private String jsonStringify(Object o) {
        String result = null;
        try {
            result = new ObjectMapper().writeValueAsString(o);
        } catch (JsonProcessingException e) {
            log.info("catch e {} in jsonStringify() message {} object {}", e.getClass(), e.getMessage(), o);
        }
        return result;
    }

    private void unblockMoney(SslCertificateOrder order, String reason) {
        try {
            accountHelper.unblock(order.getPersonalAccountId(), order.getDocumentNumber());
            history.save(
                    order.getPersonalAccountId(),
                    "???????????????? ?? ?????????????? " + order.getDocumentNumber()
                            + " ???? ???????????????????? ?????? " + order.getDomainName()
                            + " ??????????????, ??????????????: " + reason,
                    order.getOperator()
            );
        } catch (Exception e) {
            log.error("on unblock money for order {} e {} message {}", order, e.getClass(), e.getMessage());
        }
    }
}
