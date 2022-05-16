package com.nagisazz.livestore.pojo;

import lombok.Data;

import java.io.File;

@Data
public class FileInfo {

    private String url;

    //分块编号
    private int num;

    //当前分段大小
    private long partSize;

    //当前分段在输入流中的起始位置
    private long partStart;

    //总文件
    private File file;

    private int partCount;

    private String uploadId;

    private String auth;
}
