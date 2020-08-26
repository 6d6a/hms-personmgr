package ru.majordomo.hms.personmgr.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.xbill.DNS.*;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.feign.RcUserFeignClient;
import ru.majordomo.hms.rc.user.resources.Domain;

import java.net.IDN;
import java.net.UnknownHostException;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class NsCheckService {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final RcUserFeignClient rcUserFeignClient;

    /** список разрешенных NS записей без точек вконце */
    @Value("redirect.allowedNSList")
    private final Set<String> allowedNSList;
    @Value("redirect.dnsResolverIp")
    private final String dnsResolverIp;

    public boolean checkOurNs(Domain domain) {
        if (domain.getParentDomainId() != null) {
            Domain originDomain;
            try {
                originDomain = rcUserFeignClient.getDomain(domain.getAccountId(), domain.getParentDomainId());
            } catch (Exception e) {
                throw new ParameterValidationException("Основной домен для поддомена не найден на вашем аккаунте");
            }
            return checkOurNs(originDomain);
        } else {
            try {
                boolean hasAlienNS = false;

                Lookup lookup = new Lookup(IDN.toASCII(domain.getName()), Type.NS);
                lookup.setResolver(new SimpleResolver(dnsResolverIp));
                lookup.setCache(null);

                Record[] records = lookup.run();

                if (records != null) {
                    for (Record record : records) {
                        String nsRecordTarget = ((NSRecord) record).getTarget().toString(true);
                        if (!allowedNSList.contains(nsRecordTarget)) {
                            hasAlienNS = true;
                            break;
                        }
                    }
                }
                return !hasAlienNS;
            } catch (UnknownHostException | TextParseException e) {
                logger.error("Ошибка при получении NS-записей: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }
    }
}
