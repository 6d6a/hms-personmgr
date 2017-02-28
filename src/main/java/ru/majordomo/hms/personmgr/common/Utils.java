package ru.majordomo.hms.personmgr.common;

import java.util.LinkedList;
import java.util.List;

/**
 * Utils
 */
public class Utils {
    public static String pluralizef(String form1, String form2, String form3, Integer number)
    {
        List<String> messages = new LinkedList<>();
        messages.add(form1);
        messages.add(form2);
        messages.add(form3);

        Integer index = number % 10 == 1 && number % 100 != 11 ? 0 : (number % 10 >= 2 && number % 10 <= 4 && (number % 100 < 10 || number % 100 >= 20) ? 1 : 2);

        return String.format(messages.get(index), number);
    }
}
