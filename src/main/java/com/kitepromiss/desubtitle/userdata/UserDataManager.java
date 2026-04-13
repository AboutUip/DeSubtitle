package com.kitepromiss.desubtitle.userdata;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.kitepromiss.desubtitle.user.AnonymousUserPrincipal;
import com.kitepromiss.desubtitle.user.AnonymousUserRequestAttributes;
import com.kitepromiss.desubtitle.user.UserTokenManagementService;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 内部使用的多用户数据与会话辅助：从请求上下文解析当前匿名用户；按用户 ID 分区维护线程安全的内存态；并可撤销当前 JWT（SQLite）。
 */
@Service
public class UserDataManager {

    private final UserTokenManagementService userTokenManagementService;

    /** userId → domain → key → value */
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<String, Object>>> compartments =
            new ConcurrentHashMap<>();

    public UserDataManager(UserTokenManagementService userTokenManagementService) {
        this.userTokenManagementService = userTokenManagementService;
    }

    private static boolean usableKey(String s) {
        return s != null && !s.isBlank();
    }

    private Optional<HttpServletRequest> currentRequest() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (!(attrs instanceof ServletRequestAttributes sra)) {
            return Optional.empty();
        }
        return Optional.of(sra.getRequest());
    }

    /** 当前请求中已通过 Bearer 校验的匿名用户 id（JWT {@code sub}）。 */
    public Optional<String> currentUserId() {
        return currentRequest()
                .map(r -> r.getAttribute(AnonymousUserRequestAttributes.ANONYMOUS_USER_ID))
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .filter(s -> !s.isBlank());
    }

    /** 当前会话主体（含 {@code jti}，用于撤销等）。 */
    public Optional<AnonymousUserPrincipal> currentPrincipal() {
        return currentRequest()
                .map(r -> r.getAttribute(AnonymousUserRequestAttributes.ANONYMOUS_USER_PRINCIPAL))
                .filter(AnonymousUserPrincipal.class::isInstance)
                .map(AnonymousUserPrincipal.class::cast);
    }

    public String requireUserId() {
        return currentUserId().orElseThrow(NoCurrentUserContextException::new);
    }

    public AnonymousUserPrincipal requirePrincipal() {
        return currentPrincipal().orElseThrow(NoCurrentUserContextException::new);
    }

    /**
     * 撤销当前请求携带的 JWT（将 {@code user_tokens} 对应行标记撤销）；之后同一 token 将无法通过 Bearer 校验。
     */
    public void revokeCurrentUserToken() {
        userTokenManagementService.revokeByJti(requirePrincipal().tokenId());
    }

    private ConcurrentHashMap<String, Object> compartmentForWrite(String userId, String domain) {
        if (!usableKey(userId) || !usableKey(domain)) {
            throw new IllegalArgumentException("userId 与 domain 须非空");
        }
        return compartments
                .computeIfAbsent(userId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(domain, k -> new ConcurrentHashMap<>());
    }

    /** 只读路径：不存在时不创建空分区，避免纯读取污染 {@link #compartments}。 */
    private ConcurrentHashMap<String, Object> compartmentForRead(String userId, String domain) {
        if (!usableKey(userId) || !usableKey(domain)) {
            return null;
        }
        ConcurrentHashMap<String, ConcurrentHashMap<String, Object>> domains = compartments.get(userId);
        if (domains == null) {
            return null;
        }
        return domains.get(domain);
    }

    /** 使用「当前请求用户」写入域内键值（并发安全）。 */
    public void putState(String domain, String key, Object value) {
        putState(requireUserId(), domain, key, value);
    }

    public void putState(String userId, String domain, String key, Object value) {
        if (!usableKey(key)) {
            throw new IllegalArgumentException("key 须非空");
        }
        compartmentForWrite(userId, domain).put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> getState(String domain, String key, Class<T> type) {
        return getState(requireUserId(), domain, key, type);
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> getState(String userId, String domain, String key, Class<T> type) {
        if (!usableKey(key)) {
            return Optional.empty();
        }
        ConcurrentHashMap<String, Object> inner = compartmentForRead(userId, domain);
        if (inner == null) {
            return Optional.empty();
        }
        Object o = inner.get(key);
        if (o == null) {
            return Optional.empty();
        }
        if (!type.isInstance(o)) {
            throw new ClassCastException("键 " + key + " 期望 " + type.getName() + " 实际 " + o.getClass().getName());
        }
        return Optional.of((T) o);
    }

    public Optional<Object> getState(String userId, String domain, String key) {
        if (!usableKey(key)) {
            return Optional.empty();
        }
        ConcurrentHashMap<String, Object> inner = compartmentForRead(userId, domain);
        if (inner == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(inner.get(key));
    }

    /**
     * 按用户 + 域懒创建值（{@link ConcurrentHashMap#computeIfAbsent}，并发下同一 key 仅执行一次 {@code factory}）。
     */
    @SuppressWarnings("unchecked")
    public <T> T getOrCreateState(String userId, String domain, String key, Supplier<T> factory) {
        if (!usableKey(key)) {
            throw new IllegalArgumentException("key 须非空");
        }
        return (T) compartmentForWrite(userId, domain).computeIfAbsent(key, k -> factory.get());
    }

    public <T> T getOrCreateStateForCurrentUser(String domain, String key, Supplier<T> factory) {
        return getOrCreateState(requireUserId(), domain, key, factory);
    }

    public void removeState(String userId, String domain, String key) {
        if (!usableKey(key)) {
            return;
        }
        ConcurrentHashMap<String, Object> inner = compartmentForRead(userId, domain);
        if (inner != null) {
            inner.remove(key);
        }
    }

    public void removeStateForCurrentUser(String domain, String key) {
        removeState(requireUserId(), domain, key);
    }

    /**
     * 原子计算域内键（{@link ConcurrentHashMap#compute}）；旧值不存在时 {@code remapping} 的第二个参数为 {@code null}。
     */
    @SuppressWarnings("unchecked")
    public <T> T computeState(String userId, String domain, String key, BiFunction<String, T, T> remapping) {
        if (!usableKey(key)) {
            throw new IllegalArgumentException("key 须非空");
        }
        return (T) compartmentForWrite(userId, domain).compute(key, (k, oldVal) -> remapping.apply(k, (T) oldVal));
    }

    /** 清空某用户在某 domain 下的全部条目（不影响其它用户）。 */
    public void clearDomain(String userId, String domain) {
        if (!usableKey(userId) || !usableKey(domain)) {
            return;
        }
        compartments.computeIfPresent(userId, (uid, domains) -> {
            domains.remove(domain);
            return domains.isEmpty() ? null : domains;
        });
    }

    public void clearDomainForCurrentUser(String domain) {
        clearDomain(requireUserId(), domain);
    }

    /** 移除某用户在内存中的全部分区（不撤销 JWT）。 */
    public void clearAllUserData(String userId) {
        if (!usableKey(userId)) {
            return;
        }
        compartments.remove(userId);
    }

    /** 不可变快照：当前用户某 domain 下所有键值拷贝。 */
    public Map<String, Object> snapshotDomainForCurrentUser(String domain) {
        return snapshotDomain(requireUserId(), domain);
    }

    public Map<String, Object> snapshotDomain(String userId, String domain) {
        ConcurrentHashMap<String, Object> inner = compartmentForRead(userId, domain);
        if (inner == null || inner.isEmpty()) {
            return Map.of();
        }
        return Map.copyOf(inner);
    }
}
