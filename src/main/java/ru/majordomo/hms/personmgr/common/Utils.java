package ru.majordomo.hms.personmgr.common;

import ru.majordomo.hms.personmgr.exception.ParameterValidationException;

import java.io.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

import javax.servlet.http.HttpServletRequest;

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
            return Long.compare(x, y);
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


    /**
     * get different in days between two dates
     *
     * @param startDate
     * @param endDate
     * @return
     */
    public static int getDifferentInDaysBetweenDates(LocalDate startDate, LocalDate endDate) {
        return ((Long) ChronoUnit.DAYS.between(startDate, endDate)).intValue();
    }

    public static void checkRequiredParams(Map<String, ?> params, Set<String> requiredParams) {
        for (String field : requiredParams) {
            if (params.get(field) == null) {
                throw new ParameterValidationException("В запросе не передан обязательный параметр '" + field + "'");
            }
        }
    }

    public static String convertToUTF8(String input, String charsetName) throws UnsupportedEncodingException {
        return new String(input.getBytes(charsetName), "UTF-8");
    }

    public static void saveByteArrayToFile(byte[] bytes, File file) throws Exception {
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(bytes);
        fos.close();
    }

    public static ByteArrayOutputStream convertFileToByteArrayOutputStream(File file) {

        InputStream inputStream = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {

            inputStream = new FileInputStream(file);
            byte[] buffer = new byte[1024];
            baos = new ByteArrayOutputStream();

            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return baos;
    }

    public static ByteArrayOutputStream convertFileToByteArrayOutputStream(String fileName) {
        return convertFileToByteArrayOutputStream(new File(fileName));
    }

    public static String getMonthName(int monthNumber) {
        List<String> monthNames = new ArrayList<>(Arrays.asList("января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря"));

        monthNumber = monthNumber - 1;

        if (monthNames.get(monthNumber) != null) {
            return monthNames.get(monthNumber);
        }

        return String.valueOf(monthNumber);
    }
}
