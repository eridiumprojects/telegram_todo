package com.example.telegram.model.dto;

import com.example.telegram.model.enums.BotState;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BotChange {
    BotState botState;
    String message;
}
