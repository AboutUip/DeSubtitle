package com.kitepromiss.desubtitle.credential;

import java.util.Optional;

/** 提供调用阿里云 API 时的 AccessKey（库、内存或环境变量）。 */
public interface AliyunCredentialsSource {

    Optional<ResolvedAliyunKeys> resolve();
}
