package com.kitepromiss.desubtitle.aliyun;

/** EraseVideoSubtitles 归一化矩形：BX/BY 左上角，BW/BH 相对视频宽高的比例，范围 [0,1]。 */
public record SubtitleEraseBand(float bx, float by, float bw, float bh) {}
