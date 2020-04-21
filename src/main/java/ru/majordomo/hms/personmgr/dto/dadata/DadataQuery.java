package ru.majordomo.hms.personmgr.dto.dadata;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DadataQuery {
    public DadataQuery(String query) {
        this.query = query;
    }

    private String query;
    private String type = null;
}
