package ru.majordomo.hms.personmgr.service.converter;

import org.apache.commons.lang.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.common.FileType;

@Service
public class ConverterFactory {

    private final HtmlToPdfConverter htmlToPdfConverter;
    private final EmptyConverter emptyConverter;

    @Autowired
    public ConverterFactory(
            HtmlToPdfConverter htmlToPdfConverter,
            EmptyConverter emptyConverter
    ){
        this.htmlToPdfConverter = htmlToPdfConverter;
        this.emptyConverter = emptyConverter;
    }

    public Converter getConverter(FileType srcFormat, FileType dstFormat){
        if (dstFormat == null){
            return emptyConverter;
        }

        switch (srcFormat){
            case HTML:
                switch (dstFormat) {
                    case PDF:
                        return htmlToPdfConverter;
                    default:
                        throw new NotImplementedException();
                }
            default:
                throw new NotImplementedException();
        }
    }
}
