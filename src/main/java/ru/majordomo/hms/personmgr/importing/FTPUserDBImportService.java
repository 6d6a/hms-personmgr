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
import org.springframework.stereotype.Component;
import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.Utils;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.service.BusinessHelper;
import ru.majordomo.hms.rc.user.resources.FTPUser;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Component
@AllArgsConstructor
public class FTPUserDBImportService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Qualifier("namedParameterJdbcTemplate")
    private final NamedParameterJdbcTemplate billingDbJdbcTemplate;
    private final BusinessHelper businessHelper;
    private final ImportHelper importHelper;

    private final static Set<String> webFtpIps = new HashSet<>(Arrays.asList("185.84.108.40", "78.108.80.199"));

    private String translateHomeDir(String oldHomeDir) {
        if (StringUtils.isBlank(oldHomeDir)) {
            return "";
        }
        oldHomeDir = oldHomeDir.trim();
        if (!oldHomeDir.startsWith("/home")) {
            return "";
        }

        ArrayList<String> pathParts = new ArrayList<>(Arrays.asList(oldHomeDir.split("/")));
        for (int i = 0; i < 3; i++) {
            if (!pathParts.isEmpty()) {
                pathParts.remove(0);
            }
        }
        oldHomeDir = String.join("/", pathParts);

        if (oldHomeDir.startsWith("/")) {
            oldHomeDir = oldHomeDir.substring(1);
        }
        return oldHomeDir;
    }

    private FTPUser rowMap(ResultSet rs, int rowNum, String accountId, String unixAccountId, String operationId, boolean accountEnabled) throws SQLException {
        logger.debug("Found FTPUser for id: " + rs.getString("id") + " login: " + rs.getString("login"));

        FTPUser ftpUser = new FTPUser();
        ftpUser.setAccountId(accountId);
        ftpUser.setPasswordHash(rs.getString("password"));
        ftpUser.setSwitchedOn(accountEnabled);
        ftpUser.setName(rs.getString("login"));
        String homeDir = rs.getString("HomeDir");

        ftpUser.setHomeDir(translateHomeDir(homeDir));
        ftpUser.setUnixAccountId(unixAccountId);

        String query = "SELECT fa.id, fa.acc_id, fa.remote_ip " +
                "FROM ftp_access fa " +
                "WHERE fa.acc_id = :accountId";
        SqlParameterSource sqlParam = new MapSqlParameterSource("accountId", rs.getString("id"));

        ArrayList<String> allowedIPAddresses = new ArrayList<>();

        billingDbJdbcTemplate.query(query,
                sqlParam,
                (rs1 -> {
                    String ip = rs1.getString("remote_ip");
                    if (StringUtils.isBlank(ip)) {
                        return;
                    }
                    ip = ip.trim();
                    if (!webFtpIps.contains(ip) && Utils.cidrOrIpValid(ip)) {
                        allowedIPAddresses.add(ip);
                    }
                })
        );
        ftpUser.setAllowedIPAddresses(allowedIPAddresses);

        SimpleServiceMessage message = importHelper.makeServiceMessage(accountId, operationId, ftpUser);
        message.addParam("homedir", ftpUser.getHomeDir());
        message.addParam("unixAccountId", ftpUser.getUnixAccountId());
        businessHelper.buildActionByOperationId(BusinessActionType.FTP_USER_CREATE_RC, message, operationId);

        return null;
    }

    public void importToMongo(String accountId, String serverId, String operationId, boolean accountEnabled) {
        String query = "SELECT a.id, f.ID, f.Status, f.login, f.password, f.UID, f.HomeDir " +
                "FROM ftp f " +
                "JOIN account a ON f.UID = a.uid " +
                "WHERE a.id = :accountId";
        SqlParameterSource sqlParam = new MapSqlParameterSource("accountId", accountId);

        String unixAccountId = "unixAccount_" + accountId;

        billingDbJdbcTemplate.query(query, sqlParam, (rs, rowNum) -> rowMap(rs, rowNum, accountId, unixAccountId, operationId, accountEnabled));
    }
}
