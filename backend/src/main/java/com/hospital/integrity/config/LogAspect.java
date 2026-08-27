package com.hospital.integrity.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.integrity.annotation.Log;
import com.hospital.integrity.entity.SysLog;
import com.hospital.integrity.mapper.SysLogMapper;
import com.hospital.integrity.security.LoginUser;
import com.hospital.integrity.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 操作日志切面：所有 @Log 标注的方法自动落库（审计留痕，不可删除）
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LogAspect {

    private final SysLogMapper sysLogMapper;
    private final ObjectMapper objectMapper;

    @Around("@annotation(logAnno)")
    public Object around(ProceedingJoinPoint joinPoint, Log logAnno) throws Throwable {
        long start = System.currentTimeMillis();
        int status = 1;
        String errorMsg = null;
        try {
            return joinPoint.proceed();
        } catch (Throwable t) {
            status = 0;
            errorMsg = truncate(t.getMessage(), 2000);
            throw t;
        } finally {
            try {
                saveLog(joinPoint, logAnno, status, errorMsg, System.currentTimeMillis() - start);
            } catch (Exception e) {
                log.error("记录操作日志失败", e);
            }
        }
    }

    private void saveLog(ProceedingJoinPoint joinPoint, Log logAnno, int status, String errorMsg, long cost) {
        SysLog sysLog = new SysLog();
        try {
            LoginUser user = SecurityUtils.currentUser();
            sysLog.setUserId(user.getUserId());
            sysLog.setUsername(user.getUsername());
        } catch (Exception ignored) {
            // 未登录场景
        }
        sysLog.setModule(logAnno.module());
        sysLog.setOperation(logAnno.operation());
        sysLog.setMethod(joinPoint.getSignature().toShortString());
        sysLog.setParams(truncate(serializeArgs(joinPoint.getArgs()), 1000));
        sysLog.setIp(getIp());
        sysLog.setStatus(status);
        sysLog.setErrorMsg(errorMsg);
        sysLog.setCostTime(cost);
        sysLogMapper.insert(sysLog);
    }

    private String serializeArgs(Object[] args) {
        try {
            return objectMapper.writeValueAsString(args);
        } catch (Exception e) {
            return "[]";
        }
    }

    private String getIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String ip = request.getHeader("X-Forwarded-For");
                if (ip == null || ip.isBlank()) {
                    ip = request.getRemoteAddr();
                }
                return truncate(ip, 64);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
