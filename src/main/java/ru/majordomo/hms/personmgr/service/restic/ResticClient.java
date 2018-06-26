package ru.majordomo.hms.personmgr.service.restic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;

@Service
public class ResticClient {

    private final static Logger log = LoggerFactory.getLogger(ResticClient.class);
    private String fileBackupStorage;
    private String mysqlBackupStorage;

    @Autowired
    public ResticClient(
            @Value("${backup.file.url}") String fileBackupStorage,
            @Value("${backup.mysql.url}") String mysqlBackupStorage
    ) throws MalformedURLException {
        new URL(fileBackupStorage);
        new URL(mysqlBackupStorage);
        this.fileBackupStorage = fileBackupStorage;
        this.mysqlBackupStorage = mysqlBackupStorage;
    }

    public List<Snapshot> getFileSnapshots(String homeDir, String serverName) {
        String snapshotsUrl = format("%s/_snapshot/%s%s", fileBackupStorage, serverName, homeDir);

        try {
            SnapshotList response = new RestTemplate().getForObject(snapshotsUrl, SnapshotList.class);

            if (response == null) {
                log.error("SnapshotResponse = null from '" + snapshotsUrl + "' return empty list");
                return Collections.emptyList();
            }

            response.forEach(snapshot -> {
                snapshot.setPaths(null);
                snapshot.setServerName(serverName);
            });

            return response;

        } catch (Exception e) {
            log.error("Can't fetch snapshots from '" + snapshotsUrl
                    + "' return empty list, exceptionClass: " + e.getClass().getName()
                    + " exception : " + e.getMessage()
            );
            return Collections.emptyList();
        }
    }

    public Optional<Snapshot> getFileSnapshot(String homeDir, String serverName, String snapshotId) {
        return getFileSnapshots(homeDir, serverName)
                .stream()
                .filter(snapshot -> snapshot.getShortId().equals(snapshotId))
                .findFirst();
    }

    public List<Snapshot> getDBSnapshots(String personalAccountId) {
        String snapshotsUrl = format("%s/_dump/%s", mysqlBackupStorage, personalAccountId);
        try {
            SnapshotList response = new RestTemplate().getForObject(snapshotsUrl, SnapshotList.class);

            if (response == null) {
                log.error("SnapshotResponse = null from '" + snapshotsUrl + "' return empty list");
                return Collections.emptyList();
            }

            return response;

        } catch (Exception e) {
            log.error("Can't fetch snapshots from '" + snapshotsUrl
                    + "' return empty list, exceptionClass: " + e.getClass().getName()
                    + " exception : " + e.getMessage()
            );
            return Collections.emptyList();
        }
    }

    public Optional<Snapshot> getDBSnapshot(String personalAccountId, String snapshotId) {
        return getDBSnapshots(personalAccountId)
                .stream()
                .filter(snapshot -> snapshot.getShortId().equals(snapshotId))
                .findFirst();
    }
}