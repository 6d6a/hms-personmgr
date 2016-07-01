package ru.majordomo.hms.personmgr.Test;
import org.springframework.web.client.RestTemplate;
import ru.majordomo.hms.personmgr.Test.helpers.RestConsumer;

public class RestTest {

    public static void main(String[] args)
    {
        RestTemplate restTemplate = new RestTemplate();
        RestConsumer restConsumer = restTemplate.getForObject("http://127.0.0.1:8080/account/12/ftp/create?operationIdentity=123", RestConsumer.class);
        System.out.println("Reject: " + restConsumer.toString());
        System.out.println("Reject getOperationIdentity(): " + restConsumer.getOperationIdentity());
        System.out.println("Reject getContent(): " + restConsumer.getContent());
        restConsumer = restTemplate.getForObject("http://127.0.0.1:8080/account/12/ftp/create?name=ololo&operationIdentity=123", RestConsumer.class);
        System.out.println("Accept: " + restConsumer.toString());
        System.out.println("Accept getOperationIdentity(): " + restConsumer.getOperationIdentity());
        System.out.println("Accept getContent(): " + restConsumer.getContent());
    }
}
