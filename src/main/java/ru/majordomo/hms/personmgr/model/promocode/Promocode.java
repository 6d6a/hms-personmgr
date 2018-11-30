package ru.majordomo.hms.personmgr.model.promocode;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;

import ru.majordomo.hms.personmgr.common.PromocodeType;
import ru.majordomo.hms.personmgr.model.BaseModel;
import ru.majordomo.hms.personmgr.validation.ObjectId;

@Data
@EqualsAndHashCode(callSuper = true)
@Document
public class Promocode extends BaseModel {
    @NotNull
    @Indexed
    private PromocodeType type;

    @NotNull
    @Indexed(unique = true)
    private String code;

    @NotNull
    private LocalDate createdDate;

    private LocalDate usedDate;

    private LocalDate expired;

    @NotNull
    @Indexed
    private boolean active;

    private List<String> actionIds = new ArrayList<>();

    @Transient
    private List<PromocodeAction> actions = new ArrayList<>();

    @ObjectId(PromocodeTag.class)
    private String tagId;

    @Transient
    private PromocodeTag tag;
}
