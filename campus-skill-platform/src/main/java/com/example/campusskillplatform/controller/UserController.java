package com.example.campusskillplatform.controller;

import com.example.campusskillplatform.common.Constants;
import com.example.campusskillplatform.common.Result;
import com.example.campusskillplatform.dto.ForgotPasswordDTO;
import com.example.campusskillplatform.dto.LoginDTO;
import com.example.campusskillplatform.dto.RegisterDTO;
import com.example.campusskillplatform.dto.UserUpdateDTO;
import com.example.campusskillplatform.entity.User;
import com.example.campusskillplatform.service.SmsService;
import com.example.campusskillplatform.service.UserService;
import com.example.campusskillplatform.utils.TokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 用户模块控制器
 * 处理用户注册、登录、信息查询、修改、头像上传等接口
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private SmsService smsService;

    @Autowired
    private TokenUtil tokenUtil;

    @Value("${upload.path}")
    private String uploadPath;

    /**
     * 根据用户ID查询用户信息（用于前端显示昵称等）
     * @param id 用户ID
     * @return 用户信息（密码已隐藏）
     */
    @GetMapping("/{id}")
    public Result<User> getUserById(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null) {
            return Result.success(null); // 避免前端报错，返回null
        }
        user.setPassword(null); // 安全处理：隐藏密码
        return Result.success(user);
    }

    /**
     * 用户注册
     * @param registerDTO 注册信息（用户名、密码、手机号、验证码，带校验注解）
     * @return 新用户的ID
     */
    @PostMapping("/register")
    public Result<Long> register(@Valid @RequestBody RegisterDTO registerDTO) {
        if (!smsService.verifyCode(registerDTO.getPhone(), registerDTO.getCode())) {
            throw new RuntimeException("验证码错误或已过期");
        }

        Long userId = userService.register(
                registerDTO.getUsername(),
                registerDTO.getPassword(),
                registerDTO.getPhone()
        );
        return Result.success(userId);
    }

    /**
     * 用户登录
     * @param loginDTO 登录信息（用户名、密码，带校验注解）
     * @return JWT令牌（用于后续请求的身份认证）
     */
    @PostMapping("/login")
    public Result<String> login(@Valid @RequestBody LoginDTO loginDTO) {
        String token = userService.login(
                loginDTO.getUsername(),
                loginDTO.getPassword()
        );
        return Result.success(token);
    }

    /**
     * 忘记密码（通过手机号验证重置密码）
     * @param forgotPasswordDTO 忘记密码信息（手机号、验证码、新密码、确认密码）
     * @return 操作结果提示
     */
    @PostMapping("/forgot-password")
    public Result<String> forgotPassword(@Valid @RequestBody ForgotPasswordDTO forgotPasswordDTO) {
        if (!forgotPasswordDTO.getNewPassword().equals(forgotPasswordDTO.getConfirmPassword())) {
            throw new RuntimeException("两次输入的密码不一致");
        }

        userService.forgotPassword(
                forgotPasswordDTO.getPhone(),
                forgotPasswordDTO.getCode(),
                forgotPasswordDTO.getNewPassword()
        );
        return Result.success("密码重置成功");
    }

    @PostMapping("/send-code")
    public Result<String> sendCode(@RequestParam String phone) {
        smsService.sendCode(phone);
        return Result.success("验证码发送成功");
    }

    /**
     * 获取当前登录用户的信息
     * @param request HTTP请求（包含Token）
     * @return 当前用户信息（密码已隐藏）
     */
    @GetMapping("/info")
    public Result<User> getUserInfo(HttpServletRequest request) {
        Long userId = tokenUtil.getUserId(request);
        if (userId == null) {
            throw new RuntimeException("未登录或Token无效");
        }
        User user = userService.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        user.setPassword(null); // 安全处理：隐藏密码
        return Result.success(user);
    }

    /**
     * 更新当前登录用户的信息
     * @param request HTTP请求（包含Token）
     * @param userUpdateDTO 要更新的字段（昵称、手机号等，带校验注解）
     * @return 操作结果提示
     */
    @PutMapping("/update")
    public Result<String> updateUser(HttpServletRequest request,
                                     @Valid @RequestBody UserUpdateDTO userUpdateDTO) {
        Long userId = tokenUtil.getUserId(request);
        if (userId == null) {
            throw new RuntimeException("未登录或Token无效");
        }
        boolean success = userService.updateUser(userId, userUpdateDTO);
        if (!success) {
            throw new RuntimeException("更新失败");
        }
        return Result.success("更新成功");
    }

    /**
     * 检查Token是否有效（用于前端路由守卫）
     * @param request HTTP请求（包含Token）
     * @return Token有效性确认信息
     */
    @GetMapping("/check-token")
    public Result<String> checkToken(HttpServletRequest request) {
        Long userId = tokenUtil.getUserId(request);
        if (userId == null) {
            throw new RuntimeException("Token无效");
        }
        return Result.success("Token有效，用户ID: " + userId);
    }

    /**
     * 上传用户头像
     * @param request HTTP请求（包含Token）
     * @param file 头像图片文件（支持jpg/jpeg/png/gif/bmp/webp）
     * @return 头像的访问URL路径
     */
    @PostMapping("/upload-avatar")
    public Result<String> uploadAvatar(HttpServletRequest request,
                                       @RequestParam("file") MultipartFile file) {
        // 身份校验
        Long userId = tokenUtil.getUserId(request);
        if (userId == null) {
            throw new RuntimeException("未登录或Token无效");
        }

        // 文件非空检查
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("请选择要上传的文件");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new RuntimeException("文件名不能为空");
        }

        // 文件类型校验（仅允许图片格式）
        String fileExtension = "";
        int dotIndex = originalFilename.lastIndexOf(".");
        if (dotIndex > 0) {
            fileExtension = originalFilename.substring(dotIndex).toLowerCase();
        }
        boolean allowed = false;
        for (String type : Constants.ALLOWED_IMAGE_TYPES) {
            if (type.equals(fileExtension)) {
                allowed = true;
                break;
            }
        }
        if (!allowed) {
            throw new RuntimeException("只允许上传图片文件（jpg, jpeg, png, gif, bmp, webp）");
        }

        // 创建上传目录（如果不存在）
        String uploadDir = uploadPath + File.separator + "avatar" + File.separator;
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new RuntimeException("创建上传目录失败: " + uploadDir);
            }
        }

        // 生成唯一文件名（UUID+原扩展名），防止文件名冲突
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String newFilename = uuid + fileExtension;
        String filePath = uploadDir + newFilename;

        try {
            // 先保存到临时文件，校验MIME类型后再移动到正式位置
            Path tempFile = Files.createTempFile("upload-", fileExtension);
            file.transferTo(tempFile.toFile());
            String contentType = Files.probeContentType(tempFile);
            if (contentType == null || !contentType.startsWith("image/")) {
                Files.delete(tempFile);
                throw new RuntimeException("文件不是有效的图片");
            }
            Files.move(tempFile, Paths.get(filePath));
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }

        // 更新数据库中的头像URL
        String avatarUrl = "/upload/avatar/" + newFilename;
        boolean success = userService.updateAvatar(userId, avatarUrl);
        if (!success) {
            throw new RuntimeException("头像更新失败");
        }
        return Result.success(avatarUrl);
    }

    /**
     * 测试文件上传功能（开发调试用）
     * @param file 上传的文件
     * @return 文件基本信息
     */
    @PostMapping("/test-file")
    public Result<String> testFileUpload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("文件为空");
        }
        String fileName = file.getOriginalFilename();
        long fileSize = file.getSize();
        return Result.success("收到文件：" + fileName + "，大小：" + fileSize + "字节");
    }
}
