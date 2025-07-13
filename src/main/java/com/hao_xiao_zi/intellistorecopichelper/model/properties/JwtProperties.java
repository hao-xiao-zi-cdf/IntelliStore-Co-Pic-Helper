package com.hao_xiao_zi.intellistorecopichelper.model.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "icph.jwt")
@Data
public class JwtProperties {

    /**
     * 密钥
     */
    private String adminSecretKey;

    /**
     * 令牌的有效期
     */
    private long adminTtl;

    /**
     * 设置前端传递过来的令牌名称
     */
    private String adminTokenName;
}
