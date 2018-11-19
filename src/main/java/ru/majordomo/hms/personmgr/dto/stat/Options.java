package ru.majordomo.hms.personmgr.dto.stat;

import lombok.Data;

import java.util.List;

@Data
public class Options {
    private String label;
    private List<String> values;
}
