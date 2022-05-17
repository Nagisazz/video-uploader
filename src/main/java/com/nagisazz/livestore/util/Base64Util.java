package com.nagisazz.livestore.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import sun.misc.BASE64Encoder;

@Slf4j
@UtilityClass
public class Base64Util {

    /**
     * 将图片文件转化为Base64字符串
     *
     * @param imgFile 本地图片地址
     * @return
     */
    public static String transformImg(String imgFile) {
        byte[] data = null;
        // 读取图片字节数组
        try (InputStream in = new FileInputStream(imgFile)) {
            data = new byte[in.available()];
            in.read(data);
        } catch (IOException e) {
            log.error("图片转Base64失败，filePath：{}", imgFile, e);
        }
        // 对字节数组Base64编码
        BASE64Encoder encoder = new BASE64Encoder();
        // 返回Base64编码过的字节数组字符串
        return encoder.encode(data);
    }
}
