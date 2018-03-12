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
    private String activeRequestsPerHour;

    @JsonProperty("active_requests_per_day")
    private String activeRequestsPerDay;

    @JsonProperty("max_requests_per_hour")
    private String maxRequestsPerHour;

    @JsonProperty("max_requests_per_day")
    private String maxRequestsPerDay;

    @JsonProperty("queued")
    private String queued;

    @JsonProperty("queue_length")
    private String queueLength;

    @JsonProperty("tasks_in_progress")
    private String tasksInProgress;

    @JsonProperty("key_expiration")
    private String keyExpiration;

    @JsonProperty("time_based")
    private String timeBased;
}
