package ru.majordomo.hms.personmgr.importing;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;

import static ru.majordomo.hms.personmgr.common.BusinessActionType.*;
import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.exception.BaseException;
import ru.majordomo.hms.personmgr.exception.InternalApiException;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.feign.FinFeignClient;
import ru.majordomo.hms.personmgr.feign.RcStaffFeignClient;
import ru.majordomo.hms.personmgr.dto.importing.BillingDBAccountStatus;
import ru.majordomo.hms.personmgr.feign.RcUserFeignClient;
import ru.majordomo.hms.personmgr.manager.PlanManager;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessOperation;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.plan.VirtualHostingPlanProperties;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessOperationRepository;
import ru.majordomo.hms.personmgr.service.BusinessHelper;
import ru.majordomo.hms.rc.staff.resources.Resource;
import ru.majordomo.hms.rc.staff.resources.Server;
import ru.majordomo.hms.rc.staff.resources.template.DatabaseServer;

import javax.annotation.Nullable;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DBImportService {
    private final static Logger logger = LoggerFactory.getLogger(DBImportService.class);

    private final AccountHistoryDBImportService accountHistoryDBImportService;
    private final AccountNotificationDBImportService accountNotificationDBImportService;
    private final PersonalAccountDBImportService personalAccountDBImportService;
    private final AccountAbonementDBImportService accountAbonementDBImportService;
    private final AccountServicesDBImportService accountServicesDBImportService;
    private final AccountCommentDBImportService accountCommentDBImportService;
    private final AccountOwnerDBImportService accountOwnerDBImportService;
    @Qualifier("namedParameterJdbcTemplate")
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final BusinessHelper businessHelper;
    private final UnixAccountDBImportService unixAccountDBImportService;
    private final RcStaffFeignClient rcStaffClient;
    private final FinFeignClient finFeignClient;
    private final RcUserFeignClient rcUserClient;
    private final DatabaseUserDBImportService databaseUserDBImportService;
    private final DatabaseDBImportService databaseDBImportService;
    private final FTPUserDBImportService ftpUserDBImportService;
    private final ProcessingBusinessOperationRepository operationRepository;
    private final WebsiteDBImportService websiteDBImportService;
    private final PartnerPromocodeDBImportService partnerPromocodeDBImportService;
    private final PlanManager planManager;
    private final WebAccessAccountDBImportService webAccessAccountDBImportService;

    public enum ImportStage {
        CREATED,
        DIRECT_PM,
        DIRECT_FIN,
        DIRECT_SI,
        DIRECT_PARTNER,
        BUSINESS_REMOVE_RESOURCES,
        BUSINESS_REMOVE_RESOURCES_MESSAGES_SENT,
        BUSINESS_FIRST,
        BUSINESS_FIRST_MESSAGES_SENT,
        BUSINESS_SECOND,
        BUSINESS_SECOND_MESSAGES_SENT,
        FINISH
    }

    @Nullable
    public BillingDBAccountStatus getAccountStatus(String accountId, boolean isAdmin) {
        String query = "SELECT id, on_hms, client_type, acc_create_date, plan_id, status, server_id FROM account WHERE id = :accountId";
        SqlParameterSource parameter = new MapSqlParameterSource("accountId", accountId);
        try {
            return jdbcTemplate.queryForObject(query, parameter, (rs, rowNum) -> {
                boolean onHms = rs.getBoolean("on_hms");
                int oldServerId = rs.getInt("server_id");
                String serverId = oldServerId != 5 && oldServerId != 0 ? "web_server_" + oldServerId : "";
                BillingDBAccountStatus result = new BillingDBAccountStatus();
                result.setAccountId(accountId);
                result.setOnHms(onHms);
                result.setHmsServerId(serverId);
                result.setAllowImport((oldServerId != 5 && oldServerId != 124) || isAdmin);
                result.setPlanOldId(rs.getInt("plan_id"));
                result.setStatus("1".equals(rs.getString("status")));
                result.setCreateDate(rs.getDate("acc_create_date").toLocalDate());
                result.setHmsPlan(planManager.findByOldId(Integer.toString(result.getPlanOldId())));
                try {
//                    result.setHmsServer(rcStaffClient.getServerById(serverId)); // r??-staff ?????????????? ?????? ?????????????? ?????????????????????????? ??????????????
                } catch (FeignException | BaseException ignore) {}
                return result;
            });
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    public void setOnHms(String accountId, boolean onHms, boolean isAdmin) throws ParameterValidationException, InternalApiException {
        MapSqlParameterSource sqlParams = new MapSqlParameterSource("accountId", accountId);

        try {
            Boolean onHmsBd = jdbcTemplate.queryForObject("SELECT on_hms FROM account WHERE id = :accountId", sqlParams, Boolean.class);
            if (Boolean.TRUE.equals(onHmsBd)) {
                throw new InternalApiException("?????????????????? ?????????? on_hms ??????????????????");
            }
            sqlParams.addValue("onHms", onHms);
            String query = "UPDATE account SET on_hms = :onHms WHERE id = :accountId";
            jdbcTemplate.update(query, sqlParams);

        } catch (EmptyResultDataAccessException ex) {
            throw new ParameterValidationException("???? ???????????? ??????????????: " + accountId);
        } catch (RuntimeException ex) {
            throw new InternalApiException("???????????? ?????? ?????????????????? ???????????????????? ?????????? on_hms", ex);
        }
    }

    public String getMysqlServiceId(String serverId) {
        Server server = rcStaffClient.getServerById(serverId);
        if (server == null) {
            return "";
        }
        return server.getServices().stream().filter(service -> service.getTemplate() instanceof DatabaseServer
                && ((DatabaseServer) service.getTemplate()).getType() == DatabaseServer.Type.MYSQL).findFirst()
                .map(Resource::getId).orElse("");
    }

    public void startStageFirstIfNeed(ProcessingBusinessOperation operation) {
        if (operation == null) {
            return;
        }
        String serverId = (String) operation.getParam("serverId");
        String mysqlServiceId = (String) operation.getParam("mysqlServiceId");
        startStageFirstIfNeed(operation.getPersonalAccountId(), operation.getId(), serverId, mysqlServiceId);
    }

    public void startStageFirstIfNeed(String accountId, String operationId, String serverId, String mysqlServiceId) {
        try {
            if (StringUtils.isEmpty(accountId) || StringUtils.isEmpty(operationId) || StringUtils.isEmpty(serverId)) {
                return;
            }
            if (businessHelper.existsActiveActions(operationId)) {
                logger.debug("Skip startStageFirstIfNeed because exists active actions for operation: {}", operationId);
                return;
            }
            if (!businessHelper.setStage(operationId, ImportStage.BUSINESS_FIRST, ImportStage.BUSINESS_REMOVE_RESOURCES_MESSAGES_SENT)) {
                return;
            }

            ProcessingBusinessOperation operation = businessHelper.findOperation(operationId);
            if (operation == null) {
                throw new InternalApiException("???? ?????????????? ?????????????????? ???????????????? ??????????????");
            }
            boolean accountEnabled = MapUtils.getBooleanValue(operation.getParams(), "accountEnabled", true);
            boolean allowAntispam = MapUtils.getBooleanValue(operation.getParams(), "allowAntispam", false);
            boolean unixAccountDenied = MapUtils.getBooleanValue(operation.getParams(), "unixAccountDenied", false);
            boolean databaseDenied = MapUtils.getBooleanValue(operation.getParams(), "databaseDenied", false);
            long quotaBytes = MapUtils.getLongValue(operation.getParams(), "quotaBytes");

            try {
                rcUserClient.importToMongo(accountId, accountEnabled, allowAntispam);
                unixAccountDBImportService.importToMongo(accountId, serverId, operationId, accountEnabled, quotaBytes, unixAccountDenied);
            } catch (FeignException | BaseException | DataAccessException ex) {
                businessHelper.setErrorStatus(operationId, "???? ?????????????? ?????????????????????????? ???????????????? ?????????????? ????????????????");
                logger.error("Got exception in Stage First. account: " + accountId, ex);
                return;
            }
            try {
                if (!databaseDenied) {
                    databaseUserDBImportService.importToMongo(accountId, mysqlServiceId, operationId, accountEnabled); // databaseDBImportService.importToMongo ???????????????????? ?? CommonAmqpController ?????????? ???????????????? ????????????????????????
                }
            } catch (FeignException | BaseException | DataAccessException ex) {
                logger.error("Got exception when import database users. account: " + accountId, ex);
                businessHelper.addWarning(operationId, "???? ?????????????? ?????????????????????????? ?????????????????????????? ?????? ????????????. ???????????????????? ?????????????? ???? ??????????????");
            }
            businessHelper.setStage(operationId, ImportStage.BUSINESS_FIRST_MESSAGES_SENT);
            startStageSecondIfNeed(accountId, operationId, serverId, mysqlServiceId);
        } catch (Exception ex) {
            logger.error("Got exception in first import stage. Account: " + accountId, ex);
            ex.printStackTrace();
            businessHelper.setErrorStatus(operationId, "???????????????????? ???????????? ?????? ??????????????");
        }
    }

    public void startStageSecondIfNeed(ProcessingBusinessOperation operation) {
        String serverId = (String) operation.getParam("serverId");
        String mysqlServiceId = (String) operation.getParam("mysqlServiceId");
        startStageSecondIfNeed(operation.getPersonalAccountId(), operation.getId(), serverId, mysqlServiceId);
    }

    public void startStageSecondIfNeed(String accountId, String operationId, String serverId, String mysqlServiceId) {
        try {
            if (StringUtils.isEmpty(accountId) || StringUtils.isEmpty(operationId) || StringUtils.isEmpty(serverId)) {
                return;
            }
            if (businessHelper.existsActiveActions(operationId)) {
                logger.debug("Skip startStageSecondIfNeed because exists active actions for operation: {}", operationId);
                return;
            }
            if (!businessHelper.setStage(operationId, ImportStage.BUSINESS_SECOND, ImportStage.BUSINESS_FIRST_MESSAGES_SENT)) {
                return;
            }

            ProcessingBusinessOperation operation = businessHelper.findOperation(operationId);
            if (operation == null) {
                throw new InternalApiException("???? ?????????????? ?????????????????? ???????????????? ??????????????");
            }
            boolean accountEnabled = MapUtils.getBooleanValue(operation.getParams(), "accountEnabled", true);
            boolean databaseDenied = MapUtils.getBooleanValue(operation.getParams(), "databaseDenied", false);
            boolean ftpUserDenied = MapUtils.getBooleanValue(operation.getParams(), "ftpUserDenied", false);
            boolean websiteDenied = MapUtils.getBooleanValue(operation.getParams(), "websiteDenied", false);

            try {
                if (!websiteDenied) {
                    websiteDBImportService.importToMongo(accountId, serverId, operationId, accountEnabled);
                }
            } catch (BaseException | FeignException | DataAccessException ex) {
                logger.error("Got exception when import websites. account: " + accountId, ex);
                businessHelper.addWarning(operationId, "???? ?????????????? ?????????????????????????? ??????????. ???????????????????? ?????????????? ???? ??????????????");
            }

            try {
                if (!databaseDenied) {
                    databaseDBImportService.importToMongo(accountId, mysqlServiceId, operationId, accountEnabled);
                }
            } catch (BaseException | FeignException | DataAccessException ex) {
                logger.error("Got exception when import databases. account: " + accountId, ex);
                businessHelper.addWarning(operationId, "???? ?????????????? ?????????????????????????? ???????? ????????????. ???????????????????? ?????????????? ???? ??????????????");
            }

            try {
                if (!ftpUserDenied) {
                    ftpUserDBImportService.importToMongo(accountId, serverId, operationId, accountEnabled);
                }
            } catch (BaseException | FeignException | DataAccessException ex) {
                logger.error("Got exception when import ftp-users. account: " + accountId, ex);
                businessHelper.addWarning(operationId, "???? ?????????????? ?????????????????????????? ftp-??????????????????????????. ???????????????????? ?????????????? ???? ??????????????");
            }

            businessHelper.setStage(operationId, ImportStage.BUSINESS_SECOND_MESSAGES_SENT);
            finishImportIfNeed(accountId, operationId);
        } catch (Exception ex) {
            logger.error("Got exception in second import stage. Account: " + accountId, ex);
            ex.printStackTrace();
            businessHelper.setErrorStatus(operationId, "???????????????????? ???????????? ?????? ??????????????");
        }
    }

    public void finishImportIfNeed(ProcessingBusinessOperation operation) {
        finishImportIfNeed(operation.getPersonalAccountId(), operation.getId());
    }

    public void finishImportIfNeed(String accountId, String operationId) {
        if (StringUtils.isEmpty(accountId) || StringUtils.isEmpty(operationId)) {
            return;
        }
        if (businessHelper.existsActiveActions(operationId)) {
            logger.debug("Skip finishImportIfNeed because exists active actions for operation: {}", operationId);
            return;
        }
        if (!businessHelper.setStage(operationId, ImportStage.FINISH, ImportStage.BUSINESS_SECOND_MESSAGES_SENT)) {
            return;
        }
        ProcessingBusinessOperation operation = operationRepository.findById(operationId).orElseThrow(InternalApiException::new);

        operation.setUpdatedDate(LocalDateTime.now());
        operation.setState(State.FINISHED);
        operationRepository.save(operation);
    }

    private String extractErrorMessage(ProcessingBusinessAction action) {
        BusinessActionType type = action.getBusinessActionType();
        String name = MapUtils.getString(action.getParams(), "name", "");
        if (StringUtils.isNotEmpty(name)) {
            if (type == WEB_SITE_DELETE_RC) {
                return "???????????? ?????? ???????????????? ??????????: " + name;
            }
            return "???????????? ?????? ???????????????? ??????????????: " + name;
        } else {
            return "???????????? ?????? ???????????????????? ????????????????: " + action.getName();
        }
    }

    public void processErrorAction(ProcessingBusinessAction action, ProcessingBusinessOperation operation, SimpleServiceMessage message) {
        if (action.getBusinessActionType() == BusinessActionType.DATABASE_USER_CREATE_RC) {
            businessHelper.addWarning(operation.getId(), extractErrorMessage(action));
            startStageSecondIfNeed(operation);
        } else if (EnumSet.of(WEB_SITE_CREATE_RC, DATABASE_CREATE_RC, FTP_USER_CREATE_RC).contains(action.getBusinessActionType())) {
            businessHelper.addWarning(operation.getId(), extractErrorMessage(action));
            finishImportIfNeed(operation);
        } else {
            businessHelper.setErrorStatus(operation.getId(), extractErrorMessage(action));
        }
    }

    public boolean importToMongo(String accountId, String serverId, String operationId, String mysqlServiceId) {
        try {
            logger.debug("Start import for account {}, to server {}, mysqlService {}, operation {}", accountId, serverId, mysqlServiceId, operationId);
            businessHelper.setStage(operationId, ImportStage.DIRECT_PM);
            Plan plan = personalAccountDBImportService.importToMongo(accountId, operationId);

            accountNotificationDBImportService.importToMongo(accountId);
            accountServicesDBImportService.importToMongo(accountId, plan, operationId);
            accountCommentDBImportService.importToMongo(accountId);
            accountAbonementDBImportService.importToMongo(accountId);
            accountOwnerDBImportService.importToMongo(accountId);
            accountHistoryDBImportService.importToMongo(accountId);

            businessHelper.setStage(operationId, ImportStage.DIRECT_SI);

            webAccessAccountDBImportService.importToMongo(accountId, operationId); // ?? ???????????? ???????????? ???????????? InternalApiError ?? ???????????????? ?? ???????????????? ?????????????? ?? ?????????? catch

            businessHelper.setStage(operationId, ImportStage.DIRECT_FIN);
            try {
                finFeignClient.importToMongo(accountId);
            } catch (FeignException ex) {
                logger.error("Got exception when import account: " + accountId, ex);
                businessHelper.setErrorStatus(operationId, "???? ?????????????? ?????????????????? ???????????? ????????????????. ???????? ?????? ???????????????????? ?????????????? ?? ?????????????????????? ??????????????");
                return false;
            } catch (BaseException ex) {
                logger.error("Got exception when import account: " + accountId, ex);
                businessHelper.setErrorStatus(operationId, "???? ?????????????? ?????????????????? ???????????? ????????????????. ????????????????????: " + ex.toString());
                return false;
            }

            businessHelper.setStage(operationId, ImportStage.DIRECT_PARTNER);
            partnerPromocodeDBImportService.importToMongo(accountId, operationId); // ???????????????????? ???? rest, ???? ???????????????????? ???? ??????????????. ?????????????????? ???????????????????????????? ?? ProcessedBusinessOperation

            // ?? ???????? ?????????? ?????????????? ?????????????????????? ?????????????????? ?????? ??????????????????????????

            businessHelper.setStage(operationId, ImportStage.BUSINESS_REMOVE_RESOURCES);
            websiteDBImportService.removeWebsites(accountId, operationId); // ???????????????????? ???? rest, ???????????? InternalApiException, ?????????? ?????????????????????? ?? ????????????????
            // ?????? ?????? te ???????????????? ?? ?????????????? ???? mongo id, ?????????? ?????????????????????? ???????????????????? ???? ???????????? ???????? ?????????????? ???????????? ???? ???????????????????? ??????????????

            businessHelper.setStage(operationId, ImportStage.BUSINESS_REMOVE_RESOURCES_MESSAGES_SENT);

            startStageFirstIfNeed(accountId, operationId, serverId, mysqlServiceId);
        } catch (BaseException ex) {
            logger.error("Got exception when import account: " + accountId, ex);
            businessHelper.setErrorStatus(operationId, ex.getMessage());
            return false;
        } catch (Exception ex) {
            logger.error("Got exception when import account: " + accountId, ex);
            ex.printStackTrace();
            businessHelper.setErrorStatus(operationId, "???? ?????????????? ?????????????????? ??????????????");
            return false;
        }
        return true;
    }

}
