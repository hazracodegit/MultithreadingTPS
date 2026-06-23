const app = {
    token: localStorage.getItem("task_token"),
    user: localStorage.getItem("task_user") || "Guest",
    role: localStorage.getItem("task_role") || "",
    tasks: [],
    stats: {},
    analytics: {},
    logs: []
};

const $ = (id) => document.getElementById(id);

document.addEventListener("DOMContentLoaded", () => {
    bindEvents();
    updateAuthState();
    if (app.token) {
        connectWebSocket();
        refreshAll();
        setInterval(refreshAll, 2000);
    }
});

function bindEvents() {
    document.querySelectorAll("[data-auth-tab]").forEach((button) => {
        button.addEventListener("click", () => switchAuthTab(button.dataset.authTab));
    });
    document.querySelectorAll("[data-view]").forEach((button) => {
        button.addEventListener("click", () => {
            document.querySelectorAll("[data-view]").forEach((item) => item.classList.remove("active"));
            button.classList.add("active");
        });
    });
    $("loginForm").addEventListener("submit", login);
    $("registerForm").addEventListener("submit", register);
    $("taskForm").addEventListener("submit", createTask);
    $("logoutBtn").addEventListener("click", logout);
}

function switchAuthTab(tab) {
    document.querySelectorAll("[data-auth-tab]").forEach((button) => button.classList.toggle("active", button.dataset.authTab === tab));
    $("loginForm").classList.toggle("hidden", tab !== "login");
    $("registerForm").classList.toggle("hidden", tab !== "register");
    $("authMessage").textContent = "";
}

async function login(event) {
    event.preventDefault();
    await authenticate("/auth/login", {
        username: $("loginUsername").value,
        password: $("loginPassword").value
    });
}

async function register(event) {
    event.preventDefault();
    await authenticate("/auth/register", {
        username: $("registerUsername").value,
        email: $("registerEmail").value,
        password: $("registerPassword").value,
        role: $("registerRole").value
    });
}

async function authenticate(url, payload) {
    try {
        const response = await fetch(url, {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify(payload)
        });
        const data = await response.json();
        if (!response.ok) {
            throw new Error(data.message || "Authentication failed");
        }
        app.token = data.token;
        app.user = data.username;
        app.role = data.role;
        localStorage.setItem("task_token", app.token);
        localStorage.setItem("task_user", app.user);
        localStorage.setItem("task_role", app.role);
        updateAuthState();
        connectWebSocket();
        refreshAll();
    } catch (error) {
        $("authMessage").textContent = error.message;
    }
}

function logout() {
    localStorage.removeItem("task_token");
    localStorage.removeItem("task_user");
    localStorage.removeItem("task_role");
    app.token = null;
    app.user = "Guest";
    updateAuthState();
}

function updateAuthState() {
    $("sessionUser").textContent = app.user + (app.role ? ` (${app.role})` : "");
    $("authView").classList.toggle("hidden", Boolean(app.token));
    $("dashboardView").classList.toggle("hidden", !app.token);
    $("logoutBtn").classList.toggle("hidden", !app.token);
}

async function createTask(event) {
    event.preventDefault();
    $("taskMessage").textContent = "";
    const scheduledValue = $("scheduledAt").value;
    const payload = {
        taskName: $("taskName").value,
        taskDescription: $("taskDescription").value,
        priority: $("priority").value,
        executionTime: Number($("executionTime").value),
        timeoutMillis: Number($("timeoutMillis").value),
        scheduledAt: scheduledValue ? new Date(scheduledValue).toISOString() : null,
        dependencyIds: parseIds($("dependencyIds").value)
    };
    try {
        await api("/tasks/create", {method: "POST", body: JSON.stringify(payload)});
        $("taskForm").reset();
        $("executionTime").value = 3000;
        $("timeoutMillis").value = 15000;
        $("taskMessage").textContent = "Task queued successfully.";
        refreshAll();
    } catch (error) {
        $("taskMessage").textContent = cleanError(error.message);
    }
}

async function pauseTask(id) {
    await api(`/tasks/${id}/pause`, {method: "PUT"});
    refreshAll();
}

async function resumeTask(id) {
    await api(`/tasks/${id}/resume`, {method: "PUT"});
    refreshAll();
}

async function cancelTask(id) {
    await api(`/tasks/${id}/cancel`, {method: "DELETE"});
    refreshAll();
}

async function deleteTask(id) {
    await api(`/tasks/${id}`, {method: "DELETE"});
    refreshAll();
}

async function refreshAll() {
    if (!app.token) {
        return;
    }
    try {
        const [tasks, stats, analytics, logs] = await Promise.all([
            api("/tasks"),
            api("/dashboard/stats"),
            api("/analytics/report"),
            api("/dashboard/logs")
        ]);
        app.tasks = tasks;
        app.stats = stats;
        app.analytics = analytics;
        app.logs = logs;
        render();
    } catch (error) {
        if (String(error.message).includes("401") || String(error.message).includes("403")) {
            logout();
        }
    }
}

async function api(url, options = {}) {
    const response = await fetch(url, {
        ...options,
        headers: {
            "Content-Type": "application/json",
            "Authorization": `Bearer ${app.token}`,
            ...(options.headers || {})
        }
    });
    if (!response.ok) {
        const text = await response.text();
        throw new Error(extractError(text) || `HTTP ${response.status}`);
    }
    if (response.status === 204) {
        return null;
    }
    const text = await response.text();
    return text ? JSON.parse(text) : null;
}

function extractError(text) {
    try {
        return JSON.parse(text).message;
    } catch (error) {
        return text;
    }
}

function cleanError(message) {
    return String(message || "Request failed").replaceAll("{", "").replaceAll("}", "");
}

function connectWebSocket() {
    const protocol = window.location.protocol === "https:" ? "wss" : "ws";
    const socket = new WebSocket(`${protocol}://${window.location.host}/ws/tasks`);
    socket.addEventListener("open", () => $("liveState").textContent = "Live");
    socket.addEventListener("message", refreshAll);
    socket.addEventListener("close", () => {
        $("liveState").textContent = "Reconnecting";
        if (app.token) {
            setTimeout(connectWebSocket, 1500);
        }
    });
}

function render() {
    renderStats();
    renderTasks();
    renderLogs();
    renderChart();
    $("updatedAt").textContent = new Date().toLocaleTimeString();
}

function renderStats() {
    const stats = app.stats;
    $("activeThreadCount").textContent = stats.activeThreadCount ?? 0;
    $("queueSize").textContent = stats.queueSize ?? 0;
    $("totalTasks").textContent = stats.totalTasks ?? 0;
    $("completedTasks").textContent = stats.completedTasks ?? 0;
    $("failedTasks").textContent = stats.failedTasks ?? 0;
    $("pendingTasks").textContent = stats.pendingTasks ?? 0;
    $("successRate").textContent = Number(stats.successRate ?? 0).toFixed(1);
    $("averageExecutionTime").textContent = Math.round(stats.averageExecutionTime ?? 0);
    $("throughput").textContent = Number(app.analytics.taskThroughputPerMinute ?? 0).toFixed(2);
    $("failureRate").textContent = Number(app.analytics.failureRate ?? 0).toFixed(1);
    $("retryRate").textContent = Number(app.analytics.retryRate ?? 0).toFixed(1);
}

function renderTasks() {
    $("taskCards").innerHTML = app.tasks.map((task) => `
        <article class="task-card">
            <div>
                <h4>${escapeHtml(task.taskName)}</h4>
                <small>#${task.taskId} • ${escapeHtml(task.taskDescription)}</small>
            </div>
            <div class="pill-row">
                <span class="pill ${task.priority}">${task.priority}</span>
                <span class="pill ${task.status}">${task.status}</span>
            </div>
            <small>Execution ${task.executionTime}ms • Timeout ${task.timeoutMillis}ms • Retry ${task.retryCount}/3</small>
            <small>Dependencies: ${(task.dependencyIds || []).join(", ") || "None"}</small>
            <div class="actions">
                ${task.status === "PAUSED" ? `<button onclick="resumeTask(${task.taskId})">Resume</button>` : ""}
                ${["PENDING", "RETRYING"].includes(task.status) ? `<button class="ghost" onclick="pauseTask(${task.taskId})">Pause</button>` : ""}
                ${!["COMPLETED", "FAILED", "CANCELLED"].includes(task.status) ? `<button class="ghost" onclick="cancelTask(${task.taskId})">Cancel</button>` : ""}
                ${app.role === "ADMIN" && task.status !== "RUNNING" ? `<button class="ghost" onclick="deleteTask(${task.taskId})">Delete</button>` : ""}
            </div>
        </article>
    `).join("") || `<div class="task-card"><h4>No tasks yet</h4><small>Create a task to start processing.</small></div>`;
}

function renderLogs() {
    $("logs").innerHTML = app.logs.map((log) => `
        <div class="log-item">
            <strong>${log.event}</strong>
            <small>#${log.taskId} • retry ${log.retryAttempt}</small><br>
            ${escapeHtml(log.failureReason || "Lifecycle event recorded")}
        </div>
    `).join("") || `<div class="log-item"><strong>No logs yet</strong><br>Task lifecycle events will appear here.</div>`;
}

function renderChart() {
    const canvas = $("analyticsChart");
    const ctx = canvas.getContext("2d");
    const values = [
        ["Throughput", app.analytics.taskThroughputPerMinute || 0, "#38bdf8"],
        ["Failure", app.analytics.failureRate || 0, "#fb7185"],
        ["Retry", app.analytics.retryRate || 0, "#fbbf24"],
        ["Success", app.stats.successRate || 0, "#34d399"]
    ];
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    ctx.fillStyle = "rgba(148, 163, 184, 0.12)";
    ctx.fillRect(0, 0, canvas.width, canvas.height);
    const max = Math.max(100, ...values.map((item) => item[1]));
    values.forEach((item, index) => {
        const x = 40 + index * 115;
        const height = Math.max(6, (item[1] / max) * 170);
        ctx.fillStyle = item[2];
        ctx.fillRect(x, 210 - height, 58, height);
        ctx.fillStyle = "#f4f8ff";
        ctx.font = "12px Inter";
        ctx.fillText(item[0], x - 4, 235);
        ctx.fillText(Number(item[1]).toFixed(1), x + 8, 198 - height);
    });
}

function parseIds(value) {
    return value
        .split(",")
        .map((item) => item.trim())
        .filter(Boolean)
        .map(Number)
        .filter((item) => !Number.isNaN(item));
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}
