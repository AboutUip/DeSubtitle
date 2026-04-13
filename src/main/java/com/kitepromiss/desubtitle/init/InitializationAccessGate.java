package com.kitepromiss.desubtitle.init;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.stereotype.Component;

import com.kitepromiss.desubtitle.config.JsonConfigLoader;
import com.kitepromiss.desubtitle.workspace.WorkspacePaths;

import tools.jackson.databind.JsonNode;

/**
 * 未初始化或初始化执行期间，配合 {@link com.kitepromiss.desubtitle.web.InitializationGateInterceptor} 限制对业务 API 的访问。
 *
 * <p>完成态以 {@code runtime.json} 为准；若进程运行期间用户删除 {@code data/} 或该文件，通过比较文件 mtime 使缓存与磁盘重新对齐。
 */
@Component
public class InitializationAccessGate {

    private final WorkspacePaths paths;
    private final AtomicBoolean initExecutionInProgress = new AtomicBoolean(false);
    private final Object initCacheLock = new Object();
    private volatile boolean cachedInitComplete;
    /** 上次观测到的 {@code runtime.json} 修改时间（毫秒）；变化时重新读盘。 */
    private volatile long cachedRuntimeJsonMtimeMillis = Long.MIN_VALUE;

    public InitializationAccessGate(WorkspacePaths paths) {
        this.paths = paths;
    }

    /**
     * 是否已完成初始化（{@code runtime.json} 中 {@code initialization_completed=true}）。
     */
    public boolean isInitializationComplete() {
        Path p = paths.runtimeJson();
        long mtimeObserved = runtimeFileMtimeMillis(p);
        if (mtimeObserved != cachedRuntimeJsonMtimeMillis) {
            synchronized (initCacheLock) {
                if (mtimeObserved != cachedRuntimeJsonMtimeMillis) {
                    cachedInitComplete = readCompleteFromFile();
                    cachedRuntimeJsonMtimeMillis = mtimeObserved;
                }
            }
        }
        return cachedInitComplete;
    }

    private static long runtimeFileMtimeMillis(Path p) {
        try {
            if (!Files.isRegularFile(p)) {
                return -1L;
            }
            return Files.getLastModifiedTime(p).toMillis();
        } catch (IOException e) {
            return -1L;
        }
    }

    private boolean readCompleteFromFile() {
        Path p = paths.runtimeJson();
        if (!Files.isRegularFile(p)) {
            return false;
        }
        try {
            JsonNode n = JsonConfigLoader.loadTree(p);
            return n.path("initialization_completed").asBoolean(false);
        } catch (IOException e) {
            return false;
        }
    }

    public void beginInitExecution() {
        initExecutionInProgress.set(true);
    }

    public void endInitExecution() {
        initExecutionInProgress.set(false);
    }

    public boolean isInitExecutionInProgress() {
        return initExecutionInProgress.get();
    }

    /**
     * 在 {@link InitService#run()} 的 {@code finally} 中调用，使缓存与磁盘一致。
     *
     * @param wroteCompletionFlag 本次是否已将 {@code initialization_completed} 写为 true
     */
    public void syncStateAfterInitRun(boolean wroteCompletionFlag) {
        synchronized (initCacheLock) {
            if (wroteCompletionFlag) {
                cachedInitComplete = true;
            } else {
                cachedInitComplete = readCompleteFromFile();
            }
            cachedRuntimeJsonMtimeMillis = runtimeFileMtimeMillis(paths.runtimeJson());
        }
    }
}
