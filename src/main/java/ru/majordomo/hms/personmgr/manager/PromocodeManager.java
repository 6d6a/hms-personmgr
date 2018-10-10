package ru.majordomo.hms.personmgr.manager;

import ru.majordomo.hms.personmgr.common.PromocodeType;
import ru.majordomo.hms.personmgr.model.promocode.Promocode;

public interface PromocodeManager {

    Promocode generatePromocodeUnlimitedOneMonth();

    Promocode generatePromocodeUnlimitedThreeMonth();

    Promocode generatePromocodeParkingThreeMonth();

    Promocode findOne(String id);

    Promocode findByCodeIgnoreCase(String code);

    Promocode findByCodeAndActive(String promocodeString, boolean active);

    Promocode findByTypeAndActive(PromocodeType type, boolean active);

    Promocode save(Promocode promocode);
}
