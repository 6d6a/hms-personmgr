package ru.majordomo.hms.personmgr.common;

import java.io.UnsupportedEncodingException;

import static org.apache.commons.codec.digest.DigestUtils.md5Hex;
import static org.apache.commons.codec.digest.DigestUtils.sha1;
import static org.apache.commons.codec.digest.DigestUtils.sha1Hex;

public class PasswordManager {
    public static String forMySQL5(String plainPassword) throws UnsupportedEncodingException {
        byte[] password = plainPassword.getBytes("UTF-8");

        return "*" + sha1Hex(sha1(password)).toUpperCase();
    }

    public static String forPostgres(String plainPassword) throws UnsupportedEncodingException {
        byte[] password = plainPassword.getBytes("UTF-8");
        return md5Hex(password);
    }
}


