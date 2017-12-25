package ru.majordomo.hms.personmgr.service.converter;

import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class EmptyConverter implements Converter{

    @Override
    public File convert(File file){
        return file;
    }
}
