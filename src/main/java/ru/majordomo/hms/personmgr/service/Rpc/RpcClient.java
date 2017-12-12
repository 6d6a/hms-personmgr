package ru.majordomo.hms.personmgr.service.Rpc;

import org.apache.xmlrpc.XmlRpcException;

import java.util.List;

public interface RpcClient {
    Object callMethod(String methodName, List<?> params) throws XmlRpcException;
}
