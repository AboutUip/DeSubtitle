package com.kitepromiss.desubtitle.api;

import java.util.List;

/** {@code POST /sendToDeSubtitle} 成功时的 JSON 体。 */
public record SendToDeSubtitleBatchResponse(List<SendToDeSubtitleItemResult> results) {}
