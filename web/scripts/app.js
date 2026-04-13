/**
 * SPA 入口：启动遮罩 → 初始化 → JWT → 协议（仅 sessionStorage）→ 主界面。
 * 竞态代次、主界面单次绑定、Blob 释放、401 广播。
 */

import { api, getToken, setToken } from "./api.js";
import { toast, confirmAsync, prefersReducedMotion } from "./ui-shell.js";

const AGREEMENT_SESSION = "desubtitle_agreement_ok";

/** 前端轮询 GET /life；服务端「在线用户」量表依赖此前端心跳。 */
const LIFE_HEARTBEAT_INTERVAL_MS = 12_000;

/** 全局 1s 递减：上传/成品剩余秒数、JWT 剩余秒数均在此计时；服务端时间由 GET /life（含 expiresInSeconds、indicators.videoLifecycles）与 GET /myVideos 矫正。 */
const LIFECYCLE_TICK_MS = 1000;

const el = (id) => document.getElementById(id);

const LANE_COUNT_MIN = 1;
const LANE_COUNT_MAX = 8;

/** 当前并行路数；DOM 由 GET /life 的 videoProcessingLanes 挂载（1–8）。 */
let laneCount = 0;

function le(lane, suffix) {
  return document.getElementById(`${suffix}-${lane}`);
}

/** 去字幕等接口在未配置 AK/SK 时返回 428 + need_credentials */
function friendlyNeedCredentialsMessage(e) {
  if (e?.status === 428 && e?.body?.error === "need_credentials") {
    return "未配置阿里云 AccessKey（去字幕会调云端）。若删过 data/，SQLite 里的密钥也没了。请刷新页面走「初始化」重新填 AK/SK 并点执行初始化；或设置环境变量 ALIBABA_CLOUD_ACCESS_KEY_ID / ALIBABA_CLOUD_ACCESS_SECRET 后重启后端。若刷新后仍不出现初始化页，把仓库里 config/json/runtime.json 的 initialization_completed 改为 false 后再刷新。";
  }
  return "";
}

function friendlyStartupError(e) {
  const code = e?.body?.error || e?.message || "";
  if (code === "jwt_secret_not_configured") {
    return "未配置 JWT 签名密钥：请在运行环境中设置 DESUBTITLE_JWT_SECRET（建议≥32 字符随机串），参见 config/lua/user_token.lua 注释；保存后重启 Spring Boot。";
  }
  if (code === "initializing" || code === "not_initialized") {
    return "后端仍在初始化或未执行初始化，请稍后重试或先完成初始化向导。";
  }
  if (e?.status === 503 && typeof code === "string" && code) {
    return `服务暂不可用（${code}）。请查看后端日志或配置说明。`;
  }
  return code || e?.message || String(e);
}

let blobUrlA = [];
let blobUrlB = [];
let lifeTimer = null;
let lifecycleTickTimer = null;

/** @type {Map<string, { upload: number, output: number | null }>} */
const lifecycleRemainByVideoId = new Map();
let selectedLocalFile = [];
let mainBound = false;

let lifeTickGen = 0;
let videoListFetchGen = 0;
let loadASeq = [];
let loadBSeq = [];
let metricsHydrated = false;
/** 由 GET /life 的 expiresInSeconds 写入；与视频列表倒计时共用 1s 定时器递减。 */
let jwtRemainSeconds = null;
/** 各路独立忙碌；任一路忙时禁用列表「撤销」以防状态错乱。 */
let laneBusy = [];

function metricSuffix(userId) {
  return userId.replaceAll(/[^a-zA-Z0-9_-]/g, "_");
}

function jwtSub(jwt) {
  try {
    const p = jwt.split(".")[1];
    const b64 = p.replace(/-/g, "+").replace(/_/g, "/");
    const json = JSON.parse(atob(b64));
    return json.sub || "";
  } catch {
    return "";
  }
}

function revokeBlob(u) {
  if (u) {
    URL.revokeObjectURL(u);
  }
}

function anyLaneBusy() {
  return laneBusy.some(Boolean);
}

function updateUserChip() {
  const chip = el("user-chip");
  if (!chip) {
    return;
  }
  const sub = jwtSub(getToken());
  let text = sub ? `用户 ${sub.slice(0, 8)}…` : "";
  if (jwtRemainSeconds != null && Number.isFinite(jwtRemainSeconds)) {
    const j = Math.max(0, Math.floor(jwtRemainSeconds));
    text = text ? `${text} · JWT 剩余 ${j}s` : `JWT 剩余 ${j}s`;
  }
  chip.textContent = text;
}

function clampNonNegSec(n) {
  const x = Math.floor(Number(n));
  return Number.isFinite(x) && x > 0 ? x : 0;
}

/** 用单条服务端生命周期写入/覆盖本地剩余秒（矫正）。 */
function setLifecycleRemainFromServerVideo(v) {
  if (!v?.videoId) return;
  const out = v.desubtitleOutputExpiresInSeconds;
  lifecycleRemainByVideoId.set(v.videoId, {
    upload: clampNonNegSec(v.uploadExpiresInSeconds),
    output: out == null ? null : clampNonNegSec(out),
  });
}

/** 仅保留仍在列表中的 videoId，避免撤销后残留递减。 */
function pruneLifecycleRemainToVideoIds(keepIds) {
  const keep = new Set(keepIds);
  for (const id of [...lifecycleRemainByVideoId.keys()]) {
    if (!keep.has(id)) {
      lifecycleRemainByVideoId.delete(id);
    }
  }
}

function mergeLifecycleFromLifeIndicators(life) {
  const sub = jwtSub(getToken());
  const rows = life?.indicators?.videoLifecycles;
  if (!Array.isArray(rows) || !sub) {
    return;
  }
  for (const v of rows) {
    if (v.userId === sub) {
      setLifecycleRemainFromServerVideo(v);
    }
  }
}

function updateVideoListLifecycleSpans() {
  const list = el("video-list");
  if (!list) {
    return;
  }
  list.querySelectorAll(".js-video-expires").forEach((span) => {
    const videoId = span.dataset.videoId;
    if (!videoId) {
      return;
    }
    const row = span.closest("[data-video-row-id]");
    const st = row?.getAttribute("data-status") || "—";
    const rec = lifecycleRemainByVideoId.get(videoId);
    if (!rec) {
      span.textContent = `状态 ${st} · 上传剩余 —`;
      return;
    }
    const up = Math.max(0, rec.upload);
    let text = `状态 ${st} · 上传剩余 ${up}s`;
    if (rec.output != null) {
      text += ` · 成品剩余 ${Math.max(0, rec.output)}s`;
    }
    span.textContent = text;
  });
}

function startLifecycleTick() {
  if (lifecycleTickTimer) {
    clearInterval(lifecycleTickTimer);
  }
  const tick = () => {
    for (const rec of lifecycleRemainByVideoId.values()) {
      if (rec.upload > 0) {
        rec.upload -= 1;
      }
      if (rec.output != null && rec.output > 0) {
        rec.output -= 1;
      }
    }
    if (jwtRemainSeconds != null && jwtRemainSeconds > 0) {
      jwtRemainSeconds -= 1;
    }
    updateUserChip();
    updateVideoListLifecycleSpans();
  };
  lifecycleTickTimer = setInterval(tick, LIFECYCLE_TICK_MS);
}

function applyVideoProcessingLanesFromLifePayload(life) {
  const raw = life?.videoProcessingLanes;
  let n = typeof raw === "number" && Number.isFinite(raw) ? Math.trunc(raw) : 3;
  if (n < LANE_COUNT_MIN) n = LANE_COUNT_MIN;
  if (n > LANE_COUNT_MAX) n = LANE_COUNT_MAX;
  return n;
}

function resizeLaneArrays(n) {
  const oldLen = blobUrlA.length;
  if (n < oldLen) {
    for (let lane = n; lane < oldLen; lane++) {
      revokeBlob(blobUrlA[lane]);
      revokeBlob(blobUrlB[lane]);
    }
  }
  blobUrlA = Array.from({ length: n }, (_, i) => (i < oldLen ? blobUrlA[i] : null) ?? null);
  blobUrlB = Array.from({ length: n }, (_, i) => (i < oldLen ? blobUrlB[i] : null) ?? null);
  selectedLocalFile = Array.from({ length: n }, (_, i) => (i < oldLen ? selectedLocalFile[i] : null) ?? null);
  laneBusy = Array.from({ length: n }, (_, i) => (i < oldLen ? !!laneBusy[i] : false));
  loadASeq = Array.from({ length: n }, (_, i) => (i < oldLen ? loadASeq[i] ?? 0 : 0));
  loadBSeq = Array.from({ length: n }, (_, i) => (i < oldLen ? loadBSeq[i] ?? 0 : 0));
}

function updateLaneDownloadLink(lane, videoId) {
  const a = le(lane, "lane-download");
  if (!a) return;
  const url = blobUrlB[lane];
  if (!url || laneBusy[lane]) {
    a.removeAttribute("href");
    a.classList.add("is-disabled");
    a.setAttribute("aria-disabled", "true");
    a.setAttribute("tabindex", "-1");
    return;
  }
  const idPart = (videoId || getSelectedVideoId(lane) || "out").slice(0, 8);
  a.href = url;
  a.download = `desubtitle-${idPart}.mp4`;
  a.classList.remove("is-disabled");
  a.setAttribute("aria-disabled", "false");
  a.removeAttribute("tabindex");
}

function syncRevokeButtonsDisabled() {
  const busy = anyLaneBusy();
  el("video-list")
    .querySelectorAll("button")
    .forEach((b) => {
      b.disabled = busy;
    });
}

function clearLaneMedia(lane, { clearSelect = false } = {}) {
  loadASeq[lane]++;
  loadBSeq[lane]++;
  revokeBlob(blobUrlA[lane]);
  revokeBlob(blobUrlB[lane]);
  blobUrlA[lane] = null;
  blobUrlB[lane] = null;
  selectedLocalFile[lane] = null;
  const a = le(lane, "video-a");
  const b = le(lane, "video-b");
  if (a) {
    a.removeAttribute("src");
    a.load?.();
  }
  if (b) {
    b.removeAttribute("src");
    b.load?.();
  }
  le(lane, "file-local").value = "";
  if (clearSelect) {
    le(lane, "select-video").value = "";
  }
  updateLaneDownloadLink(lane);
}

function releaseAllMediaBlobs() {
  for (let lane = 0; lane < laneCount; lane++) {
    clearLaneMedia(lane);
  }
}

function buildLaneElement(lane) {
  const n = lane + 1;
  const box = document.createElement("div");
  box.className = "video-io video-lane ms-elevate";
  box.dataset.laneIndex = String(lane);
  box.innerHTML = `
    <p class="video-lane-title">处理项 <span class="accent-num">${n}</span></p>
    <div id="video-pane-a-${lane}" class="video-pane">
      <div class="pane-label">原始输入 <span class="muted">A</span></div>
      <video id="video-a-${lane}" class="video-slot" controls playsinline></video>
      <div class="io-toolbar">
        <select id="select-video-${lane}" class="ms-select" aria-label="已上传视频 ${n}">
          <option value="">选择已上传视频…</option>
        </select>
        <label class="file-pill ms-button ms-button-secondary">
          <img class="file-pill-icon" src="/icons/folder.svg" width="16" height="16" alt="" decoding="async" />
          本地文件
          <input id="file-local-${lane}" type="file" accept="video/*" hidden />
        </label>
        <button type="button" id="btn-upload-${lane}" class="ms-button ms-button-primary">上传</button>
      </div>
      <p id="upload-msg-${lane}" class="inline-hint"></p>
    </div>
    <div class="video-bridge">
      <div class="video-bridge-spacer" aria-hidden="true"></div>
      <div class="video-bridge-center">
        <button type="button" id="btn-desub-${lane}" class="ms-button-bridge" title="本路提交去字幕" aria-label="去字幕">
          <img class="bridge-icon" src="/icons/arrow-right.svg" width="22" height="22" alt="" decoding="async" />
        </button>
        <p class="bridge-hint">去字幕</p>
      </div>
      <div class="video-bridge-spacer" aria-hidden="true"></div>
      <div class="video-bridge-footer">
        <label class="bridge-pos-label muted" for="select-subtitle-pos-${lane}">字幕位置</label>
        <select id="select-subtitle-pos-${lane}" class="ms-select subtitle-pos-select" aria-label="烧录字幕竖直位置 ${n}">
          <option value="full">全屏</option>
          <option value="upper_half">上半屏</option>
          <option value="lower_half">下半屏</option>
          <option value="top">顶部</option>
          <option value="upper">偏上</option>
          <option value="middle">中</option>
          <option value="lower">偏下</option>
          <option value="bottom" selected>底部</option>
        </select>
      </div>
    </div>
    <div id="video-pane-b-${lane}" class="video-pane">
      <div class="pane-label">解析结果 <span class="muted">B</span></div>
      <video id="video-b-${lane}" class="video-slot" controls playsinline></video>
      <div class="pane-b-actions">
        <a id="lane-download-${lane}" class="ms-button ms-button-secondary lane-download is-disabled" aria-disabled="true" tabindex="-1">
          <img src="/icons/download.svg" width="16" height="16" alt="" decoding="async" />
          下载成品
        </a>
      </div>
      <p id="desub-msg-${lane}" class="inline-hint"></p>
    </div>
  `;
  return box;
}

function mountVideoLaneElements() {
  const root = el("video-lanes-root");
  root.innerHTML = "";
  root.dataset.lanes = String(laneCount);
  for (let lane = 0; lane < laneCount; lane++) {
    root.appendChild(buildLaneElement(lane));
  }
}

function updateVideoSectionDesc() {
  const p = el("video-section-desc");
  if (!p) return;
  const n = laneCount > 0 ? laneCount : 1;
  p.textContent = `由于服务器算力有限,每个用户仅开放${n}路并发-小萱baibai`;
}

function remountVideoLanesFromCount(next) {
  const root = el("video-lanes-root");
  const clamped = applyVideoProcessingLanesFromLifePayload({ videoProcessingLanes: next });
  if (
    clamped === laneCount &&
    laneCount > 0 &&
    root.querySelectorAll(".video-lane").length === laneCount
  ) {
    return;
  }
  laneCount = clamped;
  resizeLaneArrays(laneCount);
  mountVideoLaneElements();
  bindVideoLaneListeners();
  updateVideoSectionDesc();
  for (let lane = 0; lane < laneCount; lane++) {
    clearLaneMedia(lane, { clearSelect: true });
    le(lane, "upload-msg").textContent = "";
    le(lane, "desub-msg").textContent = "";
  }
  refreshVideoList();
}

async function bootstrapVideoLanesFromLife() {
  const root = el("video-lanes-root");
  root.innerHTML = '<p class="muted video-lanes-loading">正在同步布局…</p>';
  const body = await pullLifeStateOnce({ silent: true });
  if (!body) {
    remountVideoLanesFromCount(3);
  }
}

function showBootLoader() {
  el("boot-loader").classList.remove("hidden");
  el("boot-init").classList.add("hidden");
}

function showBootInit() {
  el("boot-loader").classList.add("hidden");
  el("boot-init").classList.remove("hidden");
}

function hideBootVeil() {
  const v = el("boot-veil");
  if (prefersReducedMotion()) {
    v.classList.add("hidden");
    v.setAttribute("aria-hidden", "true");
    return;
  }
  const done = () => {
    v.classList.add("hidden");
    v.classList.remove("boot-veil--exiting");
    v.setAttribute("aria-hidden", "true");
  };
  v.classList.add("boot-veil--exiting");
  const t = setTimeout(done, 400);
  v.addEventListener(
    "transitionend",
    (e) => {
      if (e.propertyName === "opacity") {
        clearTimeout(t);
        done();
      }
    },
    { once: true },
  );
}

function setInitError(msg) {
  const n = el("init-err");
  if (!msg) {
    n.classList.add("hidden");
    n.textContent = "";
  } else {
    n.textContent = msg;
    n.classList.remove("hidden");
  }
}

function waitInitUser(status) {
  return new Promise((resolve) => {
    const needCreds = !status.credentialsConfigured;
    if (needCreds) {
      el("field-ak").classList.remove("hidden");
      el("field-sk").classList.remove("hidden");
      el("btn-init-submit").textContent = "保存并执行初始化";
    } else {
      el("field-ak").classList.add("hidden");
      el("field-sk").classList.add("hidden");
      el("btn-init-submit").textContent = "执行初始化";
    }
    const form = el("boot-init-form");
    const btn = el("btn-init-submit");
    let initSubmitInFlight = false;
    form.onsubmit = async (ev) => {
      ev.preventDefault();
      if (initSubmitInFlight) return;
      initSubmitInFlight = true;
      setInitError("");
      btn.disabled = true;
      try {
        if (needCreds) {
          const ak = el("ak").value.trim();
          const sk = el("sk").value.trim();
          if (!ak || !sk) {
            setInitError("请填写 AccessKey ID 与 Secret。");
            return;
          }
          await api.initCredentials(ak, sk);
        }
        await api.initRun();
        const st = await api.initStatus();
        if (st.initialized) {
          resolve();
        } else {
          setInitError("初始化未完成，请重试或查看服务端日志。");
        }
      } catch (e) {
        setInitError(friendlyStartupError(e));
      } finally {
        btn.disabled = false;
        initSubmitInFlight = false;
      }
    };
  });
}

function openAgreementModal(modal) {
  modal.classList.remove("hidden");
  modal.setAttribute("aria-hidden", "false");
  requestAnimationFrame(() => {
    modal.classList.add("modal--open");
    el("btn-agreement-accept").focus();
  });
}

async function showAgreementGate() {
  const modal = el("agreement-modal");
  const body = el("agreement-body");
  let text = "";
  try {
    text = await api.getAgreement();
  } catch {
    text = "";
  }
  body.textContent = text.trim() ? text : "（当前无协议正文，可在 config/json/agreement.json 配置 text 字段。）";

  return new Promise((resolve) => {
    openAgreementModal(modal);
    const acceptHandler = () => {
      el("btn-agreement-accept").disabled = true;
      el("btn-agreement-decline").disabled = true;
      el("btn-agreement-accept").onclick = null;
      el("btn-agreement-decline").onclick = null;
      document.removeEventListener("keydown", trap);
      sessionStorage.setItem(AGREEMENT_SESSION, "1");
      modal.classList.remove("modal--open");
      const done = () => {
        modal.classList.add("hidden");
        modal.setAttribute("aria-hidden", "true");
        resolve(true);
      };
      if (prefersReducedMotion()) {
        done();
      } else {
        setTimeout(done, 320);
      }
    };
    const trap = (e) => {
      if (e.key === "Tab") {
        const panel = modal.querySelector(".modal-panel");
        const focusables = panel.querySelectorAll("button");
        const list = [...focusables];
        if (list.length === 0) return;
        const idx = list.indexOf(document.activeElement);
        if (e.shiftKey && (idx <= 0 || idx === -1)) {
          e.preventDefault();
          list[list.length - 1].focus();
        } else if (!e.shiftKey && idx === list.length - 1) {
          e.preventDefault();
          list[0].focus();
        }
      }
      if (e.key === "Escape") {
        e.preventDefault();
        body.textContent =
          "须同意协议后才能使用本应用。请关闭页面或点击「同意并继续」。";
        toast("请先阅读并同意协议", "info");
      }
    };
    document.addEventListener("keydown", trap);
    el("btn-agreement-decline").onclick = () => {
      body.textContent =
        "须同意协议后才能使用本应用。请关闭页面或点击「同意并继续」。";
      toast("需同意协议后方可继续", "err");
    };
    el("btn-agreement-accept").onclick = acceptHandler;
  });
}

/**
 * 拉取 GET /life：Token、indicators（指标卡片）、videoLifecycles 矫正、并行路数。
 * @param {{ silent?: boolean }} options silent 为 false 时拉取失败会 toast（用于用户操作后主动刷新）。
 */
async function pullLifeStateOnce(options = {}) {
  const { silent = true } = options;
  const gen = ++lifeTickGen;
  updateUserChip();
  if (!metricsHydrated) {
    renderMetricSkeletons();
  }
  try {
    const body = await api.life();
    if (gen !== lifeTickGen) {
      return body;
    }
    if (body?.tokenRefreshed && body.token) {
      setToken(body.token);
    }
    if (typeof body?.expiresInSeconds === "number") {
      jwtRemainSeconds = Math.max(0, Math.floor(body.expiresInSeconds));
    }
    mergeLifecycleFromLifeIndicators(body);
    updateVideoListLifecycleSpans();
    if (body?.indicators) {
      renderMetricsFromSnapshot(body.indicators);
      metricsHydrated = true;
    }
    const want = applyVideoProcessingLanesFromLifePayload(body);
    if (want !== laneCount) {
      remountVideoLanesFromCount(want);
    }
    updateUserChip();
    return body;
  } catch {
    if (gen !== lifeTickGen) {
      return null;
    }
    el("metrics-captured").textContent = "指标拉取失败";
    if (!silent) {
      toast("指标暂时无法刷新", "err");
    }
    return null;
  }
}

function renderMetricsFromSnapshot(snap) {
  const sub = jwtSub(getToken());
  const suf = metricSuffix(sub);
  el("metrics-captured").textContent = snap.capturedAtEpochMillis
    ? `快照时间：${new Date(snap.capturedAtEpochMillis).toLocaleString()}`
    : "";
  const grid = el("metrics-grid");
  grid.innerHTML = "";

  const online = snap.gauges?.online_users;
  addMetricCard(
    grid,
    "在线 userId（近 life 心跳）",
    online != null ? String(Math.round(online)) : "—",
    "gauge online_users · 多标签/多 JWT 可能多计",
  );

  const up = snap.counters?.[`video_uploads_user_${suf}`];
  addMetricCard(grid, "我的上传次数", up != null ? String(up) : "0", `counter …user_${suf}`);

  const batch = snap.counters?.[`desubtitle_batch_requests_user_${suf}`];
  const single = snap.counters?.[`desubtitle_single_requests_user_${suf}`];
  addMetricCard(grid, "去字幕请求（批/单）", `${batch ?? 0} / ${single ?? 0}`, "desubtitle_*");

  const okJobs = snap.counters?.[`desubtitle_job_success_user_${suf}`];
  addMetricCard(grid, "去字幕成功落盘", okJobs != null ? String(okJobs) : "0", `success …${suf}`);

  const rev = snap.counters?.[`video_revokes_user_${suf}`];
  addMetricCard(grid, "我的撤销次数", rev != null ? String(rev) : "0", `revokes …${suf}`);

  const mine = (snap.videoLifecycles || []).filter((v) => v.userId === sub);
  addMetricCard(grid, "当前视频条数", String(mine.length), "videoLifecycles ∩ sub");

  if (!prefersReducedMotion()) {
    [...grid.children].forEach((c, i) => {
      c.style.animationDelay = `${i * 45}ms`;
      c.classList.add("metric-card--enter");
    });
  }
}

function startLifeHeartbeat() {
  if (lifeTimer) {
    clearInterval(lifeTimer);
  }
  const tick = () => {
    void pullLifeStateOnce({ silent: true });
  };
  tick();
  lifeTimer = setInterval(tick, LIFE_HEARTBEAT_INTERVAL_MS);
}

function renderMetricSkeletons() {
  const grid = el("metrics-grid");
  grid.innerHTML = "";
  for (let i = 0; i < 6; i++) {
    const d = document.createElement("div");
    d.className = "metric-card metric-card--skeleton";
    d.innerHTML = "<div class='sk-line sk-line--sm'></div><div class='sk-line sk-line--lg'></div><div class='sk-line sk-line--md'></div>";
    grid.appendChild(d);
  }
}

function addMetricCard(grid, label, value, sub) {
  const d = document.createElement("div");
  d.className = "metric-card";
  const lb = document.createElement("div");
  lb.className = "label";
  lb.textContent = label;
  const val = document.createElement("div");
  val.className = "value";
  val.textContent = value;
  const su = document.createElement("div");
  su.className = "sub";
  su.textContent = sub;
  d.append(lb, val, su);
  grid.appendChild(d);
}

function getSelectedVideoId(lane) {
  return le(lane, "select-video").value || "";
}

function repopulateLaneSelects(rows) {
  const placeholderText = "选择已上传视频…";
  const prev = [];
  for (let lane = 0; lane < laneCount; lane++) {
    prev[lane] = le(lane, "select-video").value;
    const sel = le(lane, "select-video");
    sel.innerHTML = "";
    const ph = document.createElement("option");
    ph.value = "";
    ph.textContent = placeholderText;
    sel.appendChild(ph);
    for (const v of rows) {
      const opt = document.createElement("option");
      opt.value = v.videoId;
      const oName = v.originalFileName || v.storedFileName || "";
      const st = v.desubtitleLastStatus || "—";
      opt.textContent = `${oName} · ${st}`;
      sel.appendChild(opt);
    }
    if (prev[lane] && [...sel.options].some((o) => o.value === prev[lane])) {
      sel.value = prev[lane];
    }
  }
}

async function refreshVideoList() {
  const gen = ++videoListFetchGen;
  const list = el("video-list");

  list.innerHTML = "";

  try {
    const rows = await api.myVideos();
    if (gen !== videoListFetchGen) {
      return;
    }
    repopulateLaneSelects(rows);

    pruneLifecycleRemainToVideoIds(rows.map((r) => r.videoId));
    for (const v of rows) {
      setLifecycleRemainFromServerVideo(v);
    }

    for (const v of rows) {
      const oName = v.originalFileName || v.storedFileName || "";
      const st = v.desubtitleLastStatus || "—";

      const row = document.createElement("div");
      row.className = "video-row";
      row.setAttribute("data-video-row-id", v.videoId);
      row.setAttribute("data-status", st);
      const meta = document.createElement("div");
      meta.className = "meta";
      const strong = document.createElement("strong");
      strong.textContent = `${v.videoId.slice(0, 8)}…`;
      meta.append(strong);
      meta.append(document.createTextNode(` · ${oName}`));
      const br = document.createElement("br");
      meta.append(br);
      const span = document.createElement("span");
      span.className = "muted js-video-expires";
      span.dataset.videoId = v.videoId;
      meta.append(span);

      const rb = document.createElement("button");
      rb.type = "button";
      rb.className = "ms-button ms-button-secondary";
      rb.textContent = "撤销";
      rb.onclick = async () => {
        if (rb.disabled || anyLaneBusy()) return;
        rb.disabled = true;
        try {
          const ok = await confirmAsync(`确定撤销视频 ${v.videoId.slice(0, 8)}…？`);
          if (!ok) return;
          await api.revokeVideo(v.videoId);
          for (let lane = 0; lane < laneCount; lane++) {
            if (getSelectedVideoId(lane) === v.videoId) {
              clearLaneMedia(lane, { clearSelect: true });
              le(lane, "upload-msg").textContent = "";
              le(lane, "desub-msg").textContent = "";
            }
          }
          await refreshVideoList();
          await pullLifeStateOnce({ silent: false });
          toast("已撤销", "ok");
        } catch (e) {
          toast(e.message || "撤销失败", "err");
        } finally {
          rb.disabled = false;
        }
      };
      row.append(meta, rb);
      list.appendChild(row);
    }
    updateVideoListLifecycleSpans();
    syncRevokeButtonsDisabled();
  } catch {
    if (gen !== videoListFetchGen) {
      return;
    }
    list.textContent = "";
    const p = document.createElement("p");
    p.className = "muted";
    p.textContent = "无法加载列表";
    list.appendChild(p);
    toast("视频列表加载失败", "err");
  }
}

async function bindVideoAFromServer(lane, videoId) {
  const seq = ++loadASeq[lane];
  revokeBlob(blobUrlA[lane]);
  blobUrlA[lane] = null;
  const va = le(lane, "video-a");
  va.removeAttribute("src");
  le(lane, "video-pane-a")?.classList.add("video-pane--loading");
  try {
    const url = await api.videoBlobUrl("source", videoId);
    if (seq !== loadASeq[lane]) {
      URL.revokeObjectURL(url);
      return;
    }
    blobUrlA[lane] = url;
    va.src = blobUrlA[lane];
    le(lane, "upload-msg").textContent = "已加载服务器源视频";
  } catch {
    if (seq === loadASeq[lane]) {
      le(lane, "upload-msg").textContent = "无法加载源视频流";
      toast("源视频不可用（可能已删除或无权限）", "err");
    }
  } finally {
    if (seq === loadASeq[lane]) {
      le(lane, "video-pane-a")?.classList.remove("video-pane--loading");
    }
  }
}

function bindVideoAFromLocal(lane, file) {
  loadASeq[lane]++;
  revokeBlob(blobUrlA[lane]);
  blobUrlA[lane] = null;
  selectedLocalFile[lane] = file;
  blobUrlA[lane] = URL.createObjectURL(file);
  le(lane, "video-a").src = blobUrlA[lane];
  le(lane, "upload-msg").textContent = `已选择本地：${file.name}`;
}

async function bindVideoBFromServer(lane, videoId) {
  const seq = ++loadBSeq[lane];
  revokeBlob(blobUrlB[lane]);
  blobUrlB[lane] = null;
  const vb = le(lane, "video-b");
  vb.removeAttribute("src");
  le(lane, "video-pane-b")?.classList.add("video-pane--loading");
  try {
    const url = await api.videoBlobUrl("output", videoId);
    if (seq !== loadBSeq[lane]) {
      URL.revokeObjectURL(url);
      return;
    }
    blobUrlB[lane] = url;
    vb.src = blobUrlB[lane];
    le(lane, "desub-msg").textContent = "已加载成品";
    updateLaneDownloadLink(lane, videoId);
  } catch {
    if (seq === loadBSeq[lane]) {
      le(lane, "desub-msg").textContent = "暂无成品或已过期";
      updateLaneDownloadLink(lane);
    }
  } finally {
    if (seq === loadBSeq[lane]) {
      le(lane, "video-pane-b")?.classList.remove("video-pane--loading");
    }
  }
}

function setLaneBusy(lane, busy) {
  laneBusy[lane] = busy;
  le(lane, "btn-upload").disabled = busy;
  le(lane, "btn-desub").disabled = busy;
  le(lane, "select-video").disabled = busy;
  le(lane, "file-local").disabled = busy;
  le(lane, "select-subtitle-pos").disabled = busy;
  updateLaneDownloadLink(lane);
  syncRevokeButtonsDisabled();
}

function setAllLanesBusy(busy) {
  for (let lane = 0; lane < laneCount; lane++) {
    setLaneBusy(lane, busy);
  }
}

function bindVideoLaneListeners() {
  for (let lane = 0; lane < laneCount; lane++) {
    le(lane, "select-video").addEventListener("change", async () => {
      const id = getSelectedVideoId(lane);
      if (!id) {
        selectedLocalFile[lane] = null;
        le(lane, "file-local").value = "";
        loadASeq[lane]++;
        revokeBlob(blobUrlA[lane]);
        blobUrlA[lane] = null;
        le(lane, "video-a").removeAttribute("src");
        le(lane, "upload-msg").textContent = "未选择视频";
        updateLaneDownloadLink(lane);
        return;
      }
      selectedLocalFile[lane] = null;
      le(lane, "file-local").value = "";
      await bindVideoAFromServer(lane, id);
    });

    le(lane, "file-local").addEventListener("change", () => {
      const f = le(lane, "file-local").files?.[0];
      if (f) {
        le(lane, "select-video").value = "";
        bindVideoAFromLocal(lane, f);
      }
    });

    le(lane, "btn-upload").addEventListener("click", async () => {
      if (laneBusy[lane]) return;
      if (!selectedLocalFile[lane]) {
        le(lane, "upload-msg").textContent = "请先选择本地视频文件";
        toast("请选择要上传的本地文件", "info");
        return;
      }
      setLaneBusy(lane, true);
      le(lane, "upload-msg").textContent = "上传中…";
      le(lane, "video-pane-a")?.classList.add("video-pane--loading");
      try {
        const r = await api.uploadVideo(selectedLocalFile[lane]);
        le(lane, "upload-msg").textContent = `已上传 ${r.id.slice(0, 8)}…`;
        selectedLocalFile[lane] = null;
        le(lane, "file-local").value = "";
        await refreshVideoList();
        await pullLifeStateOnce({ silent: false });
        le(lane, "select-video").value = r.id;
        await bindVideoAFromServer(lane, r.id);
        toast("上传成功", "ok");
      } catch (e) {
        le(lane, "upload-msg").textContent = e.body?.error || e.message;
        toast(e.body?.error || e.message || "上传失败", "err");
      } finally {
        le(lane, "video-pane-a")?.classList.remove("video-pane--loading");
        setLaneBusy(lane, false);
      }
    });

    le(lane, "btn-desub").addEventListener("click", async () => {
      if (laneBusy[lane]) return;
      const id = getSelectedVideoId(lane);
      if (!id) {
        le(lane, "desub-msg").textContent = "请先在左侧选择一条已上传视频";
        toast("请选择已上传的视频", "info");
        return;
      }
      if (selectedLocalFile[lane]) {
        le(lane, "desub-msg").textContent = "请先上传本地文件再处理";
        toast("请先将本地文件上传", "info");
        return;
      }
      setLaneBusy(lane, true);
      le(lane, "desub-msg").textContent = "处理中（可能较久）…";
      le(lane, "btn-desub").classList.add("ms-button-bridge--pulse");
      try {
        const subtitlePosition = le(lane, "select-subtitle-pos").value;
        const r = await api.sendVideoToDeSubtitle(id, subtitlePosition);
        if (r.outcome === "success") {
          le(lane, "desub-msg").textContent = "完成";
          await bindVideoBFromServer(lane, id);
          toast("去字幕完成", "ok");
        } else {
          const msg = `${r.outcome || "unknown"}${r.error ? ` · ${r.error}` : ""}`;
          le(lane, "desub-msg").textContent = msg;
          toast(msg, "err");
        }
        await refreshVideoList();
        await pullLifeStateOnce({ silent: false });
      } catch (e) {
        const cred = friendlyNeedCredentialsMessage(e);
        if (cred) {
          le(lane, "desub-msg").textContent = cred;
          toast("缺少阿里云密钥：请查看本路下方说明或刷新后完成初始化", "err");
        } else {
          const line = e.body?.error || e.message || "请求失败";
          le(lane, "desub-msg").textContent = line;
          toast(line, "err");
        }
      } finally {
        le(lane, "btn-desub").classList.remove("ms-button-bridge--pulse");
        setLaneBusy(lane, false);
      }
    });
  }
}

async function startMainAsync() {
  if (mainBound) {
    return;
  }
  mainBound = true;

  el("app-main").classList.remove("hidden");
  requestAnimationFrame(() => {
    el("app-main").classList.add("app-main--visible");
  });

  window.addEventListener("desubtitle:auth-lost", () => {
    jwtRemainSeconds = null;
    updateUserChip();
    toast("登录已失效，请刷新页面", "err");
    setAllLanesBusy(true);
  });

  window.addEventListener("beforeunload", () => {
    releaseAllMediaBlobs();
    if (lifeTimer) {
      clearInterval(lifeTimer);
    }
    if (lifecycleTickTimer) {
      clearInterval(lifecycleTickTimer);
    }
  });

  await bootstrapVideoLanesFromLife();

  startLifeHeartbeat();
  startLifecycleTick();
}

async function main() {
  const status = await api.initStatus();
  if (!status.initialized) {
    showBootInit();
    await waitInitUser(status);
  }
  showBootLoader();
  const tok = await api.getUserToken();
  setToken(tok.token);
  hideBootVeil();
  if (!sessionStorage.getItem(AGREEMENT_SESSION)) {
    await showAgreementGate();
  }
  await startMainAsync();
}

main().catch((e) => {
  showBootInit();
  const hint = el("boot-init").querySelector(".boot-hint");
  if (hint) {
    hint.textContent = "启动失败：" + friendlyStartupError(e);
  }
  toast(friendlyStartupError(e), "err");
});
