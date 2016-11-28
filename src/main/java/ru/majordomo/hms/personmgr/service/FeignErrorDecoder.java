//package ru.majordomo.hms.personmgr.service;
//
//import feign.Response;
//import feign.codec.ErrorDecoder;
//
//import static feign.FeignException.errorStatus;
//
//public class FeignErrorDecoder implements ErrorDecoder {
//
//    @Override
//    public Exception decode(String methodKey, Response response) {
//        if (response.status() >= 400 && response.status() <= 499) {
//            return new StashClientException(
//                    response.status(),
//                    response.reason()
//            );
//        }
//        return errorStatus(methodKey, response);
//    }
//}