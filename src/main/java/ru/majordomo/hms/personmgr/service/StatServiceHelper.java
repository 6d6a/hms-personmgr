package ru.majordomo.hms.personmgr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.common.AccountStatType;
import ru.majordomo.hms.personmgr.common.BusinessOperationType;
import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.dto.stat.*;
import ru.majordomo.hms.personmgr.feign.FinFeignClient;
import ru.majordomo.hms.personmgr.feign.RcStaffFeignClient;
import ru.majordomo.hms.personmgr.feign.RcUserFeignClient;
import ru.majordomo.hms.personmgr.manager.AccountOwnerManager;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.manager.PlanManager;
import ru.majordomo.hms.personmgr.model.abonement.Abonement;
import ru.majordomo.hms.personmgr.model.abonement.AccountServiceAbonement;
import ru.majordomo.hms.personmgr.model.account.AccountStat;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.account.projection.PlanByServerProjection;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.service.PaymentService;
import ru.majordomo.hms.personmgr.repository.*;
import ru.majordomo.hms.rc.staff.resources.Resource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;
import static ru.majordomo.hms.personmgr.common.AccountStatType.*;
import static ru.majordomo.hms.personmgr.common.Constants.DOMAIN_NAME_KEY;

@Service
public class StatServiceHelper {

    private final Logger logger = LoggerFactory.getLogger(StatServiceHelper.class);
    private final MongoOperations mongoOperations;
    private final AbonementRepository abonementRepository;
    private final PlanManager planManager;
    private final PersonalAccountManager accountManager;
    private final PaymentServiceRepository paymentServiceRepository;
    private final AccountStatRepository accountStatRepository;
    private final RcUserFeignClient rcUserFeignClient;
    private final RcStaffFeignClient rcStaffFeignClient;
    private final JongoManager jongoManager;
    private final FinFeignClient finFeignClient;
    private final AccountOwnerManager ownerManager;
    private final AccountNotificationHelper notificationHelper;
    private String serviceEmailTemplateName;

    @Autowired
    public StatServiceHelper(
            MongoOperations mongoOperations,
            AbonementRepository abonementRepository,
            PlanManager planManager,
            PersonalAccountManager accountManager,
            PaymentServiceRepository paymentServiceRepository,
            AccountStatRepository accountStatRepository,
            RcUserFeignClient rcUserFeignClient,
            RcStaffFeignClient rcStaffFeignClient,
            JongoManager jongoManager,
            FinFeignClient finFeignClient,
            AccountOwnerManager ownerManager,
            AccountNotificationHelper notificationHelper,
            @Value("mail_manager.service_message_api_name") String serviceEmailTemplateName
    ) {
        this.mongoOperations = mongoOperations;
        this.abonementRepository = abonementRepository;
        this.planManager = planManager;
        this.accountManager = accountManager;
        this.paymentServiceRepository = paymentServiceRepository;
        this.accountStatRepository = accountStatRepository;
        this.rcUserFeignClient = rcUserFeignClient;
        this.rcStaffFeignClient = rcStaffFeignClient;
        this.jongoManager = jongoManager;
        this.finFeignClient = finFeignClient;
        this.ownerManager = ownerManager;
        this.notificationHelper = notificationHelper;
        this.serviceEmailTemplateName = serviceEmailTemplateName;
    }

    public List<PlanCounter> getAllPlanCounters() {
        return this.getPlanCountersFilterByAbonementOnlyPlan(true);
    }

    public List<PlanCounter> getPlanCountersWithoutAbonement() {
        return this.getPlanCountersFilterByAbonementOnlyPlan(false);
    }

    public List<PlanCounter> getPlanCountersFilterByAbonementOnlyPlan(Boolean withoutFilter) {
        MatchOperation match = match(Criteria.where("active").is(true));

        ProjectionOperation project = project("planId");

        GroupOperation group = group("planId").count().as("count")
                .first("planId").as("planId");

        SortOperation sort = sort(new Sort(Sort.Direction.DESC, "count"));

        Aggregation aggregation = newAggregation(
                match,
                project,
                group,
                sort
        );

        List<PlanCounter> result = new ArrayList<>();
        List<PlanCounter> all = mongoOperations.aggregate(
                aggregation, "personalAccount", PlanCounter.class).getMappedResults();

        List<Plan> planList = planManager.findAll();
        Map<String, Plan> planMap = planList.stream().collect(Collectors.toMap(Plan::getId, x->x));

        all.forEach(counter -> {
                    Plan plan = planMap.get(counter.getPlanId());
                    if (withoutFilter || !plan.isAbonementOnly()) {
                        counter.unSetId();
                        counter.setName(plan.getName());
                        counter.setActive(plan.isActive());
                        result.add(counter);
                    }
                }
        );

        return result;
    }

    //Выполняется дольше 17 секунд
    public List<AbonementCounter> getAbonementCounters() {

        UnwindOperation unwind = unwind("abonementIds");
        ProjectionOperation project = project("abonementIds", "name", "active");

        LookupOperation lookup = lookup(
                "accountAbonement",
                "abonementIds",
                "abonementId",
                "accountAbonements");

        ProjectionOperation countProject = project("name", "active")
                .and("accountAbonements").size().as("count")
                .and("abonementIds").as("abonementId")
                ;

        SortOperation sort = sort(new Sort(Sort.Direction.DESC, "count"));

        Aggregation aggregation = newAggregation(
                unwind,
                project,
                lookup,
                countProject,
                sort
        );

        AggregationResults<AbonementCounter> abonementCounters = mongoOperations.aggregate(
                aggregation,
                "plan",
                AbonementCounter.class
        );

        List<AbonementCounter> abonementCountersByPlanAndAbonement = abonementCounters.getMappedResults();

        List<Abonement> abonements = abonementRepository.findAll();
        Map<String, Abonement> abonementMap = abonements.stream().collect(Collectors.toMap(Abonement::getId, x->x));

        for (AbonementCounter counter: abonementCountersByPlanAndAbonement
                ) {

            counter.setPlanId(counter.getId());
            counter.unSetId();

            Abonement abonement = abonementMap.get(counter.getAbonementId());

            if (abonement != null) {
                counter.setInternal(abonement.isInternal());
                counter.setPeriod(abonement.getPeriod());
                counter.setName(abonement.getName());
            }
        }

        return abonementCountersByPlanAndAbonement;
    }

    public List<PlanCounter> getDailyPlanCounters() {
        //все аккаунты с дневными списаниями и абонементами
        List<PlanCounter> all = this.getPlanCountersWithoutAbonement();

        //Только абонементы
        List<AbonementCounter> abonements = this.getAbonementCounters();

        //Вычитаем абонементы из общего числа
        all.forEach(counter ->
                abonements.forEach(abonementCounter -> {
                    if (abonementCounter.getPlanId().equals(counter.getPlanId())) {
                        counter.setCount(counter.getCount() - abonementCounter.getCount());
                    }
                })
        );

        return all;
    }

    public List<AccountServiceCounter> getActiveAccountServiceCounters() {
        List<String> accountIds = accountManager.findAccountIdsByActiveAndNotDeleted(true);

        MatchOperation match = match(
                Criteria.where("enabled")
                        .is(true)
                        .and("quantity").gte(1)
                        .and("personalAccountId").in(accountIds)
        );

        LookupOperation lookup = lookup(
                "plan",
                "serviceId",
                "serviceId",
                "plan");

        ProjectionOperation project = project("serviceId")
                .and("_id").as("id")
                .and("plan").size().as("plan")
                .and("quantity").as("quantity")
                .and("personalAccountId").as("personalAccountId");

        MatchOperation planFilter = match(Criteria.where("plan").is(0));

        GroupOperation groupById = group("id")
                .first("serviceId").as("serviceId")
                .first("personalAccountId").as("personalAccountId")
                .sum("quantity").as("quantity")
                .count().as("count");

        GroupOperation groupByAccountAndService = group("personalAccountId", "serviceId")
                .first("serviceId").as("serviceId")
                .first("count").as("count")
                .sum("quantity").as("quantity");

        GroupOperation group = group("serviceId")
                .first("serviceId").as("resourceId")
                .sum("count").as("count")
                .sum("quantity").as("quantity");

        SortOperation sort = sort(Sort.Direction.DESC,"count");

        Aggregation aggregation = newAggregation(
                match,
                lookup,
                project,
                planFilter,
                groupById,
                groupByAccountAndService,
                group,
                sort
        );

        List<AccountServiceCounter> accountServiceCounters = mongoOperations.aggregate(
                aggregation, "accountService", AccountServiceCounter.class
        ).getMappedResults();

        accountServiceCounters.forEach(element -> element.setName(
                paymentServiceRepository.findById(element.getResourceId())
                        .orElse(new PaymentService()).getName()));

        return accountServiceCounters;
    }

    public List<DomainCounter> getDomainCountersByDateAndStatType(LocalDate date, AccountStatType type) {
        LocalDateTime startDateTime = LocalDateTime.of(date, LocalTime.MIN);
        LocalDateTime endDateTime = LocalDateTime.of(date, LocalTime.MAX);

        MatchOperation match = match(
                Criteria.where("created")
                        .gte(Date.from(startDateTime.toInstant(ZoneOffset.ofHours(3))))
                        .lte(Date.from(endDateTime.toInstant(ZoneOffset.ofHours(3))))
                        .and("type").is(type.name())
        );

        ProjectionOperation project = project()
                .and("data." + DOMAIN_NAME_KEY).as("name")
                .and("created").as("dateTime");

        Aggregation aggregation = newAggregation(
                match,
                project
        );

        List<DomainCounter> domains = mongoOperations.aggregate(
                aggregation, "accountStat", DomainCounter.class
        ).getMappedResults();

        List<DomainCounter> result = groupByTld(domains);

        return setNameByStatType(result, type);
    }

    private List<DomainCounter> groupByTld(List<DomainCounter> domainList) {
        Map<String, DomainCounter> tldMap = new HashMap<>();
        List<DomainCounter> result = new ArrayList<>();
        for(DomainCounter element: domainList) {
            try {
                String[] splitDomain = element.getName().split("\\.", 2);
                String tld = splitDomain[1];
                element.setResourceId(tld);
                if (!tldMap.containsKey(tld)) {
                    element.unSetId();
                    element.setResourceId(tld);
                    element.setDateTime(LocalDateTime.of(element.getDateTime().toLocalDate(), LocalTime.MIN));
                    element.setCount(1);
                    tldMap.put(tld, element);
                    result.add(element);
                } else {
                    tldMap.get(tld).countPlusOne();
                }
            } catch (Exception e) {
                logger.error("Catch exception in StatServiceHelper.groupByTld with DomainCounter :" + element);
                e.printStackTrace();
            }
        }
        return result;
    }

    private List<DomainCounter> setNameByStatType(List<DomainCounter> list, AccountStatType type) {
        for(DomainCounter counter: list) {
            String action = "";
            switch (type){
                case VIRTUAL_HOSTING_AUTO_RENEW_DOMAIN:
                    action = "Автопродление домена в зоне .";
                    break;
                case VIRTUAL_HOSTING_MANUAL_RENEW_DOMAIN:
                    action = "Ручное продление домена в зоне .";
                    break;
                case VIRTUAL_HOSTING_REGISTER_DOMAIN:
                    action = "Регистрация домена в зоне .";
                    break;
            }
            counter.setName(action + counter.getResourceId());
        }
        return list;
    }

    public Integer getAccountCountsWithFirstRealPaymentByDate(LocalDate date) {
        return accountStatRepository
                .countAccountStatByTypeAndCreatedIsBetween(
                        VIRTUAL_HOSTING_FIRST_REAL_PAYMENT,
                        LocalDateTime.of(date.minusDays(1), LocalTime.MAX),
                        LocalDateTime.of(date.plusDays(1), LocalTime.MIN));
    }

    public List<ResourceCounter> getRegisterWithPlanCounters(LocalDate date) {
        List<PersonalAccount> accounts = accountManager.findByCreatedDate(date);

        if (accounts == null || accounts.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, String> accountMap = accounts.stream().collect(
                Collectors.toMap(PersonalAccount::getId, PersonalAccount::getPlanId));

        List<AccountStat> accountStats = accountStatRepository.findByPersonalAccountIdInAndType(
                accounts.stream().map(PersonalAccount::getId).collect(Collectors.toList()),
                VIRTUAL_HOSTING_PLAN_CHANGE
        );

        Map<String, AccountStat> firstPlanChange = new HashMap<>();

        if (accountStats != null && !accountStats.isEmpty()) {

            for (AccountStat item : accountStats) {
                String personalAccountId = item.getPersonalAccountId();

                if (!firstPlanChange.containsKey(personalAccountId)) {
                    firstPlanChange.put(personalAccountId, item);
                } else {
                    if (firstPlanChange.get(personalAccountId).getCreated().isAfter(item.getCreated())) {
                        firstPlanChange.put(personalAccountId, item);
                    }
                }
            }

            for (Map.Entry<String, AccountStat> entry: firstPlanChange.entrySet()) {
                accountMap.put(entry.getKey(), entry.getValue().getData().get("oldPlanId"));
            }
        }

        Map<String, String> planIdAndName = planManager.findAll().stream().collect(Collectors.toMap(Plan::getId, Plan::getName));

        Map<String, ResourceCounter> planStats = new HashMap<>();

        for(String planId: accountMap.values()){
            if (!planStats.containsKey(planId)){
                ResourceCounter counter = new ResourceCounter();
                counter.setName(planIdAndName.get(planId));
                counter.setResourceId(planId);
                counter.setCount(1);
                counter.setDateTime(LocalDateTime.of(date, LocalTime.MIN));
                planStats.put(planId, counter);
            } else {
                planStats.get(planId).countPlusOne();
            }
        }
        return new ArrayList<>(planStats.values());
    }

    public List<Map> getBusinessOperationsStat(Set<BusinessOperationType> types, Set<State> states , LocalDate start, LocalDate end) {
        Set<String> stateStrings = states.stream().map(Enum::name).collect(Collectors.toSet());
        Set<String> typeStrings = types.stream().map(Enum::name).collect(Collectors.toSet());

        String createdDate = "createdDate";
        LocalDateTime startDateTime = LocalDateTime.of(start, LocalTime.MIN);
        LocalDateTime endDateTime = LocalDateTime.of(end, LocalTime.MAX);

        MatchOperation match = match(
                Criteria.where("createdDate")
                        .gte(Date.from(startDateTime.toInstant(ZoneOffset.ofHours(3))))
                        .lte(Date.from(endDateTime.toInstant(ZoneOffset.ofHours(3))))
                        .and("state").in(stateStrings)
                        .and("type").in(typeStrings)
        );

        ProjectionOperation project = project(createdDate, "personalAccountId", "state", "type")
                .andExpression("year(" + createdDate + ")").as("year")
                .andExpression("month(" + createdDate + ")").as("month")
                .andExpression("dayOfMonth(" + createdDate + ")").as("day");

        GroupOperation group0 = group("type", "state", "year", "month", "day", "personalAccountId")
                .count().as("uniqueAccountCount")
                .first("year").as("year")
                .first("month").as("month")
                .first("day").as("day")
                .first(createdDate).as(createdDate);

        GroupOperation group1 = group("type", "state", "year", "month", "day")
                .count().as("uniqueAccountCount")
                .sum("uniqueAccountCount").as("allCount")
                .first(createdDate).as(createdDate);

        Aggregation aggregation = newAggregation(
                match,
                project,
                group0,
                group1
        );

        return mongoOperations.aggregate(
                aggregation, "processingBusinessOperation", Map.class
        ).getMappedResults();
    }

    public List<AccountServiceCounter> getServiceAbonementCounters() {
        List<String> accountIds = accountManager.findAccountIdsByActiveAndNotDeleted(true);

        MatchOperation match = match(
                Criteria.where("expired").gte(LocalDateTime.now())
                        .and("personalAccountId").in(accountIds)
        );

        GroupOperation groupByAccountAndAbonement = group("abonementId", "personalAccountId")
                .first("abonementId").as("resourceId")
                .first("personalAccountId").as("personalAccountId")
                .count().as("quantity");

        GroupOperation groupByAbonement = group("resourceId")
                .first("resourceId").as("resourceId")
                .count().as("count")
                .sum("quantity").as("quantity");

        Aggregation aggregation = newAggregation(
                match,
                groupByAccountAndAbonement,
                groupByAbonement
        );

        List<AccountServiceCounter> accountServiceCounters = mongoOperations.aggregate(
                aggregation, AccountServiceAbonement.class, AccountServiceCounter.class
        ).getMappedResults();

        accountServiceCounters.forEach(element ->
                abonementRepository.findById(element.getResourceId())
                        .ifPresent(abonement -> element.setName(abonement.getName())
                )
        );

        return accountServiceCounters;
    }

    public List<PlanByServerCounter> getPlanByServerStat() {
        logger.info("start getPlanByServerStat");

        Map<String, String> accountIdAndServerId = rcUserFeignClient.getAccountIdAndField("unix-account", "serverId");

        logger.info("got accountIdAndServerId from rcUser size: " + accountIdAndServerId.size());

        Map<String, String> serverIdAndName = rcStaffFeignClient
                .getServersOnlyIdAndName().stream().collect(Collectors.toMap(Resource::getId, Resource::getName));

        logger.info("got serverIdAndName from rcStaff size: " + serverIdAndName.size());

        Map<String, String> planIdAndName = planManager.findAll().stream().collect(Collectors.toMap(Plan::getId, Plan::getName));

        logger.info("got planIdAndName");

        List<PlanByServerProjection> projections = accountManager.getAccountIdAndPlanId();

        logger.info("got projection from accountManager size: " + projections.size());

        Map<String, Map<String, PlanByServerProjection>> groupByPlan = new HashMap<>();

        projections.forEach(p -> {
            Map<String, PlanByServerProjection> ids = groupByPlan.getOrDefault(p.getPlanId(), new HashMap<>());
            ids.put(p.getPersonalAccountId(), p);
            groupByPlan.put(p.getPlanId(), ids);
        });

        logger.info("got groupByPlan size: " + groupByPlan.size());

        Map<String, List<String>> groupByServer = new HashMap<>();

        accountIdAndServerId.forEach((accountId, serverId) -> {
            List<String> ids = groupByServer.getOrDefault(serverId, new ArrayList<>());
            ids.add(accountId);
            groupByServer.put(serverId, ids);
        });

        logger.info("got groupByServer size: " + groupByServer.size());

        Map<String, Map<String, Map<Boolean, Integer>>> serverMap = new HashMap<>();

        groupByServer.forEach((serverId, accountIdsOnServer) -> {
            Map<String, Map<Boolean, Integer>> planWithCount = new HashMap<>();
            accountIdsOnServer.forEach(accountIdOnServer -> {
                for(Map.Entry<String, Map<String, PlanByServerProjection>> e: groupByPlan.entrySet()){
                    Map<String, PlanByServerProjection> accIdAndProject = e.getValue();
                    PlanByServerProjection p = accIdAndProject.remove(accountIdOnServer);
                    if (p != null) {
                        Map<Boolean, Integer> countMap = planWithCount.getOrDefault(p.getPlanId(), new HashMap<>());
                        Integer i = countMap.getOrDefault(p.isActive(), 0);
                        countMap.put(p.isActive(), i + 1);
                        planWithCount.put(p.getPlanId(), countMap);
                    }
                }
            });
            serverMap.put(serverId, planWithCount);
        });

        logger.info("got serverMap size: " + serverMap.size());

        List<PlanByServerCounter> result = new ArrayList<>();

        serverMap.forEach((serverId, planWithCount) -> {
            String serverName = serverIdAndName.get(serverId);
            planWithCount.forEach((planId, count) -> {
                PlanByServerCounter c = new PlanByServerCounter();
                c.setInactiveCount(count.getOrDefault(false, 0));
                c.setActiveCount(count.getOrDefault(true, 0));
                c.setPlanId(planId);
                c.setServerId(serverId);
                c.setServerName(serverName);
                c.setPlanName(planIdAndName.get(planId));
                result.add(c);
            });
        });

        logger.info("end getPlanByServerStat");

        return result;
    }

    public List<Options> getMetaOptions() {
        return jongoManager.getMetaOptions();
    }

    public Collection<MetaProjection> getMetaStat(LocalDate start, LocalDate end, Map<String, String> search) {
        Criteria criteria = Criteria
                .where("type").is(BusinessOperationType.ACCOUNT_CREATE.name())
                .and("params.meta").exists(true)
                .and("createdDate")
                .lte(Date.from(LocalDateTime.of(end, LocalTime.MAX).toInstant(ZoneOffset.ofHours(3))));

        for (Map.Entry<String, String> entry: search.entrySet()) {
            criteria = criteria.and("params.meta." + entry.getKey()).is(entry.getValue());
        }

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(criteria),
                Aggregation.project("createdDate", "personalAccountId")
                        .andExpression("year(createdDate)").as("year")
                        .andExpression("month(createdDate)").as("month")
                        .andExpression("dayOfMonth(createdDate)").as("day"),
                Aggregation.group("year", "month", "day")
                        .last("createdDate").as("created")
                        .count().as("count")
                        .addToSet("personalAccountId").as("accountIds")
        );

        List<MetaProjection> projections = mongoOperations
                .aggregate(aggregation, "processingBusinessOperation", MetaProjection.class)
                .getMappedResults();

        List<String> accountIds = projections
                .stream()
                .flatMap(p -> p.getAccountIds().stream())
                .collect(Collectors.toList());

        Map<LocalDate, MetaProjection> money = finFeignClient.generateMoneyMetaData(
                accountIds, start.format(DateTimeFormatter.ISO_LOCAL_DATE), end.format(DateTimeFormatter.ISO_LOCAL_DATE));

        BinaryOperator<MetaProjection> mergeFunction = (a, b) -> {
            a.setCount(a.getCount() + b.getCount());
            return a;
        };

        Map<LocalDate, MetaProjection> map = projections
                .stream().collect(Collectors.toMap(MetaProjection::getCreated, p-> p, mergeFunction));

        while (start.isBefore(end)) {
            if (map.get(start) == null) {
                MetaProjection metaProjection = new MetaProjection();
                metaProjection.setCreated(start);
                map.put(start, metaProjection);
            }
            start = start.plusDays(1);
        }

        money.forEach((e, m) -> {
            MetaProjection p = map.get(e);
            if (p == null) {
                m.setCreated(e);
                map.put(e, m);
            } else {
                p.setChargesAmount(p.getChargesAmount().add(m.getChargesAmount()));
                p.setPaymentsAmount(p.getPaymentsAmount().add(m.getPaymentsAmount()));
            }
        });

        return map.values();
    }

    public void sendLostClientsInfo(LocalDate date, List<String> emails) {
        String table = toTable(
                getLostClientInfoList(date)
        );

        String subject = "Статистика по отключенным клиентам за " + date.toString();
        String body = subject + ". Собрано " + LocalDate.now().toString() + "<br/><br/>" + table;

        Map<String, String> params = new HashMap<>();
        params.put("subject", subject);
        params.put("body", body);

        notificationHelper.sendInternalEmail(
                String.join(",", emails), serviceEmailTemplateName, null,10, params
        );
    }

    private List<LostClientInfo> getLostClientInfoList(LocalDate date) {
        LocalDateTime importToHmsDate = LocalDateTime.of(LocalDate.of(2017, 8, 1), LocalTime.MIN);

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
                .filter(
                        info -> BigDecimal.ZERO.compareTo(info.getOverallPaymentAmount()) > 0
                                || info.getAccount().getCreated().isBefore(importToHmsDate)
                )
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
                )
                .add(info.getAccount().getName())
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
}
