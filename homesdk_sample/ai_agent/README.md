# AI Agent Component / AI 智能体基座组件

English | [中文](#中文说明)

A self-contained demo component for building AI-agent chat experiences on the
ThingSmart Android SDK. It wraps the AI Stream SDK (realtime streaming
connection) and the AI Agent ATOP APIs (role / memory / history management)
behind ready-to-use pages styled after the production app.

---

## Getting Started

### 1. Include the module

```gradle
// settings.gradle
include ':ai_agent'

// your module's build.gradle
implementation project(':ai_agent')
```

The module declares its own audio stack (required by hold-to-talk, the voice
call and TTS playback), so integrators get them transitively:

```gradle
// recording
api 'com.thingclips.smart:thingsmart-audio-engine-sdk:7.5.3'
api 'com.thingclips.smart:thingsmart-avlogger-sdk:7.5.3'
// audio codecs
api 'com.thingclips.smart:thingsmart-mp3-codec-sdk:7.5.3'
api 'com.thingclips.smart:thingsmart-opus-utils:6.7.2'
```

The host project must also provide the ThingSmart base SDK (here via the
`:base_res` module).

### 2. Configure the identity keys (read this first)

Each chat identity needs its own `aiSolutionCode` + `miniProgramId` pair,
obtained from the AI solution you created on the Tuya developer platform
([iot.tuya.com](https://iot.tuya.com)):

- **App identity pair** — from a solution created on the platform and
  **published to this app**.
- **Device identity pair** — from a solution **published to the device PID**;
  it only works with devices of that PID, the values are not interchangeable.

The keys are secrets — **never commit them to git**. Pick ONE of the two
places below to put them. At runtime `AiIdentityConfig` resolves them in this
priority order (the higher layer overrides the lower one):

| Priority | Where | File / page | Scope |
|---|---|---|---|
| 1 (highest) | **In-app config page** | Entry page → gear icon (top-right) → `AiConfigActivity` | This phone only (SharedPreferences). Blank-and-save reverts to layer 2. The current defaults show as input hints. |
| 2 | **Assets file** | `app/src/main/assets/ai_identity.properties` | This checkout. The file is gitignored; a committed template sits next to it. |

**Recommended setup (layer 2):** copy the template and fill in all four
values —

```bash
cp app/src/main/assets/ai_identity.properties.example \
   app/src/main/assets/ai_identity.properties
```

```properties
# app/src/main/assets/ai_identity.properties  (gitignored)
app.aiSolutionCode=aipt_xxxxxxxxxxxx      # solution published to this app
app.miniProgramId=tyxxxxxxxxxxxxxxxx
device.aiSolutionCode=aipt_xxxxxxxxxxxx   # solution published to the device PID
device.miniProgramId=tyxxxxxxxxxxxxxx
```

**No build-time setup at all?** Run the app, open the entry page, tap the
gear and type the keys into the config page (layer 1).

The entry page shows a 已配置/未配置 (configured / not configured) chip per
identity; tapping an unconfigured identity jumps straight to the config page.

### 3. Launch

```kotlin
startActivity(
    Intent(context, AiEntryActivity::class.java)
        .putExtra("homeId", currentHomeId)
)
```

The entry page offers the two identities:

| | App identity | Device identity |
|---|---|---|
| Connection | `connectWithApp()` (account-scoped) | `connectWithDevice(devId)` |
| Device required | No | Yes — pick one from the current home |
| Solution code | Created on the platform, published to this app | Published to the **device PID** |
| Role / memory management | Not available (those ATOP APIs are devId-scoped) | Full: switch/create/edit roles, role memory, chat summary |

### Features

- **Chat page** — streaming text replies with TTS playback, image and
  hold-to-talk voice input, ASR captions, reply interruption (the send button
  becomes a stop button while a reply is in flight; tapping it stops playback
  and chat-breaks the event so the cloud stops streaming).
- **Voice call** (phone icon, both identities) — hands-free free talk in
  cloud long-event mode: one session, one long event with
  `enableVad + enableInterrupt`; the cloud segments speech and handles
  barge-in, so no local VAD/AEC model files are needed.
- **Role management** (device identity) — production-style role card,
  role switching (recommended templates / custom), creation and editing with
  cloud avatars, languages and timbres.
- **Role memory** (device identity) — clear chat history / context, format
  memory, datasheet memory, conversation summaries.
- **Offline-first chat history** — messages persist to a local SQLite store
  keyed by (device, role); the last bound role is cached so the page renders
  instantly while the network chain (connect → session → role binding)
  refreshes in the background.

## Token Billing / Quota

- **Device identity** — billing is bound to the **device license**; there is
  no separate token billing for device-identity conversations.
- **App identity** — there is currently no dedicated consumer-side (C-end)
  billing. Usage is billed **pay-as-you-go** and topped up on
  [iot.tuya.com](https://iot.tuya.com); recharge the account that owns the AI
  solution.

---

# 中文说明

基于 ThingSmart Android SDK 的 AI 智能体对话基座组件。封装了 AI Stream SDK
（实时流式连接）与 AI Agent ATOP 接口（角色/记忆/历史管理），提供一套对齐公版
App 风格的现成页面。

## 快速开始

### 1. 引入模块

```gradle
// settings.gradle
include ':ai_agent'

// 业务模块 build.gradle
implementation project(':ai_agent')
```

模块自带音频依赖（按住说话、语音通话、TTS 播放都需要），接入方无需重复声明，
会随模块传递引入：

```gradle
// 录音组件
api 'com.thingclips.smart:thingsmart-audio-engine-sdk:7.5.3'
api 'com.thingclips.smart:thingsmart-avlogger-sdk:7.5.3'
// 音频编解码组件
api 'com.thingclips.smart:thingsmart-mp3-codec-sdk:7.5.3'
api 'com.thingclips.smart:thingsmart-opus-utils:6.7.2'
```

宿主工程还需提供 ThingSmart 基础 SDK（本工程通过 `:base_res` 模块引入）。

### 2. 配置身份密钥（重点，先读这节）

每种对话身份需要各自的一对 `aiSolutionCode` + `miniProgramId`，来源于你在涂鸦
开发者平台（[iot.tuya.com](https://iot.tuya.com)）创建的 AI 方案：

- **App 身份密钥对**：平台创建并**发布到本 App** 的方案。
- **设备身份密钥对**：**发布到设备 PID** 的方案，只对该 PID 的设备生效，
  两套密钥不可混用。

密钥**禁止提交到代码仓库**。从下面两个位置任选其一填写；运行时
`AiIdentityConfig` 按以下优先级解析（高层覆盖低层）：

| 优先级 | 位置 | 文件/页面 | 作用范围 |
|---|---|---|---|
| 1（最高） | **App 内配置页** | 入口页右上角齿轮 → `AiConfigActivity` | 仅本手机（SharedPreferences）。留空保存即回退到第 2 层；当前默认值会显示为输入框提示。 |
| 2 | **assets 文件** | `app/src/main/assets/ai_identity.properties` | 本工作区。文件已 gitignore，旁边有已提交的模板。 |

**推荐做法（第 2 层）**：复制模板，四个值填全——

```bash
cp app/src/main/assets/ai_identity.properties.example \
   app/src/main/assets/ai_identity.properties
```

```properties
# app/src/main/assets/ai_identity.properties（已 gitignore）
app.aiSolutionCode=aipt_xxxxxxxxxxxx      # 发布到本 App 的方案
app.miniProgramId=tyxxxxxxxxxxxxxxxx
device.aiSolutionCode=aipt_xxxxxxxxxxxx   # 发布到设备 PID 的方案
device.miniProgramId=tyxxxxxxxxxxxxxx
```

**完全不想动构建文件？** 直接装机运行，入口页点齿轮，在配置页里手输密钥
（第 1 层）。

入口页每种身份都有「已配置/未配置」状态标签；点击未配置的身份会直接跳到
配置页。

### 3. 启动入口

```kotlin
startActivity(
    Intent(context, AiEntryActivity::class.java)
        .putExtra("homeId", currentHomeId)
)
```

入口页提供两种身份：

| | App 身份 | 设备身份 |
|---|---|---|
| 连接方式 | `connectWithApp()`（账号级） | `connectWithDevice(devId)` |
| 是否需要设备 | 否 | 是——从当前家庭选择设备 |
| 方案码来源 | 平台创建并发布到本 App | 发布到**设备 PID** |
| 角色/记忆管理 | 不可用（相关 ATOP 接口为 devId 维度） | 完整：切换/创建/编辑角色、角色记忆、对话总结 |

### 功能

- **对话页**：流式文本回复 + TTS 播放、图片与按住说话语音输入、ASR 字幕、
  响应中打断（回复进行中发送键变为停止键，点击同时停止播放并对事件发
  chat-break，云端停止继续推流）。
- **语音通话**（右上角电话图标，双身份可用）：云端长事件模式的免提自由对话
  ——单 session、单长事件（`enableVad + enableInterrupt`），断句与插话打断
  全部由云端完成，**无需本地 VAD/AEC 模型文件**。
- **角色管理**（设备身份）：公版样式角色卡片，角色切换（推荐模板/自定义）、
  创建与编辑（云端头像、语言、音色）。
- **角色记忆**（设备身份）：清除聊天记录/上下文、格式记忆、数据表记忆、
  对话总结。
- **本地优先的聊天历史**：消息按（设备, 角色）维度持久化到本地 SQLite；
  上次绑定的角色有本地缓存，进入页面立即渲染，网络链路
  （连接 → 会话 → 角色绑定）在后台刷新。

## Token 计费策略

- **设备身份**：计费**绑定设备 license**，设备身份对话没有单独的 token 计费。
- **App 身份**：目前没有专门的 C 端计费，按量计费，在
  [iot.tuya.com](https://iot.tuya.com) 对持有 AI 方案的账号进行充值。
