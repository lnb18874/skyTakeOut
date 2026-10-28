package com.sky.utils;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.sky.constant.MessageConstant;
import com.sky.exception.FileUploadException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import java.io.ByteArrayInputStream;

@Data
@AllArgsConstructor
@Slf4j
public class AliOssUtil {

    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;

    /**
     * 文件上传
     *
     * @param bytes
     * @param objectName
     * @return
     */
    public String upload(byte[] bytes, String objectName) {

        // 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        try {
            // 本地代理链路不稳定时响应可能被污染（InvalidResponse等），重试一次大概率能躲过瞬时抖动
            for (int attempt = 1; ; attempt++) {
                try {
                    // 创建PutObject请求。
                    ossClient.putObject(bucketName, objectName, new ByteArrayInputStream(bytes));
                    break;
                } catch (OSSException oe) {
                    //请求已到达OSS，但因服务端拒绝或响应被篡改而失败（鉴权、bucket、代理污染等）
                    if (attempt >= 2) {
                        throw new FileUploadException(MessageConstant.UPLOAD_FAILED);
                    }
                    log.warn("OSS上传被拒绝, 重试第{}次, ErrorMessage:{}, ErrorCode:{}, RequestId:{}, HostId:{}",
                            attempt, oe.getErrorMessage(), oe.getErrorCode(), oe.getRequestId(), oe.getHostId());
                } catch (ClientException ce) {
                    //客户端与OSS通信异常（网络不可达、代理链路异常等本地问题）
                    if (attempt >= 2) {
                        log.error("OSS客户端通信异常, ErrorMessage:{}", ce.getMessage());
                        throw new FileUploadException(MessageConstant.UPLOAD_FAILED);
                    }
                    log.warn("OSS客户端通信异常, 重试第{}次, ErrorMessage:{}", attempt, ce.getMessage());
                }
            }
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }

        //文件访问路径规则 https://BucketName.Endpoint/ObjectName
        StringBuilder stringBuilder = new StringBuilder("https://");
        stringBuilder
                .append(bucketName)
                .append(".")
                .append(endpoint)
                .append("/")
                .append(objectName);

        log.info("文件上传到:{}", stringBuilder.toString());

        return stringBuilder.toString();
    }
}
