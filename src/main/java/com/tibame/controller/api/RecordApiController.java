package com.tibame.controller.api;

import com.tibame.common.response.ApiResponse;
import com.tibame.common.security.UserContext;
import com.tibame.model.dto.RecordCreateRequestDto;
import com.tibame.model.dto.RecordUpdateRequestDto;
import com.tibame.model.vo.MonthlySummaryVo;
import com.tibame.model.vo.RecordResponseVo;
import com.tibame.service.LedgerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/records")
@RequiredArgsConstructor
public class RecordApiController {

    private final LedgerService ledgerService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RecordResponseVo> createRecord(@Valid @RequestBody RecordCreateRequestDto requestDto) {
        Long userId = UserContext.requireUserId();
        RecordResponseVo vo = ledgerService.createRecord(userId, requestDto);
        return ApiResponse.ok("記帳成功", vo);
    }

    @PostMapping("/quick")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RecordResponseVo> quickCreateRecord(@RequestBody Map<String, String> body) {
        Long userId = UserContext.requireUserId();
        String rawText = body != null ? body.get("text") : null;
        RecordResponseVo vo = ledgerService.quickCreateRecord(userId, rawText);
        return ApiResponse.ok("快速記帳成功", vo);
    }

    @GetMapping
    public ApiResponse<Page<RecordResponseVo>> queryRecords(
            @RequestParam(required = false) String recordType,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 15) Pageable pageable) {

        Long userId = UserContext.requireUserId();
        Page<RecordResponseVo> page = ledgerService.queryRecords(
                userId, recordType, categoryId, startDate, endDate, keyword, pageable);
        return ApiResponse.ok(page);
    }

    @GetMapping("/{id}")
    public ApiResponse<RecordResponseVo> getRecordById(@PathVariable Long id) {
        Long userId = UserContext.requireUserId();
        RecordResponseVo vo = ledgerService.getRecordById(id, userId);
        return ApiResponse.ok(vo);
    }

    @PutMapping("/{id}")
    public ApiResponse<RecordResponseVo> updateRecord(
            @PathVariable Long id,
            @Valid @RequestBody RecordUpdateRequestDto requestDto) {
        Long userId = UserContext.requireUserId();
        RecordResponseVo vo = ledgerService.updateRecord(id, userId, requestDto);
        return ApiResponse.ok("紀錄更新成功", vo);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteRecord(@PathVariable Long id) {
        Long userId = UserContext.requireUserId();
        ledgerService.deleteRecord(id, userId);
        return ApiResponse.ok("紀錄刪除成功", null);
    }

    @GetMapping("/summary")
    public ApiResponse<MonthlySummaryVo> getMonthlySummary(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {

        Long userId = UserContext.requireUserId();
        LocalDate now = LocalDate.now();
        int queryYear = (year != null && year > 1900) ? year : now.getYear();
        int queryMonth = (month != null && month >= 1 && month <= 12) ? month : now.getMonthValue();

        MonthlySummaryVo summary = ledgerService.getMonthlySummary(userId, queryYear, queryMonth);
        return ApiResponse.ok(summary);
    }
}
