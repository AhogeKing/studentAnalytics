package com.ahogeking.studentanalytics.controller;

import com.ahogeking.studentanalytics.annotation.RequireRole;
import com.ahogeking.studentanalytics.common.Result;
import com.ahogeking.studentanalytics.dto.UserCreateRequest;
import com.ahogeking.studentanalytics.dto.UserPasswordResetRequest;
import com.ahogeking.studentanalytics.dto.UserStatusUpdateRequest;
import com.ahogeking.studentanalytics.dto.UserUpdateRequest;
import com.ahogeking.studentanalytics.service.SysUserService;
import com.ahogeking.studentanalytics.vo.AdminUserVO;
import com.ahogeking.studentanalytics.vo.PageResultVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/users")
@RequireRole({"ADMIN"})
public class AdminUserController {

    private final SysUserService sysUserService;

    @GetMapping
    public Result<PageResultVO<AdminUserVO>> listUsers(
            @RequestParam(name = "page_num", required = false) Integer pageNum,
            @RequestParam(name = "page_size", required = false) Integer pageSize) {
        return Result.success(sysUserService.selectAdminUsers(pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public Result<AdminUserVO> getUser(@PathVariable Integer id) {
        return Result.success(sysUserService.selectAdminUserById(id));
    }

    @PostMapping
    public Result<AdminUserVO> createUser(@RequestBody @Valid UserCreateRequest request) {
        return Result.success(sysUserService.createAdminUser(request));
    }

    @PutMapping("/{id}")
    public Result<AdminUserVO> updateUser(
            @PathVariable Integer id,
            @RequestBody @Valid UserUpdateRequest request) {
        return Result.success(sysUserService.updateAdminUser(id, request));
    }

    @PatchMapping("/{id}/status")
    public Result<AdminUserVO> updateUserStatus(
            @PathVariable Integer id,
            @RequestBody @Valid UserStatusUpdateRequest request) {
        return Result.success(sysUserService.updateAdminUserStatus(id, request));
    }

    @PatchMapping("/{id}/password")
    public Result<Void> resetPassword(
            @PathVariable Integer id,
            @RequestBody @Valid UserPasswordResetRequest request) {
        sysUserService.resetAdminUserPassword(id, request);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable Integer id) {
        sysUserService.disableAdminUser(id);
        return Result.success();
    }
}
