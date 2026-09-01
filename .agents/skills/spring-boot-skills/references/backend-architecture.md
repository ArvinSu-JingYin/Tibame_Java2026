# 後端分層架構規範 (Backend Layered Architecture)

本文件詳細規範 Spring Boot 後端分層架構（Repository / Service / MVC Controller / Web API Controller）、數據傳輸模型（Entity / DTO / VO）、事務控制、統一 API 響應以及全域異常處理標準。

---

## 1. 架構分層與職責邊界

後端嚴格採用四層架構，各層之間遵循**單向向下依賴**原則，嚴禁跨層或逆向調用：

```
                  ┌─────────────────────────────────────┐
                  │          Client Browser             │
                  └─────────┬─────────────────┬─────────┘
                            │ (Page Request)  │ (AJAX / REST Request)
                            ▼                 ▼
   ┌───────────────────────────────────┐  ┌───────────────────────────────────┐
   │          MVC Controller           │  │        Web API Controller         │
   │  (@Controller, Thymeleaf Views)   │  │   (@RestController, JSON REST)    │
   └─────────────────┬─────────────────┘  └─────────────────┬─────────────────┘
                     │                                      │
                     └──────────────────┬───────────────────┘
                                        ▼
                     ┌─────────────────────────────────────┐
                     │            Service Layer            │
                     │  (Business Logic, Transaction, DTO) │
                     └──────────────────┬──────────────────┘
                                        ▼
                     ┌─────────────────────────────────────┐
                     │          Repository Layer           │
                     │    (Spring Data JPA / MyBatis)      │
                     └──────────────────┬──────────────────┘
                                        ▼
                     ┌─────────────────────────────────────┐
                     │         Database (RDBMS)            │
                     └─────────────────────────────────────┘
```

---

## 2. 各層詳細職責與實作規範

### 2.1 Repository 層 (數據訪問層)
- **核心職責**：專注於資料庫 CRUD、持久化查詢、分頁與多條件動態過濾。
- **技術選型**：Spring Data JPA (`JpaRepository` / `JpaSpecificationExecutor`) 或 MyBatis (`@Mapper`)。
- **規範要點**：
  1. **參數與返回值**：僅接收及返回 **Entity** 實體或特定的 Projection 介面/類別。
  2. **禁包含業務邏輯**：嚴禁包含任何業務計算、權限校驗或狀態流轉邏輯。
  3. **命名規範**：遵循 Spring Data 方法命名規則（例如：`findByEmailAndStatus`, `existsByUsername`）。
  4. **跨層隔離**：僅由 Service 層注入調用，嚴禁被 MVC Controller 或 Web API Controller 直接調用。

#### 程式碼範例 (`UserRepository.java`)：
```java
package com.company.project.repository;

import com.company.project.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    Page<User> findByStatus(Integer status, Pageable pageable);
}
```

---

### 2.2 Service 層 (業務邏輯層)
- **核心職責**：封裝所有核心業務邏輯、交易事務控制（Transaction）、跨 Repository 協調調度、模型轉換（Entity ↔ DTO / VO）。
- **規範要點**：
  1. **介面與實作分離**：一律定義介面（`*Service`）並由實作類（`*ServiceImpl`）具體實現。
  2. **事務控制**：
     - 事務標註 `@Transactional` 應加在 Service 實作方法上。
     - 查詢方法明確聲明 `@Transactional(readOnly = true)`。
     - 多表寫入、金流交易或涉及異常回滾操作必須聲明 `@Transactional(rollbackFor = Exception.class)`。
  3. **異常拋出**：遇業務邏輯衝突或非預期狀態，統一拋出具備明確錯誤碼與語意的業務自定義異常（例如 `BusinessException`, `ResourceNotFoundException`）。
  4. **邊界防護**：接收 Controller 傳入的 DTO，返回 DTO 或 VO 給 Controller，不將底層 Entity 直接外洩。

#### 程式碼範例 (`UserService.java` & `UserServiceImpl.java`)：
```java
package com.company.project.service;

import com.company.project.model.dto.UserCreateRequestDto;
import com.company.project.model.dto.UserUpdateRequestDto;
import com.company.project.model.vo.UserResponseVo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
    UserResponseVo getUserById(Long id);
    UserResponseVo createUser(UserCreateRequestDto requestDto);
    UserResponseVo updateUser(Long id, UserUpdateRequestDto requestDto);
    void deleteUser(Long id);
    Page<UserResponseVo> queryUsers(String keyword, Integer status, Pageable pageable);
}
```

```java
package com.company.project.service.impl;

import com.company.project.common.exception.BusinessException;
import com.company.project.common.exception.ResourceNotFoundException;
import com.company.project.model.dto.UserCreateRequestDto;
import com.company.project.model.dto.UserUpdateRequestDto;
import com.company.project.model.entity.User;
import com.company.project.model.vo.UserResponseVo;
import com.company.project.repository.UserRepository;
import com.company.project.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserResponseVo getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("找不到使用者 ID: " + id));
        return convertToVo(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserResponseVo createUser(UserCreateRequestDto requestDto) {
        if (userRepository.existsByUsername(requestDto.getUsername())) {
            throw new BusinessException(4001, "使用者名稱已被註冊");
        }
        if (userRepository.existsByEmail(requestDto.getEmail())) {
            throw new BusinessException(4002, "電子郵件已被註冊");
        }

        User user = new User();
        user.setUsername(requestDto.getUsername());
        user.setEmail(requestDto.getEmail());
        user.setPasswordHash(hashPassword(requestDto.getPassword()));
        user.setStatus(1); // 預設啟用

        User savedUser = userRepository.save(user);
        log.info("成功建立使用者: ID={}, Username={}", savedUser.getId(), savedUser.getUsername());
        return convertToVo(savedUser);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserResponseVo updateUser(Long id, UserUpdateRequestDto requestDto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("找不到使用者 ID: " + id));
        
        user.setEmail(requestDto.getEmail());
        if (requestDto.getStatus() != null) {
            user.setStatus(requestDto.getStatus());
        }

        User updatedUser = userRepository.save(user);
        return convertToVo(updatedUser);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("找不到欲刪除的使用者 ID: " + id);
        }
        userRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponseVo> queryUsers(String keyword, Integer status, Pageable pageable) {
        // 分頁與條件查詢實作
        return userRepository.findAll(pageable).map(this::convertToVo);
    }

    private UserResponseVo convertToVo(User user) {
        UserResponseVo vo = new UserResponseVo();
        BeanUtils.copyProperties(user, vo);
        vo.setActive(user.getStatus() != null && user.getStatus() == 1);
        return vo;
    }

    private String hashPassword(String rawPassword) {
        // 密碼加密實作 (例如 BCrypt)
        return "hashed_" + rawPassword;
    }
}
```

---

### 2.3 MVC Controller 層 (頁面視圖控制器)
- **核心職責**：處理瀏覽器頁面請求、渲染伺服器端 Thymeleaf 模板（SSR）、視圖導向（Page Navigation）與基礎 Model 裝載。
- **規範要點**：
  1. **註解使用**：使用 `@Controller`，不使用 `@RestController`。
  2. **方法返回值**：返回視圖路徑字串（例如 `return "pages/user/index";`）。
  3. **精簡調用**：僅調用 Service 層提供頁面初始化必要的 ViewModel/Model 數據，禁止調用 Repository。
  4. **職責分離**：所有複雜數據異動與動態互動由前端 Vue 3 調用 Web API 處理，MVC Controller 保持輕量視圖路由職責。

#### 程式碼範例 (`UserViewController.java`)：
```java
package com.company.project.controller.mvc;

import com.company.project.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserViewController {

    private final UserService userService;

    @GetMapping
    public String indexPage(Model model) {
        model.addAttribute("pageTitle", "使用者管理系統");
        model.addAttribute("systemCode", "SYS-USER-01");
        return "pages/user/index";
    }

    @GetMapping("/create")
    public String createPage(Model model) {
        model.addAttribute("pageTitle", "新增使用者");
        return "pages/user/create";
    }

    @GetMapping("/{id}/detail")
    public String detailPage(@PathVariable Long id, Model model) {
        model.addAttribute("user", userService.getUserById(id));
        return "pages/user/detail";
    }
}
```

---

### 2.4 Web API Controller 層 (RESTful 數據控制器)
- **核心職責**：處理前端 Ajax/Axios 非同步請求、微服務間接口調用，提供標準化的 JSON RESTful API。
- **規範要點**：
  1. **註解使用**：使用 `@RestController` 與統一根路徑 `@RequestMapping("/api/v1/...")`。
  2. **語意化 HTTP 動詞**：
     - `GET`：獲取單一資源或分頁清單
     - `POST`：新增資源
     - `PUT`：全量更新資源
     - `PATCH`：局部更新資源狀態
     - `DELETE`：刪除資源
  3. **參數校驗**：請求 DTO 參數一律標註 `@Valid` 或 `@Validated`。
  4. **統一包裝格式**：所有接口均返回標準 `ApiResponse<T>`。

#### 程式碼範例 (`UserApiController.java`)：
```java
package com.company.project.controller.api;

import com.company.project.common.response.ApiResponse;
import com.company.project.model.dto.UserCreateRequestDto;
import com.company.project.model.dto.UserUpdateRequestDto;
import com.company.project.model.vo.UserResponseVo;
import com.company.project.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserApiController {

    private final UserService userService;

    @GetMapping("/{id}")
    public ApiResponse<UserResponseVo> getUserById(@PathVariable Long id) {
        UserResponseVo vo = userService.getUserById(id);
        return ApiResponse.ok(vo);
    }

    @GetMapping
    public ApiResponse<Page<UserResponseVo>> queryUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<UserResponseVo> page = userService.queryUsers(keyword, status, pageable);
        return ApiResponse.ok(page);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponseVo> createUser(@Valid @RequestBody UserCreateRequestDto requestDto) {
        UserResponseVo vo = userService.createUser(requestDto);
        return ApiResponse.ok("使用者建立成功", vo);
    }

    @PutMapping("/{id}")
    public ApiResponse<UserResponseVo> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequestDto requestDto) {
        UserResponseVo vo = userService.updateUser(id, requestDto);
        return ApiResponse.ok("使用者更新成功", vo);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ApiResponse.ok("使用者刪除成功", null);
    }
}
```

---

## 3. 數據傳遞與模型規範 (Entity / DTO / VO)

```
[Client / Vue 3]
       │  ▲
(DTO)  │  │ (VO)
       ▼  │
[Web API Controller]
       │  ▲
(DTO)  │  │ (VO / DTO)
       ▼  │
[Service Layer]
       │  ▲
(Entity)│ │ (Entity)
       ▼  │
[Repository Layer] ── (Entity) ── [Database]
```

| 模型類型 | 所在 Package | 核心職責 | 規範要求 |
| :--- | :--- | :--- | :--- |
| **Entity** | `model.entity` | 映射資料庫表格結構與欄位 | 僅限於 Repository 與 Service 層流轉，嚴禁作為 Controller 接口的請求參數或響應對象。 |
| **DTO** | `model.dto` | 承載客戶端輸入參數與驗證規則 | 包含 `jakarta.validation` 註解（如 `@NotBlank`, `@NotNull`, `@Size`），不包含業務邏輯與敏感內部狀態。 |
| **VO** | `model.vo` | 返回給前端呈現的純數據視圖對象 | 遮蔽敏感資料（例如密碼、Salt、金流金鑰），針對前端表格、卡片與統計視圖進行結構最佳化。 |

---

## 4. 統一響應包裝與全域異常處理

### 4.1 統一響應物件 (`ApiResponse<T>`)
```java
package com.company.project.common.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private int code;           // 狀態碼：200 代表成功，其餘為自定義業務錯誤碼
    private boolean success;    // 標記請求是否成功
    private String message;     // 提示訊息
    private T data;             // 核心承載數據
    @Builder.Default
    private long timestamp = Instant.now().toEpochMilli(); // 時間戳記

    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder()
                .code(200)
                .success(true)
                .message("操作成功")
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return ApiResponse.<T>builder()
                .code(200)
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return ApiResponse.<T>builder()
                .code(code)
                .success(false)
                .message(message)
                .data(null)
                .build();
    }
}
```

### 4.2 自定義業務異常 (`BusinessException`)
```java
package com.company.project.common.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message) {
        super(message);
        this.code = 400;
    }
}
```

### 4.3 全域異常攔截器 (`GlobalExceptionHandler`)
```java
package com.company.project.common.exception;

import com.company.project.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice(basePackages = "com.company.project.controller.api")
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleBusinessException(BusinessException ex) {
        log.warn("業務異常: code={}, message={}", ex.getCode(), ex.getMessage());
        return ApiResponse.error(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleResourceNotFoundException(ResourceNotFoundException ex) {
        return ApiResponse.error(404, ex.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidationException(Exception ex) {
        String defaultMsg = "參數校驗失敗";
        if (ex instanceof MethodArgumentNotValidException manv) {
            FieldError fieldError = manv.getBindingResult().getFieldError();
            if (fieldError != null) {
                defaultMsg = fieldError.getDefaultMessage();
            }
        }
        return ApiResponse.error(400, defaultMsg);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleGeneralException(Exception ex) {
        log.error("未預期系統異常", ex);
        return ApiResponse.error(500, "系統發生未預期錯誤，請聯絡系統管理員");
    }
}
```
