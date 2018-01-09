package ru.majordomo.hms.personmgr.dto.rpc;

import java.util.Map;

public interface RpcResponse {
    void mapping(Map<?, ?> response);
}
