package ru.majordomo.hms.personmgr.service.task.executor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.model.task.SendMailIfAbonementWasNotBought;
import ru.majordomo.hms.personmgr.model.task.Task;

@Service
@Slf4j
public class ExecutorFactory {
    private final AbonementWasNotBoughtExecutor abonementWasNotBoughtExecutor;

    @Autowired
    public ExecutorFactory(AbonementWasNotBoughtExecutor abonementWasNotBoughtExecutor) {
        this.abonementWasNotBoughtExecutor = abonementWasNotBoughtExecutor;
    }

    public <T extends Task> Executor getExecutor(Class<T> tClass) {

        if (tClass == SendMailIfAbonementWasNotBought.class) {
            return abonementWasNotBoughtExecutor;
        } else {
            return null;
        }
    }
}
