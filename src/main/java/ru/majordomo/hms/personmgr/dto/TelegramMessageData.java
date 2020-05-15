package ru.majordomo.hms.personmgr.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TelegramMessageData {
    private long chatId;
    private String message;

    private String apiName;
    private Map<String, String> data = new HashMap<>();
}
