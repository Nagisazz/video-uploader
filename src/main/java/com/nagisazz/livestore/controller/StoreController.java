package com.nagisazz.livestore.controller;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.http.HttpServletResponse;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nagisazz.livestore.annotation.RestKeeper;
import com.nagisazz.livestore.constants.Constants;
import com.nagisazz.livestore.pojo.VideoInfo;
import com.nagisazz.livestore.service.StreamService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class StoreController {

    @Value("${file.rootpath}")
    private String rootpath;

    @Autowired
    private StreamService streamService;

    @GetMapping("/upload")
    @RestKeeper
    public String startUpload(@RequestParam("path") String path, @RequestParam("fileName") String fileName,
                              @RequestParam("title") String title, @RequestParam("desc") String desc,
                              @RequestParam("tag") String tag) {
        VideoInfo videoInfo = VideoInfo.builder().path(rootpath + path).fileName(fileName).title(title).desc(desc).tag(tag).build();
        streamService.upload(videoInfo);
        return "success";
    }

    @GetMapping("/token")
    public String getToken() {
        Constants.TOKEN = String.valueOf(System.currentTimeMillis());
        return Constants.TOKEN;
    }

    @GetMapping("/getFile/{path}/{file}")
    public void getFile(@PathVariable("path") String path, @PathVariable("file") String file, HttpServletResponse response) {
        try (InputStream inputStream = new FileInputStream(rootpath + path + "/" + file);
             OutputStream outputStream = response.getOutputStream()) {
            IOUtils.copy(inputStream, outputStream);
            outputStream.flush();
        } catch (Exception e) {
            log.info("获取输出流失败", e);
        }
    }
}
