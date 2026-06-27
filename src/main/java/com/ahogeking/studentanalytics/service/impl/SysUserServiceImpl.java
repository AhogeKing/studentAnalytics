package com.ahogeking.studentanalytics.service.impl;

import com.ahogeking.studentanalytics.common.JwtUtil;
import com.ahogeking.studentanalytics.common.PasswordUtil;
import com.ahogeking.studentanalytics.dto.LoginRequest;
import com.ahogeking.studentanalytics.dto.LoginResponse;
import com.ahogeking.studentanalytics.dto.RegisterRequest;
import com.ahogeking.studentanalytics.dto.UserCreateRequest;
import com.ahogeking.studentanalytics.dto.UserPasswordResetRequest;
import com.ahogeking.studentanalytics.dto.UserStatusUpdateRequest;
import com.ahogeking.studentanalytics.dto.UserUpdateRequest;
import com.ahogeking.studentanalytics.context.SysUserContext;
import com.ahogeking.studentanalytics.entity.SysUser;
import com.ahogeking.studentanalytics.exception.BusinessException;
import com.ahogeking.studentanalytics.mapper.SysUserMapper;
import com.ahogeking.studentanalytics.service.SysUserService;
import com.ahogeking.studentanalytics.vo.AdminUserVO;
import com.ahogeking.studentanalytics.vo.PageResultVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SysUserServiceImpl implements SysUserService {
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_TEACHER = "TEACHER";

    private final SysUserMapper sysUserMapper;
    private final JwtUtil jwtUtil;

    public SysUserServiceImpl(SysUserMapper sysUserMapper, JwtUtil jwtUtil) {
        this.sysUserMapper = sysUserMapper;
        this.jwtUtil = jwtUtil;
    }

    @Override
    @Transactional
    public void register(RegisterRequest registerRequest) {
        String username = normalizeUsername(registerRequest.getUsername());
        // 1. 检查用户名是否已存在
        SysUser existUser = sysUserMapper.selectAnyByUsername(username);
        if (existUser != null) {
            throw new BusinessException("用户名已存在");
        }
        String role = registerRequest.getRole() == null || registerRequest.getRole().isBlank()
                ? ROLE_TEACHER
                : registerRequest.getRole().trim().toUpperCase();
        if (!ROLE_TEACHER.equals(role)) {
            throw new BusinessException("注册接口只允许创建教师账号");
        }

        // 2. 创建用户对象
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(PasswordUtil.encode(registerRequest.getPassword()));
        user.setRealName(normalizeNullableText(registerRequest.getRealName()));
        user.setRole(role);
        user.setStatus(1);

        sysUserMapper.insert(user);
    }

    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        // 根据用户名查询用户
        SysUser dbUser = sysUserMapper.selectByUsername(normalizeUsername(loginRequest.getUsername()));
        if (dbUser == null) {   // 用户不存在
            throw new BusinessException("用户名或密码错误");
        }

        // 校验密码
        if (!PasswordUtil.matches(loginRequest.getPassword(), dbUser.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        String token = jwtUtil.createLoginToken(
                dbUser.getId(),
                dbUser.getUsername(),
                dbUser.getRealName(),
                dbUser.getRole()
        );

        // 组装返回结果
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUserId(dbUser.getId());
        response.setUserName(dbUser.getUsername());
        response.setRealName(dbUser.getRealName());
        response.setRole(dbUser.getRole());

        return response;
    }

    @Override
    public PageResultVO<AdminUserVO> selectAdminUsers(Integer pageNum, Integer pageSize) {
        long safePageNum = normalizePageNum(pageNum);
        long safePageSize = normalizePageSize(pageSize);

        List<AdminUserVO> records = sysUserMapper.selectAdminUserPage((safePageNum - 1) * safePageSize, safePageSize)
                .stream()
                .map(AdminUserVO::from)
                .toList();
        return new PageResultVO<>(sysUserMapper.countAdminUsers(), records);
    }

    @Override
    public AdminUserVO selectAdminUserById(Integer id) {
        return AdminUserVO.from(requireUser(id));
    }

    @Override
    @Transactional
    public AdminUserVO createAdminUser(UserCreateRequest request) {
        if (request == null) {
            throw new BusinessException("用户信息不能为空");
        }
        String username = normalizeUsername(request.getUsername());
        if (sysUserMapper.selectAnyByUsername(username) != null) {
            throw new BusinessException("用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(PasswordUtil.encode(request.getPassword()));
        user.setRealName(normalizeNullableText(request.getRealName()));
        user.setRole(normalizeManageableRole(request.getRole()));
        user.setStatus(1);
        sysUserMapper.insert(user);
        return AdminUserVO.from(user);
    }

    @Override
    @Transactional
    public AdminUserVO updateAdminUser(Integer id, UserUpdateRequest request) {
        if (request == null) {
            throw new BusinessException("用户信息不能为空");
        }
        if (request.getRealName() == null && request.getRole() == null && request.getStatus() == null) {
            throw new BusinessException("没有可修改的字段");
        }

        SysUser user = requireUser(id);
        String newRole = request.getRole() == null ? user.getRole() : normalizeManageableRole(request.getRole());
        Integer newStatus = request.getStatus() == null ? user.getStatus() : request.getStatus();
        validateAdminDemotionOrDisable(user, newRole, newStatus);
        validateSelfDisable(user.getId(), newStatus);

        if (request.getRealName() != null) {
            user.setRealName(normalizeNullableText(request.getRealName()));
        }
        user.setRole(newRole);
        user.setStatus(newStatus);
        sysUserMapper.updateById(user);
        return AdminUserVO.from(requireUser(id));
    }

    @Override
    @Transactional
    public AdminUserVO updateAdminUserStatus(Integer id, UserStatusUpdateRequest request) {
        if (request == null) {
            throw new BusinessException("用户状态不能为空");
        }
        SysUser user = requireUser(id);
        validateSelfDisable(user.getId(), request.getStatus());
        validateAdminDemotionOrDisable(user, user.getRole(), request.getStatus());
        user.setStatus(request.getStatus());
        sysUserMapper.updateById(user);
        return AdminUserVO.from(requireUser(id));
    }

    @Override
    @Transactional
    public void resetAdminUserPassword(Integer id, UserPasswordResetRequest request) {
        if (request == null) {
            throw new BusinessException("新密码不能为空");
        }
        SysUser user = requireUser(id);
        user.setPassword(PasswordUtil.encode(request.getNewPassword()));
        sysUserMapper.updateById(user);
    }

    @Override
    @Transactional
    public void disableAdminUser(Integer id) {
        SysUser user = requireUser(id);
        validateSelfDisable(user.getId(), 0);
        validateAdminDemotionOrDisable(user, user.getRole(), 0);
        user.setStatus(0);
        sysUserMapper.updateById(user);
    }

    private SysUser requireUser(Integer id) {
        if (id == null) {
            throw new BusinessException("用户ID不能为空");
        }
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    private String normalizeUsername(String username) {
        String trimmed = username == null ? "" : username.trim();
        if (trimmed.isBlank()) {
            throw new BusinessException("用户名不能为空");
        }
        return trimmed;
    }

    private String normalizeNullableText(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private String normalizeManageableRole(String role) {
        String normalized = role == null ? "" : role.trim().toUpperCase();
        if (!ROLE_ADMIN.equals(normalized) && !ROLE_TEACHER.equals(normalized)) {
            throw new BusinessException("角色只能是ADMIN或TEACHER");
        }
        return normalized;
    }

    private void validateSelfDisable(Integer targetUserId, Integer newStatus) {
        if (targetUserId != null && targetUserId.equals(SysUserContext.getUserId()) && Integer.valueOf(0).equals(newStatus)) {
            throw new BusinessException("不能禁用当前登录用户");
        }
    }

    private void validateAdminDemotionOrDisable(SysUser user, String newRole, Integer newStatus) {
        if (!ROLE_ADMIN.equals(user.getRole())) {
            return;
        }
        boolean remainsActiveAdmin = ROLE_ADMIN.equals(newRole) && Integer.valueOf(1).equals(newStatus);
        if (remainsActiveAdmin) {
            return;
        }
        if (countActiveAdmins() <= 1) {
            throw new BusinessException("不能禁用或降级最后一个管理员");
        }
    }

    private Long countActiveAdmins() {
        return sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRole, ROLE_ADMIN)
                .eq(SysUser::getStatus, 1));
    }

    private long normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1L : pageNum;
    }

    private long normalizePageSize(Integer pageSize) {
        if (pageSize == null) {
            return 20L;
        }
        if (pageSize < 1) {
            return 20L;
        }
        return Math.min(pageSize, 100);
    }
}
