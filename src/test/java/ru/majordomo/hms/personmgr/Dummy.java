package ru.majordomo.hms.personmgr;

import com.google.common.net.InternetDomainName;
import org.junit.Test;

import java.math.BigDecimal;
import java.net.IDN;

public class Dummy {
    @Test
    public void fuck() throws Exception {
        BigDecimal amount = new BigDecimal(29.9987);
        System.out.println(amount);
        System.out.println(amount.abs());
    }
}
