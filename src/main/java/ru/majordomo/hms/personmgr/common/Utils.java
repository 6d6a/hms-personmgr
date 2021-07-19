package ru.majordomo.hms.personmgr.common;

import org.apache.commons.lang.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.rc.staff.resources.Service;
import ru.majordomo.hms.rc.user.resources.*;

import java.io.*;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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

        int index = getFormIndex(number);

        return String.format(messages.get(index), number);
    }

    public static String pluralizeDays(Integer days) {
        return pluralizef("%d день", "%d дня", "%d дней", days);
    }

    public static String currencyValue(BigDecimal bigDecimal) {
        DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols();
        decimalFormatSymbols.setDecimalSeparator('.');
        decimalFormatSymbols.setGroupingSeparator(' ');

        List<String> messages = new LinkedList<>();

        messages.add("#0.00 рубль");
        messages.add("#0.00 рубля");
        messages.add("#0.00 рублей");

        int index = getFormIndex(bigDecimal.intValue());

        return new DecimalFormat(messages.get(index), decimalFormatSymbols).format(bigDecimal);
    }

    private static int getFormIndex(Integer number) {
        return number % 10 == 1 && number % 100 != 11 ? 0 : (number % 10 >= 2 && number % 10 <= 4 && (number % 100 < 10 || number % 100 >= 20) ? 1 : 2);
    }

    public static <E extends Enum<E>> boolean isInEnum(String value, Class<E> enumClass) {
        for (E e : enumClass.getEnumConstants()) {
            if (e.name().equals(value)) {
                return true;
            }
        }
        return false;
    }

    public static int planLimitsComparator(long x, long y) {
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

    @Nonnull
    public static String humanizeResourceType(@Nullable Class<?> resourceClass) {
        if (resourceClass == null) {
            return "";
        } else if (resourceClass == WebSite.class) {
            return UserConstants.WEB_SITE;
        } else if (resourceClass == Database.class) {
            return UserConstants.DATABASE;
        } else if (resourceClass == SSLCertificate.class) {
            return UserConstants.SSL_CERTIFICATE;
        } else if (resourceClass == DatabaseUser.class) {
            return UserConstants.DATABASE_USER;
        } else if (resourceClass == Redirect.class) {
            return UserConstants.REDIRECT;
        } else if (resourceClass == ResourceArchive.class) {
            return UserConstants.RESOURCE_ARCHIVE;
        } else if (resourceClass == Domain.class) {
            return UserConstants.DOMAIN;
        } else if (resourceClass == Person.class) {
            return UserConstants.PERSON;
        } else if (resourceClass == UnixAccount.class) {
            return UserConstants.UNIX_ACCOUNT;
        } else if (resourceClass == FTPUser.class) {
            return UserConstants.FTP_USER;
        } else if (resourceClass == Mailbox.class) {
            return UserConstants.MAILBOX;
        } else if (resourceClass == Service.class) {
            return UserConstants.DEDICATED_APP_SERVICE;
        } else {
            return UserConstants.UNKNOWN_RESOURCE;
        }
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
    public static int differenceInDays(LocalDate startDate, LocalDate endDate) {
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

    public static Boolean cleanBooleanSafe(Object booleanObject) {
        if (booleanObject == null) {
            return Boolean.FALSE;
        } else if (booleanObject instanceof String ) {
            return Boolean.valueOf((String) booleanObject);
        } else {
            return (Boolean) booleanObject;
        }
    }

    @Nullable
    public static String diffFieldsString(String fieldName, Object oldField, Object newField){
        String oldFieldString = oldField == null ? "" : oldField.toString();
        String newFieldString = newField == null ? "" : newField.toString();

        if (!oldFieldString.equals(newFieldString)) {
            return "'" + fieldName + "' с '" + oldFieldString + "' на '" + newFieldString + "'";
        }
        return null;
    }

    public static String joinStringsWithDelimeterExceptNullStrings(CharSequence delimiter, CharSequence... strings){
        StringJoiner joiner = new StringJoiner(delimiter);
        int i = 0;
        while (i < strings.length) {
            if (strings[i] != null && strings[i].length() > 0) {
                joiner.add(strings[i]);
            }
            i++;
        }
        return joiner.toString();
    }

    public static String humanizePeriod(Period period) {
        StringBuilder result = new StringBuilder();
        if (period.getYears() > 0) {
            result.append(Utils.pluralizef("%d год", "%d года", "%d лет", period.getYears()));
        }
        if (period.getMonths() > 0) {
            result.append(Utils.pluralizef("%d месяц", "%d месяца", "%d месяцев", period.getMonths()));
        }
        if (period.getDays() > 0) {
            result.append(Utils.pluralizef("%d день", "%d дня", "%d дней", period.getDays()));
        }
        return result.toString().isEmpty() ? period.toString() : result.toString();
    }

    public static Boolean isInsideInRootDir(String insideDir, String rootDir) {
        Path rootPath = Paths.get(rootDir);

        Path insidePath = rootPath.resolve(insideDir);

        return insidePath.normalize().startsWith(rootPath);
    }

    public static Map<String, String> buildAttachment(MultipartFile[] files) throws IOException {
        byte[] fileBytes;
        String fileType, fileName;

        if (files.length == 1 && !files[0].isEmpty()) {
            fileName = files[0].getOriginalFilename();
            fileBytes = files[0].getBytes();
            fileType = files[0].getContentType();
        } else {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ZipOutputStream zipOut = new ZipOutputStream(outputStream);
            for (MultipartFile oneFile : files) {
                InputStream inputStream = oneFile.getInputStream();
                ZipEntry zipEntry = new ZipEntry(oneFile.getOriginalFilename());
                zipOut.putNextEntry(zipEntry);

                byte[] bytes = new byte[1024];
                int length;
                while((length = inputStream.read(bytes)) >= 0) {
                    zipOut.write(bytes, 0, length);
                }
                inputStream.close();
            }
            zipOut.close();
            outputStream.close();
            fileBytes = outputStream.toByteArray();
            fileName = "attachment.zip";
            fileType = "application/zip";
        }

        if (fileBytes != null && fileType != null && fileName != null) {
            Map<String, String> attachment = new HashMap<>();
            attachment.put("body", Base64.getMimeEncoder().encodeToString(fileBytes));
            attachment.put("mime_type", fileType);
            attachment.put("filename", fileName);

            return attachment;
        }
        return null;
    }

    private final static Pattern CIDR_OR_IP_PATTERN = Pattern.compile("^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])(/([1-9]|[1-2]\\d|3[0-2]))?$");
    private final static Pattern CIDR_PATTERN = Pattern.compile("^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])/([1-9]|[1-2]\\d|3[0-2])$");

    public static boolean cidrOrIpValid(String cidrOrIp) {
        return CIDR_OR_IP_PATTERN.matcher(cidrOrIp).matches();
    }

    public static boolean cidrValid(String cidr) {
        return CIDR_PATTERN.matcher(cidr).matches();
    }

    /**
     * Метод проверяет подходит ли версия
     * @param requiredVersion - требуемая версия, * - любая
     * @param version - имеющаяся версия
     * @return результат
     */
    public static boolean isSuitableVersion(@Nullable String requiredVersion, @Nullable String version) {
        if ("*".equals(requiredVersion)) {
            return true;
        }
        if (StringUtils.isEmpty(requiredVersion) || StringUtils.isEmpty(version)) {
            return false;
        }
        return requiredVersion.equals(version);
    }
}
