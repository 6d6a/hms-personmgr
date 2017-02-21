package ru.majordomo.hms.personmgr.common;


import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
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
}
