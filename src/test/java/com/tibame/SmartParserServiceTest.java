package com.tibame;

import com.tibame.model.dto.RecordCreateRequestDto;
import com.tibame.model.entity.Category;
import com.tibame.repository.CategoryRepository;
import com.tibame.service.impl.RegexSmartParserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SmartParserServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private RegexSmartParserServiceImpl parserService;

    private Category expenseCat;
    private Category incomeCat;

    @BeforeEach
    void setUp() {
        expenseCat = Category.builder()
                .id(1L)
                .name("飲食聚餐")
                .type("EXPENSE")
                .isSystem(true)
                .build();

        incomeCat = Category.builder()
                .id(2L)
                .name("薪資所得")
                .type("INCOME")
                .isSystem(true)
                .build();
    }

    @Test
    void testParseExpenseInput() {
        when(categoryRepository.findAvailableCategoriesByType(anyLong(), eq("EXPENSE")))
                .thenReturn(List.of(expenseCat));

        RecordCreateRequestDto result = parserService.parseQuickInput("午餐便當 120 飲食聚餐", 1L);

        assertNotNull(result);
        assertEquals(new BigDecimal("120"), result.getAmount());
        assertEquals("EXPENSE", result.getRecordType());
        assertEquals(1L, result.getCategoryId());
        assertEquals(LocalDate.now(), result.getRecordDate());
    }

    @Test
    void testParseIncomeWithDate() {
        when(categoryRepository.findAvailableCategoriesByType(anyLong(), eq("INCOME")))
                .thenReturn(List.of(incomeCat));

        RecordCreateRequestDto result = parserService.parseQuickInput("8月份薪水 50000 薪資所得 2026-08-31", 1L);

        assertNotNull(result);
        assertEquals(new BigDecimal("50000"), result.getAmount());
        assertEquals("INCOME", result.getRecordType());
        assertEquals(2L, result.getCategoryId());
        assertEquals(LocalDate.of(2026, 8, 31), result.getRecordDate());
    }
}
