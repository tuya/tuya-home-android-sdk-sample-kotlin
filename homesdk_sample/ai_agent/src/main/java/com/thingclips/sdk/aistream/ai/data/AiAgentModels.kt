package com.thingclips.sdk.aistream.ai.data

/** Role binding type used across role/chat ATOP APIs. */
object BindRoleType {
    const val CUSTOM = 0   // user-defined custom role
    const val TEMPLATE = 1 // role template
    const val DEFAULT = 2  // single-scene default role
}

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
