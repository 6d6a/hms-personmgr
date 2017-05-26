package ru.majordomo.hms.personmgr.common;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

public class Utils {

    private static PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();

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

    private static Boolean phoneValid(String phone) throws NumberParseException {
        Phonenumber.PhoneNumber phoneNumber;
        phoneNumber = phoneNumberUtil.parse(phone, "RU");

        return phoneNumberUtil.isValidNumber(phoneNumber);
    }

    public static boolean isPhoneValid(String phone) {
        try {
            return phoneValid(phone);
        } catch (NumberParseException e) {
            return false;
        }
    }

    /**
     * Get the IP address of client making the request.
     *
     * Uses the "x-forwarded-for" HTTP header if available, otherwise uses the remote
     * IP of requester.
     *
     * @param request <code>HttpServletRequest</code>
     * @return <code>String</code> IP address
     */
    public static String getClientIP(HttpServletRequest request) {
        final String xForwardedFor = request.getHeader("x-forwarded-for");
        String clientIP = null;
        if (xForwardedFor == null) {
            clientIP = request.getRemoteAddr();
        } else {
            clientIP = extractClientIpFromXForwardedFor(xForwardedFor);
        }
        return clientIP;
    }

    /**
     * Extract the client IP address from an x-forwarded-for header. Returns null if there is no x-forwarded-for header
     *
     * @param xForwardedFor a <code>String</code> value
     * @return a <code>String</code> value
     */
    public static String extractClientIpFromXForwardedFor(String xForwardedFor) {
        if (xForwardedFor == null) {
            return null;
        }
        xForwardedFor = xForwardedFor.trim();
        String tokenized[] = xForwardedFor.split(",");
        if (tokenized.length == 0) {
            return null;
        } else {
            return tokenized[0].trim();
        }
    }

    public static BigDecimal getBigDecimalFromUnexpectedInput(Object input) {

        BigDecimal bigDecimal;

        if (input instanceof Integer) {
            bigDecimal = BigDecimal.valueOf((Integer) input);
        } else if (input instanceof Long) {
            bigDecimal = BigDecimal.valueOf((Long) input);
        } else if (input instanceof Double) {
            bigDecimal = BigDecimal.valueOf((Double) input);
        } else if (input instanceof String) {
            bigDecimal = new BigDecimal((String) input);
        } else {
            try {
                bigDecimal = (BigDecimal) input;
            } catch (Exception e) {
                e.printStackTrace();
                throw new ParameterValidationException("Ошибка при попытке получить BigDecimal из входных данных");
            }
        }

        return bigDecimal;
    }
}
