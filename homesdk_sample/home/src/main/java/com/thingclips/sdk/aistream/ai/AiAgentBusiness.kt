package com.thingclips.sdk.aistream.ai

import android.text.TextUtils
import com.alibaba.fastjson.JSONObject
import com.thingclips.smart.android.base.ApiParams
import com.thingclips.smart.android.network.Business
import com.thingclips.smart.android.network.http.BusinessResponse

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

    /**
     * Paged endpoints return {list,page,total,...} which neither asyncArrayList
     * nor asyncPageList parse (101001). Read the raw JSONObject and pull "list".
     */
    private fun <T> pageToList(clazz: Class<T>, outer: ResultListener<ArrayList<T>>): ResultListener<JSONObject> =
        object : ResultListener<JSONObject> {
            override fun onSuccess(r: BusinessResponse?, result: JSONObject?, api: String?) {
                val list = ArrayList<T>()
                val arr = result?.getJSONArray("list")
                if (arr != null) {
                    for (i in 0 until arr.size) {
                        arr.getObject(i, clazz)?.let { list.add(it) }
                    }
                }
                outer.onSuccess(r, list, api)
            }

            override fun onFailure(r: BusinessResponse?, result: JSONObject?, api: String?) =
                outer.onFailure(r, ArrayList(), api)
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
        p.putPostData("pageNo", pageNo)
        p.putPostData("pageSize", pageSize)
        p.putIfNotEmpty("tag", tag)
        p.putIfNotEmpty("keyWord", keyWord)
        p.putIfNotEmpty("lang", lang)
        // Cloud returns a paged object {list,page,total,...}, not a bare array.
        asyncRequest(p, pageToList(Timbre::class.java, l))
    }

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
        p.putPostData("pageNo", pageNo)
        p.putPostData("pageSize", pageSize)
        p.putIfNotEmpty("roleCategory", roleCategory)
        // Cloud returns a paged object {list,page,total,...}, not a bare array.
        asyncRequest(p, pageToList(RoleSummary::class.java, l))
    }

    fun customRoleDetail(devId: String, roleId: String, l: ResultListener<RoleDetail>) {
        val p = params(API_ROLE_DETAIL, devId)
        p.putPostData("roleId", roleId)
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
        p.putIfNotEmpty("roleName", roleName)
        p.putIfNotEmpty("roleDesc", roleDesc)
        p.putIfNotEmpty("roleIntroduce", roleIntroduce)
        p.putIfNotEmpty("roleImgUrl", roleImgUrl)
        p.putIfNotEmpty("useLangCode", useLangCode)
        p.putIfNotEmpty("useTimbreId", useTimbreId)
        p.putIfNotEmpty("speed", speed)
        p.putIfNotNull("needBind", needBind)
        asyncRequestBoolean(p, l)
    }

    fun deleteCustomRole(devId: String, roleId: String, l: ResultListener<Boolean>) {
        val p = params(API_ROLE_DELETE, devId)
        p.putPostData("roleId", roleId)
        asyncRequestBoolean(p, l)
    }

    fun listRoleTemplates(
        devId: String, tagCode: String?, l: ResultListener<ArrayList<RoleTemplate>>
    ) {
        val p = params(API_TPL_LIST, devId)
        p.putIfNotEmpty("tagCode", tagCode)
        asyncArrayList(p, RoleTemplate::class.java, l)
    }

    fun roleTemplateDetail(devId: String, roleId: String, l: ResultListener<RoleDetail>) {
        val p = params(API_TPL_DETAIL, devId)
        p.putPostData("roleId", roleId)
        asyncRequest(p, RoleDetail::class.java, l)
    }

    fun bindRole(devId: String, bindRoleType: Int, roleId: String, l: ResultListener<Boolean>) {
        val p = params(API_BIND, devId)
        p.putPostData("bindRoleType", bindRoleType)
        p.putPostData("roleId", roleId)
        asyncRequestBoolean(p, l)
    }

    fun getBindRole(devId: String, l: ResultListener<RoleDetail>) =
        asyncRequest(params(API_GET_BIND, devId), RoleDetail::class.java, l)

    fun initAgentRoleBinding(devId: String, l: ResultListener<RoleDetail>) =
        asyncRequest(params(API_INIT_BIND, devId), RoleDetail::class.java, l)

    // --- chat ---
    fun fetchHistory(
        devId: String, bindRoleType: Int, roleId: String,
        gmtStart: Long?, gmtEnd: Long?, fetchSize: Int, timeAsc: Boolean?,
        l: ResultListener<ArrayList<ChatHistoryItem>>
    ) {
        val p = params(API_HISTORY_FETCH, devId)
        p.putPostData("bindRoleType", bindRoleType)
        p.putPostData("roleId", roleId)
        p.putPostData("fetchSize", fetchSize)
        p.putIfNotNull("gmtStart", gmtStart)
        p.putIfNotNull("gmtEnd", gmtEnd)
        p.putIfNotNull("timeAsc", timeAsc)
        asyncArrayList(p, ChatHistoryItem::class.java, l)
    }

    fun deleteHistory(
        devId: String, bindRoleType: Int, roleId: String,
        clearAllHistory: Boolean, requestIds: String?, l: ResultListener<Boolean>
    ) {
        val p = params(API_HISTORY_DELETE, devId)
        p.putPostData("bindRoleType", bindRoleType)
        p.putPostData("roleId", roleId)
        p.putPostData("clearAllHistory", clearAllHistory)
        p.putIfNotEmpty("requestIds", requestIds)
        asyncRequestBoolean(p, l)
    }

    fun getMemorySwitch(devId: String, l: ResultListener<MemorySwitch>) =
        asyncRequest(params(API_MEM_SWITCH, devId), MemorySwitch::class.java, l)

    fun listMemory(
        devId: String, bindRoleType: Int, roleId: String,
        l: ResultListener<ArrayList<MemoryGroup>>
    ) {
        val p = params(API_MEM_LIST, devId)
        p.putPostData("bindRoleType", bindRoleType)
        p.putPostData("roleId", roleId)
        asyncArrayList(p, MemoryGroup::class.java, l)
    }

    fun deleteMemory(
        devId: String, bindRoleType: Int, roleId: String,
        clearAllMemory: Boolean, memoryKeys: String?, l: ResultListener<Boolean>
    ) {
        val p = params(API_MEM_DELETE, devId)
        p.putPostData("bindRoleType", bindRoleType)
        p.putPostData("roleId", roleId)
        p.putPostData("clearAllMemory", clearAllMemory)
        p.putIfNotEmpty("memoryKeys", memoryKeys)
        asyncRequestBoolean(p, l)
    }

    fun getSummary(devId: String, bindRoleType: Int, roleId: String, l: ResultListener<String>) {
        val p = params(API_SUMMARY_GET, devId)
        p.putPostData("bindRoleType", bindRoleType)
        p.putPostData("roleId", roleId)
        asyncRequest(p, String::class.java, l)
    }

    fun updateSummary(
        devId: String, bindRoleType: Int, roleId: String, summaryItems: String,
        l: ResultListener<Boolean>
    ) {
        val p = params(API_SUMMARY_UPDATE, devId)
        p.putPostData("bindRoleType", bindRoleType)
        p.putPostData("roleId", roleId)
        p.putPostData("summaryItems", summaryItems)
        asyncRequestBoolean(p, l)
    }

    fun clearContext(devId: String, bindRoleType: Int, roleId: String, l: ResultListener<Boolean>) {
        val p = params(API_CONTEXT_CLEAR, devId)
        p.putPostData("bindRoleType", bindRoleType)
        p.putPostData("roleId", roleId)
        asyncRequestBoolean(p, l)
    }

    fun currentEmotion(devId: String, l: ResultListener<ChatEmotion>) =
        asyncRequest(params(API_EMOTION, devId), ChatEmotion::class.java, l)
}
