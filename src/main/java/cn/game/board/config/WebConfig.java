package cn.game.board.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author <a href="mailto:sunchao@gtmap.cn">sunchao</a
 * @version 1.0, 2025/10/27
 * @description CORS 跨域配置
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 允许所有路径 ("/**")
        registry.addMapping("/api/**")
                // 允许所有来源 (比如 file://, localhost:xxxx 等)
                .allowedOrigins("*")
                // 允许的方法
                .allowedMethods("GET", "POST");
    }
}
