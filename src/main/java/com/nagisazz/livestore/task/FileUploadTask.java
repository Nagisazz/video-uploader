package com.nagisazz.livestore.task;

import com.nagisazz.livestore.pojo.FileInfo;
import com.nagisazz.livestore.util.RequestsBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.InputStreamEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
public class FileUploadTask {

    @Value("${bilibili.cookie}")
    private String cookie;

    private ExecutorService executorService = Executors.newFixedThreadPool(1);

    public void start(FileInfo fileInfo, CountDownLatch countDownLatch, long startTime) {
        executorService.execute(new FileUploadRunnable(fileInfo, countDownLatch, startTime));
    }

    public String put(String url, String auth, InputStreamEntity entity) {
        Map<String, String> headers = new HashMap();
        headers.put("Accept", "*/*");
        headers.put("Accept-Encoding", "gzip, deflate, br");
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.66 Safari/537.36");
        headers.put("Cookie", cookie);
        headers.put("Origin", "https://member.bilibili.com");
        headers.put("Referer", "https://member.bilibili.com/video/upload.html");
        headers.put("X-Upos-Auth", auth);
        return RequestsBuilder.sendFilePut(url, entity, headers);
    }

    class FileUploadRunnable implements Runnable {

        private FileInfo fileInfo;

        private CountDownLatch countDownLatch;

        private long startTime;

        public FileUploadRunnable(FileInfo fileInfo, CountDownLatch countDownLatch, long startTime) {
            this.fileInfo = fileInfo;
            this.countDownLatch = countDownLatch;
            this.startTime = startTime;
        }

        public void run() {
            handleUpload(false);
        }

        private boolean handleUpload(Boolean retryFlag) {
            try (FileInputStream fis = new FileInputStream(fileInfo.getFile())) {
                long startPartTime = System.currentTimeMillis();
                String url = fileInfo.getUrl() + "?partNumber=" + fileInfo.getNum() +
                        "&uploadId=" + fileInfo.getUploadId() + "&chunks=" + fileInfo.getPartCount() +
                        "&chunk=" + fileInfo.getNum() + "&size=" + fileInfo.getPartSize() +
                        "&start=" + fileInfo.getPartStart() + "&end=" + (fileInfo.getPartStart() + fileInfo.getPartSize()) +
                        "&total=" + fileInfo.getFile().length();
                //跳过起始位置
                fis.skip(fileInfo.getPartStart());
                log.info("开始上传分块:{}", fileInfo.getNum());

                String response = put(url, fileInfo.getAuth(), new InputStreamEntity(fis, fileInfo.getPartSize()));
                if (StringUtils.isNotBlank(response) && response.contains("MULTIPART_PUT_SUCCESS")) {
                    countDownLatch.countDown();
                    log.info("进度{}，分片耗时：{}秒，总耗时：{}秒", (fileInfo.getPartCount() - countDownLatch.getCount()) + "/" +
                                    fileInfo.getPartCount(), (System.currentTimeMillis() - startPartTime) / 1000,
                            (System.currentTimeMillis() - startTime) / 1000);
                    return true;
                } else {
                    //失败时重试一次
                    log.error("分块:{}上传失败", fileInfo.getNum());
                    if (!retryFlag && !handleUpload(true)) {
                        countDownLatch.countDown();
                    }
                }
            } catch (Exception e) {
                log.error("分块:{}上传失败", fileInfo.getNum(), e);
            }
            return false;
        }
    }
}
