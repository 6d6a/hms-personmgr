package ru.majordomo.hms.personmgr.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TelegramMessageData {
    private long chatId;
    @Nullable
    private String message;

    @Nullable
    private String apiName;
    @Nullable
    private Map<String, String> data = new HashMap<>();
}
