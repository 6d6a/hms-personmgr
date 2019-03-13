package ru.majordomo.hms.personmgr.manager;

import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.personmgr.model.BaseModel;

import java.util.List;

public interface EntityBuilder<T extends BaseModel> {
    void build(T entity) throws ResourceNotFoundException;
    T findById(String id) throws ResourceNotFoundException;
    List<T> findAll();
}
