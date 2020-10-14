package ru.majordomo.hms.personmgr.dto;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@Data
@RedisHash("authIPs")
public class NewAuth {
    @Id
    private String name;

    private String ip;
}
