package ru.majordomo.hms.personmgr.common;

import ru.majordomo.hms.personmgr.exception.ParameterValidationException;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class Utils {
    public static String formatBigDecimal(BigDecimal value) {
        DecimalFormatSymbols russianDecimalSymbols = new DecimalFormatSymbols(Locale.forLanguageTag("ru"));
        russianDecimalSymbols.setDecimalSeparator(',');
        russianDecimalSymbols.setGroupingSeparator(' ');

        DecimalFormat df = new DecimalFormat("###.##", russianDecimalSymbols);

        return df.format(value);
    }

    public static String formatBigDecimalWithCurrency(BigDecimal value) {
        return formatBigDecimal(value) + " руб.";
    }

    public static String pluralizef(String form1, String form2, String form3, Integer number) {
        List<String> messages = new LinkedList<>();
        messages.add(form1);
        messages.add(form2);
        messages.add(form3);

        Integer index = number % 10 == 1 && number % 100 != 11 ? 0 : (number % 10 >= 2 && number % 10 <= 4 && (number % 100 < 10 || number % 100 >= 20) ? 1 : 2);

        return String.format(messages.get(index), number);
    }

    public static <E extends Enum<E>> boolean isInEnum(String value, Class<E> enumClass) {
        for (E e : enumClass.getEnumConstants()) {
            if (e.name().equals(value)) {
                return true;
            }
        }
        return false;
    }

    public static int planChangeComparator(long x, long y) {
        if (x < -1L || y < -1L) {
            throw new ParameterValidationException("Found not positive Long value, can not compare.");
        }

        if (x != -1L && y == -1L) {
            return -1;
        }
        else if (x == -1L && y != -1L) {
            return 1;
        } else {
            return (x < y) ? -1 : ((x == y) ? 0 : 1);
        }
    }
}
