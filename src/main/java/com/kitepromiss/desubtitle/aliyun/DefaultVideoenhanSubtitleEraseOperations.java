package com.kitepromiss.desubtitle.aliyun;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;
import com.aliyun.videoenhan20200320.Client;
import com.aliyun.videoenhan20200320.models.EraseVideoSubtitlesAdvanceRequest;
import com.aliyun.videoenhan20200320.models.EraseVideoSubtitlesResponse;
import com.aliyun.videoenhan20200320.models.GetAsyncJobResultRequest;
import com.aliyun.videoenhan20200320.models.GetAsyncJobResultResponse;
import com.aliyun.videoenhan20200320.models.GetAsyncJobResultResponseBody;

/**
 * 使用官方 SDK 访问上海地域 videoenhan：Advance 流式提交 + 轮询查询。
 *
 * <p>源视频经 {@link EraseVideoSubtitlesAdvanceRequest#setVideoUrlObject} 以<strong>请求体流</strong>送达阿里云，
 * 与「仅传公网 {@code VideoUrl}」的同步 URL 模式不同：<strong>不要求</strong>本机 {@code data/videos/} 文件对公网或阿里云侧 HTTP 直连可见。
 * 任务成功后返回的 {@code Result} 内 {@code VideoUrl} 由<strong>本服务</strong>（{@code SendToDeSubtitleService}）出网下载落盘。
 *
 * <p>字幕擦除区域：未传 {@code BX/BY/BW/BH} 时，阿里云默认仅处理画面<strong>底部约 25% 通栏</strong>；字幕在顶部/中部或超出该带时会<strong>看起来完全没擦掉</strong>。可通过 {@code desubtitle.aliyun.subtitle-erase-region} 改为 {@code full}（全画面 0,0,1,1）。
 */
@Component
public class DefaultVideoenhanSubtitleEraseOperations implements VideoenhanSubtitleEraseOperations {

    private static final String ENDPOINT = "videoenhan.cn-shanghai.aliyuncs.com";
    private static final String REGION_ID = "cn-shanghai";

    /**
     * {@code bottom}：不传四参数，与官方默认底部字幕带一致；{@code full}：显式全画面归一化矩形，适合非底部硬字幕。
     */
    private final String subtitleEraseRegion;

    public DefaultVideoenhanSubtitleEraseOperations(
            @Value("${desubtitle.aliyun.subtitle-erase-region:full}") String subtitleEraseRegion) {
        this.subtitleEraseRegion = subtitleEraseRegion == null ? "full" : subtitleEraseRegion.trim();
    }

    @Override
    public String submitEraseLocalFile(
            Path videoFile,
            String accessKeyId,
            String accessKeySecret,
            SubtitleEraseBand regionOverride)
            throws Exception {
        Client client = newClient(accessKeyId, accessKeySecret);
        RuntimeOptions runtime = new RuntimeOptions();
        runtime.setReadTimeout(120_000);
        try (InputStream in = Files.newInputStream(videoFile)) {
            EraseVideoSubtitlesAdvanceRequest req = new EraseVideoSubtitlesAdvanceRequest().setVideoUrlObject(in);
            if (regionOverride != null) {
                req.setBX(regionOverride.bx())
                        .setBY(regionOverride.by())
                        .setBW(regionOverride.bw())
                        .setBH(regionOverride.bh());
            } else {
                applySubtitleEraseRegion(req);
            }
            EraseVideoSubtitlesResponse res = client.eraseVideoSubtitlesAdvance(req, runtime);
            if (res == null || res.getBody() == null) {
                throw new IllegalStateException("eraseVideoSubtitlesAdvance 响应体为空");
            }
            String rid = res.getBody().getRequestId();
            if (rid == null || rid.isBlank()) {
                throw new IllegalStateException("EraseVideoSubtitles 未返回 RequestId");
            }
            return rid;
        }
    }

    @Override
    public SubtitleAsyncJobState getAsyncJob(String jobId, String accessKeyId, String accessKeySecret)
            throws Exception {
        Client client = newClient(accessKeyId, accessKeySecret);
        RuntimeOptions runtime = new RuntimeOptions();
        runtime.setReadTimeout(45_000);
        GetAsyncJobResultRequest req = new GetAsyncJobResultRequest().setJobId(jobId);
        GetAsyncJobResultResponse res = client.getAsyncJobResultWithOptions(req, runtime);
        if (res == null || res.getBody() == null) {
            return new SubtitleAsyncJobState(null, null, null, "GetAsyncJobResult 响应体为空");
        }
        GetAsyncJobResultResponseBody body = res.getBody();
        if (body.getData() == null) {
            return new SubtitleAsyncJobState(null, null, null, "GetAsyncJobResult.Data 为空");
        }
        var d = body.getData();
        return new SubtitleAsyncJobState(d.getStatus(), d.getResult(), d.getErrorCode(), d.getErrorMessage());
    }

    private void applySubtitleEraseRegion(EraseVideoSubtitlesAdvanceRequest req) {
        if ("bottom".equalsIgnoreCase(subtitleEraseRegion)) {
            return;
        }
        if ("full".equalsIgnoreCase(subtitleEraseRegion)) {
            req.setBX(0f).setBY(0f).setBW(1f).setBH(1f);
            return;
        }
        throw new IllegalStateException(
                "未知 desubtitle.aliyun.subtitle-erase-region="
                        + subtitleEraseRegion
                        + "，请使用 full 或 bottom");
    }

    private static Client newClient(String accessKeyId, String accessKeySecret) throws Exception {
        Config config = new Config().setAccessKeyId(accessKeyId).setAccessKeySecret(accessKeySecret);
        config.endpoint = ENDPOINT;
        config.regionId = REGION_ID;
        return new Client(config);
    }
}
