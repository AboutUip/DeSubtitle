/**
 * 轻量 UI：Toast、无障碍确认框；无第三方依赖。
 */

const TOAST_DURATION = 4200;

export function prefersReducedMotion() {
  return window.matchMedia?.("(prefers-reduced-motion: reduce)")?.matches === true;
}

/**
 * @param {string} message
 * @param {"info"|"ok"|"err"} kind
 */
export function toast(message, kind = "info") {
  let root = document.getElementById("toast-root");
  if (!root) {
    root = document.createElement("div");
    root.id = "toast-root";
    root.setAttribute("aria-live", "polite");
    document.body.appendChild(root);
  }
  const t = document.createElement("div");
  t.className = `toast toast--${kind}`;
  t.textContent = message;
  root.appendChild(t);
  requestAnimationFrame(() => t.classList.add("toast--in"));
  const remove = () => {
    t.classList.remove("toast--in");
    t.classList.add("toast--out");
    const wait = prefersReducedMotion() ? 0 : 280;
    setTimeout(() => t.remove(), wait);
  };
  setTimeout(remove, TOAST_DURATION);
}

/**
 * @param {string} message
 * @returns {Promise<boolean>}
 */
export function confirmAsync(message) {
  return new Promise((resolve) => {
    const backdrop = document.createElement("div");
    backdrop.className = "confirm-backdrop";
    backdrop.setAttribute("role", "dialog");
    backdrop.setAttribute("aria-modal", "true");

    const panel = document.createElement("div");
    panel.className = "confirm-panel ms-elevate";
    const msg = document.createElement("p");
    msg.className = "confirm-msg";
    msg.textContent = message;
    const actions = document.createElement("div");
    actions.className = "confirm-actions";
    const btnNo = document.createElement("button");
    btnNo.type = "button";
    btnNo.className = "ms-button ms-button-secondary";
    btnNo.textContent = "取消";
    const btnYes = document.createElement("button");
    btnYes.type = "button";
    btnYes.className = "ms-button ms-button-primary";
    btnYes.textContent = "确定";
    let settled = false;
    const close = (v) => {
      if (settled) return;
      settled = true;
      btnNo.disabled = true;
      btnYes.disabled = true;
      btnNo.onclick = null;
      btnYes.onclick = null;
      backdrop.classList.add("confirm-backdrop--out");
      setTimeout(() => {
        backdrop.remove();
        resolve(v);
      }, prefersReducedMotion() ? 0 : 200);
    };
    btnNo.onclick = () => close(false);
    btnYes.onclick = () => close(true);
    actions.append(btnNo, btnYes);
    panel.append(msg, actions);
    backdrop.append(panel);
    document.body.append(backdrop);
    requestAnimationFrame(() => backdrop.classList.add("confirm-backdrop--in"));
    btnYes.focus();
    backdrop.addEventListener("keydown", (e) => {
      if (e.key === "Escape") {
        e.preventDefault();
        close(false);
      }
    });
  });
}
