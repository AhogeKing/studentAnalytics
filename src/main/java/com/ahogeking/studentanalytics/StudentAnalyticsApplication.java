package com.ahogeking.studentanalytics;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.ahogeking.studentanalytics.mapper")
@SpringBootApplication
public class StudentAnalyticsApplication {
    public static void main(String[] args) {
        SpringApplication.run(StudentAnalyticsApplication.class, args);
    }
}
