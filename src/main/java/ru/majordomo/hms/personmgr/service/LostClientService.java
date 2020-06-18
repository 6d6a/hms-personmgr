package ru.majordomo.hms.personmgr.service;

import com.google.common.net.InternetDomainName;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xbill.DNS.*;
import ru.majordomo.hms.personmgr.common.AvailabilityInfo;
import ru.majordomo.hms.personmgr.config.LostClientConfig;
import ru.majordomo.hms.personmgr.dto.stat.*;
import ru.majordomo.hms.personmgr.feign.DomainRegistrarFeignClient;
import ru.majordomo.hms.personmgr.feign.FinFeignClient;
import ru.majordomo.hms.personmgr.feign.RcUserFeignClient;
import ru.majordomo.hms.personmgr.manager.AccountOwnerManager;
import ru.majordomo.hms.personmgr.manager.AccountPromotionManager;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.manager.PlanManager;
import ru.majordomo.hms.personmgr.model.promotion.Promotion;
import ru.majordomo.hms.personmgr.repository.AbonementRepository;
import ru.majordomo.hms.personmgr.repository.AccountStatRepository;
import ru.majordomo.hms.personmgr.repository.PromotionRepository;
import ru.majordomo.hms.personmgr.service.Rpc.RegRpcClient;
import ru.majordomo.hms.rc.user.resources.Resource;

import java.math.BigDecimal;
import java.net.IDN;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import static ru.majordomo.hms.personmgr.common.AccountStatType.VIRTUAL_HOSTING_ABONEMENT_DELETE;
import static ru.majordomo.hms.personmgr.model.account.AccountOwner.Type.INDIVIDUAL;

@Slf4j
@Service
public class LostClientService {
    private final AbonementRepository abonementRepository;
    private final PlanManager planManager;
    private final PersonalAccountManager accountManager;
    private final AccountStatRepository accountStatRepository;
    private final RcUserFeignClient rcUserFeignClient;
    private final FinFeignClient finFeignClient;
    private final DomainRegistrarFeignClient registrarFeignClient;
    private final RegRpcClient regRpc;
    private final AccountOwnerManager ownerManager;
    private final AccountNotificationHelper notificationHelper;
    private final LostClientConfig lostClientConfig;
    private final GiftHelper giftHelper;
    private final PromotionRepository promotionRepository;
    private final AccountPromotionManager accountPromotionManager;

    @Autowired
    public LostClientService(
            AbonementRepository abonementRepository,
            PlanManager planManager,
            PersonalAccountManager accountManager,
            AccountStatRepository accountStatRepository,
            RcUserFeignClient rcUserFeignClient,
            FinFeignClient finFeignClient,
            DomainRegistrarFeignClient registrarFeignClient,
            RegRpcClient regRpc,
            AccountOwnerManager ownerManager,
            AccountNotificationHelper notificationHelper,
            LostClientConfig lostClientConfig,
            GiftHelper giftHelper,
            PromotionRepository promotionRepository,
            AccountPromotionManager accountPromotionManager
    ) {
        this.abonementRepository = abonementRepository;
        this.planManager = planManager;
        this.accountManager = accountManager;
        this.accountStatRepository = accountStatRepository;
        this.rcUserFeignClient = rcUserFeignClient;
        this.finFeignClient = finFeignClient;
        this.registrarFeignClient = registrarFeignClient;
        this.regRpc = regRpc;
        this.ownerManager = ownerManager;
        this.notificationHelper = notificationHelper;
        this.lostClientConfig = lostClientConfig;
        this.giftHelper = giftHelper;
        this.promotionRepository = promotionRepository;
        this.accountPromotionManager = accountPromotionManager;
    }

    public void sendLostClientsInfo() {
        sendLostClientsInfo(
                lostClientConfig
        );
    }

    private void sendLostClientsInfo(LostClientConfig config) {
        LocalDate disableDate = LocalDate.now().minusDays(config.getDisabledDaysAgo());
        List<LostClientInfo> lostClientInfoList = getLostClientInfoList(
                disableDate,
                config.getMinOverallPaymentAmount()
        );

        if (config.isNeedSendStatistics()) {
            String table = toTable(
                    lostClientInfoList
            );

            String subject = "Статистика по отключенным клиентам за " + disableDate.toString();
            String body = subject + ". Собрано " + LocalDate.now().toString() + "<br/><br/>" + table;

            notificationHelper.emailBuilder()
                    .apiName(config.getStatTemplateApiName())
                    .emails(config.getStatEmails())
                    .priority(10)
                    .param("subject", subject)
                    .param("body", body)
                    .send();
        }

        if (config.isNeedSendToClient()) {
            Promotion promotion = config.getGiftPromotionName() != null
                    ? promotionRepository.findByName(config.getGiftPromotionName())
                    : null;

            lostClientInfoList.forEach(info -> {
                try {
                    if (promotion != null && canAddPromotion(config, info, promotion)) {
                        log.info("give gift to {} created {} payment amount {}",
                                info.getAccount().getName(), info.getAccount().getCreated(), info.getOverallPaymentAmount());

                        giftHelper.giveGift(info.getAccount(), promotion);

                        notificationHelper
                                .emailBuilder()
                                .from(config.getFeedbackFrom())
                                .account(info.getAccount())
                                .apiName(config.getGiftFeedbackTemplateApiName())
                                .param("acc_id", info.getAccount().getAccountId())
                                .param("name", info.getOwner().getType().equals(INDIVIDUAL) ? info.getOwner().getName() : "")
                                .send();
                    } else {
                        log.info("only mail to {} created {} payment amount {}",
                                info.getAccount().getName(), info.getAccount().getCreated(), info.getOverallPaymentAmount());
                        notificationHelper
                                .emailBuilder()
                                .from(config.getFeedbackFrom())
                                .account(info.getAccount())
                                .apiName(config.getFeedbackTemplateApiName())
                                .param("acc_id", info.getAccount().getAccountId())
                                .send();
                    }
                } catch (Exception e) {
                    log.error("can't send feedback mail to lost client {} e {} message {}",
                            info.getAccount().getName(), e.getClass(), e.getMessage());
                }
            });
        }
    }

    public void sendLostDomainsInfo() {
        LocalDate disableDate = LocalDate.now().minusMonths(2);
        List<SiteInfo> lostClientDomains = getLostDomainsStat(disableDate);

        if (lostClientConfig.isNeedSendStatistics()) {
            String table = siteInfoToTable(
                    lostClientDomains
            );

            String subject = "Статистика по отключенным клиентским доменам с " + disableDate.minusMonths(1).toString() + " по " + disableDate.toString();
            String body = subject + ". Собрано " + LocalDate.now().toString() + "<br/><br/>" + table;

            notificationHelper.emailBuilder()
                    .apiName(lostClientConfig.getStatTemplateApiName())
                    .emails(lostClientConfig.getStatEmails())
                    .priority(10)
                    .param("subject", subject)
                    .param("body", body)
                    .send();
        }
    }

    private boolean canAddPromotion(LostClientConfig config, LostClientInfo info, Promotion promotion) {
        return info.getOverallPaymentAmount()
                    .compareTo(config.getPaymentAmountForAbonementDiscount()) >= 0
                && info.getAccount().getCreated().plus(config.getMinLivePeriodForDiscount())
                    .isBefore(LocalDateTime.now())
                && !accountPromotionManager.existsByPersonalAccountIdAndPromotionId(
                        info.getAccount().getId(), promotion.getId()
        );
    }

    private List<LostClientInfo> getLostClientInfoList(LocalDate date, BigDecimal minOverallPaymentAmount) {
        return accountManager.findByActiveAndDeactivatedBetween(false, LocalDateTime.of(date, LocalTime.MIN),
                LocalDateTime.of(date, LocalTime.MAX))
                .stream()
                .map(accountId -> new LostClientInfo(
                                accountManager.findOne(accountId)
                        )
                )
                .peek(info -> {
                    info.setOwner(
                            ownerManager.findOneByPersonalAccountId(
                                    info.getAccount().getId()
                            )
                    );
                    info.setOverallPaymentAmount(
                            finFeignClient.getOverallPaymentAmount(
                                    info.getAccount().getId()
                            )
                    );
                    info.setDomains(
                            rcUserFeignClient.getDomains(
                                    info.getAccount().getId()
                            )
                    );
                    info.setPlan(
                            planManager.findOne(
                                    info.getAccount().getPlanId()
                            )
                    );

                    accountStatRepository
                            .findByPersonalAccountIdAndType(info.getAccount().getId(), VIRTUAL_HOSTING_ABONEMENT_DELETE)
                            .stream()
                            .reduce((first, second) -> second)
                            .ifPresent(
                                    stat -> abonementRepository
                                            .findById(stat.getData().get("abonementId"))
                                            .ifPresent(info::setAbonement)
                            );
                })
                .filter(info -> info.getOverallPaymentAmount().compareTo(minOverallPaymentAmount) >= 0)
                .collect(Collectors.toList());

    }

    private String toTable(List<LostClientInfo> infoList) {
        String tdOpen = "<td style=\"border-bottom: 1px solid #a9a9a9; border-left: 1px solid #a9a9a9; border-collapse: collapse;\">";

        String headRows = new StringJoiner("</td>" + tdOpen, "<tr>" + tdOpen, "</td></tr>")
                .add("Аккаунт")
                .add("Создан")
                .add("Тариф")
                .add("Абонемент")
                .add("Заплатил")
                .add("Выключен")
                .add("Доменов")
                .add("Имя владельца")
                .add("email-ы")
                .toString();

        String bodyRows = infoList.stream().map(info -> new StringJoiner(
                        "</td>" + tdOpen, "<tr>" + tdOpen, "</td></tr>"
                ).add(info.getAccount().getName())
                .add(info.getAccount().getCreated() != null ? info.getAccount().getCreated().toLocalDate().toString() : "нет данных")
                .add(info.getPlan().getName())
                .add(info.getAbonement() == null ? "нет" : info.getAbonement().isInternal() ? "тестовый" : info.getAbonement().getName())
                .add(info.getOverallPaymentAmount() != null ? info.getOverallPaymentAmount().toString() : "0")
                .add(info.getAccount().getDeactivated() != null ? info.getAccount().getDeactivated().toLocalDate().toString() : "нет данных")
                .add(info.getDomains() != null ? String.valueOf(info.getDomains().size()) : "0")
                .add(info.getOwner().getName())
                .add(String.join(", ", info.getOwner().getContactInfo().getEmailAddresses()))
                .toString()
        ).collect(Collectors.joining());

        return "<table><thead>" + headRows + "</thead><tbody>" + bodyRows + "</tbody></table>";
    }

    private String siteInfoToTable(List<SiteInfo> infoList) {
        String tdOpen = "<td style=\"border-bottom: 1px solid #a9a9a9; border-left: 1px solid #a9a9a9; border-collapse: collapse;\">";

        String headRows = new StringJoiner("</td>" + tdOpen, "<tr>" + tdOpen, "</td></tr>")
                .add("№")
                .add("Домен")
                .add("Делегирован")
                .add("Зарегистрирован")
                .add("IP-адреса")
                .add("Владелец IP")
                .toString();

        StringBuilder bodyRows = new StringBuilder();

        int index = 1;
        for (SiteInfo info: infoList) {
            bodyRows.append(
                    new StringJoiner("</td>" + tdOpen, "<tr>" + tdOpen, "</td></tr>")
                            .add(String.valueOf(index) + ".")
                            .add(info.getDomainName())
                            .add(info.isDelegated() ? "да" : "нет")
                            .add(info.isRegistered() ? "да" : "нет")
                            .add(String.join(", ", info.getARecords()))
                            .add(String.join(", ", info.getHostInfo()))
                            .toString()
            );
            index++;
        }

        return "<table><thead>" + headRows + "</thead><tbody>" + bodyRows + "</tbody></table>";
    }

    /**
     * Сбор статистики по отключенным аккаунтам
     * По всем отключенным аккаунтам забираем домены из rc-user, исключая поддомены
     * проверяем, что они были зарегистрированы в нашей системе через reg-rpc,
     * по оставшимся строим объект SiteInfo:
     * - Доменное имя
     * - Зарегистрирован
     * - Делегирован
     * - А записи
     * - Имена хостов, на которые в данный момент делегирован домен
     *
     * @param date Сбор статистики выполняется за прошедший до этой даты месяц
     * @return Собранная статистика доменов по отключенным за указанный период аккаунтам
     * @see SiteInfo
     */
    private List<SiteInfo> getLostDomainsStat(LocalDate date) {
        List<String> domains = accountManager.findByActiveAndDeactivatedBetween(
                false,
                LocalDateTime.of(date.minusMonths(1), LocalTime.MIN),
                LocalDateTime.of(date, LocalTime.MAX)
        )
                .stream()
                .map(rcUserFeignClient::getDomains)
                .flatMap(Collection::stream)
                .filter(d -> {  //Исключаем домены 3 уровня и выше
                    //Добавленные прямо как поддомен
                    if (d.getParentDomainId() != null) return false;

                    //Либо добавленные в качестве отдельного домена
                    String topDomainName = d.getName();
                    try {
                        InternetDomainName internetDomainName = InternetDomainName.from(d.getName());
                        topDomainName = IDN.toUnicode(internetDomainName.topPrivateDomain().toString());
                    } catch (Exception ignored) {
                    }
                    return d.getName().equals(topDomainName);
                })
                .map(Resource::getName)
                .collect(Collectors.toList());

        return regRpc.checkDomainsInRegRpc(domains)
                .stream()
                .map(IDN::toUnicode)
                .map(SiteInfo::new)
                .peek(info -> {
                    List<ARecord> a = getA(IDN.toASCII(info.getDomainName()));
                    a = (a == null ? Collections.emptyList() : a);

                    log.info("domain {} a records: {}", info.getDomainName(), a.stream()
                            .map(ARecord::getAddress).map(InetAddress::getHostAddress)
                            .collect(Collectors.joining(", ")));

                    info.getARecords().addAll(
                            a.stream()
                                    .map(ARecord::getAddress)
                                    .map(InetAddress::getHostAddress)
                                    .collect(Collectors.toList())
                    );
                })
                .peek(i -> {
                    if (i.getARecords().size() > 0) {
                        i.setDelegated(true);
                        i.setRegistered(true);

                        String firstA = i.getARecords().get(0);
                        InetAddress address = null;
                        try {
                            address = InetAddress.getByName(firstA);
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        }

                        if (address != null) {
                            String asn = new RipeClient().getAsnByAddress(address.getHostAddress());

                            if (asn != null) {
                                String holder = new RipeClient().getHolderByAsn(asn);
                                if (holder != null) {
                                    i.getHostInfo().add(holder);
                                }
                            }
                        }
                    } else {
                        try {
                            AvailabilityInfo availabilityInfo = registrarFeignClient.getAvailabilityInfo(i.getDomainName());
                            log.info("domain {} availabilityInfo: {}", i.getDomainName(), availabilityInfo);
                            if (availabilityInfo != null) {
                                i.setRegistered(!availabilityInfo.getFree());
                            }
                        } catch (Exception ignore) {
                        }
                    }
                })
                .collect(Collectors.toList());
    }

    private List<ARecord> getA(String name) {
        SimpleResolver resolver;
        try {
            resolver = new SimpleResolver("8.8.8.8");
        } catch (UnknownHostException e) {
            e.printStackTrace();
            log.error("Catch UnknownHostException message: %s, name: %s",
                    e.getMessage(), name);
            return null;
        }

        Record[] records = get(resolver, name, Type.A);

        return Arrays.asList(Arrays.asList(records).toArray(new ARecord[0]));
    }

    private Record[] get(SimpleResolver resolver, String name, int type) {
        Lookup lookup;
        try {
            lookup = new Lookup(name, type);
        } catch (TextParseException e) {
            log.error("Catch {}, message: {}, name: {}, type: {}", e.getClass().getName(), e.getMessage(), name, type);
            return new Record[0];
        }

        lookup.setResolver(resolver);
        lookup.setCache(null);

        Record[] records = lookup.run();
        return records == null ? new Record[0] : records;
    }
}
