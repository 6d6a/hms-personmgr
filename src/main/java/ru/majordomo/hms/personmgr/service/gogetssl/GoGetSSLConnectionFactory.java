package ru.majordomo.hms.personmgr.service.gogetssl;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.config.GoGetSSLConfig;

@Service
@AllArgsConstructor
public class GoGetSSLConnectionFactory {
    private final GoGetSSLConfig config;

    public GoGetSSLConnection getConnection() {
        return new GoGetSSLConnection(config);
    }
}
