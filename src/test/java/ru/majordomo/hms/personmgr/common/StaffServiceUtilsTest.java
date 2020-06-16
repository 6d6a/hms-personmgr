package ru.majordomo.hms.personmgr.common;

import org.junit.Before;
import org.junit.Test;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import ru.majordomo.hms.rc.staff.resources.Service;
import ru.majordomo.hms.rc.staff.resources.socket.NetworkSocket;
import ru.majordomo.hms.rc.staff.resources.socket.UnixSocket;
import ru.majordomo.hms.rc.staff.resources.template.ApplicationServer;
import ru.majordomo.hms.rc.staff.resources.template.DatabaseServer;
import ru.majordomo.hms.rc.staff.resources.template.HttpServer;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class StaffServiceUtilsTest extends StaffServiceUtils {
    private ApplicationServer applicationServer;
    private HttpServer httpServer;
    private DatabaseServer databaseServer;
    private NetworkSocket networkSocket;
    private NetworkSocket networkSocketOff;
    private UnixSocket unixSocket;

    @Before
    public void setUp() {
        applicationServer = new ApplicationServer();
        applicationServer.setLanguage(ApplicationServer.Language.PHP);
        applicationServer.setVersion("7.4");
        httpServer = new HttpServer();
        databaseServer = new DatabaseServer();
        networkSocket = new NetworkSocket();
        networkSocket.setAddress("8.8.8.8");
        networkSocket.setSwitchedOn(true);
        networkSocketOff = new NetworkSocket();
        networkSocketOff.setAddress("8.8.4.4");
        networkSocketOff.setSwitchedOn(false);
        unixSocket = new UnixSocket();
        unixSocket.setSwitchedOn(true);
    }

    @Test
    public void equivalentTest() {
        assertTrue(equivalent(Language.PHP, ApplicationServer.Language.PHP));
        assertFalse(equivalent(null, null));
        assertFalse(equivalent(Language.STATIC, ApplicationServer.Language.JAVASCRIPT));
    }

    @Test
    public void isSuitableTemplateSimpleTest() {
        assertTrue(isSuitableTemplate(applicationServer, Language.PHP, "7.4"));
        assertTrue(isSuitableTemplate(applicationServer, Language.PHP, "*"));
        assertTrue(isSuitableTemplate(httpServer, Language.STATIC, null));
        assertTrue(isSuitableTemplate(httpServer, Language.STATIC, "any text"));

        assertFalse(isSuitableTemplate(applicationServer, Language.PERL, "*"));
        assertFalse(isSuitableTemplate(applicationServer, Language.PHP, "7.3"));
        assertFalse(isSuitableTemplate(applicationServer, Language.STATIC, "7.4"));
        assertFalse(isSuitableTemplate(httpServer, Language.PHP, "7.3"));
        assertFalse(isSuitableTemplate(null, Language.PHP, "7.4"));
        assertFalse(isSuitableTemplate(applicationServer, Language.PHP, null));
        assertFalse(isSuitableTemplate(applicationServer, null, "7.4"));
        assertFalse(isSuitableTemplate(databaseServer, Language.STATIC, null));
    }

    @Test
    public void isSuitableTemplateListPhp() {
        Map<Language, List<String>> allowPhp = CollectionUtils.unmodifiableMultiValueMap(new LinkedMultiValueMap<Language, String>() {{
            add(Language.PHP, "*");
        }});
        Map<Language, List<String>> allowPhpRightVersion = CollectionUtils.unmodifiableMultiValueMap(new LinkedMultiValueMap<Language, String>() {{
            add(Language.PHP, "7.4");
        }});
        Map<Language, List<String>> allowPhpWrongVersion = CollectionUtils.unmodifiableMultiValueMap(new LinkedMultiValueMap<Language, String>() {{
            add(Language.PHP, "7.3");
        }});
        Map<Language, List<String>> allowPhpTwoVersion = CollectionUtils.unmodifiableMultiValueMap(new LinkedMultiValueMap<Language, String>() {{
            add(Language.PHP, "7.3");
            add(Language.PHP, "7.4");
        }});
        Map<Language, List<String>> allowThreeVersion = CollectionUtils.unmodifiableMultiValueMap(new LinkedMultiValueMap<Language, String>() {{
            add(Language.PHP, "7.3");
            add(Language.PHP, "7.4");
            add(Language.STATIC, "");
        }});
        Map<Language, List<String>> allowStatic = CollectionUtils.unmodifiableMultiValueMap(new LinkedMultiValueMap<Language, String>() {{
            add(Language.STATIC, "");
        }});
        Map<Language, List<String>> allowStaticNull = CollectionUtils.unmodifiableMultiValueMap(new LinkedMultiValueMap<Language, String>() {{
            add(Language.STATIC, null);
        }});

        assertTrue(isSuitableTemplate(applicationServer, allowPhp));
        assertTrue(isSuitableTemplate(applicationServer, allowPhpRightVersion));
        assertTrue(isSuitableTemplate(applicationServer, allowPhpTwoVersion));
        assertTrue(isSuitableTemplate(applicationServer, allowThreeVersion));
        assertTrue(isSuitableTemplate(httpServer, allowThreeVersion));
        assertTrue(isSuitableTemplate(httpServer, allowStatic));
        assertTrue(isSuitableTemplate(httpServer, allowStaticNull));

        assertFalse(isSuitableTemplate(null, allowPhp));
        assertFalse(isSuitableTemplate(applicationServer, allowPhpWrongVersion));
        assertFalse(isSuitableTemplate(applicationServer, (Map<Language, List<String>>) null));
        assertFalse(isSuitableTemplate(databaseServer, allowPhp));
        assertFalse(isSuitableTemplate(httpServer, allowPhp));
        assertFalse(isSuitableTemplate(applicationServer, allowStatic));
        assertFalse(isSuitableTemplate(applicationServer, allowStaticNull));
        assertFalse(isSuitableTemplate(databaseServer, allowStatic));
        assertFalse(isSuitableTemplate(httpServer, (Map<Language, List<String>>) null));
    }
    @Test
    public void getFirstNginxIpAddressTest() {
        NetworkSocket networkSocketWrong = new NetworkSocket();
        networkSocketWrong.setSwitchedOn(true);
        networkSocketWrong.setAddress("1.1.1.1");

        Service php74;
        php74 = new Service();
        applicationServer = new ApplicationServer();
        php74.setId("7.4");
        php74.setSwitchedOn(true);
        php74.setAccountId(null);
        php74.addInstanceProp(ApplicationServer.Spec.SECURITY_LEVEL, ApplicationServer.Security.DEFAULT);
        php74.setTemplate(applicationServer);
        applicationServer.setVersion("7.4");
        applicationServer.setLanguage(ApplicationServer.Language.PHP);
        php74.addSocket(unixSocket);
        php74.addSocket(networkSocketOff);
        php74.addSocket(networkSocketWrong);

        Service httpOff;
        httpOff = new Service();
        httpOff.setSwitchedOn(false);
        httpOff.setTemplate(new HttpServer());
        httpOff.addSocket(unixSocket);
        httpOff.addSocket(networkSocketOff);
        httpOff.addSocket(networkSocketWrong);

        Service http;
        http = new Service();
        http.setSwitchedOn(true);
        http.setTemplate(new HttpServer());
        http.addSocket(unixSocket);
        http.addSocket(networkSocketOff);
        http.addSocket(networkSocket);

        assertEquals(StaffServiceUtils.getFirstNginxIpAddress(Arrays.asList(php74, httpOff, http)), "8.8.8.8");
        assertEquals(StaffServiceUtils.getFirstNginxIpAddress(Arrays.asList(php74, httpOff)), "");
        assertEquals(StaffServiceUtils.getFirstNginxIpAddress(null), "");

    }

    @Test
    public void getFirstIpAddressTest() {
        Service http;
        http = new Service();
        http.setSwitchedOn(true);
        http.setTemplate(new HttpServer());
        http.addSocket(unixSocket);
        http.addSocket(networkSocketOff);
        http.addSocket(networkSocket);

        assertEquals(StaffServiceUtils.getFirstIpAddress(http), "8.8.8.8");
        http.getSockets().remove(networkSocket);
        assertEquals(StaffServiceUtils.getFirstIpAddress(http), "");
        assertEquals(StaffServiceUtils.getFirstIpAddress(null), "");
    }
}
