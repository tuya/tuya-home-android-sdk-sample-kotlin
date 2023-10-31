package com.tuya.appbizsdk.scenebiz.act

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.alibaba.fastjson.JSON
import com.thingclips.scene.core.bean.ActionBase
import com.thingclips.scene.core.protocol.b.usualimpl.LinkageRuleActionBuilder
import com.thingclips.smart.family.FamilyManagerCoreKit
import com.thingclips.smart.family.base.BizResponseData
import com.thingclips.smart.family.bean.FamilyBean
import com.thingclips.smart.family.callback.IFamilyDataCallback
import com.thingclips.smart.home.sdk.ThingHomeSdk
import com.thingclips.smart.scene.api.IResultCallback
import com.thingclips.smart.scene.api.service.IBaseService
import com.thingclips.smart.scene.model.NormalScene
import com.thingclips.smart.scene.model.action.SceneAction
import com.thingclips.smart.scene.model.constant.ACTION_TYPE_ENABLE_AUTOMATION
import com.thingclips.smart.scene.model.constant.ACTION_TYPE_TRIGGER
import com.thingclips.smart.scene.model.constant.SceneType
import com.tuya.appbizsdk.scenebiz.R
import com.tuya.appbizsdk.scenebiz.databinding.LinkageListActivityBinding


class LinkageChooseListActivity : AppCompatActivity() {
    companion object {
        const val TAG = "LinkageListActivity"
        const val KEY_RESULT_DATA = "linkage_action_list"
    }

    private lateinit var binding: LinkageListActivityBinding
    private lateinit var linkageChooseListAdapter: LinkageChooseListAdapter

    private val baseService: IBaseService? = ThingHomeSdk.getSceneServiceInstance()?.baseService()
    private var curGid: Long? = null
    private var linkageList: List<NormalScene>? = null
    private var linkageCheckItemData: List<LinkageCheckItemData>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LinkageListActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        binding.toolbar.run {
            setNavigationOnClickListener {
                finish()
            }

            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_next -> {
                        val checkedList = linkageCheckItemData?.filter { it.checked } ?: emptyList()
                        val checkedSceneActions = checkedList.map {
                            val actionExecutor = if (it.sceneType == SceneType.SCENE_TYPE_MANUAL) {
                                ACTION_TYPE_TRIGGER
                            } else {
                                ACTION_TYPE_ENABLE_AUTOMATION
                            }
                            val actionBase: ActionBase = LinkageRuleActionBuilder(it.id, actionExecutor).build() as ActionBase
                            SceneAction(actionBase).apply {
                                entityName = it.name // 联动名称
                            }
                        }
                        val intent = Intent().apply {
                            putExtra(KEY_RESULT_DATA, JSON.toJSONString(checkedSceneActions))
                        }
                        setResult(RESULT_OK, intent)
                        finish()
                        return@setOnMenuItemClickListener true
                    }
                }
                return@setOnMenuItemClickListener false
            }
        }

        linkageChooseListAdapter = LinkageChooseListAdapter { linkageId, sceneType, isChecked ->
            updateLinkageList(linkageId, isChecked)
        }
        binding.rvChooseSceneList.adapter = linkageChooseListAdapter

        FamilyManagerCoreKit.getFamilyUseCase().getCurrentDefaultFamilyDetail(object : IFamilyDataCallback<BizResponseData<FamilyBean>> {
            override fun onSuccess(result: BizResponseData<FamilyBean>?) {
                val msg = "getCurrentDefaultFamilyDetail onSuccess, result: $result"
                Log.i(TAG, msg)
                //successful return result。
                val gid = result?.data?.homeId
                if (gid == null) {
                    val msg1 = "gid is null"
                    Log.e(TAG, msg1)
                    Toast.makeText(this@LinkageChooseListActivity, msg1, Toast.LENGTH_LONG).show()
                    return
                }
                curGid = gid
                if (linkageList == null) {
                    getLinkageListData(gid)
                }
            }

            override fun onError(errcode: String?, errMsg: String?) {
                val msg = "getCurrentDefaultFamilyDetail, errCode: $errcode, errMsg: $errMsg"
                Log.e(TAG, msg)
                Toast.makeText(this@LinkageChooseListActivity, msg, Toast.LENGTH_LONG).show()
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        // 加载菜单资源文件
        inflater.inflate(R.menu.linkage_choose_menu, menu)
        return true
    }

    override fun onResume() {
        super.onResume()
        curGid?.let {
            if (linkageList == null) {
                getLinkageListData(it)
            }
        }
    }

    private fun getLinkageListData(gid: Long) {
        baseService?.getSimpleSceneAll(gid, object : IResultCallback<List<NormalScene>?> {
            override fun onError(errorCode: String?, errorMessage: String?) {
                val msg1 = "getSimpleSceneAll, errCode: $errorCode, errMsg: $errorMessage"
                Log.e(TAG, msg1)
                Toast.makeText(this@LinkageChooseListActivity, msg1, Toast.LENGTH_LONG).show()
            }

            override fun onSuccess(result: List<NormalScene>?) {
                val msg1 = "getSimpleSceneAll onSuccess, result.size: ${result?.size}"
                Log.i(TAG, msg1)
                result?.let {
                    linkageList = it

                    it.map { normalScene ->
                        LinkageCheckItemData(normalScene.id, normalScene.sceneType(), normalScene.name)
                    }.run {
                        linkageCheckItemData = this
                        linkageChooseListAdapter.submitList(this)
                    }
                }
            }
        })
    }

    private fun updateLinkageList(linkageId: String, isChecked: Boolean) {
        val updateList: MutableList<LinkageCheckItemData> = linkageCheckItemData?.toMutableList() ?: mutableListOf()
        val originalData = updateList.find { it.id == linkageId }
        originalData?.let {
            val index = updateList.indexOf(it)
            LinkageCheckItemData(
                id = it.id,
                name = it.name,
                sceneType = it.sceneType,
                checked = isChecked,
            ).run {
                updateList[index] = this
                linkageCheckItemData = updateList.toList()
            }

            // updateList.toList().run {
            //     linkageChooseListAdapter.submitList(this)
            // }
        }
    }
}