package ru.majordomo.hms.personmgr.service.promocode;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.common.PromocodeType;

@Slf4j
@Service
public class PromocodeProcessorFactory {
    private final BonusPmPromocodeProcessor bonusPromocodeProcessor;

    @Autowired
    public PromocodeProcessorFactory(BonusPmPromocodeProcessor bonusPromocodeProcessor) {
        this.bonusPromocodeProcessor = bonusPromocodeProcessor;
    }

    public PmPromocodeProcessor getProcessor(PromocodeType type) {
        switch (type) {
            case BONUS:
                return bonusPromocodeProcessor;

            case GOOGLE:
                return new GoogleDummyProcessorPm();

            case PARTNER:
            default:
                return null;
        }
    }
}
