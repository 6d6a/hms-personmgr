package ru.majordomo.hms.personmgr.controller.rest;

import com.jayway.jsonpath.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.majordomo.hms.personmgr.common.FullName;
import ru.majordomo.hms.personmgr.dto.dadata.BankInfo;
import ru.majordomo.hms.personmgr.dto.dadata.OwnerInfo;
import ru.majordomo.hms.personmgr.feign.DadataFeignClient;
import ru.majordomo.hms.personmgr.dto.dadata.DadataQuery;
import javax.annotation.Nonnull;
import java.util.Objects;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/find-data")
public class FindDataRestController {
    private final DadataFeignClient dadataClient;

    private ParseContext jsonPath = JsonPath.using(
            Configuration.builder().options(Option.SUPPRESS_EXCEPTIONS, Option.DEFAULT_PATH_LEAF_TO_NULL).build()
    );

    private FullName splitName(String fullName) {
        String[] parts = StringUtils.trimToEmpty(fullName).split("\\s+");
        FullName result = new FullName();
        if (parts.length > 0) {
            result.setFirstName(parts[0]);
        }
        if (parts.length > 1) {
            result.setLastName(parts[1]);
        }
        if (parts.length > 2) {
            result.setMiddleName(parts[2]);
        }
        return result;
    }

    @Nonnull
    private String read(DocumentContext jsonDoc, String part) {
        return Objects.toString(jsonDoc.read(part), "");
    }

    @GetMapping("/inn/{inn}")
    public OwnerInfo findByInn(@PathVariable String inn) {
        OwnerInfo result = new OwnerInfo();
        try {
            String jsonResponse = dadataClient.findByInn(new DadataQuery(inn));
            DocumentContext response = jsonPath.parse(jsonResponse);
            result.setKpp(read(response, "$.suggestions[0].data.kpp"));
            result.setInn(read(response, "$.suggestions[0].data.inn"));
            result.setOgrn(read(response, "$.suggestions[0].data.ogrn"));
            result.setOkpo(read(response, "$.suggestions[0].data.okpo"));
            result.setOkved(read(response, "$.suggestions[0].data.okved"));
            result.setAddressFull(read(response, "$.suggestions[0].data.address.unrestricted_value"));
            result.setPostal(read(response, "$.suggestions[0].data.address.data.postal_code"));
            result.setCity(read(response, "$.suggestions[0].data.address.data.city"));
            result.setFullName(read(response, "$.suggestions[0].data.name.full"));
            result.setType(read(response, "$.suggestions[0].data.type"));
            result.setOrgType(read(response, "$.suggestions[0].data.opf.short"));
            result.setManager(read(response, "$.suggestions[0].data.management.name"));

            result.setAddressShort(result.getAddressFull().replaceFirst(result.getPostal() + "\\s*,?\\s*", ""));
            if (StringUtils.isNotBlank(result.getCity())) {
                int startIndex = result.getAddressShort().toLowerCase().indexOf(result.getCity().toLowerCase());
                if (startIndex >= 0) {
                    result.setAddressShort(result.getAddressShort().substring(startIndex + result.getCity().length()));
                    result.setAddressShort(result.getAddressShort().replaceFirst("^[,\\s]+", ""));
                }
            }
            if ("INDIVIDUAL".equals(result.getType())) {
                FullName fullName = splitName(result.getFullName());
                result.setFirstName(fullName.getFirstName());
                result.setLastName(fullName.getLastName());
                result.setMiddleName(fullName.getMiddleName());
            }
            FullName mFullName = splitName(result.getManager());
            result.setMgrFirstName(mFullName.getFirstName());
            result.setMgrLastName(mFullName.getLastName());
            result.setMgrMiddleName(mFullName.getMiddleName());
        } catch (RuntimeException ex) {
            log.error("Got exception when process dadata bank request", ex);
        }
        return result;
    }


    @GetMapping("/bic/{bic}")
    public BankInfo findByBic(@PathVariable String bic) {
        try {
            String jsonResponse = dadataClient.findByBic(new DadataQuery(bic));
            DocumentContext response = jsonPath.parse(jsonResponse);
            String bicNew = Objects.toString(response.read("$.suggestions[0].data.bic"), "");
            String correspondentAccount = Objects.toString(response.read("$.suggestions[0].data.correspondent_account"), "");
            String name = Objects.toString(response.read("$.suggestions[0].data.name.payment"), "");
            if (StringUtils.isEmpty(name)) {
                name = Objects.toString(response.read("$.suggestions[0].value"), "");
            }

            if (!bic.equals(bicNew)) { // в ответе часто другая организация, наверное лучше ничего не возвращать
                bicNew = "";
                name = "";
                correspondentAccount = "";
            }
            return new BankInfo(bicNew, name, correspondentAccount);
        } catch (RuntimeException ex) {
            log.error("Got exception when process dadata bank request", ex);
            return new BankInfo("", "", "");
        }
    }
}
