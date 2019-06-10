package ru.majordomo.hms.personmgr.dto.stat;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestTemplate;
import ru.majordomo.hms.personmgr.dto.Container;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ru.majordomo.hms.personmgr.common.Utils.getResourceAsString;
import static ru.majordomo.hms.personmgr.common.Utils.toLong;

@Slf4j
public class WhoIsServersSupplier implements Supplier<List<WhoIsServer>> {
    private List<WhoIsServer> servers;

    public List<WhoIsServer> get() {

        if (servers == null) {
            servers = getWhoIsServers();
        }
        return servers;
    }

    private String getWhoIsServersFromInternet() {
        return new RestTemplate().getForObject("https://www.nirsoft.net/whois-servers.txt", String.class);
    }

    public List<WhoIsServer> getWhoIsServers() {
        List<WhoIsServer> servers = convert(getWhoIsServersFromFile());
        String[] rows = getIpDiapasonsFromFile().split("\r\n");
//        log.info("getIpDiapasonsFromFile {}", Arrays.toString(rows));
        Container<WhoIsServer> container = new Container<>();
        final String countryPrefix = ";country: ";
        for (String row : rows) {
            if (row == null || row.trim().isEmpty()) continue;
            if (row.startsWith(countryPrefix)) {
                String countryKey = row.substring(countryPrefix.length());
                log.info("country key : '" + countryKey + "'" );
                container.setData(null);
                servers.stream().filter(s -> s.getCountry().equals(countryKey)).findFirst().ifPresent(data -> {
                    log.info("found country : {} row {}", countryKey, row);
                    container.setData(data);
                });
            } else if (!row.startsWith(";")) {
//                log.info("row : {}", row);
                if (row.matches("^(\\d{1,3}\\.){3}\\d{1,3},(\\d{1,3}\\.){3}\\d{1,3},\\d+,\\d{2}/\\d{2}/\\d{2},.*$")) {
                    String[] s = row.split(",");
                    WhoIsServer server = container.getData();
                    if (server != null) {
                        try {
                            InetAddress start = InetAddress.getByName(s[0]);
                            InetAddress end = InetAddress.getByName(s[1]);
                            server.getIpDiapasons().add(new IpDiapason(
                                    start,
                                    end,
                                    Long.parseLong(s[2]),
                                    s.length > 3 ? s[4] : null,
                                    toLong(start),
                                    toLong(end)
                            ));
                        } catch (Exception e) {
                            log.error("can't add ipdiapason row {} server {} e {} message {}", row, server, e.getClass(), e.getMessage());
//                            e.printStackTrace();
                        }
                    }
                } else {
                    log.error("not match row {} ", row);
                }
            }

        }
//                .filter(s -> s != null && !s.trim().isEmpty())
//                .map(s -> s.split(","));
//        ;
        return servers;
    }

    private List<WhoIsServer> convert(String raw) {
        if (raw == null) return new ArrayList<>();
        return Stream.of(raw.split("\n"))
                .filter(s -> !s.startsWith(";"))
                .map(s -> s.split("\\s+"))
                .filter(s -> s.length > 1)
                .map(s -> new WhoIsServer(s[0], s[1]))
                .collect(Collectors.toList());
    }

    private String getWhoIsServersFromFile() {
        String path = "/whois/whois-servers.txt";
        String encoding = "UTF-8";
        try {
            return getResourceAsString(path, encoding);
        } catch (Exception e) {
            log.error("catch e {} message {} with open {} encoding {}", e.getClass(), e.getMessage(), path, encoding);
            return "";
        }
    }

    private String getIpDiapasonsFromFile() {
        String path = "/whois/ip-diapasons.csv";
        String encoding = "UTF-8";
        try {
            return getResourceAsString(path, encoding);
        } catch (Exception e) {
            log.error("catch e {} message {} with open {} encoding {}", e.getClass(), e.getMessage(), path, encoding);
            return "";
        }
    }
}
