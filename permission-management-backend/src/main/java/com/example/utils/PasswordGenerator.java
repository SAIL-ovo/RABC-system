package com.example.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = "admin123";
        String hashedPassword = encoder.encode(password);
        System.out.println("原始密码: " + password);
        System.out.println("加密后密码: " + hashedPassword);
    }
}
