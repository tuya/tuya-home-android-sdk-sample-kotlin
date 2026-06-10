# AI Chat 云端能力接入 — 设计文档

日期: 2026-06-10
模块: `home`
入口类: `home/src/main/java/com/thingclips/sdk/aistream/ai/AiChatActivity.kt`

## 1. 背景

`AiChatActivity` 当前只实现了 AI 基座的实时流通道（connect / session / event /
音频 / 文本 / 图片 / emoji skill）。云端通过 ATOP 开放了 C 端智能体业务能力
（wiki page 2062425875911020598），共约 22 个接口，分 5 个子系统。本设计把这些
能力全量接入 demo，并把建连方式从 App 身份改为设备身份。

参考实现:
- ATOP 调用范式: `home/.../com/thingclips/sdk/album/AlbumCalendarBusiness.java`
  （`Business` + `ApiParams` + `asyncRequest` / `asyncArrayList`）。
- 本地数据库范式: `TUNIAIStreamManager/.../db/RecordDatabaseHelper.java`
  （`SQLiteOpenHelper`，按用户 uid 建库，安全 PRAGMA + 列名白名单）。

## 2. 目标 & 范围

全量接入 + 全量 UI 集成，全部 Kotlin。5 个子系统:

1. 历史会话 + 本地 DB
2. 角色管理（自定义角色 CRUD、模板、绑定）
3. 记忆 / 总结 / 上下文 / 心情
4. 智能体配置（头像 / 语言）+ 音色

建连改造: 用 `connectWithDevice(devId)` 建立连接，devId 经新增 Intent extra 传入。

## 3. 架构

分层，贴现有模式:

```
AiChatActivity (Kotlin)
  ├─ AiAgentBusiness (Kotlin, extends Business)  ──ATOP──> 云端
  ├─ AiAgentModels (Kotlin data class beans)
  └─ AiChatRecordDbHelper (Kotlin, SQLiteOpenHelper)  本地聊天记录
  + dialog / bottomsheet UI 组件
```

各层职责单一、接口清晰、可独立验证。

### 3.1 API 层 — `AiAgentBusiness`

Kotlin 类 `extends com.thingclips.smart.android.network.Business`。每个接口一个方法，
内部 `newApiParams(api, devId)` → `putPostData` → `asyncRequest` / `asyncArrayList`，
回调 `Business.ResultListener<T>`。所有接口都带 `devId`（body）。

API 清单（API_VERSION = "1.0"）:

配置:
- `m.life.ai.agent.config.list-support-avatars` → `List<Avatar>`
- `m.life.ai.agent.config.list-support-languages` → `List<Language>`

音色:
- `m.life.ai.timbre.page` (tag/keyWord/lang/pageNo/pageSize/preferredVoiceId/categoryTagCode) → `List<Timbre>`

角色:
- `m.life.ai.agent.role.custom-role.add` (roleName/roleDesc/roleIntroduce/roleImgUrl/useLangCode/useTimbreId/speed) → `String`(roleId)
- `m.life.ai.agent.role.custom-role.page` (pageNo/pageSize/roleCategory) → `List<RoleSummary>`
- `m.life.ai.agent.role.custom-role.detail` (roleId) → `RoleDetail`
- `m.life.ai.agent.role.custom-role.update` (roleId/...可选/needBind) → `Boolean`
- `m.life.ai.agent.role.custom-role.delete` (roleId) → `Boolean`
- `m.life.ai.agent.role.role-template.list` (tagCode) → `List<RoleTemplate>`
- `m.life.ai.agent.role.role-template.detail` (roleId) → `RoleDetail`
- `m.life.ai.agent.role.bind-with-role` (bindRoleType/roleId) → `Boolean`
- `m.life.ai.agent.role.get-bind-role` () → `RoleDetail`
- `m.life.ai.agent.role.initialize-agent-role-binding` () → `RoleDetail`

聊天:
- `m.life.ai.agent.chat.history.fetch` (bindRoleType/roleId/gmtStart/gmtEnd/fetchSize/timeAsc) → `List<ChatHistoryItem>`
- `m.life.ai.agent.chat.history.delete` (bindRoleType/roleId/clearAllHistory/requestIds) → `Boolean`
- `m.life.ai.agent.chat.memory.get-switch` () → `MemorySwitch`
- `m.life.ai.agent.chat.memory.list` (bindRoleType/roleId) → `List<MemoryGroup>`
- `m.life.ai.agent.chat.memory.delete` (bindRoleType/roleId/clearAllMemory/memoryKeys) → `Boolean`
- `m.life.ai.agent.chat.chat-summary.get` (bindRoleType/roleId) → `String`
- `m.life.ai.agent.chat.chat-summary.update` (bindRoleType/roleId/summaryItems) → `Boolean`
- `m.life.ai.agent.chat.context.clear` (bindRoleType/roleId) → `Boolean`
- `m.life.ai.agent.chat.chat-emotion.current` () → `ChatEmotion`

`bindRoleType`: 0=自定义角色, 1=角色模板, 2=单角色场景默认角色。

### 3.2 模型 — `AiAgentModels`

Kotlin `data class`，字段对齐 wiki result。主要类型:
`Avatar(avatarId,url)`、`Language(langCode,langName,hasDefault)`、
`Timbre(voiceId,voiceName,descTags,supportLangs,speed,tone,demoUrl)`、
`RoleSummary`、`RoleDetail(roleId,roleName,roleDesc,roleIntroduce,roleImgUrl,
useLangCode/Name,useTimbreId/Name,isUserCloneTimbre,useTimbreSupportLangs,
useTimbreTags,speed,tone,templateId,bindRoleType,lastTextAnswer,roleCode?,defaultFlag?)`、
`RoleTemplate`、`ChatHistoryItem(requestId,createTime,gmtCreate,question:List<ChatPart>,
answer:List<ChatPart>)`、`ChatPart(context,type)`、`MemorySwitch(summaryOpen,memoryOpen)`、
`MemoryGroup(effectiveScope,effectiveScopeName,memoryList:List<MemoryItem>)`、
`MemoryItem(memoryKey,memoryValue,memoryName,effectiveScope,effectiveScopeName,shareMemory)`、
`ChatEmotion(emotionOpen,emotion,text,url,gmtCreate,gmtModified)`。

### 3.3 本地持久层 — `AiChatRecordDbHelper`

Kotlin `SQLiteOpenHelper`，仿 `RecordDatabaseHelper`:
- 库名按用户 uid: `thing_ai_chat_<uid>`；安全 PRAGMA（foreign_keys / disable_load_extension）。
- 单表 `chat_messages`，列: `id, dev_id, role_id, biz_id, sender(0=user/1=ai),
  msg_type(text/voice/nlg_text/image/nlg_image), content, image_uri, ts`。
- 索引: `(dev_id, role_id, ts)`。
- 方法: `insert(ChatMessageRecord)`、`query(devId, roleId, limit, offset)`、
  `deleteByRole(devId, roleId)`、`deleteAll(devId)`。
- 列名白名单校验，参数化查询（防注入）。

### 3.4 UI 层

`AiChatActivity` 工具栏新增菜单/按钮，触发各能力:
- **角色切换**: BottomSheet 列「模板角色 + 自定义角色」，选中 `bind-with-role`，
  切换后清空当前列表、按新 roleId 重新加载历史。
- **历史加载**: 进页面或切角色时，先读本地 DB 渲染，再 `history.fetch` 拉云端合并去重（按 requestId/gmtCreate）。
- **角色创建/编辑**: 表单 Dialog/Activity，调 `list-support-avatars` / `list-support-languages` /
  `timbre.page` 填充选择器，提交 `custom-role.add` / `.update`。
- **记忆管理**: 列表展示 `memory.list`（分组），支持单条/全部删除 `memory.delete`，显示 `memory.get-switch`。
- **总结**: `chat-summary.get` 展示，可编辑提交 `chat-summary.update`。
- **清上下文**: 按钮 → `context.clear`。
- **心情**: `chat-emotion.current` 展示（可与现有 emoji toolbar 复用）。

### 3.5 建连改造

- `AiChatActivity` 新增 Intent extra `devId`（由 `HomeFuncWidget` 传入）。
- 连接: `connectWithApp` → `connectWithDevice(devId, ConnectCallback)`。
- `isConnected(Constants.ClientType.APP, null)` → 设备 clientType + devId。
- 建会话: 现用 `createSession(AgentTokenRequestParams, null, cb)`（ownerId/aiSolutionCode/
  miniProgramId）。改设备身份后用设备重载 `createSession(devId, aiSolutionCode, attributeList, cb)`
  （位于 `com.thingclips.smart.ai.stream.IThingAiStream`）。
  **phase 1 待确认**: `ThingAiStream.newInstance()` 返回 `com.thingclips.sdk.aistream.IThingAiStream`，
  其 `AgentTokenRequestParams.Builder` 无 devId 字段；需确认设备 createSession 的正确工厂/接口类型，
  或确认 token 重载是否接受 devId 形式。若设备重载不可达，回退方案: 保留 token-based createSession，
  仅建连改 `connectWithDevice`。

## 4. 错误处理

- 所有 ATOP 回调 `onFailure` → log + Toast（错误码 + msg），不崩溃。
- DB 操作 try/catch，失败返回 -1 / 空，记 log，不阻塞 UI。
- 会话未激活时业务操作给出 Toast 提示并禁用入口（复用现有 `updateUiForSessionState`）。
- 历史合并：本地与云端按 requestId/gmtCreate 去重，云端为准。

## 5. 测试 / 验证

每阶段以「可编译 + 真机点测 + log/toast 验证」为准：
- API 层: 各接口单独触发，log 打印 result。
- DB: 收发消息后重进页面，历史正确恢复。
- 角色: 切换后会话与历史隔离正确。
- UI: golden path（发消息→回复→落库→重进恢复）+ 边界（无角色、空历史、网络失败）。

UI 正确性需真机验证；类型检查/编译只保证代码正确，不保证功能正确。

## 6. 分阶段交付

每阶段独立可跑可验证:

1. **Phase 1 — API 层 + 建连改造**: `AiAgentBusiness` + `AiAgentModels` 全量接口；
   新增 devId Intent extra；`connectWithDevice` + 设备 createSession（含上面待确认项）。
2. **Phase 2 — 本地 DB**: `AiChatRecordDbHelper`；收发消息落库；进页 / 切角色加载本地历史。
3. **Phase 3 — 角色 + 配置**: 角色切换 BottomSheet、bind/get-bind/init；
   创建/编辑角色（头像 / 语言 / 音色选择器）；云端 `history.fetch` 合并。
4. **Phase 4 — 记忆 / 总结 / 上下文 / 心情**: 记忆列表 + 删除 + 开关；总结查看 / 编辑；
   清上下文；心情展示。

## 7. 非目标 (YAGNI)

- 不做视频通道。
- 不做音色克隆（仅用标准音色列表）。
- 不做角色分类标签的复杂筛选 UI（只传基础参数）。
- 不重构现有 stream 流程，只在收发点加落库 hook。
