package ru.majordomo.hms.personmgr.models;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class OperationState {

    private final String STATE_NEW = "new";
    private final String STATE_SUCCESS = "success";
    private final String STATE_ERROR = "error";

    private String operationIdentity;
    private String state;
    private RedisTemplate<String, String> myRedisTemplate;

    @Autowired
    public OperationState(RedisTemplate<String, String> redisTemplate, @Value("") String operationIdentity) {
        this.myRedisTemplate = redisTemplate;
        this.operationIdentity = operationIdentity;
    }

    public void newOperation() {
        BoundValueOperations<String, String> keyMap = myRedisTemplate.boundValueOps(operationIdentity);
        keyMap.set(STATE_NEW);
    }

    public void successOperation() {
        BoundValueOperations<String, String> keyMap = myRedisTemplate.boundValueOps(operationIdentity);
        keyMap.set(STATE_SUCCESS);
    }

    public void errorOperation() {
        BoundValueOperations<String, String> keyMap = myRedisTemplate.boundValueOps(operationIdentity);
        keyMap.set(STATE_ERROR);
    }

    public String fetchOperationState() {
        BoundValueOperations<String, String> keyMap = myRedisTemplate.boundValueOps(operationIdentity);
        return keyMap.get();
    }

    public String getOperationIdentity() {
        return operationIdentity;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}
