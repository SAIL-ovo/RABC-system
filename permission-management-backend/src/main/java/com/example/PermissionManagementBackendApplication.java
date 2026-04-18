package com.example;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.mapper") // 添加这行，扫描 mapper 包
public class PermissionManagementBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(PermissionManagementBackendApplication.class, args);
    }
}