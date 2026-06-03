package com.example.campusskillplatform.service;

import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.dypnsapi20170525.models.CheckSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.CheckSmsVerifyCodeResponse;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsService.class);

    private static final long SEND_INTERVAL_MS = 60_000;

    private final ConcurrentHashMap<String, Long> lastSendTime = new ConcurrentHashMap<>();

    @Autowired
    private Client smsClient;

    @Value("${aliyun.sms.sign-name}")
    private String signName;

    @Value("${aliyun.sms.template-code}")
    private String templateCode;

    public void sendCode(String phone) {
        Long lastTime = lastSendTime.get(phone);
        if (lastTime != null && System.currentTimeMillis() - lastTime < SEND_INTERVAL_MS) {
            throw new RuntimeException("发送过于频繁，请60秒后再试");
        }

        try {
            SendSmsVerifyCodeRequest request = new SendSmsVerifyCodeRequest()
                    .setPhoneNumber(phone)
                    .setSignName(signName)
                    .setTemplateCode(templateCode)
                    .setTemplateParam("{\"code\":\"##code##\",\"min\":\"5\"}");
            SendSmsVerifyCodeResponse response = smsClient.sendSmsVerifyCode(request);
            if (!"OK".equals(response.getBody().getCode())) {
                log.error("短信发送失败：{}", response.getBody().getMessage());
                throw new RuntimeException("验证码发送失败：" + response.getBody().getMessage());
            }
            lastSendTime.put(phone, System.currentTimeMillis());
            log.info("验证码发送成功，手机号：{}", phone);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("短信发送异常", e);
            throw new RuntimeException("验证码发送失败，请稍后重试");
        }
    }

    public boolean verifyCode(String phone, String code) {
        try {
            CheckSmsVerifyCodeRequest request = new CheckSmsVerifyCodeRequest()
                    .setPhoneNumber(phone)
                    .setVerifyCode(code);
            CheckSmsVerifyCodeResponse response = smsClient.checkSmsVerifyCode(request);
            boolean pass = "PASS".equals(response.getBody().getModel().getVerifyResult());
            if (pass) {
                lastSendTime.remove(phone);
            }
            return pass;
        } catch (Exception e) {
            log.error("验证码校验异常", e);
            return false;
        }
    }
}
