package com.liuhecai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * M2：启用数据源 + MyBatis-Plus；仍排除 Redis（后续模块再开）。
 * M6：启用定时开奖拉取。
 */
@SpringBootApplication(exclude = {
        RedisAutoConfiguration.class
})
@EnableScheduling
public class LiuhecaiApplication {

    public static void main(String[] args) {
        SpringApplication.run(LiuhecaiApplication.class, args);
    }
}
