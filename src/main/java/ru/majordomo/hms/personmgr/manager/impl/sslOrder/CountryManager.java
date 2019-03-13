package ru.majordomo.hms.personmgr.manager.impl.sslOrder;

import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.personmgr.manager.EntityBuilder;
import ru.majordomo.hms.personmgr.model.order.ssl.Country;
import ru.majordomo.hms.personmgr.repository.CountryRepository;

import java.util.List;

@Component
@AllArgsConstructor
public class CountryManager implements EntityBuilder<Country> {
    private CountryRepository repository;

    @Override
    public void build(Country entity) {}

    @Cacheable("countryById")
    @Override
    public Country findById(String id) throws ResourceNotFoundException {
        return repository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Не найдена страна по id: " + id)
        );
    }

    @Cacheable("countryFindAll")
    @Override
    public List<Country> findAll() {
        return repository.findAll();
    }
}
