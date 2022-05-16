package com.nagisazz.livestore.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.nagisazz.livestore.pojo.VideoInfo;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class StreamService {

    @Autowired
    private BilibiliUploadService bilibiliUploadService;

    @Async
    public void upload(VideoInfo videoInfo) {
        try {
            bilibiliUploadService.upload(videoInfo, false);
        } catch (Exception e) {
            log.error("上传文件失败", e);
        }
    }
}
