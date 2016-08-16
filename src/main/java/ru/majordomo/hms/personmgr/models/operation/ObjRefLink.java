//package ru.majordomo.hms.personmgr.models.operation;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.redis.core.BoundListOperations;
//import org.springframework.data.redis.core.RedisTemplate;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//
//public class ObjRefLink {
//
//    @Autowired
//    private RedisTemplate<String> redisTemplate;
//
//    private String serviceLink;
//    private List<String> resourceLinks;
//    private BoundListOperations<String, String> listOperations;
//
//    public ObjRefLink(String serviceLink) {
//        this.serviceLink = serviceLink;
//        listOperations = redisTemplate.boundListOps(serviceLink);
//    }
//
//    public List<String> getResourceLinks() {
//        this.resourceLinks = (List<String>) listOperations.range(0, listOperations.size());
//        return resourceLinks;
//    }
//
//    public void addLink(String resourceLink) {
//        this.resourceLinks.add(resourceLink);
//        listOperations.rightPush(resourceLink)
//    }
//}
