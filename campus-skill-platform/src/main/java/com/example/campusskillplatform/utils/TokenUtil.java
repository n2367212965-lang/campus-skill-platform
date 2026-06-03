package com.example.campusskillplatform.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TokenUtil {

    private static final Logger log = LoggerFactory.getLogger(TokenUtil.class);

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 从请求头中提取token（去掉Bearer前缀）
     */
    public String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }
        return null;
    }

    /**
     * 从HttpServletRequest中获取用户ID
     * @param request HttpServletRequest
     * @return 用户ID，如果token无效或未提供返回null
     */
    public Long getUserId(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            String token = extractToken(authHeader);
            if (token == null || token.isEmpty()) {
                return null;
            }
            if (!jwtUtil.validateToken(token)) {
                return null;
            }
            return jwtUtil.getUserIdFromToken(token);
        } catch (Exception e) {
            log.error("获取用户ID失败", e);
            return null;
        }
    }

    /**
     * 从HttpServletRequest中获取用户角色
     * @param request HttpServletRequest
     * @return 角色值，如果token无效或未提供返回null
     */
    public Integer getRole(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            String token = extractToken(authHeader);
            if (token == null || token.isEmpty()) {
                return null;
            }
            if (!jwtUtil.validateToken(token)) {
                return null;
            }
            return jwtUtil.getRoleFromToken(token);
        } catch (Exception e) {
            log.error("获取用户角色失败", e);
            return null;
        }
    }

    /**
     * 从authHeader字符串中校验token并获取用户ID
     */
    public Long validateAndGetUserId(String authHeader) {
        try {
            String token = extractToken(authHeader);
            if (token == null || token.isEmpty()) {
                return null;
            }
            if (!jwtUtil.validateToken(token)) {
                return null;
            }
            return jwtUtil.getUserIdFromToken(token);
        } catch (Exception e) {
            log.error("校验token获取用户ID失败", e);
            return null;
        }
    }
}
