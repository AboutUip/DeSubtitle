package com.kitepromiss.desubtitle.video;

import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 将 {@code data/videos/}、{@code data/desubtitle/} 下的文件名限制为「单段名 + 可选扩展」，与上传/去字幕落盘规则一致，防止路径穿越。
 */
public final class VideoStorageFileNames {

    private static final Pattern SAFE_NAME =
            Pattern.compile(
                    "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}(\\.[a-zA-Z0-9]{1,8})?$");

    private VideoStorageFileNames() {}

    /** 与 {@link VideoUploadService} 生成的 {@code storedName} 及去字幕成品 {@code UUID+ext} 对齐。 */
    public static boolean isSafeStoredOrOutputFileName(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        if (name.indexOf('/') >= 0 || name.indexOf('\\') >= 0) {
            return false;
        }
        if (name.contains("..")) {
            return false;
        }
        return SAFE_NAME.matcher(name).matches();
    }

    /**
     * 在目录下安全解析单文件名：拒绝穿越、绝对路径片段及非法名。
     *
     * @return 规范化后的绝对路径；不合法时为空
     */
    public static Optional<Path> safeResolve(Path directory, String singleFileName) {
        if (!isSafeStoredOrOutputFileName(singleFileName)) {
            return Optional.empty();
        }
        Path base = directory.toAbsolutePath().normalize();
        Path candidate = base.resolve(singleFileName).normalize();
        if (!candidate.startsWith(base)) {
            return Optional.empty();
        }
        return Optional.of(candidate);
    }
}
