package com.tibame.service;

import com.tibame.model.dto.RecordCreateRequestDto;

public interface SmartParserService {
    RecordCreateRequestDto parseQuickInput(String rawInput, Long userId);
}
