package com.sky.properties;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "sky.alioss")
@Data
//覆盖 @Data 生成的 toString，避免 AccessKey 随对象打印泄露到日志
@ToString(exclude = {"accessKeyId", "accessKeySecret"})
public class AliOssProperties {

    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;

}
