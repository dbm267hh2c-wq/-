package com.example;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class PasswordEncoder {

    /**
     * 将密码转为 Base64，再把编码结果的前 5 个字符移到末尾。
     *
     * @param password 原始密码
     * @return 处理后的字符串
     */
    public static String encodePassword(String password) {
        String base64 = Base64.getEncoder()
                .encodeToString(password.getBytes(StandardCharsets.UTF_8));

        if (base64.length() <= 5) {
            return base64;
        }

        return base64.substring(5) + base64.substring(0, 5);
    }

    public static void main(String[] args) {
        String[] samples = {"hello", "123456", "password", "a", "test1234"};

        for (String password : samples) {
            String base64 = Base64.getEncoder()
                    .encodeToString(password.getBytes(StandardCharsets.UTF_8));
            String encoded = encodePassword(password);

            System.out.printf("password=%s%n", password);
            System.out.printf("base64=%s%n", base64);
            System.out.printf("result=%s%n%n", encoded);
        }
    }
}
