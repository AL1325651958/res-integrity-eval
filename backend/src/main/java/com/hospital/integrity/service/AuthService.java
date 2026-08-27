package com.hospital.integrity.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hospital.integrity.common.BusinessException;
import com.hospital.integrity.dto.LoginDTO;
import com.hospital.integrity.dto.LoginVO;
import com.hospital.integrity.dto.PasswordDTO;
import com.hospital.integrity.dto.UserInfoVO;
import com.hospital.integrity.entity.SysDept;
import com.hospital.integrity.entity.SysLoginLog;
import com.hospital.integrity.entity.SysUser;
import com.hospital.integrity.mapper.SysDeptMapper;
import com.hospital.integrity.mapper.SysLoginLogMapper;
import com.hospital.integrity.mapper.SysUserMapper;
import com.hospital.integrity.security.JwtUtil;
import com.hospital.integrity.security.LoginUser;
import com.hospital.integrity.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;

/**
 * 认证服务
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final SysUserMapper sysUserMapper;
    private final SysDeptMapper sysDeptMapper;
    private final SysLoginLogMapper sysLoginLogMapper;
    private final PasswordEncoder passwordEncoder;

    public LoginVO login(LoginDTO dto) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword()));
            LoginUser loginUser = (LoginUser) authentication.getPrincipal();
            if (!loginUser.isEnabled()) {
                throw new BusinessException(400, "账号已被禁用，请联系管理员");
            }
            String token = jwtUtil.createToken(loginUser.getUserId(), loginUser.getUsername());
            // 更新最后登录时间
            sysUserMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                    .eq(SysUser::getUserId, loginUser.getUserId())
                    .set(SysUser::getLastLoginTime, LocalDateTime.now()));
            saveLoginLog(dto.getUsername(), 1, "登录成功");
            return new LoginVO(token, buildUserInfo(loginUser));
        } catch (DisabledException e) {
            saveLoginLog(dto.getUsername(), 0, "账号已被禁用");
            throw new BusinessException(400, "账号已被禁用");
        } catch (AuthenticationException e) {
            saveLoginLog(dto.getUsername(), 0, "账号或密码错误");
            throw new BusinessException(400, "账号或密码错误");
        }
    }

    private void saveLoginLog(String username, int status, String msg) {
        try {
            SysLoginLog loginLog = new SysLoginLog();
            loginLog.setUsername(username);
            loginLog.setIp(com.hospital.integrity.util.IpUtil.getClientIp());
            loginLog.setStatus(status);
            loginLog.setMsg(msg);
            sysLoginLogMapper.insert(loginLog);
        } catch (Exception ignored) {
            // 登录日志失败不影响登录
        }
    }

    public UserInfoVO info() {
        return buildUserInfo(SecurityUtils.currentUser());
    }

    public void changePassword(PasswordDTO dto) {
        LoginUser current = SecurityUtils.currentUser();
        SysUser user = sysUserMapper.selectById(current.getUserId());
        if (user == null || !passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException(400, "原密码错误");
        }
        if (dto.getNewPassword().length() < 8) {
            throw new BusinessException(400, "新密码长度不能少于8位");
        }
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        sysUserMapper.updateById(user);
    }

    private UserInfoVO buildUserInfo(LoginUser u) {
        UserInfoVO vo = new UserInfoVO();
        vo.setUserId(u.getUserId());
        vo.setUsername(u.getUsername());
        vo.setRealName(u.getRealName());
        vo.setDeptId(u.getDeptId());
        vo.setTitle(u.getTitle());
        if (u.getDeptId() != null) {
            SysDept dept = sysDeptMapper.selectById(u.getDeptId());
            if (dept != null) {
                vo.setDeptName(dept.getDeptName());
            }
        }
        vo.setRoles(new ArrayList<>(u.getRoleKeys()));
        vo.setPerms(new ArrayList<>(u.getPerms()));
        return vo;
    }
}
