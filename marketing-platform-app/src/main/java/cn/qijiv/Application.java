package cn.qijiv;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 应用启动类，负责启动营销抽奖平台 Spring Boot 应用
 */
@SpringBootApplication
@EnableScheduling
public class Application {

    /**
     * 程序入口，启动 Spring Boot 应用
     *
     * @param args 命令行参数
     */
    public static void main(String[] args){
        SpringApplication.run(Application.class);
    }

}
