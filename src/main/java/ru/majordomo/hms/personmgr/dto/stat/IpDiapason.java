package ru.majordomo.hms.personmgr.dto.stat;

import lombok.Data;

import java.net.InetAddress;

@Data
public class IpDiapason {
    private final InetAddress start;
    private final InetAddress end;
    private final long ipsCount;
    private final String owner;
    private final long startLong;
    private final long endLong;
}
