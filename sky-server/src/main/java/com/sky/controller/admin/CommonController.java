package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/admin/common")
public class CommonController {

    @Autowired
    private FileService fileService;

    /*
        文件上传
        * @param file
        * @return
        */
    @PostMapping("/upload")
    public Result<String> upload(MultipartFile file){
        log.info("文件上传：{}", file);
        String filePath = fileService.upload(file);
        return Result.success(filePath);
    }
}
