package ru.majordomo.hms.personmgr.manager;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.majordomo.hms.personmgr.common.PromocodeType;
import ru.majordomo.hms.personmgr.model.promocode.Promocode;

public interface PromocodeManager {

    Promocode findById(String id);

    Promocode findByCodeIgnoreCase(String code);

    Promocode findByCodeAndActive(String promocodeString, boolean active);

    Promocode findByTypeAndActive(PromocodeType type, boolean active);

    Promocode save(Promocode promocode);

    Promocode generatePromocode(Promocode example);

    Page<Promocode> findByTagIdsIn(Iterable<String> tagIds, Pageable pageable);
}