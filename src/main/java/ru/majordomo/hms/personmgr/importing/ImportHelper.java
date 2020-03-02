package ru.majordomo.hms.personmgr.importing;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.rc.user.resources.Resource;

import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class ImportHelper {
    private final ObjectMapper oMapper;

    protected SimpleServiceMessage makeServiceMessage(String accountId, String operationId, Resource resource) {
            SimpleServiceMessage message = new SimpleServiceMessage();
            message.setAccountId(accountId);
            message.setOperationIdentity(operationId);
            if (resource != null) {
                try {
                    @SuppressWarnings("unchecked")
                    HashMap<String, Object> params = oMapper.convertValue(resource, HashMap.class);

                    params.put("replaceOldResource", true);
                    message.setParams(params);
                } catch (ClassCastException ignore) { }
            }
            return message;
    }
}
