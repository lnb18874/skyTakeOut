package com.sky.service.impl;

import com.sky.constant.MessageConstant;
import com.sky.exception.FileUploadException;
import com.sky.service.FileService;
import com.sky.utils.AliOssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
@Slf4j
public class FileServiceImpl implements FileService {

    @Autowired
    private AliOssUtil aliOssUtil;

    @Override
    public String upload(MultipartFile file) {
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            log.error("读取上传文件流失败：{}", file.getOriginalFilename(), e);
            throw new FileUploadException(MessageConstant.UPLOAD_FAILED);
        }
        String originalFilename = file.getOriginalFilename();
        if(originalFilename==null||!originalFilename.contains(".")){
            throw new FileUploadException(MessageConstant.UPLOAD_FAILED);
        }
        String suffix=originalFilename.substring(originalFilename.lastIndexOf("."));
        String objectName= UUID.randomUUID().toString()+suffix;
        String filePath = aliOssUtil.upload(bytes, objectName);
        return filePath;
    }

}
