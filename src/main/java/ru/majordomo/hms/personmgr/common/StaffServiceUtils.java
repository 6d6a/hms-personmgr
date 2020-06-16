package ru.majordomo.hms.personmgr.common;

import org.apache.commons.lang.StringUtils;
import ru.majordomo.hms.rc.staff.resources.Service;
import ru.majordomo.hms.rc.staff.resources.socket.NetworkSocket;
import ru.majordomo.hms.rc.staff.resources.template.ApplicationServer;
import ru.majordomo.hms.rc.staff.resources.template.HttpServer;
import ru.majordomo.hms.rc.staff.resources.template.Template;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public class StaffServiceUtils {

    public static boolean equivalent(@Nullable Language pmLanguage, @Nullable ApplicationServer.Language staffLanguage) {
        return  pmLanguage != null && staffLanguage != null && pmLanguage.name().equals(staffLanguage.name());
    }

    public static boolean isSuitableTemplate(@Nullable Template template, @Nullable Language language) {
        if (template == null) {
            return false;
        }

        return (template instanceof ApplicationServer && equivalent(language, ((ApplicationServer) template).getLanguage()))
                || (template instanceof HttpServer && language == Language.STATIC);
    }

    public static boolean isSuitableTemplate(@Nullable Template template, @Nullable Language language, String requiredVersion) {
        return isSuitableTemplate(template, language) && (language == Language.STATIC
                || Utils.isSuitableVersion(requiredVersion, ((ApplicationServer) template).getVersion()));
    }

    public static boolean isSuitableTemplate(@Nullable Template template, @Nullable Map<Language, List<String>> requiredLanguageVersions) {
        if (template == null || requiredLanguageVersions == null) {
            return false;
        }
        for (Map.Entry<Language, List<String>> oneReqLang : requiredLanguageVersions.entrySet()) {
            if (isSuitableTemplate(template, oneReqLang.getKey())) {
                if (oneReqLang.getKey() == Language.STATIC) {
                    return true;
                }
                String version = ((ApplicationServer) template).getVersion();
                for (String requiredVersion : oneReqLang.getValue()) {
                    if (Utils.isSuitableVersion(requiredVersion, version)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Вернет первый ip4 адрес сервиса
     * @param staffService сервис
     * @return ip адрес или пустую строку
     */
    @Nonnull
    public static String getFirstIpAddress(@Nullable Service staffService) {
        if (staffService == null || staffService.getSockets() == null) {
            return "";
        }
        return staffService.getSockets().stream()
                .filter(socket -> socket instanceof NetworkSocket && socket.switchedOn && StringUtils.isNotEmpty(((NetworkSocket) socket).getAddressAsString()))
                .map(socket -> ((NetworkSocket) socket).getAddressAsString())
                .findFirst().orElse("");
    }

    /**
     * Вернет первый ip4 адрес сервиса вебсервера
     * @param services список сервисов
     * @return ip адрес или пустую строку
     */
    @Nonnull
    public static String getFirstNginxIpAddress(@Nullable List<Service> services) {
        if (services == null) {
            return "";
        }
        return services.stream()
                .filter(service -> service != null && service.isSwitchedOn() && service.getTemplate() instanceof HttpServer && service.getSockets() != null)
                .flatMap(service -> service.getSockets().stream())
                .filter(socket -> socket instanceof NetworkSocket && socket.switchedOn && StringUtils.isNotEmpty(((NetworkSocket) socket).getAddressAsString()))
                .map(socket -> ((NetworkSocket) socket).getAddressAsString())
                .findFirst().orElse("");
    }
}
