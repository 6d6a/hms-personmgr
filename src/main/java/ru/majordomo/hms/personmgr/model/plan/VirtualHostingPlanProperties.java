package ru.majordomo.hms.personmgr.model.plan;

import java.util.*;

import javax.validation.constraints.NotNull;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.majordomo.hms.personmgr.common.DBType;
import ru.majordomo.hms.personmgr.common.Language;
import ru.majordomo.hms.personmgr.common.ResourceType;

@EqualsAndHashCode(callSuper = true)
@Data
public class VirtualHostingPlanProperties extends PlanProperties {
    @NotNull
    private PlanPropertyLimit sitesLimit = new PlanPropertyLimit();

    @NotNull
    private PlanPropertyLimit webCpuLimit = new PlanPropertyLimit();

    @NotNull
    private PlanPropertyLimit dbCpuLimit = new PlanPropertyLimit();

    @NotNull
    private PlanPropertyLimit quotaKBLimit = new PlanPropertyLimit();

    @NotNull
    private PlanPropertyLimit ftpLimit = new PlanPropertyLimit();

    @NotNull
    private PlanPropertyLimit sshLimit = new PlanPropertyLimit();

    @NotNull
    private boolean businessServices = false;

    @NotNull
    private Map<DBType, PlanPropertyLimit> db = new HashMap<>();

    @Deprecated
    private Map<ResourceType, Set<String>> allowedServiceTypes = new HashMap<>();

    @Deprecated
    public Set<String> getWebSiteAllowedServiceTypes() {
        return allowedServiceTypes.getOrDefault(ResourceType.WEB_SITE, new HashSet<>());
    }

    /**
     * Список разрешенных языков и их версий для тарифного плана
     * value - * любая версия
     */
    private Map<Language, List<String>> allowedLanguages = new HashMap<>();
}
