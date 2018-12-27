package ru.majordomo.hms.personmgr.service.promocode;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.common.PromocodeType;

@Slf4j
@Service
public class PromocodeProcessorFactory {
    private final BonusPromocodeProcessor bonusPromocodeProcessor;

    @Autowired
    public PromocodeProcessorFactory(BonusPromocodeProcessor bonusPromocodeProcessor) {
        this.bonusPromocodeProcessor = bonusPromocodeProcessor;
    }

    public PromocodeProcessor getProcessor(PromocodeType type) {
        switch (type) {
            case BONUS:
                return bonusPromocodeProcessor;

            case GOOGLE:
            case PARTNER:
            default:
                return new AlwaysErrorPromocodeProcessor();
        }
    }
}
