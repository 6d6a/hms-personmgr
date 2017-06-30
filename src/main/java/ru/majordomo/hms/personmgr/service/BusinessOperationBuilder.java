package ru.majordomo.hms.personmgr.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ru.majordomo.hms.personmgr.common.BusinessOperationType;
import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessOperation;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessOperationRepository;

@Service
public class BusinessOperationBuilder {
    private final ProcessingBusinessOperationRepository operationRepository;

    @Autowired
    public BusinessOperationBuilder(ProcessingBusinessOperationRepository operationRepository) {
        this.operationRepository = operationRepository;
    }

    public ProcessingBusinessOperation build(BusinessOperationType operationType, SimpleServiceMessage message) {
        ProcessingBusinessOperation operation = new ProcessingBusinessOperation();

        operation.setPersonalAccountId(message.getAccountId());
        operation.setState(State.PROCESSING);
        operation.setParams(message.getParams());
        operation.setType(operationType);

        String nameInParams = (String) message.getParam("name");
        if (nameInParams != null) {
            operation.addPublicParam("name", nameInParams);
        }

        operationRepository.save(operation);

        return operation;
    }
}
