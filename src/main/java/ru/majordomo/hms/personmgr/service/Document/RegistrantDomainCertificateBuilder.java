package ru.majordomo.hms.personmgr.service.Document;

import ru.majordomo.hms.personmgr.dto.rpc.RegistrantDomain;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.service.RcUserFeignClient;
import ru.majordomo.hms.personmgr.service.Rpc.RegRpcClient;
import ru.majordomo.hms.rc.user.resources.Domain;
import ru.majordomo.hms.rc.user.resources.Person;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class RegistrantDomainCertificateBuilder extends DocumentBuilderImpl {

    private final RegRpcClient regRpcClient;
    private final Map<String, String> params;
    private RcUserFeignClient rcUserFeignClient;
    private String personalAccountId;
    private Domain domain;

    public RegistrantDomainCertificateBuilder(
            String personalAccountId,
            RegRpcClient regRpcClient,
            RcUserFeignClient rcUserFeignClient,
            Map<String, String> params
    ) {
        this.params = params;
        this.regRpcClient = regRpcClient;
        this.rcUserFeignClient = rcUserFeignClient;
        this.personalAccountId = personalAccountId;
    }

    @Override
    public void checkRequireParams(){
        String domainId = params.get("domainId");

        if (domainId == null || domainId.isEmpty()) {
            throw new ParameterValidationException("Укажите домен, для которого хотите заказать сертификат");
        }
    }

    @Override
    public void buildTemplate() {
        String domainId = params.get("domainId");

        domain = rcUserFeignClient.getDomain(personalAccountId, domainId);

        checkDomainAndPerson(domain);

        Person person = domain.getPerson();

        RegistrantDomain registrantDomain = getDomainFromRegistrant(domain.getName(), person.getNicHandle());

        if (registrantDomain == null) {
            throw new ParameterValidationException("Домен не найден у регистратора");
        }

        if (!registrantDomain.getState().equals("ok")) {
            throw new ParameterValidationException("Домен в состоянии ошибки");
        }

        String registrantDomainId = registrantDomain.getDomainId();

        setFile(
                regRpcClient.getDomainCertificateInPng(registrantDomainId)
        );
    }

    private RegistrantDomain getDomainFromRegistrant(String domainName, String nicHandle) {
        RegistrantDomain registrantDomain;

        int offset = 0;
        int limit = 500;
        do {
            List<RegistrantDomain> domainsFromRegistrant = regRpcClient.getDomainsByName(domain.getName(), offset, limit);

            if (domainsFromRegistrant == null || domainsFromRegistrant.isEmpty()) {
                return null;
            }

            registrantDomain = getFirstFromDomainListByFqdnAndNicHandle(domainsFromRegistrant, domainName, nicHandle);

            if (registrantDomain == null) {
                offset += limit;
            }

        } while (registrantDomain == null);

        return registrantDomain;
    }

    private RegistrantDomain getFirstFromDomainListByFqdnAndNicHandle(
            List<RegistrantDomain> domainsFromRegistrant,
            String domainName,
            String nicHandle
    ){
        return domainsFromRegistrant
                .stream()
                .filter(item ->
                        item.getFqdn().equals(domainName)
                                && item.getNicHandle().equals(nicHandle)
                )
                .limit(1)
                .findFirst()
                .orElse(null);
    }

    private void checkDomainAndPerson(Domain domain){

        if (domain == null) {
            throw new ParameterValidationException("На аккаунте не найден домен");
        }

        if (domain.getRegSpec() == null) {
            throw new ParameterValidationException("У домена не найдены регистрационные данные");
        }

        if (domain.getRegSpec().getPaidTill() == null || domain.getRegSpec().getPaidTill().isAfter(LocalDate.now())) {
            throw new ParameterValidationException("Нельзя заказать сертификат на истекший домен");
        }

        Person person = domain.getPerson();

        if (person == null) {
            throw new ParameterValidationException("Не найдена персона, на которую зарегистрирован домен");
        }

        if (person.getNicHandle() == null || person.getNicHandle().isEmpty()) {
            throw new ParameterValidationException("Не найдет Nic-handle персоны");
        }
    }

    @Override
    public void replaceFields() {

    }

    @Override
    public void convert() {

    }

    @Override
    public void saveAccountDocument() {

    }
}
