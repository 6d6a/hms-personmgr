package ru.majordomo.hms.personmgr.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.whois.WhoisClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.xbill.DNS.*;
import ru.majordomo.hms.personmgr.config.LostClientConfig;
import ru.majordomo.hms.personmgr.dto.Container;
import ru.majordomo.hms.personmgr.dto.stat.*;
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
import ru.majordomo.hms.rc.user.resources.Resource;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.IDN;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import static ru.majordomo.hms.personmgr.common.AccountStatType.VIRTUAL_HOSTING_ABONEMENT_DELETE;
import static ru.majordomo.hms.personmgr.common.Utils.toLong;
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
    private final AccountOwnerManager ownerManager;
    private final AccountNotificationHelper notificationHelper;
    private final LostClientConfig lostClientConfig;
    private final AccountHelper accountHelper;
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
            AccountOwnerManager ownerManager,
            AccountNotificationHelper notificationHelper,
            LostClientConfig lostClientConfig,
            AccountHelper accountHelper,
            PromotionRepository promotionRepository,
            AccountPromotionManager accountPromotionManager
    ) {
        this.abonementRepository = abonementRepository;
        this.planManager = planManager;
        this.accountManager = accountManager;
        this.accountStatRepository = accountStatRepository;
        this.rcUserFeignClient = rcUserFeignClient;
        this.finFeignClient = finFeignClient;
        this.ownerManager = ownerManager;
        this.notificationHelper = notificationHelper;
        this.lostClientConfig = lostClientConfig;
        this.accountHelper = accountHelper;
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

                        accountHelper.giveGift(info.getAccount(), promotion);

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

    public Object getLostDomainsStat() {
        LocalDate date = LocalDate.now().minusMonths(1);
//        WhoisTest tester = new WhoisTest();
        List<WhoIsServer> servers = new WhoIsServersSupplier().get();


        return accountManager.findByActiveAndDeactivatedBetween(false, LocalDateTime.of(date.minusMonths(1), LocalTime.MIN),
                LocalDateTime.of(date, LocalTime.MAX)).stream().map(rcUserFeignClient::getDomains)
                .flatMap(Collection::stream)
                .filter(d -> d.getParentDomainId() == null)
                .map(Resource::getName)
                .map(SiteInfo::new)
                .peek(info -> {
                    List<ARecord> a = getA(IDN.toASCII(info.getDomainName()));
                    a = a == null ? Collections.emptyList() : a;
                    info.setRegistered(!a.isEmpty());
                    log.info("domain {} a records: {}", info.getDomainName(), a.stream().map(ARecord::getAddress).map(InetAddress::getHostAddress).collect(Collectors.joining(", ")));

                    info.getARecords().addAll(a.stream().map(ARecord::getAddress).map(InetAddress::getHostAddress).collect(Collectors.toList()));
                })
                .peek(i -> {
                    if (i.getARecords().size() > 0) {
                        String firstA = i.getARecords().get(0);
                        Container<WhoIsServer> container = new Container<>();
                        try {
                            long a = toLong(InetAddress.getByName(firstA));
                            servers.stream().filter(s -> {
                                IpDiapason diapason = s.getIpDiapasons().stream().filter(ip -> ip.getStartLong() <= a && ip.getEndLong() >= a)
                                        .findFirst().orElse(null);
                                if (diapason != null) {
                                    i.getHostInfo().add(diapason.getOwner());
                                    return true;
                                } else {
                                    return false;
                                }
                            })
                                    .findFirst().ifPresent(s -> i.getHostInfo().add(s.getCountry()))
                            ;
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        }

                        if (container.getData() != null) {
                            try {
                                i.getHostInfo().addAll(Arrays.asList(new WhoisTest(container.getData().getWhoIsServer()).getWhois(firstA).split("\n")));
                            } catch (Exception e) {
                                log.error("can't get whois info by ip {} e {} message {}", i.getARecords().get(0), e.getClass(), e.getMessage());
                            }
                        }
                    }
                })
//                .peek(System.out::println)
                .collect(Collectors.toList());
    }

    public static class WhoisTest {
        private String server;
        public String getWhois(String domainName) {

            StringBuilder result = new StringBuilder();

            WhoisClient whois = new WhoisClient();
            try {

                //default is internic.net
                whois.connect(server == null ? WhoisClient.DEFAULT_HOST : server);
//                whois.connect("www.arin.net");
                String whoisData1 = whois.query("=" + domainName);
                result.append(whoisData1);
                whois.disconnect();

            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return result.toString();

        }

        public WhoisTest() {}

        public WhoisTest(String server) {
            this.server = server;
        }
    }


    public static void main(String[] args) throws IOException {
        List<WhoIsServer> servers = new WhoIsServersSupplier().get();

//        log.info("{}", servers);

//        RestTemplate rest = new RestTemplate();

//        StringJoiner j = new StringJoiner("\n");
//
//        servers.keySet().forEach(country -> {
//            try {
//                j.add("\n;country: " + country);
//                j.add(rest.getForObject("https://www.nirsoft.net/countryip/" + country + ".csv", String.class));
//            } catch (Exception e) {
//                j.add(";not found " + country + " " + e.getMessage());
//            }
//        });
//
//        String content = j.toString();
//        File file = new File("/home/val/git/personmgr/ip-diapasons.csv");
//        FileUtils.write(file, content, "UTF8");
    }

    public List<ARecord> getA(String name) {
        List<Boolean> results = new ArrayList<>();

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
//        log.info("On {} correct TXT record {} found {}", nsServer, (found ? "" : "not"), data.toString());
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
