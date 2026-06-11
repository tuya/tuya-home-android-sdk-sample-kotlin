package com.thingclips.sdk.aistream.ai.data

import com.thingclips.smart.android.network.Business
import com.thingclips.smart.android.network.http.BusinessResponse

/**
 * Result callback for [AiAgentManager]. Hides the underlying network layer
 * (Business / BusinessResponse) from SDK consumers.
 */
interface Cb<T> {
    fun onOk(data: T?)
    fun onErr(code: Int, msg: String?)
}

/**
 * High-level entry point for the AI agent cloud (ATOP) capabilities.
 *
 * Bind a device id once on construction; every call below targets that device.
 * Consumers never touch [AiAgentBusiness], ApiParams, or Business.ResultListener.
 *
 * ```
 * val mgr = AiAgentManager(devId)
 * mgr.getBindRole(object : Cb<RoleDetail> {
 *     override fun onOk(data: RoleDetail?) { ... }
 *     override fun onErr(code: Int, msg: String?) { ... }
 * })
 * ```
 */
class AiAgentManager(private val devId: String) {

    private val biz = AiAgentBusiness()

    // --- config / timbre ---
    fun listAvatars(cb: Cb<ArrayList<Avatar>>) = biz.listAvatars(devId, cb.wrap())

    fun listLanguages(cb: Cb<ArrayList<Language>>) = biz.listLanguages(devId, cb.wrap())

    fun timbrePage(
        pageNo: Int, pageSize: Int, tag: String? = null,
        keyWord: String? = null, lang: String? = null, cb: Cb<ArrayList<Timbre>>
    ) = biz.timbrePage(devId, pageNo, pageSize, tag, keyWord, lang, cb.wrap())

    // --- role ---
    fun addCustomRole(
        roleName: String, roleIntroduce: String, roleImgUrl: String, useLangCode: String,
        roleDesc: String? = null, useTimbreId: String? = null, speed: String? = null,
        cb: Cb<String>
    ) = biz.addCustomRole(devId, roleName, roleIntroduce, roleImgUrl, useLangCode, roleDesc, useTimbreId, speed, cb.wrap())

    fun pageCustomRoles(
        pageNo: Int, pageSize: Int, roleCategory: String? = null, cb: Cb<ArrayList<RoleSummary>>
    ) = biz.pageCustomRoles(devId, pageNo, pageSize, roleCategory, cb.wrap())

    fun customRoleDetail(roleId: String, cb: Cb<RoleDetail>) =
        biz.customRoleDetail(devId, roleId, cb.wrap())

    fun updateCustomRole(
        roleId: String, roleName: String? = null, roleDesc: String? = null,
        roleIntroduce: String? = null, roleImgUrl: String? = null, useLangCode: String? = null,
        useTimbreId: String? = null, speed: String? = null, needBind: Boolean? = null,
        cb: Cb<Boolean>
    ) = biz.updateCustomRole(devId, roleId, roleName, roleDesc, roleIntroduce, roleImgUrl, useLangCode, useTimbreId, speed, needBind, cb.wrap())

    fun deleteCustomRole(roleId: String, cb: Cb<Boolean>) =
        biz.deleteCustomRole(devId, roleId, cb.wrap())

    fun listRoleTemplates(tagCode: String? = null, cb: Cb<ArrayList<RoleTemplate>>) =
        biz.listRoleTemplates(devId, tagCode, cb.wrap())

    fun roleTemplateDetail(roleId: String, cb: Cb<RoleDetail>) =
        biz.roleTemplateDetail(devId, roleId, cb.wrap())

    fun bindRole(bindRoleType: Int, roleId: String, cb: Cb<Boolean>) =
        biz.bindRole(devId, bindRoleType, roleId, cb.wrap())

    fun getBindRole(cb: Cb<RoleDetail>) = biz.getBindRole(devId, cb.wrap())

    fun initAgentRoleBinding(cb: Cb<RoleDetail>) = biz.initAgentRoleBinding(devId, cb.wrap())

    // --- chat ---
    fun fetchHistory(
        bindRoleType: Int, roleId: String, gmtStart: Long?, gmtEnd: Long?,
        fetchSize: Int, timeAsc: Boolean?, cb: Cb<ArrayList<ChatHistoryItem>>
    ) = biz.fetchHistory(devId, bindRoleType, roleId, gmtStart, gmtEnd, fetchSize, timeAsc, cb.wrap())

    fun deleteHistory(
        bindRoleType: Int, roleId: String, clearAllHistory: Boolean,
        requestIds: String?, cb: Cb<Boolean>
    ) = biz.deleteHistory(devId, bindRoleType, roleId, clearAllHistory, requestIds, cb.wrap())

    fun getMemorySwitch(cb: Cb<MemorySwitch>) = biz.getMemorySwitch(devId, cb.wrap())

    fun listMemory(bindRoleType: Int, roleId: String, cb: Cb<ArrayList<MemoryGroup>>) =
        biz.listMemory(devId, bindRoleType, roleId, cb.wrap())

    fun deleteMemory(
        bindRoleType: Int, roleId: String, clearAllMemory: Boolean,
        memoryKeys: String?, cb: Cb<Boolean>
    ) = biz.deleteMemory(devId, bindRoleType, roleId, clearAllMemory, memoryKeys, cb.wrap())

    fun getSummary(bindRoleType: Int, roleId: String, cb: Cb<String>) =
        biz.getSummary(devId, bindRoleType, roleId, cb.wrap())

    fun updateSummary(bindRoleType: Int, roleId: String, summaryItems: String, cb: Cb<Boolean>) =
        biz.updateSummary(devId, bindRoleType, roleId, summaryItems, cb.wrap())

    fun clearContext(bindRoleType: Int, roleId: String, cb: Cb<Boolean>) =
        biz.clearContext(devId, bindRoleType, roleId, cb.wrap())

    fun currentEmotion(cb: Cb<ChatEmotion>) = biz.currentEmotion(devId, cb.wrap())

    private fun <T> Cb<T>.wrap(): Business.ResultListener<T> =
        object : Business.ResultListener<T> {
            override fun onSuccess(r: BusinessResponse?, result: T?, api: String?) = onOk(result)
            override fun onFailure(r: BusinessResponse?, result: T?, api: String?) =
                onErr(r?.errorCode?.toIntOrNull() ?: -1, r?.errorMsg)
        }
}
