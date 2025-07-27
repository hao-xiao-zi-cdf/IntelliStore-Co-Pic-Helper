package com.hao_xiao_zi.intellistorecopichelper;

import org.apache.shardingsphere.spring.boot.ShardingSphereAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@MapperScan("com.hao_xiao_zi.intellistorecopichelper.mapper")
@EnableAspectJAutoProxy(exposeProxy = true) //开启注解用于获取当前对象的代理对象
@SpringBootApplication
//@SpringBootApplication(exclude = {ShardingSphereAutoConfiguration.class}) // 禁用分库分表sharding-jdbc
public class IntelliStoreCoPicHelperApplication {

    public static void main(String[] args) {
        SpringApplication.run(IntelliStoreCoPicHelperApplication.class, args);
    }

}
