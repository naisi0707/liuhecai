package com.liuhecai.controller;

import com.liuhecai.common.result.PageResult;
import com.liuhecai.common.result.Result;
import com.liuhecai.common.util.CsvWriter;
import com.liuhecai.dto.BatchEnabledRequest;
import com.liuhecai.dto.CoinAdjustRequest;
import com.liuhecai.dto.EnabledRequest;
import com.liuhecai.service.AgentUserService;
import com.liuhecai.vo.AgentUserDetailVO;
import com.liuhecai.vo.AgentUserListItemVO;
import com.liuhecai.vo.PasswordResetVO;
import com.liuhecai.vo.UserCoinLogVO;
import com.liuhecai.vo.UserOrderVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/agent/users")
@RequiredArgsConstructor
@Validated
public class AgentUserController {

    private final AgentUserService agentUserService;

    @GetMapping
    public Result<PageResult<AgentUserListItemVO>> page(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer enabled,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(agentUserService.page(username, enabled, page, size));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer enabled) {
        String csv = agentUserService.exportUsersCsv(username, enabled);
        byte[] body = CsvWriter.toUtf8BomBytes(csv);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"users.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(body);
    }

    @GetMapping("/{id}")
    public Result<AgentUserDetailVO> detail(@PathVariable Long id) {
        return Result.ok(agentUserService.getDetail(id));
    }

    @PutMapping("/{id}/enabled")
    public Result<AgentUserDetailVO> updateEnabled(@PathVariable Long id,
                                                   @Valid @RequestBody EnabledRequest request) {
        return Result.ok(agentUserService.updateEnabled(id, request));
    }

    @PostMapping("/{id}/reset-password")
    public Result<PasswordResetVO> resetPassword(@PathVariable Long id) {
        return Result.ok(agentUserService.resetPassword(id));
    }

    @PostMapping("/{id}/force-logout")
    public Result<Void> forceLogout(@PathVariable Long id) {
        agentUserService.forceLogout(id);
        return Result.ok(null);
    }

    @PostMapping("/batch-enabled")
    public Result<Void> batchEnabled(@Valid @RequestBody BatchEnabledRequest request) {
        agentUserService.batchUpdateEnabled(request.getIds(), request.getEnabled());
        return Result.ok(null);
    }

    @PostMapping("/{id}/coins")
    public Result<Map<String, Integer>> adjustCoins(@PathVariable Long id,
                                                    @Valid @RequestBody CoinAdjustRequest request) {
        Integer balance = agentUserService.adjustCoins(id, request);
        return Result.ok(Map.of("coinBalance", balance));
    }

    @GetMapping("/{id}/coin-logs")
    public Result<PageResult<UserCoinLogVO>> coinLogs(@PathVariable Long id,
                                                      @RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        return Result.ok(agentUserService.pageCoinLogs(id, page, size));
    }

    @GetMapping("/{id}/orders")
    public Result<PageResult<UserOrderVO>> orders(@PathVariable Long id,
                                                  @RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        return Result.ok(agentUserService.pageOrders(id, page, size));
    }
}
