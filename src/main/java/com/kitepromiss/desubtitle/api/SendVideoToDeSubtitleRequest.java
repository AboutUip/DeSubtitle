package com.kitepromiss.desubtitle.api;

/**
 * {@code POST /sendVideoToDeSubtitle} 请求体。
 *
 * @param videoId 视频 id
 * @param subtitlePosition 烧录字幕竖直位置：{@code top}|{@code upper}|{@code middle}|{@code lower}|{@code bottom}；缺省或空为
 *     {@link SubtitleVerticalPosition#BOTTOM}
 */
public record SendVideoToDeSubtitleRequest(String videoId, String subtitlePosition) {}
