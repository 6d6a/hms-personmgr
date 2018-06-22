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
    private String resticUrl;

    @Autowired
    public ResticClient(
            @Value("${restic.url}") String resticUrl
    ) throws MalformedURLException {
        new URL(resticUrl);
        this.resticUrl = resticUrl;
    }

    public List<Snapshot> getSnapshots(String homeDir, String serverName) {
        String snapshotsUrl =
                format(
                        "%s/_snapshot/%s%s",
                        resticUrl,
                        serverName,
                        homeDir
                );
        try {
            SnapshotsResponse response = new RestTemplate().getForObject(snapshotsUrl, SnapshotsResponse.class);

            if (response == null) {
                log.error("SnapshotResponse = null from '" + snapshotsUrl + "' return empty list");
                return Collections.emptyList();
            }

            response.forEach(snapshot -> snapshot.setServerName(serverName));

            return response;

        } catch (Exception e) {
            log.error("Can't fetch snapshots from '" + snapshotsUrl
                    + "' return empty list, exceptionClass: " + e.getClass().getName()
                    + " exception : " + e.getMessage()
            );
            return Collections.emptyList();
        }
    }

    public Optional<Snapshot> getSnapshot(String homeDir, String serverName, String snapshotId) {
        return getSnapshots(homeDir, serverName)
                .stream()
                .filter(snapshot -> snapshot.getShortId().equals(snapshotId))
                .findFirst();
    }

}