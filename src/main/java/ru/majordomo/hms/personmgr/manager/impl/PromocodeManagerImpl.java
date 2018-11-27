package ru.majordomo.hms.personmgr.manager.impl;

import org.apache.commons.lang.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.personmgr.common.PromocodeType;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.manager.PromocodeManager;
import ru.majordomo.hms.personmgr.model.promocode.Promocode;
import ru.majordomo.hms.personmgr.repository.PromocodeRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import static ru.majordomo.hms.personmgr.common.Constants.*;

@Component
public class PromocodeManagerImpl implements PromocodeManager {

    private final PromocodeRepository repository;

    @Autowired
    public PromocodeManagerImpl(PromocodeRepository repository) {
        this.repository = repository;
    }

    public Promocode generatePromocodeUnlimitedOneMonth() {
        return generatePromocode(PromocodeType.BONUS, BONUS_UNLIMITED_1_M_PROMOCODE_ACTION_ID);
    }

    public Promocode generatePromocodeUnlimitedThreeMonth() {
        return generatePromocode(PromocodeType.BONUS, BONUS_UNLIMITED_3_M_PROMOCODE_ACTION_ID);
    }

    public Promocode generatePromocodeParkingThreeMonth() {
        return generatePromocode(PromocodeType.BONUS, BONUS_PARKING_3_M_PROMOCODE_ACTION_ID);
    }

    @Override
    public Promocode findOne(String id) {
        return repository.findOne(id);
    }

    @Override
    public Promocode findByCodeIgnoreCase(String code) {
        return repository.findByCodeIgnoreCase(code);
    }

    @Override
    public Promocode findByCodeAndActive(String promocodeString, boolean active) {
        return repository.findByCodeAndActive(promocodeString, active);
    }

    @Override
    public Promocode findByTypeAndActive(PromocodeType type, boolean active) {
        return repository.findByTypeAndActive(type, active);
    }

    @Override
    public Promocode save(Promocode promocode) {
        return repository.save(promocode);
    }

    private Promocode generatePromocode(PromocodeType type, String actionId) {
        Promocode promocode = new Promocode();
        String code = generateNewCode(type);

        promocode.setCode(code);
        promocode.setActive(true);
        promocode.setCreatedDate(LocalDate.now());
        promocode.setType(type);
        promocode.setActionIds(Collections.singletonList(actionId));

        repository.save(promocode);

        return promocode;
    }

    private String generateNewCode(PromocodeType type) {
        String code = null;

        while (code == null) {
            switch (type) {
                case BONUS:
                    List<String> paths = new ArrayList<>();

                    int pathCount = 3;
                    int i = 0;
                    while (i < pathCount) {
                        String path = RandomStringUtils.randomAlphanumeric(4).toUpperCase();
                        if (!isBadWord(path)) {
                            i++;
                            paths.add(path);
                        }
                    }
                    code = String.join("-", paths);
                    break;

                default:
                    throw new ParameterValidationException(type.name() + " not implemented in generateNewCode");
            }

            code = repository.findByCode(code) == null ? code : null;
        }

        return code;
    }

    private boolean isBadWord(String code) {
        for (String badWord: BAD_WORD_PATTERNS) {
            if(Pattern.compile(badWord).matcher(code).matches()) {
                return true;
            }
        }
        return false;
    }
}