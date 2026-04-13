/**
 * 后端 REST 调用；Bearer 存 sessionStorage（与业务会话一致，关闭标签即失）。
 */

const TOKEN_KEY = "desubtitle_jwt";

export function getToken() {
  return sessionStorage.getItem(TOKEN_KEY) || "";
}

export function setToken(jwt) {
  if (jwt) {
    sessionStorage.setItem(TOKEN_KEY, jwt);
  } else {
    sessionStorage.removeItem(TOKEN_KEY);
  }
}

async function parseJsonResponse(res) {
  const ct = res.headers.get("content-type") || "";
  if (ct.includes("application/json")) {
    try {
      return await res.json();
    } catch {
      return null;
    }
  }
  return null;
}

export async function apiFetch(path, options = {}) {
  const { skipAuth = false, ...init } = options;
  const headers = new Headers(init.headers || {});
  if (!headers.has("Accept")) {
    headers.set("Accept", "application/json");
  }
  if (!skipAuth) {
    const t = getToken();
    if (t) {
      headers.set("Authorization", `Bearer ${t}`);
    }
  }
  const res = await fetch(path, { ...init, headers });
  const body = await parseJsonResponse(res);
  if (res.status === 401 && !skipAuth) {
    sessionStorage.removeItem(TOKEN_KEY);
    window.dispatchEvent(new CustomEvent("desubtitle:auth-lost"));
  }
  if (!res.ok) {
    const err = new Error(body?.error || res.statusText || "request_failed");
    err.status = res.status;
    err.body = body;
    throw err;
  }
  return body;
}

export const api = {
  initStatus: () => apiFetch("/init/status", { method: "GET", skipAuth: true }),

  initCredentials: (accessKeyId, accessKeySecret) =>
    apiFetch("/init/credentials", {
      method: "POST",
      skipAuth: true,
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ accessKeyId, accessKeySecret }),
    }),

  initRun: () => apiFetch("/init", { method: "POST", skipAuth: true }),

  getUserToken: () => apiFetch("/getUserToken", { method: "GET", skipAuth: true }),

  /** @returns {Promise<string>} */
  getAgreement: async () => {
    const headers = new Headers({ Accept: "text/plain,*/*" });
    const t = getToken();
    if (t) {
      headers.set("Authorization", `Bearer ${t}`);
    }
    const res = await fetch("/getAgreement", { method: "GET", headers });
    if (!res.ok) {
      const err = new Error(res.statusText);
      err.status = res.status;
      throw err;
    }
    return res.text();
  },

  /**
   * GET /life：存活、JWT 校验/刷新、expiresInSeconds、videoProcessingLanes、indicators（原指标快照）。
   * @returns {Promise<{
   *   alive: boolean,
   *   submittedTokenValid: boolean,
   *   tokenRefreshed: boolean,
   *   token: string,
   *   expiresInSeconds: number,
   *   userId: string,
   *   videoProcessingLanes: number,
   *   indicators?: Record<string, unknown>
   * }>}
   */
  life: () => apiFetch("/life", { method: "GET" }),

  myVideos: () => apiFetch("/myVideos", { method: "GET" }),

  uploadVideo: (file) => {
    const fd = new FormData();
    fd.append("file", file);
    const headers = {};
    const t = getToken();
    if (t) {
      headers.Authorization = `Bearer ${t}`;
    }
    return fetch("/uploadVideo", { method: "POST", headers, body: fd }).then(async (res) => {
      if (res.status === 401) {
        sessionStorage.removeItem(TOKEN_KEY);
        window.dispatchEvent(new CustomEvent("desubtitle:auth-lost"));
      }
      const body = await parseJsonResponse(res);
      if (!res.ok) {
        const err = new Error(body?.error || res.statusText);
        err.status = res.status;
        err.body = body;
        throw err;
      }
      return body;
    });
  },

  sendVideoToDeSubtitle: (videoId, subtitlePosition) =>
    apiFetch("/sendVideoToDeSubtitle", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        videoId,
        subtitlePosition: subtitlePosition || "bottom",
      }),
    }),

  revokeVideo: (videoId) =>
    fetch(`/userVideo/${encodeURIComponent(videoId)}`, {
      method: "DELETE",
      headers: {
        Accept: "application/json",
        ...(getToken() ? { Authorization: `Bearer ${getToken()}` } : {}),
      },
    }).then(async (res) => {
      if (res.status === 401) {
        sessionStorage.removeItem(TOKEN_KEY);
        window.dispatchEvent(new CustomEvent("desubtitle:auth-lost"));
      }
      if (res.status === 204) {
        return null;
      }
      const body = await parseJsonResponse(res);
      if (!res.ok) {
        const err = new Error(body?.error || res.statusText);
        err.status = res.status;
        err.body = body;
        throw err;
      }
      return body;
    }),

  /**
   * 带 Bearer 拉取视频流为 Blob URL（须调用者 revokeObjectURL）。
   * @returns {Promise<string>} object URL
   */
  videoBlobUrl: async (which, videoId) => {
    const path =
      which === "output"
        ? `/userVideo/${encodeURIComponent(videoId)}/output`
        : `/userVideo/${encodeURIComponent(videoId)}/source`;
    const res = await fetch(path, {
      headers: { Authorization: `Bearer ${getToken()}` },
    });
    if (res.status === 401) {
      sessionStorage.removeItem(TOKEN_KEY);
      window.dispatchEvent(new CustomEvent("desubtitle:auth-lost"));
    }
    if (!res.ok) {
      const err = new Error("stream_failed");
      err.status = res.status;
      throw err;
    }
    const blob = await res.blob();
    return URL.createObjectURL(blob);
  },
};
