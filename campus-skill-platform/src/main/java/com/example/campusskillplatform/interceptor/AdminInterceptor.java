package com.example.campusskillplatform.interceptor;

import com.example.campusskillplatform.common.Constants;
import com.example.campusskillplatform.common.Result;
import com.example.campusskillplatform.entity.User;
import com.example.campusskillplatform.mapper.UserMapper;
import com.example.campusskillplatform.utils.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.PrintWriter;

@Component
public class AdminInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AdminInterceptor.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendErrorResponse(response, 401, "未授权，请先登录");
            return false;
        }

        String token = authHeader.substring(7);
        try {
            if (!jwtUtil.validateToken(token)) {
                sendErrorResponse(response, 401, "token无效或已过期");
                return false;
            }

            Long userId = jwtUtil.getUserIdFromToken(token);
            if (userId == null) {
                sendErrorResponse(response, 401, "token解析失败");
                return false;
            }

            User user = userMapper.selectById(userId);
            if (user == null) {
                sendErrorResponse(response, 401, "用户不存在");
                return false;
            }

            if (user.getRole() != Constants.USER_ROLE_ADMIN) {
                log.warn("非管理员尝试访问管理接口，用户ID：{}", userId);
                sendErrorResponse(response, 403, "无管理员权限");
                return false;
            }

            if (user.getStatus() != Constants.USER_STATUS_NORMAL) {
                sendErrorResponse(response, 403, "账户已被禁用");
                return false;
            }

            request.setAttribute("currentUser", user);
            log.debug("管理员访问通过，用户ID：{}", userId);
            return true;
        } catch (Exception e) {
            log.error("管理员拦截器异常", e);
            sendErrorResponse(response, 500, "服务器内部错误");
            return false;
        }
    }

    private void sendErrorResponse(HttpServletResponse response, int code, String message) throws Exception {
        response.setStatus(code);
        response.setContentType("application/json;charset=UTF-8");

        Result<Void> result = Result.error(code, message);
        PrintWriter writer = response.getWriter();
        writer.write(new ObjectMapper().writeValueAsString(result));
        writer.flush();
        writer.close();
    }
}
