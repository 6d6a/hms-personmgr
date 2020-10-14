package ru.majordomo.hms.personmgr.model.account;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.util.Set;

@Data
@RedisHash("authIPs")
public class AuthIPRedis {
    @Id
    private String name;

    private String ip;

    private Set<String> ips;

    private boolean notify;
}