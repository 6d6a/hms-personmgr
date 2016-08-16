package ru.majordomo.hms.personmgr.models.operation;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public abstract class KeyBuilder {

    public static final String STATE_SUBKEY = ".state";
    public static final String ERROR_SUBKEY = ".error";
    public static final String PARAMS_SUBKEY = ".parameters";


    public static String statify(String key) {
        key = key + STATE_SUBKEY;
        return key;
    }

    public static String destatify(String key) {
        if (key.contains(STATE_SUBKEY)) {
            key = StringUtils.delete(key, STATE_SUBKEY);
        }
        return key;
    }

    public static String paramify(String key) {
        key = key + PARAMS_SUBKEY;
        return key;
    }

    public static String deparamify(String key) {
        if (key.contains(PARAMS_SUBKEY)) {
            key = StringUtils.delete(key, PARAMS_SUBKEY);
        }
        return key;
    }

    public static String errorify(String key) {
        key = key + ERROR_SUBKEY;
        return key;
    }

    public static String deerrorify(String key) {
        if (key.contains(ERROR_SUBKEY)) {
            key = StringUtils.delete(key, ERROR_SUBKEY);
        }
        return key;
    }

    public static Map<Object, Object> getStateFields(Map<Object, Object> searchData) {
        HashMap<Object, Object> result = new HashMap<>();
        searchData.forEach((K, V) -> {
            if (StringUtils.trimAllWhitespace(K.toString()).endsWith(STATE_SUBKEY)) {
                result.put(K, V);
            }
        });
        return result;
    }

    public static Map<Object, Object> getParametersFields(Map<Object, Object> searchData) {
        HashMap<Object, Object> result = new HashMap<>();
        searchData.forEach((K, V) -> {
            if (StringUtils.trimAllWhitespace(K.toString()).endsWith(PARAMS_SUBKEY)) {
                result.put(K, V);
            }
        });
        return result;
    }

    public static Map<Object, Object> getErrors(Map<Object, Object> searchData) {
        Map<Object, Object> result = new HashMap<>();
        searchData.forEach((K, V) -> {
            if (StringUtils.trimAllWhitespace(K.toString()).endsWith(ERROR_SUBKEY)) {
                result.put(K, V);
            }
        });
        return result;
    }
}
