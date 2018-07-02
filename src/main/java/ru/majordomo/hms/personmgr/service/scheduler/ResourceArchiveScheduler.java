package ru.majordomo.hms.personmgr.service.scheduler;

import net.javacrumbs.shedlock.core.SchedulerLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

import ru.majordomo.hms.personmgr.service.RcUserFeignClient;
import ru.majordomo.hms.personmgr.service.ResourceArchiveService;
import ru.majordomo.hms.rc.user.resources.ResourceArchive;

@Component
public class ResourceArchiveScheduler {
    private final static Logger logger = LoggerFactory.getLogger(ResourceArchiveScheduler.class);

    private final RcUserFeignClient rcUserFeignClient;
    private final ResourceArchiveService resourceArchiveService;

    @Autowired
    public ResourceArchiveScheduler(
            RcUserFeignClient rcUserFeignClient,
            ResourceArchiveService resourceArchiveService
    ) {
        this.rcUserFeignClient = rcUserFeignClient;
        this.resourceArchiveService = resourceArchiveService;
    }

    @SchedulerLock(name="processResourceArchives")
    public void processResourceArchives() {
        logger.info("Started processResourceArchives");
        try {
            List<ResourceArchive> resourceArchives = (List<ResourceArchive>) rcUserFeignClient.getResourceArchives();

            resourceArchives.stream()
                    .filter(resourceArchive -> !resourceArchive.isWillBeDeleted()
                            && resourceArchive.getCreatedAt().isBefore(LocalDateTime.now().minusDays(1)))
                    .forEach(resourceArchiveService::processResourceArchiveClean)
            ;
        } catch (Exception e) {
            e.printStackTrace();
        }
        logger.info("Ended processResourceArchives");
    }
}
