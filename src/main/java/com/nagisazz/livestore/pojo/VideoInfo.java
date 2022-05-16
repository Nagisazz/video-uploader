package com.nagisazz.livestore.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VideoInfo {

    /**
     * 视频文件地址
     */
    private String path;

    /**
     * 视频文件名称
     */
    private String fileName;

    /**
     * 标题
     */
    private String title;

    /**
     * 描述
     */
    private String desc;

    /**
     * 标签
     */
    private String tag;

    /**
     * 封面图
     */
    private String cover;

}
