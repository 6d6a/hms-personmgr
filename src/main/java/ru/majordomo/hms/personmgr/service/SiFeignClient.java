package ru.majordomo.hms.personmgr.service;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import ru.majordomo.hms.personmgr.common.WebAccessAccount;

@FeignClient("si")
public interface SiFeignClient {
    @RequestMapping(method = RequestMethod.POST, value = "/web-access-accounts", consumes = "application/json")
    ResponseEntity<Void> create(WebAccessAccount account);
}
