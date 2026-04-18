package com.example.test;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class TestBCrypt {

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String dbPwd = "$2a$10$qKVff0fb.pvQq/vCTFUHOOL78/9RjvnG0rxrEGtNbxMhRcLtGePua";
        String inputPwd = "admin123";
        boolean isMatch = encoder.matches(inputPwd, dbPwd);
        System.out.println("密码是否匹配：" + isMatch);
    }
}