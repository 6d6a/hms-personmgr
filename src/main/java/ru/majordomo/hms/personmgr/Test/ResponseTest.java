package ru.majordomo.hms.personmgr.Test;

import ru.majordomo.hms.personmgr.models.Response;

public class ResponseTest {

    public static void main(String[] args)
    {
        String testOperationIdentity = "1";
        String testContent = "Test Content";
        Response testResponse = new Response(testOperationIdentity, testContent);
        System.out.println("Created class Response");
        System.out.println("Response.getId: " + testResponse.getOperationIdentity());
        System.out.println("Response.getContent: " + testResponse.getContent());
    }
}
