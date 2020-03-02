package ru.majordomo.hms.personmgr.dto;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class ResultWithWarning extends ResultData<List<String>> {

    public ResultWithWarning() {
        setData(new ArrayList<>());
    }

    public ResultWithWarning addWarning(String warning) {
        if (StringUtils.isNotEmpty(warning)) {
            if (getData() == null) {
                setData(new ArrayList<>());
            }
            getData().add(warning);
        }
        return this;
    }

    public boolean hasWarning() {
        return CollectionUtils.isNotEmpty(getData());
    }

    public static ResultWithWarning success() {
        return new ResultWithWarning();
    }
}
