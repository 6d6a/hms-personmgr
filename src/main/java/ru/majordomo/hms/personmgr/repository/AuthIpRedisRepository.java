package ru.majordomo.hms.personmgr.repository;


import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.majordomo.hms.personmgr.model.account.AuthIPRedis;

@Repository
public interface AuthIpRedisRepository extends CrudRepository<AuthIPRedis, String> {
}
