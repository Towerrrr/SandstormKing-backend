package com.t0r.sandstormkingbackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true)
@MapperScan("com.t0r.sandstormkingbackend.mapper")
public class SandstormkingBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(SandstormkingBackendApplication.class, args);
    }

}
