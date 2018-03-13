package ru.majordomo.hms.personmgr.dto.revisium;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class GetStatResponse extends ApiResponse {

    @JsonProperty("balance")
    private String balance;

    @JsonProperty("active_requests_per_hour")
    private Integer activeRequestsPerHour;

    @JsonProperty("active_requests_per_day")
    private Integer activeRequestsPerDay;

    @JsonProperty("max_requests_per_hour")
    private Integer maxRequestsPerHour;

    @JsonProperty("max_requests_per_day")
    private Integer maxRequestsPerDay;

    @JsonProperty("queued")
    private Integer queued;

    @JsonProperty("queue_length")
    private Integer queueLength;

    @JsonProperty("tasks_in_progress")
    private Integer tasksInProgress;

    @JsonProperty("key_expiration")
    private String keyExpiration;

    @JsonProperty("time_based")
    private String timeBased;
}
