package ru.majordomo.hms.personmgr.common.message;


import java.util.Map;

/**
 * ImportMessageParams
 */
public class ImportMessageParams extends ServiceMessageParams {
    private Map<String, String> importValues;

    public Map<String, String> getImportValues() {
        return importValues;
    }

    public void setImportValues(Map<String, String> importValues) {
        this.importValues = importValues;
    }

    @Override
    public String toString() {
        return "ImportMessageParams{" +
                "importValues=" + importValues +
                "} " + super.toString();
    }
}
