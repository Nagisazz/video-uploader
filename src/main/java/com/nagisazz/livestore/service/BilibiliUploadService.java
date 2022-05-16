package com.nagisazz.livestore.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.nagisazz.livestore.pojo.FileInfo;
import com.nagisazz.livestore.pojo.VideoInfo;
import com.nagisazz.livestore.task.FileUploadTask;
import com.nagisazz.livestore.util.ChromeUtil;
import com.nagisazz.livestore.util.CompressUtil;
import com.nagisazz.livestore.util.RequestsBuilder;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;


@Setter
@Getter
@Slf4j
@Service
public class BilibiliUploadService {

    @Value("${bilibili.cookie}")
    private String cookie;
    @Autowired
    private FileUploadTask fileUploadTask;
    private String fileAuth;
    private String metaAuth;

    /**
     * B站发布视频入口方法
     * @param videoInfo {@link VideoInfo}
     * @param needCompress 是否需要压缩
     */
    public synchronized void upload(VideoInfo videoInfo, boolean needCompress) {
        String filePath = videoInfo.getPath();
        String fileName = videoInfo.getFileName();
        if (needCompress && !CompressUtil.compressionVideo(filePath + "/" + fileName, fileName = "c" + fileName)) {
            return;
        }
        File file = new File(filePath + "/" + fileName);

//        https://archive.biliimg.com/bfs/archive/824ea31244a18a8407b8fdb4fc6f6d0ad610107e.jpg

        JSONObject addParam = JSONObject.parseObject(
                "{\"copyright\":1," +
                        "\"videos\":" +
                        "[{\"filename\":\"$filename$\"," +
                        "\"title\":\"" + videoInfo.getTitle() + "\"," +
                        "\"desc\":\"\"," +
                        "\"cid\":\"$cid$\"}]," +
                        "\"cover\":\"" + videoInfo.getCover() + "\"," +
                        "\"interactive\":0," +
                        "\"tid\":21," +
                        "\"title\":\"" + videoInfo.getTitle() + "\"," +
                        "\"tag\":\"" + videoInfo.getTag() + "\"," +
                        "\"mission_id\":0," +
                        "\"desc_format_id\":0," +
                        "\"desc\":\"" + videoInfo.getDesc() + "\"," +
                        "\"dynamic\":\"\"," +
                        "\"act_reserve_create\":0," +
                        "\"no_disturbance\":0," +
                        "\"dolby\":0," +
                        "\"no_reprint\":1," +
                        "\"subtitle\":{\"open\":1,\"lan\":\"\"}," +
                        "\"csrf\":\"$csrf$\"}");
        start(filePath, fileName, file, addParam);
    }

    private void start(String path, String name, File file, JSONObject addParam) {
        String fileUrl = "https://member.bilibili.com/preupload?name=" + name + "&size=" + file.length() + "&upcdn=qn" +
                "&probe_version=20211012&r=upos&profile=ugcfx%2Fbup&ssl=0&version=2.10.4.0&build=2100400&webVersion=2.0.0";
        final JSONObject fileOrigin = JSON.parseObject(get(fileUrl));

        String metaUrl = "https://member.bilibili.com/preupload?name=file_meta.txt&size=2000" +
                "&r=upos&profile=fxmeta%2Fbup&ssl=0&version=2.10.4&build=2100400&webVersion=2.0.0";
        final JSONObject metaOrigin = JSON.parseObject(get(metaUrl));

        if ("1".equals(fileOrigin.get("OK") + "")) {
            fileUrl = "https:" + fileOrigin.get("endpoint") + fileOrigin.getString("upos_uri").split("upos:/")[1];
            metaUrl = "https:" + metaOrigin.get("endpoint") + metaOrigin.getString("upos_uri").split("upos:/")[1];
            fileAuth = fileOrigin.getString("auth");
            metaAuth = metaOrigin.getString("auth");

            fileOrigin.put("fileSize", file.length());
            fileOrigin.put("uploadId", getFileUploadId(fileUrl, fileOrigin, metaOrigin));

            metaOrigin.put("uploadId", getMetaUploadId(metaUrl));

            metaOrigin.put("bName", metaOrigin.getString("upos_uri").substring(metaOrigin.getString("upos_uri").lastIndexOf("/") + 1));
            metaOrigin.put("urlPrefix", metaUrl);
            metaOrigin.put("fileName", "BUploader_4_3gt4p_" + String.valueOf(System.currentTimeMillis()).substring(0, 10) + "_meta.txt");
            metaOrigin.put("path", path);
            metaOrigin.put("name", name);
            metaOrigin.put("fileSize", file.length());
            metaUpload(metaOrigin);

            fileOrigin.put("urlPrefix", fileUrl);
            fileOrigin.put("fileName", name);
            partFileUpload(file, fileOrigin, addParam);
        }
    }

    private String getFileUploadId(String url, JSONObject fileOrigin, JSONObject metaOrigin) {
        return JSONObject.parseObject(post(url + "?uploads&output=json&profile=ugcfx%2Fbup&filesize=" + fileOrigin.get("fileSize") +
                "&partsize=" + fileOrigin.get("chunk_size") + "&meta_upos_uri=" +
                metaOrigin.getString("upos_uri").replace(":", "%3A").replace("/", "%2F") +
                "&biz_id=" + fileOrigin.get("biz_id"), null, fileAuth)).getString("upload_id");
    }

    private String getMetaUploadId(String url) {
        return JSONObject.parseObject(post(url + "?uploads&output=json&", null, metaAuth)).getString("upload_id");
    }

    private void metaUpload(JSONObject origin) {
        JSONObject param = handleParam(ChromeUtil.getMeta(origin.getString("path")
                        .substring(origin.getString("path").lastIndexOf("/") + 1),
                origin.getString("name"), origin.getInteger("fileSize")));
        int size = param.toJSONString().length();
        put(origin.getString("urlPrefix") + "?partNumber=1&uploadId=" + origin.getString("uploadId") +
                "&chunk=0&chunks=1&size=" + size + "&start=0&end=" + size + "&total=" + size, param, metaAuth);
        String endStr = "{\"parts\":[{\"partNumber\":1,\"eTag\":\"etag\"}]}";
        post(origin.getString("urlPrefix") + "?output=json&name=" + origin.getString("fileName") + "&profile=&uploadId=" +
                origin.getString("uploadId") + "&biz_id=", JSONObject.parseObject(endStr), metaAuth);
    }

    private void partFileUpload(File file, JSONObject origin, JSONObject addParam) {
        long partSize = origin.getLong("chunk_size");
        long fileSize = file.length();
        int partCount = (int) (fileSize / partSize);
        if (partSize * partCount < fileSize) {
            partCount++;
        }
        log.info("文件大小：{}MB", fileSize / 1024 / 1024);
        log.info("文件一共分为{}：", partCount + "块,进度0/" + partCount);

        long startTime = System.currentTimeMillis();
        CountDownLatch countDownLatch = new CountDownLatch(partCount);
        for (int i = 0; i < partCount; i++) {
            int num = i + 1;
            long partStart = i * partSize;
            long curPartSize = (i + 1 == partCount) ? (fileSize - partStart) : partSize;

            FileInfo fileInfo = new FileInfo();
            fileInfo.setAuth(fileAuth);
            fileInfo.setFile(file);
            fileInfo.setNum(num);
            fileInfo.setPartCount(partCount);
            fileInfo.setPartSize(curPartSize);
            fileInfo.setPartStart(partStart);
            fileInfo.setUploadId(origin.getString("uploadId"));
            fileInfo.setUrl(origin.getString("urlPrefix"));

            fileUploadTask.start(fileInfo, countDownLatch, startTime);
        }
        try {
            countDownLatch.await();
            log.info("分块文件全部上传完毕，总耗时：{}秒", (System.currentTimeMillis() - startTime) / 1000);
            ArrayList list = new ArrayList();
            for (int i = 0; i < partCount; i++) {
                HashMap map = new HashMap();
                map.put("partNumber", i + 1);
                map.put("eTag", "'etag'");
                list.add(map);
            }
            HashMap map = new HashMap();
            map.put("parts", list);
            afterUpload(JSONObject.parseObject(JSON.toJSONString(map)), origin, addParam);
        } catch (InterruptedException e) {
            log.error("上传失败", e);
        }
    }

    private void afterUpload(JSONObject param, JSONObject origin, JSONObject addParam) {
        log.info("开始准备上传");
        String src = "?output=json&name=" + origin.getString("fileName") + "&profile=ugcfx%2Fbup&uploadId=" +
                origin.getString("uploadId") + "&biz_id=" + origin.getString("biz_id");
        String res = post(origin.getString("urlPrefix") + src, param, fileAuth);
        if (StringUtils.isNotBlank(res) && res.contains("\"OK\":1")) {
            endUpload(origin, addParam);
        }
    }

    private void endUpload(JSONObject origin, JSONObject addParam) {
        log.info("开始提交");
        String csrf = csrf();
        String url = "https://member.bilibili.com/x/vu/web/add/v3?t=" + System.currentTimeMillis() + "&csrf=" + csrf;
        addParam.put("csrf", csrf);

        String filename = origin.getString("upos_uri").split("upos://ugcfxcs/")[1].split("\\.")[0];
        addParam.getJSONArray("videos").getJSONObject(0).put("filename", filename);
        addParam.getJSONArray("videos").getJSONObject(0).put("cid", origin.getIntValue("biz_id"));
        post(url, addParam, fileAuth);
    }

    private String csrf() {
        return StringUtils.substringBetween(cookie, "bili_jct=", ";");
    }

    private JSONObject handleParam(String originStr) {
        JSONObject origin = JSONObject.parseObject(originStr);
        //处理stream
        //获取原始streams
        JSONArray streams = origin.getJSONArray("streams");
        //video_meta
        JSONObject videoMeta = streams.getJSONObject(0);
        videoMeta.put("level", new StringBuilder(videoMeta.getString("level")).insert(1, "."));
        videoMeta.put("rotate", 0);
        //audio_meta
        JSONObject audioMeta = streams.getJSONObject(1);
        //container_meta
        JSONObject conMeta = new JSONObject();
        conMeta.put("format_name", origin.getJSONObject("format").getString("format_name"));
        conMeta.put("duration", origin.getJSONObject("format").get("duration"));
        //封装meta
        JSONObject meta = new JSONObject();
        meta.put("video_meta", videoMeta);
        meta.put("audio_meta", audioMeta);
        meta.put("container_meta", conMeta);

        //封装完整param
        JSONObject param = new JSONObject();
        param.put("code", 0);
        param.put("filename", origin.getString("bName"));
        param.put("meta", meta);
        param.put("key_frames", new JSONArray());
        param.put("filesize", origin.getJSONObject("format").get("size"));
        param.put("version", "2.3.7");
        param.put("webVersion", "2.0.0");

        return param;
    }

    private String get(String url) {
        Map<String, String> headers = new HashMap();
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        headers.put("Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2");
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:86.0) Gecko/20100101 Firefox/86.0");
        headers.put("Cookie", cookie);
        return RequestsBuilder.sendGet(url, headers);
    }

    private String post(String url, JSONObject param, String auth) {
        Map<String, String> headers = new HashMap();
        headers.put("Accept", "*/*");
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.66 Safari/537.36");
        headers.put("Cookie", cookie);
        headers.put("Origin", "https://member.bilibili.com");
        headers.put("Referer", "https://member.bilibili.com/video/upload.html");
        headers.put("X-Upos-Auth", auth);
        String response = "";
        if (param == null) {
            response = RequestsBuilder.sendPost(url, "", headers);
        } else {
            response = RequestsBuilder.sendPost(url, param.toJSONString(), headers);
        }
        return response;
    }

    private String put(String url, JSONObject param, String auth) {
        Map<String, String> headers = new HashMap();
        headers.put("Accept", "*/*");
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.66 Safari/537.36");
        headers.put("Cookie", cookie);
        headers.put("Origin", "https://member.bilibili.com");
        headers.put("Referer", "https://member.bilibili.com/video/upload.html");
        headers.put("X-Upos-Auth", auth);
        String response = "";
        if (param == null) {
            response = RequestsBuilder.sendPut(url, "", headers);
        } else {
            response = RequestsBuilder.sendPut(url, param.toJSONString(), headers);
        }
        return response;
    }
}
