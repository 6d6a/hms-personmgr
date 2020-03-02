package ru.majordomo.hms.personmgr.importing;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.exception.InternalApiException;
import ru.majordomo.hms.personmgr.manager.PlanManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.plan.VirtualHostingPlanProperties;
import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;
import ru.majordomo.hms.personmgr.service.BusinessHelper;
import ru.majordomo.hms.rc.user.resources.CronTask;
import ru.majordomo.hms.rc.user.resources.SSHKeyPair;
import ru.majordomo.hms.rc.user.resources.UnixAccount;

import java.util.ArrayList;
import java.util.List;

@Component
@AllArgsConstructor
public class UnixAccountDBImportService{
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Qualifier("namedParameterJdbcTemplate")
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final BusinessHelper businessHelper;
    private final ImportHelper importHelper;
    private final PlanManager planManager;
    private final PersonalAccountRepository personalAccountRepository;

    private final static String QUERY = "SELECT a.id, a.name, a.plan_id, a.server_id, a.homedir, a.quotaused, a.mailquotaused, a.status, a.uid, " +
            "p.db, p.QuotaKB, " +
            "s.name, s.jail, s.localdb, " +
            "us.shell, us.changed, us.pub_key, us.send_mail, " +
            "ds.id as deny_sendmail " +
            "FROM account a " +
            "LEFT JOIN servers s ON s.id = a.server_id " +
            "JOIN plan p ON p.Plan_ID = a.plan_id " +
            "LEFT JOIN user_shell us ON us.uid = a.uid " +
            "LEFT JOIN deny_sendmail ds ON ds.uid = a.uid " +
            "WHERE a.id = :accountId";

    ProcessingBusinessAction importToMongo(String accountId, String serverId, String operationId, boolean accountEnabled, long quotaBytes, boolean unixAccountDenied) {

        SqlParameterSource paramSource = new MapSqlParameterSource("accountId", accountId);

        SqlRowSet rs = namedParameterJdbcTemplate.queryForRowSet(QUERY, paramSource);
        if (!rs.next()) {
            throw new InternalApiException("Не удалось извлечь unix-аккаунт из billingdb");
        }

        String oldServerId = rs.getString("server_id");
        long quotaUsedKB = rs.getLong("quotaused");
        long mailQuotaUsedKB = rs.getLong("mailquotaused");
        boolean denySendmail = rs.getString("deny_sendmail") != null;
        String pubKey = rs.getString("pub_key");
        int uid = rs.getInt("uid");
        String oldHome = rs.getString("homedir");
        boolean status = rs.getString("status").equals("1");

        boolean noUnixAccount = StringUtils.isBlank(oldServerId) || "0".equals(oldServerId);

        String newName = "u" + accountId;
        String newHome;
        if (StringUtils.isBlank(oldHome) || !oldHome.startsWith("/home/")) {
            newHome = "/home/" + newName;
        } else {
            newHome = oldHome;
        }

        UnixAccount unixAccount = new UnixAccount();
        unixAccount.setId("unixAccount_" + accountId);
        unixAccount.setAccountId(accountId);
        unixAccount.setUid(uid);
        unixAccount.setWritable(!unixAccountDenied);
        unixAccount.setSwitchedOn(accountEnabled && !unixAccountDenied);
        unixAccount.setName(newName);
        unixAccount.setHomeDir(newHome);

        unixAccount.setServerId(serverId);

        unixAccount.setQuota(quotaBytes);
        unixAccount.setQuotaUsed(0L);
        unixAccount.setSendmailAllowed(!denySendmail);

        SSHKeyPair sshKeyPair = new SSHKeyPair();
        sshKeyPair.setPublicKey(pubKey);
        unixAccount.setKeyPair(sshKeyPair);

        List<CronTask> cronTasks = new ArrayList<>();

        String query = "SELECT ucc.active, ucc.uid " +
                "FROM users_crontab_conf ucc " +
                "WHERE ucc.uid = :uid";

        namedParameterJdbcTemplate.query(query,
                new MapSqlParameterSource("uid", uid),
                ((rs1, rowNum1) -> {
                    if (rs1.getString("active").equals("1")) {
                        String cronQuery = "SELECT uc.id, uc.uid, uc.minute, uc.hour, uc.dayofm, uc.month, uc.dayofw, uc.command, uc.comment " +
                                "FROM users_crontab uc " +
                                "WHERE uc.uid = :uid";
                        MapSqlParameterSource cronNamedParameters1 = new MapSqlParameterSource("uid", rs1.getString("uid"));

                        cronTasks.addAll(namedParameterJdbcTemplate.query(cronQuery,
                                cronNamedParameters1,
                                ((rs2, rowNum2) -> {
                                    String minute = rs2.getString("minute");
                                    String hour = rs2.getString("hour");
                                    String dayofm = rs2.getString("dayofm");
                                    String month = rs2.getString("month");
                                    String dayOfW = rs2.getString("dayofw");

                                    dayOfW = dayOfW != null && (dayOfW.equals("0-7") || dayOfW.equals("0-6")) ? "1-7" : dayOfW;

                                    String command = rs2.getString("command");
                                    String execTime = minute + " " +
                                            hour + " " +
                                            dayofm + " " +
                                            month + " " +
                                            dayOfW;

                                    logger.debug("Cron task found for uid: " + rs1.getString("uid")
                                            + " command: " + command
                                            + " execTime: " + execTime);

                                    CronTask cronTask = new CronTask();
                                    cronTask.setSwitchedOn(true);
                                    cronTask.setCommand(command);

                                    try {
                                        cronTask.setExecTime(execTime);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        return null;
                                    }
//                                    cronTask.setExecTimeDescription(rs2.getString("comment"));

                                    return cronTask;
                                })
                        ));
                    }

                    return  null;
                })
        );

        unixAccount.setCrontab(cronTasks);

        SimpleServiceMessage message = importHelper.makeServiceMessage(accountId, operationId, unixAccount);
        message.addParam("replaceUidAndHome", true);

        ProcessingBusinessAction action = businessHelper.buildActionByOperationId(BusinessActionType.UNIX_ACCOUNT_CREATE_RC, message, operationId);
        return action;
    }
}
