package com.hospital.integrity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 医院个人科研成果诚信综合评价系统 启动类
 */
@EnableScheduling
@SpringBootApplication
public class IntegrityApplication {

    public static void main(String[] args) {
        SpringApplication.run(IntegrityApplication.class, args);
    }
}
