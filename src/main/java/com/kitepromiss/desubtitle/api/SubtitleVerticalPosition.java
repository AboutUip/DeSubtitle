package com.kitepromiss.desubtitle.api;

/**
 * 前端选择的字幕竖直区域，映射为阿里云 EraseVideoSubtitles 的矩形（多数为 BX=0、BW=1 的竖条；{@link #FULL} 为全幅
 * 0,0,1,1）。
 *
 * <p>JSON 小写英文见各常量 {@link #apiValue()}。
 */
public enum SubtitleVerticalPosition {
    /** 全画面 */
    FULL("full", 0f, 0f),
    /** 上半屏 */
    UPPER_HALF("upper_half", 0f, 0.5f),
    /** 下半屏 */
    LOWER_HALF("lower_half", 0.5f, 0.5f),
    /** 画面最上侧 */
    TOP("top", 0f, 0.20f),
    /** 偏上 */
    UPPER("upper", 0.20f, 0.20f),
    /** 垂直中部 */
    MIDDLE("middle", 0.40f, 0.20f),
    /** 偏下（与底部档衔接） */
    LOWER("lower", 0.60f, 0.15f),
    /** 底部通栏，与阿里云文档默认 BY=0.75、BH=0.25 一致 */
    BOTTOM("bottom", 0.75f, 0.25f);

    private final String apiValue;
    private final float by;
    private final float bh;

    SubtitleVerticalPosition(String apiValue, float by, float bh) {
        this.apiValue = apiValue;
        this.by = by;
        this.bh = bh;
    }

    public String apiValue() {
        return apiValue;
    }

    public float by() {
        return by;
    }

    public float bh() {
        return bh;
    }

    /**
     * 解析请求字段；空串或 {@code null} 视为 {@link #BOTTOM}。
     *
     * @throws IllegalArgumentException 非空但无法识别
     */
    public static SubtitleVerticalPosition fromRequest(String raw) {
        if (raw == null || raw.isBlank()) {
            return BOTTOM;
        }
        String t = raw.trim();
        for (SubtitleVerticalPosition p : values()) {
            if (p.apiValue.equalsIgnoreCase(t)) {
                return p;
            }
        }
        throw new IllegalArgumentException(raw);
    }
}
