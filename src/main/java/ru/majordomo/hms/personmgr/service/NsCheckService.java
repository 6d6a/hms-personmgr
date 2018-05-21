package ru.majordomo.hms.personmgr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xbill.DNS.*;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.rc.user.resources.Domain;

import java.net.IDN;
import java.net.UnknownHostException;

@Service
public class NsCheckService {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final RcUserFeignClient rcUserFeignClient;

    @Autowired
    public NsCheckService(
            RcUserFeignClient rcUserFeignClient
    ) {
        this.rcUserFeignClient = rcUserFeignClient;
    }

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
                Boolean canOrderSSL = false;
                Boolean hasAlienNS = false;

                Lookup lookup = new Lookup(IDN.toASCII(domain.getName()), Type.NS);
                lookup.setResolver(new SimpleResolver("8.8.8.8"));
                lookup.setCache(null);

                Record[] records = lookup.run();

                if (records != null) {
                    for (Record record : records) {
                        NSRecord nsRecord = (NSRecord) record;
                        if (nsRecord.getTarget().equals(Name.fromString("ns.majordomo.ru.")) ||
                                nsRecord.getTarget().equals(Name.fromString("ns2.majordomo.ru.")) ||
                                nsRecord.getTarget().equals(Name.fromString("ns3.majordomo.ru."))) {
                            canOrderSSL = true;
                        } else {
                            hasAlienNS = true;
                        }
                    }
                }
                return canOrderSSL && !hasAlienNS;
            } catch (UnknownHostException | TextParseException e) {
                logger.error("Ошибка при получении NS-записей: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }
    }
}
