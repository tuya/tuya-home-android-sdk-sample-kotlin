# AI Chat 云端能力接入 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把云端 ~22 个 ATOP 智能体接口 + 本地聊天记录 DB + 设备身份建连全量接入 `AiChatActivity`。

**Architecture:** 分层 — `AiAgentBusiness`(ATOP 调用) + `AiAgentModels`(bean) + `AiChatRecordDbHelper`(本地 SQLite) + UI 组件，全 Kotlin。建连由 App 身份改设备身份(`connectWithDevice(devId)`)。

**Tech Stack:** Kotlin, `com.thingclips.smart.android.network.Business` / `ApiParams`, Android `SQLiteOpenHelper`, thingsmart-ai-stream-lib。

**Verification model:** 无可单测 seam(ATOP/SDK/网络)。每任务验证 = `./gradlew :home:compileDebugKotlin` 通过 + 真机点测(log/toast)。频繁提交。

**Module facts:** module `home`；namespace `com.tuya.appsdk.sample.user`；R = `com.tuya.appsdk.sample.user.R`；目标包 `com.thingclips.sdk.aistream.ai`。

---

## Phase 1 — API 层 + 建连改造

### Task 1.1: AiAgentModels (全部 bean)

**Files:**
- Create: `home/src/main/java/com/thingclips/sdk/aistream/ai/AiAgentModels.kt`

- [ ] **Step 1: 写 data class**，字段对齐 wiki result。`@JvmField` 非必须(Business 用 Gson 反射)。

```kotlin
package com.thingclips.sdk.aistream.ai

data class Avatar(val avatarId: String? = null, val url: String? = null)

data class Language(
    val langCode: String? = null,
    val langName: String? = null,
    val hasDefault: Boolean = false
)

data class Timbre(
    val voiceId: String? = null,
    val voiceName: String? = null,
    val descTags: List<String>? = null,
    val supportLangs: List<String>? = null,
    val speed: Double = 0.0,
    val tone: Double = 0.0,
    val demoUrl: String? = null
)

data class RoleSummary(
    val roleId: String? = null,
    val roleName: String? = null,
    val roleDesc: String? = null,
    val roleIntroduce: String? = null,
    val roleImgUrl: String? = null,
    val useLangCode: String? = null,
    val useLangName: String? = null,
    val useTimbreId: String? = null,
    val useTimbreName: String? = null,
    val templateId: String? = null,
    val lastTextAnswer: String? = null
)

data class RoleDetail(
    val roleId: String? = null,
    val roleName: String? = null,
    val roleDesc: String? = null,
    val roleIntroduce: String? = null,
    val roleImgUrl: String? = null,
    val useLangCode: String? = null,
    val useLangName: String? = null,
    val useTimbreId: String? = null,
    val useTimbreName: String? = null,
    val isUserCloneTimbre: Boolean = false,
    val useTimbreSupportLangs: String? = null,
    val useTimbreTags: List<String>? = null,
    val speed: Double = 0.0,
    val tone: Double = 0.0,
    val templateId: String? = null,
    val bindRoleType: Int = 0,
    val lastTextAnswer: String? = null,
    val roleCode: String? = null,
    val defaultFlag: Int = 0
)

data class RoleTemplate(
    val templateId: String? = null,
    val roleId: String? = null,
    val roleCode: String? = null,
    val roleName: String? = null,
    val roleDesc: String? = null,
    val roleImgUrl: String? = null,
    val roleIntroduce: String? = null,
    val useLangCode: String? = null,
    val useLangName: String? = null,
    val useTimbreId: String? = null,
    val useTimbreName: String? = null,
    val useTimbreSupportLangs: String? = null,
    val useTimbreSupportLangNames: String? = null,
    val defaultFlag: Int = 0,
    val lastTextAnswer: String? = null
)

data class ChatPart(val context: String? = null, val type: String? = null)

data class ChatHistoryItem(
    val requestId: String? = null,
    val createTime: String? = null,
    val gmtCreate: Long = 0,
    val question: List<ChatPart>? = null,
    val answer: List<ChatPart>? = null
)

data class MemorySwitch(val summaryOpen: Boolean = false, val memoryOpen: Boolean = false)

data class MemoryItem(
    val memoryKey: String? = null,
    val memoryValue: String? = null,
    val memoryName: String? = null,
    val effectiveScope: Int = 0,
    val effectiveScopeName: String? = null,
    val shareMemory: Boolean = false
)

data class MemoryGroup(
    val effectiveScope: Int = 0,
    val effectiveScopeName: String? = null,
    val memoryList: List<MemoryItem>? = null
)

data class ChatEmotion(
    val emotionOpen: Boolean = false,
    val emotion: String? = null,
    val text: String? = null,
    val url: String? = null,
    val gmtCreate: Long = 0,
    val gmtModified: Long = 0
)
```

- [ ] **Step 2: 编译** `./gradlew :home:compileDebugKotlin` — Expected: PASS。
- [ ] **Step 3: Commit** `git add` 该文件 + 本 plan/spec；`git commit -m "feat(ai): add AiAgentModels beans for ATOP agent APIs"`。

### Task 1.2: AiAgentBusiness (全部 ATOP 接口)

**Files:**
- Create: `home/src/main/java/com/thingclips/sdk/aistream/ai/AiAgentBusiness.kt`

参考 `com/thingclips/sdk/album/AlbumCalendarBusiness.java`：`Business` 子类、`ApiParams(api, ver)`、`putPostData`、`asyncRequest(params, Clazz, listener)` / `asyncArrayList(params, Clazz, listener)`。

- [ ] **Step 1: 写类骨架 + 配置/音色接口**

```kotlin
package com.thingclips.sdk.aistream.ai

import android.text.TextUtils
import com.thingclips.smart.android.base.ApiParams
import com.thingclips.smart.android.network.Business

class AiAgentBusiness : Business() {

    companion object {
        private const val VER = "1.0"
        // config / timbre
        private const val API_AVATARS = "m.life.ai.agent.config.list-support-avatars"
        private const val API_LANGUAGES = "m.life.ai.agent.config.list-support-languages"
        private const val API_TIMBRE_PAGE = "m.life.ai.timbre.page"
        // role
        private const val API_ROLE_ADD = "m.life.ai.agent.role.custom-role.add"
        private const val API_ROLE_PAGE = "m.life.ai.agent.role.custom-role.page"
        private const val API_ROLE_DETAIL = "m.life.ai.agent.role.custom-role.detail"
        private const val API_ROLE_UPDATE = "m.life.ai.agent.role.custom-role.update"
        private const val API_ROLE_DELETE = "m.life.ai.agent.role.custom-role.delete"
        private const val API_TPL_LIST = "m.life.ai.agent.role.role-template.list"
        private const val API_TPL_DETAIL = "m.life.ai.agent.role.role-template.detail"
        private const val API_BIND = "m.life.ai.agent.role.bind-with-role"
        private const val API_GET_BIND = "m.life.ai.agent.role.get-bind-role"
        private const val API_INIT_BIND = "m.life.ai.agent.role.initialize-agent-role-binding"
        // chat
        private const val API_HISTORY_FETCH = "m.life.ai.agent.chat.history.fetch"
        private const val API_HISTORY_DELETE = "m.life.ai.agent.chat.history.delete"
        private const val API_MEM_SWITCH = "m.life.ai.agent.chat.memory.get-switch"
        private const val API_MEM_LIST = "m.life.ai.agent.chat.memory.list"
        private const val API_MEM_DELETE = "m.life.ai.agent.chat.memory.delete"
        private const val API_SUMMARY_GET = "m.life.ai.agent.chat.chat-summary.get"
        private const val API_SUMMARY_UPDATE = "m.life.ai.agent.chat.chat-summary.update"
        private const val API_CONTEXT_CLEAR = "m.life.ai.agent.chat.context.clear"
        private const val API_EMOTION = "m.life.ai.agent.chat.chat-emotion.current"
    }

    private fun params(api: String, devId: String): ApiParams {
        val p = ApiParams(api, VER)
        p.setSessionRequire(true)
        p.putPostData("devId", devId)
        return p
    }

    private fun ApiParams.putIfNotEmpty(k: String, v: String?) {
        if (!TextUtils.isEmpty(v)) putPostData(k, v)
    }
    private fun ApiParams.putIfNotNull(k: String, v: Any?) {
        if (v != null) putPostData(k, v)
    }

    // --- config / timbre ---
    fun listAvatars(devId: String, l: ResultListener<ArrayList<Avatar>>) =
        asyncArrayList(params(API_AVATARS, devId), Avatar::class.java, l)

    fun listLanguages(devId: String, l: ResultListener<ArrayList<Language>>) =
        asyncArrayList(params(API_LANGUAGES, devId), Language::class.java, l)

    fun timbrePage(
        devId: String, pageNo: Int, pageSize: Int,
        tag: String?, keyWord: String?, lang: String?,
        l: ResultListener<ArrayList<Timbre>>
    ) {
        val p = params(API_TIMBRE_PAGE, devId)
        p.putPostData("pageNo", pageNo); p.putPostData("pageSize", pageSize)
        p.putIfNotEmpty("tag", tag); p.putIfNotEmpty("keyWord", keyWord); p.putIfNotEmpty("lang", lang)
        asyncArrayList(p, Timbre::class.java, l)
    }
}
```

- [ ] **Step 2: 编译** `./gradlew :home:compileDebugKotlin` — Expected: PASS（验证 `Business`/`ApiParams`/`ResultListener`/`asyncArrayList` 签名正确）。
- [ ] **Step 3: Commit** `git commit -m "feat(ai): add AiAgentBusiness config/timbre APIs"`。

### Task 1.3: AiAgentBusiness — role 接口

**Files:** Modify: `home/src/main/java/com/thingclips/sdk/aistream/ai/AiAgentBusiness.kt`

- [ ] **Step 1: 加 role 方法**

```kotlin
    // --- role ---
    fun addCustomRole(
        devId: String, roleName: String, roleIntroduce: String, roleImgUrl: String,
        useLangCode: String, roleDesc: String?, useTimbreId: String?, speed: String?,
        l: ResultListener<String>
    ) {
        val p = params(API_ROLE_ADD, devId)
        p.putPostData("roleName", roleName)
        p.putPostData("roleIntroduce", roleIntroduce)
        p.putPostData("roleImgUrl", roleImgUrl)
        p.putPostData("useLangCode", useLangCode)
        p.putIfNotEmpty("roleDesc", roleDesc)
        p.putIfNotEmpty("useTimbreId", useTimbreId)
        p.putIfNotEmpty("speed", speed)
        asyncRequest(p, String::class.java, l)
    }

    fun pageCustomRoles(
        devId: String, pageNo: Int, pageSize: Int, roleCategory: String?,
        l: ResultListener<ArrayList<RoleSummary>>
    ) {
        val p = params(API_ROLE_PAGE, devId)
        p.putPostData("pageNo", pageNo); p.putPostData("pageSize", pageSize)
        p.putIfNotEmpty("roleCategory", roleCategory)
        asyncArrayList(p, RoleSummary::class.java, l)
    }

    fun customRoleDetail(devId: String, roleId: String, l: ResultListener<RoleDetail>) {
        val p = params(API_ROLE_DETAIL, devId); p.putPostData("roleId", roleId)
        asyncRequest(p, RoleDetail::class.java, l)
    }

    fun updateCustomRole(
        devId: String, roleId: String, roleName: String?, roleDesc: String?,
        roleIntroduce: String?, roleImgUrl: String?, useLangCode: String?,
        useTimbreId: String?, speed: String?, needBind: Boolean?,
        l: ResultListener<Boolean>
    ) {
        val p = params(API_ROLE_UPDATE, devId)
        p.putPostData("roleId", roleId)
        p.putIfNotEmpty("roleName", roleName); p.putIfNotEmpty("roleDesc", roleDesc)
        p.putIfNotEmpty("roleIntroduce", roleIntroduce); p.putIfNotEmpty("roleImgUrl", roleImgUrl)
        p.putIfNotEmpty("useLangCode", useLangCode); p.putIfNotEmpty("useTimbreId", useTimbreId)
        p.putIfNotEmpty("speed", speed); p.putIfNotNull("needBind", needBind)
        asyncRequest(p, Boolean::class.java, l)
    }

    fun deleteCustomRole(devId: String, roleId: String, l: ResultListener<Boolean>) {
        val p = params(API_ROLE_DELETE, devId); p.putPostData("roleId", roleId)
        asyncRequest(p, Boolean::class.java, l)
    }

    fun listRoleTemplates(devId: String, tagCode: String?, l: ResultListener<ArrayList<RoleTemplate>>) {
        val p = params(API_TPL_LIST, devId); p.putIfNotEmpty("tagCode", tagCode)
        asyncArrayList(p, RoleTemplate::class.java, l)
    }

    fun roleTemplateDetail(devId: String, roleId: String, l: ResultListener<RoleDetail>) {
        val p = params(API_TPL_DETAIL, devId); p.putPostData("roleId", roleId)
        asyncRequest(p, RoleDetail::class.java, l)
    }

    fun bindRole(devId: String, bindRoleType: Int, roleId: String, l: ResultListener<Boolean>) {
        val p = params(API_BIND, devId)
        p.putPostData("bindRoleType", bindRoleType); p.putPostData("roleId", roleId)
        asyncRequest(p, Boolean::class.java, l)
    }

    fun getBindRole(devId: String, l: ResultListener<RoleDetail>) =
        asyncRequest(params(API_GET_BIND, devId), RoleDetail::class.java, l)

    fun initAgentRoleBinding(devId: String, l: ResultListener<RoleDetail>) =
        asyncRequest(params(API_INIT_BIND, devId), RoleDetail::class.java, l)
```

- [ ] **Step 2: 编译** — Expected: PASS。
- [ ] **Step 3: Commit** `git commit -m "feat(ai): add AiAgentBusiness role APIs"`。

### Task 1.4: AiAgentBusiness — chat 接口

**Files:** Modify: `AiAgentBusiness.kt`

- [ ] **Step 1: 加 chat 方法**

```kotlin
    // --- chat ---
    fun fetchHistory(
        devId: String, bindRoleType: Int, roleId: String,
        gmtStart: Long?, gmtEnd: Long?, fetchSize: Int, timeAsc: Boolean?,
        l: ResultListener<ArrayList<ChatHistoryItem>>
    ) {
        val p = params(API_HISTORY_FETCH, devId)
        p.putPostData("bindRoleType", bindRoleType); p.putPostData("roleId", roleId)
        p.putPostData("fetchSize", fetchSize)
        p.putIfNotNull("gmtStart", gmtStart); p.putIfNotNull("gmtEnd", gmtEnd)
        p.putIfNotNull("timeAsc", timeAsc)
        asyncArrayList(p, ChatHistoryItem::class.java, l)
    }

    fun deleteHistory(
        devId: String, bindRoleType: Int, roleId: String,
        clearAllHistory: Boolean, requestIds: String?, l: ResultListener<Boolean>
    ) {
        val p = params(API_HISTORY_DELETE, devId)
        p.putPostData("bindRoleType", bindRoleType); p.putPostData("roleId", roleId)
        p.putPostData("clearAllHistory", clearAllHistory); p.putIfNotEmpty("requestIds", requestIds)
        asyncRequest(p, Boolean::class.java, l)
    }

    fun getMemorySwitch(devId: String, l: ResultListener<MemorySwitch>) =
        asyncRequest(params(API_MEM_SWITCH, devId), MemorySwitch::class.java, l)

    fun listMemory(devId: String, bindRoleType: Int, roleId: String, l: ResultListener<ArrayList<MemoryGroup>>) {
        val p = params(API_MEM_LIST, devId)
        p.putPostData("bindRoleType", bindRoleType); p.putPostData("roleId", roleId)
        asyncArrayList(p, MemoryGroup::class.java, l)
    }

    fun deleteMemory(
        devId: String, bindRoleType: Int, roleId: String,
        clearAllMemory: Boolean, memoryKeys: String?, l: ResultListener<Boolean>
    ) {
        val p = params(API_MEM_DELETE, devId)
        p.putPostData("bindRoleType", bindRoleType); p.putPostData("roleId", roleId)
        p.putPostData("clearAllMemory", clearAllMemory); p.putIfNotEmpty("memoryKeys", memoryKeys)
        asyncRequest(p, Boolean::class.java, l)
    }

    fun getSummary(devId: String, bindRoleType: Int, roleId: String, l: ResultListener<String>) {
        val p = params(API_SUMMARY_GET, devId)
        p.putPostData("bindRoleType", bindRoleType); p.putPostData("roleId", roleId)
        asyncRequest(p, String::class.java, l)
    }

    fun updateSummary(
        devId: String, bindRoleType: Int, roleId: String, summaryItems: String,
        l: ResultListener<Boolean>
    ) {
        val p = params(API_SUMMARY_UPDATE, devId)
        p.putPostData("bindRoleType", bindRoleType); p.putPostData("roleId", roleId)
        p.putPostData("summaryItems", summaryItems)
        asyncRequest(p, Boolean::class.java, l)
    }

    fun clearContext(devId: String, bindRoleType: Int, roleId: String, l: ResultListener<Boolean>) {
        val p = params(API_CONTEXT_CLEAR, devId)
        p.putPostData("bindRoleType", bindRoleType); p.putPostData("roleId", roleId)
        asyncRequest(p, Boolean::class.java, l)
    }

    fun currentEmotion(devId: String, l: ResultListener<ChatEmotion>) =
        asyncRequest(params(API_EMOTION, devId), ChatEmotion::class.java, l)
```

- [ ] **Step 2: 编译** — Expected: PASS。
- [ ] **Step 3: Commit** `git commit -m "feat(ai): add AiAgentBusiness chat history/memory/summary APIs"`。

### Task 1.5: 建连改设备身份 + devId Intent extra

**Files:**
- Modify: `home/src/main/java/com/tuya/appsdk/sample/home/main/HomeFuncWidget.kt:89-92`
- Modify: `home/src/main/java/com/thingclips/sdk/aistream/ai/AiChatActivity.kt`（onCreate 取 extra、connectToAiStream、isStreamConnected、createNewSession）

- [ ] **Step 1: HomeFuncWidget 传 devId**。在 `intent.putExtra("ownerId", ...)` 附近加 `intent.putExtra("devId", <devId>)`。demo 取设备来源：当前 home 下首个设备 id（或沿用已有获取方式）。若无现成 devId，加 `getMetaDataValue(it.context, "AI_DEVICE_ID")` 兜底。
- [ ] **Step 2: AiChatActivity 取 devId**。onCreate 加 `private lateinit var mDevId: String`，`mDevId = intent.getStringExtra("devId") ?: ""`；空则 Toast + 继续（建连用 devId，业务接口用 devId）。
- [ ] **Step 3: 建连改 connectWithDevice**。
  - `connectToAiStream()`: `aiStream?.connectWithApp(cb)` → `aiStream?.connectWithDevice(mDevId, cb)`。
  - `isConnected(Constants.ClientType.APP, null)` 两处 → 设备 clientType。查 `Constants.ClientType` 是否有 `DEVICE` 常量（javap 确认）；用之，连同 `isConnected(deviceType, mDevId)`。
- [ ] **Step 4: 建会话改设备重载**。`createNewSession()` 中 `createSession(AgentTokenRequestParams,...)` 改设备身份。
  **确认点**：`ThingAiStream.newInstance()` 返回 `com.thingclips.sdk.aistream.IThingAiStream`。设备 `createSession(devId, aiSolutionCode, attributeList, cb)` 在 `com.thingclips.smart.ai.stream.IThingAiStream`。
  - 先 javap/grep 确认 newInstance 实际对象是否实现设备接口，或是否有 `ThingHomeSdk`/manager 提供设备会话工厂。
  - **可达** → 用设备重载，attributeList 传 `needTts`/`onlyAsr` 组装的 JSON 或 null。
  - **不可达** → 回退：保留现有 token-based `createSession`，仅 Step 3 的建连改设备身份（满足"传 devId 建连"核心诉求）。
- [ ] **Step 5: 编译 + 真机**。`./gradlew :home:compileDebugKotlin` PASS；真机进页面 → log 见 `connectWithDevice onSuccess` + session 创建成功。
- [ ] **Step 6: Commit** `git commit -m "feat(ai): connect with device identity and pass devId"`。

---

## Phase 2 — 本地聊天记录 DB

### Task 2.1: AiChatRecordDbHelper

**Files:**
- Create: `home/src/main/java/com/thingclips/sdk/aistream/ai/AiChatRecordDbHelper.kt`

仿 `RecordDatabaseHelper`：安全 PRAGMA、列名白名单、参数化。单表 `chat_messages`。

- [ ] **Step 1: 写 helper**

```kotlin
package com.thingclips.sdk.aistream.ai

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class ChatMessageRecord(
    val id: Long = 0,
    val devId: String,
    val roleId: String,
    val bizId: String?,
    val sender: Int,        // 0=user 1=ai
    val msgType: String,    // text/voice_to_text/nlg_text/image/nlg_image
    val content: String?,
    val imageUri: String?,
    val ts: Long
)

class AiChatRecordDbHelper(context: Context, private val uid: String) :
    SQLiteOpenHelper(context, "thing_ai_chat_$uid", null, 1) {

    companion object {
        private const val T = "chat_messages"
        private const val C_ID = "id"; private const val C_DEV = "dev_id"
        private const val C_ROLE = "role_id"; private const val C_BIZ = "biz_id"
        private const val C_SENDER = "sender"; private const val C_TYPE = "msg_type"
        private const val C_CONTENT = "content"; private const val C_IMG = "image_uri"
        private const val C_TS = "ts"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("PRAGMA disable_load_extension=ON")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS $T($C_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "$C_DEV TEXT,$C_ROLE TEXT,$C_BIZ TEXT,$C_SENDER INTEGER,$C_TYPE TEXT," +
                "$C_CONTENT TEXT,$C_IMG TEXT,$C_TS INTEGER)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_chat ON $T($C_DEV,$C_ROLE,$C_TS)")
    }

    override fun onUpgrade(db: SQLiteDatabase, o: Int, n: Int) {
        db.execSQL("DROP TABLE IF EXISTS $T"); onCreate(db)
    }

    fun insert(r: ChatMessageRecord): Long {
        return try {
            val v = ContentValues().apply {
                put(C_DEV, r.devId); put(C_ROLE, r.roleId); put(C_BIZ, r.bizId)
                put(C_SENDER, r.sender); put(C_TYPE, r.msgType)
                put(C_CONTENT, r.content); put(C_IMG, r.imageUri); put(C_TS, r.ts)
            }
            writableDatabase.insert(T, null, v)
        } catch (e: Exception) { -1 }
    }

    fun query(devId: String, roleId: String, limit: Int = 200): List<ChatMessageRecord> {
        val list = ArrayList<ChatMessageRecord>()
        try {
            readableDatabase.query(
                T, null, "$C_DEV=? AND $C_ROLE=?", arrayOf(devId, roleId),
                null, null, "$C_TS ASC", limit.toString()
            ).use { c ->
                while (c.moveToNext()) {
                    list.add(
                        ChatMessageRecord(
                            id = c.getLong(c.getColumnIndexOrThrow(C_ID)),
                            devId = devId, roleId = roleId,
                            bizId = c.getString(c.getColumnIndexOrThrow(C_BIZ)),
                            sender = c.getInt(c.getColumnIndexOrThrow(C_SENDER)),
                            msgType = c.getString(c.getColumnIndexOrThrow(C_TYPE)),
                            content = c.getString(c.getColumnIndexOrThrow(C_CONTENT)),
                            imageUri = c.getString(c.getColumnIndexOrThrow(C_IMG)),
                            ts = c.getLong(c.getColumnIndexOrThrow(C_TS))
                        )
                    )
                }
            }
        } catch (e: Exception) { }
        return list
    }

    fun deleteByRole(devId: String, roleId: String): Int =
        try { writableDatabase.delete(T, "$C_DEV=? AND $C_ROLE=?", arrayOf(devId, roleId)) }
        catch (e: Exception) { -1 }
}
```

- [ ] **Step 2: 编译** — Expected: PASS。
- [ ] **Step 3: Commit** `git commit -m "feat(ai): add local chat record SQLite helper"`。

### Task 2.2: 收发消息落库 + 进页加载

**Files:** Modify: `AiChatActivity.kt`

- [ ] **Step 1: 持有 helper + uid + 当前 roleId**。加字段 `private lateinit var dbHelper: AiChatRecordDbHelper`、`private var currentRoleId: String = ""`、`private var currentBindRoleType: Int = 2`。onCreate 初始化 dbHelper（uid 取登录用户，见 RecordDatabaseHelper 取 uid 方式；sample 简化可用固定/已有 user api）。
- [ ] **Step 2: 落库 hook**。在 `addMessage(...)` 内（用户/AI 消息渲染处）追加 `dbHelper.insert(...)`，映射 ChatMessage→ChatMessageRecord（sender/msgType/content/imageUri/bizId/ts=System.currentTimeMillis()）。roleId/devId 用当前值。
  - 注意：`addMessage` 已在主线程；insert 走 helper 内 try/catch，量小可直接调（如担心阻塞，包 `Thread{}` 或现有 executor）。
- [ ] **Step 3: 进页加载本地历史**。新增 `loadLocalHistory()`：`dbHelper.query(mDevId, currentRoleId)` → 转 ChatMessage → 填 messageList → notifyDataSetChanged。会话激活且 roleId 确定后调用。
- [ ] **Step 4: 编译 + 真机**。发消息→杀进程→重进，历史恢复。
- [ ] **Step 5: Commit** `git commit -m "feat(ai): persist and restore chat messages locally"`。

---

## Phase 3 — 角色管理 + 配置

### Task 3.1: 进页初始化角色绑定

**Files:** Modify: `AiChatActivity.kt`

- [ ] **Step 1**：session 创建成功后调 `business.initAgentRoleBinding(mDevId, ...)` → 再 `getBindRole(mDevId,...)`，把返回 `RoleDetail.roleId`/`bindRoleType` 存入 `currentRoleId`/`currentBindRoleType`，更新工具栏角色名/头像。然后 `loadLocalHistory()` + Phase3.4 云端合并。
- [ ] **Step 2**：编译 + 真机 log 见 roleId。
- [ ] **Step 3**：Commit `feat(ai): init and resolve bound agent role on session start`。

### Task 3.2: 角色切换 BottomSheet

**Files:**
- Create: `home/src/main/res/layout/ai_bottomsheet_roles.xml`（RecyclerView + 两个分组标题）
- Create: `home/src/main/java/com/thingclips/sdk/aistream/ai/RolePickerSheet.kt`（`BottomSheetDialogFragment`）
- Modify: `AiChatActivity.kt`（工具栏入口 + 选中回调）

- [ ] **Step 1**：Sheet 加载 `listRoleTemplates` + `pageCustomRoles`，列表展示（名+头像 Glide）。选中回调 `(bindRoleType, roleId)`。
- [ ] **Step 2**：Activity 收到选中 → `business.bindRole(mDevId, type, roleId)` 成功后：更新 currentRoleId/type、清 messageList、`loadLocalHistory()` + 云端 fetch。
- [ ] **Step 3**：编译 + 真机切角色，历史隔离正确。
- [ ] **Step 4**：Commit `feat(ai): role switching via bottom sheet`。

### Task 3.3: 角色创建/编辑 + 配置选择器

**Files:**
- Create: `home/src/main/res/layout/ai_activity_role_edit.xml`
- Create: `home/src/main/java/com/thingclips/sdk/aistream/ai/RoleEditActivity.kt`
- Modify: `home/src/main/AndroidManifest.xml`（注册 Activity）

- [ ] **Step 1**：表单字段 roleName/roleIntroduce/roleDesc + 头像选择(`listAvatars`)+ 语言选择(`listLanguages`)+ 音色选择(`timbrePage`，可试听 demoUrl)。
- [ ] **Step 2**：提交 → 新建 `addCustomRole` / 编辑 `updateCustomRole`。成功返回上层刷新。
- [ ] **Step 3**：删除入口 `deleteCustomRole`（编辑态）。
- [ ] **Step 4**：编译 + 真机走通建/改/删。
- [ ] **Step 5**：Commit `feat(ai): custom role create/edit with avatar/language/timbre pickers`。

### Task 3.4: 云端历史合并

**Files:** Modify: `AiChatActivity.kt`

- [ ] **Step 1**：`loadCloudHistory()`：`fetchHistory(mDevId, type, roleId, null, now, 50, false)` → 把 question/answer 的 ChatPart 转 ChatMessage，按 requestId/gmtCreate 与本地去重合并，按时间排序刷新。
- [ ] **Step 2**：编译 + 真机：云端历史显示，无重复。
- [ ] **Step 3**：Commit `feat(ai): merge cloud chat history`。

---

## Phase 4 — 记忆 / 总结 / 上下文 / 心情

### Task 4.1: 记忆管理

**Files:**
- Create: `home/src/main/res/layout/ai_activity_memory.xml`
- Create: `home/src/main/java/com/thingclips/sdk/aistream/ai/MemoryActivity.kt`
- Modify: `AndroidManifest.xml`, `AiChatActivity.kt`(入口)

- [ ] **Step 1**：列表展示 `listMemory`（按 group），顶部显示 `getMemorySwitch`。每条可删 `deleteMemory(...,memoryKeys=key)`；「清空」`deleteMemory(...,clearAllMemory=true)`。
- [ ] **Step 2**：编译 + 真机。
- [ ] **Step 3**：Commit `feat(ai): memory list and delete`。

### Task 4.2: 总结查看/编辑

**Files:** Modify: `AiChatActivity.kt`（Dialog 即可，无需新 Activity）

- [ ] **Step 1**：菜单「总结」→ `getSummary` 填入可编辑 EditText 的 AlertDialog → 保存 `updateSummary(...,summaryItems=text)`。
- [ ] **Step 2**：编译 + 真机。
- [ ] **Step 3**：Commit `feat(ai): chat summary view/edit`。

### Task 4.3: 清上下文 + 心情

**Files:** Modify: `AiChatActivity.kt`

- [ ] **Step 1**：菜单「清上下文」→ 确认对话框 → `clearContext(mDevId, type, roleId)` → Toast。
- [ ] **Step 2**：心情：`currentEmotion(mDevId,...)` 在进页/回复后调用，`ChatEmotion.emotion`/`text` 展示到现有 emoji toolbar（复用 `tvEmoji`），有 `url` 用 Glide。
- [ ] **Step 3**：编译 + 真机。
- [ ] **Step 4**：Commit `feat(ai): clear context and show chat emotion`。

---

## Self-Review 结论

- **Spec 覆盖**：5 子系统 → config/timbre(1.2)、role(1.3,3.x)、chat history(1.4,2.x,3.4)、memory/summary/context/emotion(1.4,4.x)、建连改造(1.5)。全覆盖。
- **类型一致**：bean 字段名在 Business 泛型与 UI 引用一致（RoleDetail/ChatHistoryItem/MemoryGroup 等单一定义）。
- **已知风险（非 placeholder，带回退）**：① 设备 createSession 工厂(Task1.5 Step4)有回退；② `Constants.ClientType.DEVICE` 常量名需 javap 确认；③ uid 获取方式按 RecordDatabaseHelper 范式或 sample 现有 user api，执行时确认；④ Business 泛型 `Boolean::class.java` 装箱在 Kotlin 需用 `java.lang.Boolean` 时调整。这些在对应任务首步确认。
