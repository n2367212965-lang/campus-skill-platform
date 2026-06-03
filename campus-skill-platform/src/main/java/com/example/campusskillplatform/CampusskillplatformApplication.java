package com.example.campusskillplatform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.campusskillplatform.mapper")
public class CampusskillplatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusskillplatformApplication.class, args);
    }
}