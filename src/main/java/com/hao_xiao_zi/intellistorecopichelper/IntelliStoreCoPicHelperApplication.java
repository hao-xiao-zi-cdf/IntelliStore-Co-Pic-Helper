package com.hao_xiao_zi.intellistorecopichelper;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@MapperScan("com.hao_xiao_zi.intellistorecopichelper.mapper")
@EnableAspectJAutoProxy(exposeProxy = true) //开启注解用于获取当前对象的代理对象
public class IntelliStoreCoPicHelperApplication {

    public static void main(String[] args) {
        SpringApplication.run(IntelliStoreCoPicHelperApplication.class, args);
    }

}
