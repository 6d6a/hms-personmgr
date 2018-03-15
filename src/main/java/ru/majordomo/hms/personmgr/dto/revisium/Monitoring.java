package ru.majordomo.hms.personmgr.dto.revisium;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@JsonDeserialize(using = MonitoringCustomDeserializer.class)
public class Monitoring extends RawMonitoring {
}
