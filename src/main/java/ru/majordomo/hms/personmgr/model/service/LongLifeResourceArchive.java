package ru.majordomo.hms.personmgr.model.service;

import com.querydsl.core.annotations.QueryTransient;

import javax.validation.constraints.NotBlank;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

import javax.validation.constraints.NotNull;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.majordomo.hms.personmgr.model.ModelBelongsToPersonalAccount;
import ru.majordomo.hms.rc.user.resources.ResourceArchive;
import ru.majordomo.hms.rc.user.resources.ResourceArchiveType;

@Document
@Data
@EqualsAndHashCode(callSuper = true)
@CompoundIndex(unique = true, def = "{personalAccountId : 1, resourceArchiveId : 1}")
public class LongLifeResourceArchive extends ModelBelongsToPersonalAccount {
    @NotNull
    private ResourceArchiveType type;
    @NotBlank
    private String archivedResourceId;
    @NotBlank
    private String resourceArchiveId;
    private String accountServiceId;
    private LocalDateTime created;
    private LocalDateTime accountServiceDeleted;
    @Transient
    @QueryTransient
    private ResourceArchive resourceArchive;
}
