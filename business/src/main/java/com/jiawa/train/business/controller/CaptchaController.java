package com.jiawa.train.business.controller;

import com.wf.captcha.SpecCaptcha;
import com.wf.captcha.base.Captcha;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@RestController
public class CaptchaController {
    // 注入Redis模板（替换成你项目中实际的Redis操作类）
    private final StringRedisTemplate stringRedisTemplate;

    // 构造器注入（或用@Autowired，根据你项目的注入方式）
    public CaptchaController(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 图形验证码接口（替换你原来的旧接口）
     * @param imageCodeToken 验证码唯一标识（前端传的token）
     * @param response 响应对象，用于输出验证码图片
     */
    @GetMapping("/image-code/{imageCodeToken}")
    public void generateCaptcha(@PathVariable("imageCodeToken") String imageCodeToken,
                                HttpServletResponse response) throws IOException {
        // ========== 1. 配置验证码参数 ==========
        int width = 120;    // 验证码图片宽度
        int height = 40;    // 验证码图片高度
        int length = 4;     // 验证码字符位数

        // ========== 2. 创建SpecCaptcha对象（easy-captcha核心） ==========
        // SpecCaptcha：数字+字母混合验证码（最常用）
        // 可选类型：
        // - GifCaptcha：动态GIF验证码
        // - ArithmeticCaptcha：算术验证码（如 1+2=?）
        // - ChineseCaptcha：中文验证码
        SpecCaptcha captcha = new SpecCaptcha(width, height, length);

        // 可选配置（根据需求开启）
        captcha.setCharType(Captcha.TYPE_DEFAULT); // 默认：数字+字母混合
        // captcha.setCharType(Captcha.TYPE_ONLY_NUMBER); // 仅数字（推荐登录/注册用）
        // captcha.setFont(Captcha.FONT_1); // 字体（可选FONT_1到FONT_5）
        //captcha.setInterference(Captcha.INTERMEDIATE); // 干扰强度：低/中/高

        // ========== 3. 获取验证码字符，存入Redis ==========
        String captchaCode = captcha.text().toLowerCase(); // 转小写（避免大小写校验问题）
        // 存入Redis，设置5分钟过期（和原逻辑一致）
        stringRedisTemplate.opsForValue()
                .set(imageCodeToken, captchaCode, 300, TimeUnit.SECONDS);

        // ========== 4. 设置响应头，输出验证码图片 ==========
        // 禁止缓存（关键：避免浏览器缓存验证码图片）
        response.setHeader("Cache-Control", "no-store, no-cache");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
        // 指定响应类型为图片
        response.setContentType("image/png");

        // 将验证码图片写入响应输出流
        try {
            captcha.out(response.getOutputStream());
        } catch (IOException e) {
            // 异常处理：返回500错误
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "验证码生成失败");
        }
    }
}
