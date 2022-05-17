package com.nagisazz.livestore.util;

import java.io.File;

import lombok.extern.slf4j.Slf4j;
import ws.schild.jave.Encoder;
import ws.schild.jave.EncoderException;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.ScreenExtractor;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;
import ws.schild.jave.encode.VideoAttributes;
import ws.schild.jave.info.AudioInfo;
import ws.schild.jave.info.VideoSize;

@Slf4j
public class VideoUtil {

    /**
     * 压缩视频
     *
     * @param sourcePath 原视频路径
     * @param targetName 新视频文件名
     * @return
     */
    public static synchronized boolean compressionVideo(String sourcePath, String targetName) {
        File oriFile = new File(sourcePath);
        // 同目录下生成
        String newPath = sourcePath.substring(0, sourcePath.lastIndexOf("/")).concat("/").concat(targetName);
        File target = new File(newPath);
        try {
            MultimediaObject object = new MultimediaObject(oriFile);
            AudioInfo audioInfo = object.getInfo().getAudio();
            double mb = Math.ceil(sourcePath.length() / 1048576);
            log.info("开始压缩，原视频地址：{}，文件大小：{}MB", sourcePath, mb);

            long time = System.currentTimeMillis();
            // 视频属性设置
            int maxBitRate = 64000;
            int maxSamplingRate = 44100;
            int bitRate = 600000;
            int maxFrameRate = 15;
            int maxWidth = 1280;

            AudioAttributes audio = new AudioAttributes();
            // 设置通用编码格式10                   audio.setCodec("aac");
            // 设置最大值：比特率越高，清晰度/音质越好
            // 设置音频比特率,单位:b (比特率越高，清晰度/音质越好，当然文件也就越大 128000 = 182kb)
            if (audioInfo.getBitRate() > maxBitRate) {
                audio.setBitRate(new Integer(maxBitRate));
            }

            // 设置重新编码的音频流中使用的声道数（1 =单声道，2 = 双声道（立体声））。如果未设置任何声道值，则编码器将选择默认值 0。
            audio.setChannels(audioInfo.getChannels());
            // 采样率越高声音的还原度越好，文件越大
            // 设置音频采样率，单位：赫兹 hz
            // 设置编码时候的音量值，未设置为0,如果256，则音量值不会改变
            // audio.setVolume(256);
            if (audioInfo.getSamplingRate() > maxSamplingRate) {
                audio.setSamplingRate(maxSamplingRate);
            }

            // 视频编码属性配置
            ws.schild.jave.info.VideoInfo videoInfo = object.getInfo().getVideo();
            VideoAttributes video = new VideoAttributes();
            video.setCodec("h264");
            //设置音频比特率,单位:b (比特率越高，清晰度/音质越好，当然文件也就越大 800000 = 800kb)
            if (videoInfo.getBitRate() > bitRate) {
                video.setBitRate(bitRate);
            }

            // 视频帧率：15 f / s  帧率越低，效果越差
            // 设置视频帧率（帧率越低，视频会出现断层，越高让人感觉越连续），视频帧率（Frame rate）是用于测量显示帧数的量度。所谓的测量单位为每秒显示帧数(Frames per Second，简：FPS）或“赫兹”（Hz）。
            if (videoInfo.getFrameRate() > maxFrameRate) {
                video.setFrameRate(maxFrameRate);
            }

            // 限制视频宽高
            int width = videoInfo.getSize().getWidth();
            int height = videoInfo.getSize().getHeight();
            if (width > maxWidth) {
                float rat = (float) width / maxWidth;
                video.setSize(new VideoSize(maxWidth, (int) (height / rat)));
            }

            EncodingAttributes attr = new EncodingAttributes();
//                attr.setFormat("mp4");
            attr.setAudioAttributes(audio);
            attr.setVideoAttributes(video);

            // 速度最快的压缩方式， 压缩速度 从快到慢： ultrafast, superfast, veryfast, faster, fast, medium,  slow, slower, veryslow and placebo.
//                attr.setPreset(PresetUtil.VERYFAST);
//                attr.setCrf(27);
//                // 设置线程数
            attr.setEncodingThreads(Runtime.getRuntime().availableProcessors());

            Encoder encoder = new Encoder();
            encoder.encode(new MultimediaObject(oriFile), target, attr);
            log.info("压缩总耗时：" + (System.currentTimeMillis() - time) / 1000);
        } catch (Exception e) {
            log.error("压缩失败", e);
            return false;
        } finally {
            if (target.length() > 0) {
                oriFile.delete();
            }
        }
        return true;
    }

    public static boolean generateScreenImage(String sourcePath, String targetName, Integer second) {
        // 同目录下生成
        String targetUrl = sourcePath.substring(0, sourcePath.lastIndexOf("/")).concat("/").concat(targetName);

        MultimediaObject multimediaObject = new MultimediaObject(new File(sourcePath));
        ScreenExtractor screenExtractor = new ScreenExtractor();
        int width = -1;
        int height = -1;
        long millis = second * 1000;
        File outputFile = new File(targetUrl);
        int quality = 1;
        log.info("开始生成截图，原视频路径：{}，生成截图路径：{}", sourcePath, targetUrl);
        try {
            screenExtractor.renderOneImage(multimediaObject, width, height, millis, outputFile, quality);
        } catch (EncoderException e) {
            log.error("生成截图失败，原视频路径：{}，生成截图路径：{}", sourcePath, targetUrl, e);
            return false;
        }
        return true;
    }
}
