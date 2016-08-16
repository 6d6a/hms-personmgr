package ru.majordomo.hms.personmgr.models.operation;

import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Component
public class Operation {

    private final String STATE_NEW = "new";
    private final String STATE_SUCCESS = "success";
    private final String STATE_ERROR = "error";

    private String operationIdentity;

    private ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public String getOperationIdentity() {
        return operationIdentity;
    }

    public void setOperationIdentity(String operationIdentity) {
        this.operationIdentity = operationIdentity;
        redisTemplate.setHashValueSerializer(new StringRedisSerializer());
    }

    public void newOperation(ArrayList<String> requiredFeedback) {
        dropOperation();
        for (String T : requiredFeedback) {
            redisTemplate.opsForHash().put(operationIdentity, KeyBuilder.statify(T), STATE_NEW);
        }
    }

    public void successOperation(String service) {
        String key = KeyBuilder.statify(service);
        redisTemplate.opsForHash().delete(operationIdentity, key);
        redisTemplate.opsForHash().put(operationIdentity, key, STATE_SUCCESS);
    }

    public Boolean isSuccess() {
        Map<Object, Object> requiredFeedback = KeyBuilder.getStateFields(fetchOperation());
        return (! (
                requiredFeedback.containsValue(STATE_NEW) ||
                        requiredFeedback.containsValue(STATE_ERROR)
        ));
    }

    public Boolean isInProgress() {
        Map<Object, Object> requiredFeedback = KeyBuilder.getStateFields(fetchOperation());
        return (
                requiredFeedback.containsValue(STATE_NEW) &&
                        ! requiredFeedback.containsValue(STATE_ERROR)
        );
    }

    public void errorOperation(String service, String cause) {
        redisTemplate.opsForHash().delete(operationIdentity, service);
        redisTemplate.opsForHash().put(operationIdentity, service, STATE_ERROR);
        redisTemplate.opsForHash().put(operationIdentity, service, cause);
    }

    public void setParams(String service, Map<Object, Object> params) {
        String key = KeyBuilder.paramify(service);
        redisTemplate.opsForHash().delete(operationIdentity, key);
        try {
            redisTemplate.opsForHash().put(operationIdentity, key, mapper.writeValueAsString(params));
        } catch (IOException e) {
            errorOperation(service, e.getMessage());
        }
    }

    public HashMap getParams(String service) {
        String paramsString = redisTemplate.opsForHash().get(operationIdentity, KeyBuilder.paramify(service)).toString();
        HashMap message = new HashMap();
        try {
            message = mapper.readValue(paramsString, HashMap.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return message;
    }

    public Map<Object, Object> fetchOperation() {
        return redisTemplate.opsForHash().entries(operationIdentity);
    }

    private void dropOperation() {
        redisTemplate.delete(operationIdentity);
    }

    public String getReport() {
        String message;
        Map<Object, Object> params = new HashMap<>();
        params.put("fin.params", getParams("fin"));
        params.put("rc.params", getParams("rc"));
        try {
            message = mapper.writeValueAsString(params);
        } catch (IOException e) {
            message = e.getMessage();
        }
        return message;
    }
}
