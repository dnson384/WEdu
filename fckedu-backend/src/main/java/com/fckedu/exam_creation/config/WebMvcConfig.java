package com.fckedu.exam_creation.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Value("${app.static.dir}")
    private String baseStaticLocation;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/uploads/**")
                .addResourceLocations("file:" + baseStaticLocation + "uploads" + File.separator);

        registry.addResourceHandler("/static/avatars/**")
                .addResourceLocations("file:" + baseStaticLocation + "avatars" + File.separator);
    }
}
