package ru.majordomo.hms.personmgr.dto.stat;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class WhoIsServer {
    private final String country;
    private final String whoIsServer;
    private final List<IpDiapason> ipDiapasons = new ArrayList<>();
}
