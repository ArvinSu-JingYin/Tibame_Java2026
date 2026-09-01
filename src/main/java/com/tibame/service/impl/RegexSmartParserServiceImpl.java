package com.tibame.service.impl;

import com.tibame.common.exception.ApiException;
import com.tibame.model.dto.RecordCreateRequestDto;
import com.tibame.model.entity.Category;
import com.tibame.repository.CategoryRepository;
import com.tibame.service.SmartParserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegexSmartParserServiceImpl implements SmartParserService {

    private final CategoryRepository categoryRepository;

    private static final Pattern AMOUNT_PATTERN = Pattern.compile("(?<!\\d\\-)(?:\\$|NT\\$)?(\\d+(?:\\.\\d{1,2})?)(?!\\-\\d)");
    private static final Pattern DATE_PATTERN = Pattern.compile("\\b(\\d{4}-\\d{2}-\\d{2})\\b");

    @Override
    public RecordCreateRequestDto parseQuickInput(String rawInput, Long userId) {
        if (rawInput == null || rawInput.trim().isEmpty()) {
            throw new ApiException("輸入內容不得為空");
        }

        String text = rawInput.trim();

        // 1. 提取日期 (若無則預設為今天)
        LocalDate recordDate = LocalDate.now();
        Matcher dateMatcher = DATE_PATTERN.matcher(text);
        if (dateMatcher.find()) {
            recordDate = LocalDate.parse(dateMatcher.group(1));
            text = text.replace(dateMatcher.group(0), " ");
        }

        // 2. 提取金額 (避開「8月」、「8月份」、「15日」等日期描述)
        BigDecimal amount = null;
        String matchedRawAmount = null;

        // 優先匹配帶貨幣符號或單位 (如 $500, NT$500, 500元) 或獨立數字
        Pattern candidatePattern = Pattern.compile("(?<!\\d[-/])(?:\\$|NT\\$)?\\s*(\\d+(?:\\.\\d{1,2})?)\\s*(?:元|塊)?(?![-/\\d]|\\s*月|\\s*日|\\s*號)");
        Matcher amountMatcher = candidatePattern.matcher(text);
        while (amountMatcher.find()) {
            String numStr = amountMatcher.group(1);
            try {
                BigDecimal val = new BigDecimal(numStr);
                if (val.compareTo(BigDecimal.ZERO) > 0) {
                    amount = val;
                    matchedRawAmount = amountMatcher.group(0);
                    // 若找到較大數額或明確金額，優先使用
                }
            } catch (Exception ignored) {}
        }

        if (matchedRawAmount != null) {
            text = text.replace(matchedRawAmount, " ");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException("無法從文字中解析出有效金額");
        }

        // 3. 判斷記帳類型 (EXPENSE / INCOME)
        String recordType = "EXPENSE";
        if (text.contains("收入") || text.contains("薪水") || text.contains("薪資") || text.contains("獎金") || text.contains("兼職")) {
            recordType = "INCOME";
        }

        // 4. 匹配分類
        List<Category> categories = categoryRepository.findAvailableCategoriesByType(userId, recordType);
        Long matchedCategoryId = null;

        for (Category cat : categories) {
            if (text.contains(cat.getName())) {
                matchedCategoryId = cat.getId();
                text = text.replace(cat.getName(), " ");
                break;
            }
        }

        if (matchedCategoryId == null && !categories.isEmpty()) {
            matchedCategoryId = categories.get(0).getId();
        }

        if (matchedCategoryId == null) {
            throw new ApiException("查無可用分類，請先建立分類");
        }

        // 5. 剩餘字串作為備註
        String description = text.replaceAll("\\s+", " ").trim();
        if (description.isEmpty()) {
            description = recordType.equals("EXPENSE") ? "日常支出" : "收入所得";
        }

        log.debug("SmartParser parsed: amount={}, type={}, categoryId={}, date={}, desc={}",
                amount, recordType, matchedCategoryId, recordDate, description);

        return RecordCreateRequestDto.builder()
                .categoryId(matchedCategoryId)
                .recordType(recordType)
                .amount(amount)
                .description(description)
                .recordDate(recordDate)
                .build();
    }
}
