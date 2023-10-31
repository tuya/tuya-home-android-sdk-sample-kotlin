package com.tuya.appbizsdk.scenebiz.detail

import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ConcatAdapter
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import com.thingclips.scene.core.enumerate.MatchType
import com.thingclips.smart.family.FamilyManagerCoreKit
import com.thingclips.smart.family.base.BizResponseData
import com.thingclips.smart.family.bean.FamilyBean
import com.thingclips.smart.family.callback.IFamilyDataCallback
import com.thingclips.smart.home.sdk.ThingHomeSdk
import com.thingclips.smart.scene.api.IResultCallback
import com.thingclips.smart.scene.api.service.IBaseService
import com.thingclips.smart.scene.model.NormalScene
import com.thingclips.smart.scene.model.action.SceneAction
import com.thingclips.smart.scene.model.condition.SceneCondition
import com.thingclips.smart.scene.model.constant.CONDITION_TYPE_MANUAL
import com.thingclips.smart.scene.model.constant.SceneType
import com.thingclips.smart.scene.model.constant.TimeInterval
import com.thingclips.smart.scene.model.edit.PreCondition
import com.thingclips.smart.scene.model.edit.PreConditionExpr
import com.tuya.appbizsdk.scenebiz.R
import com.tuya.appbizsdk.scenebiz.databinding.SceneDetailActivityBinding
import java.util.TimeZone


class SceneDetailActivity : AppCompatActivity() {

    companion object {
        const val TAG = "SceneDetailActivity"
        const val KEY_SCENE_ID = "sceneId"
        const val COND = 1
        const val ACT = 2
        const val COND_REQ_KEY = "condition_result"
        const val ACT_REQ_KEY = "action_result"
    }

    private lateinit var binding: SceneDetailActivityBinding
    private var sceneId: String? = null
    lateinit var condAddAdapter: DetailAddAdapter
    lateinit var actAddAdapter: DetailAddAdapter
    lateinit var condTitleAdapter: DetailTitleAdapter
    lateinit var actTitleAdapter: DetailTitleAdapter
    lateinit var detailCondAdapter: DetailConditionAdapter
    lateinit var detailActAdapter: DetailActionAdapter

    private var editScene: NormalScene? = null

    private val baseService: IBaseService? = ThingHomeSdk.getSceneServiceInstance()?.baseService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SceneDetailActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        intent.getStringExtra(KEY_SCENE_ID)?.let {
            sceneId = it
        }

        binding.toolbar.run {
            setNavigationOnClickListener {
                finish()
            }
        }
        binding.btnSave.run {
            setOnClickListener {
                if (binding.etSceneName.text?.isBlank() == true) {
                    val msg = "name is empty"
                    Log.e(TAG, msg)
                    binding.etSceneNameLayout.error = msg
                    Toast.makeText(this@SceneDetailActivity, msg, Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                editScene = if (editScene == null) {
                    NormalScene()
                } else {
                    NormalScene(editScene!!)
                }.apply {
                    this.name = binding.etSceneName.text.toString()
                    // 一键执行的条件不用传递
                    this.conditions = this.conditions?.filterNot { it.entityType == CONDITION_TYPE_MANUAL }
                }
                saveNormalScene()
            }
        }
        binding.tvPreCondition.run {
            if (sceneId == null) {
                setOnClickListener {
                    val preC = PreCondition().apply {
                        val expr = PreConditionExpr()
                        expr.cityName = "杭州"
                        expr.cityId = "1001803662567473213"
                        expr.start = "00:00"
                        expr.end = "23:59"
                        expr.loops = "1111111"
                        expr.timeInterval = TimeInterval.TIME_INTERVAL_ALL_DAY.value
                        expr.timeZoneId = TimeZone.getDefault().id
                        this.expr = expr
                        condType = PreCondition.TYPE_TIME_CHECK
                    }
                    val builder: AlertDialog.Builder = AlertDialog.Builder(this@SceneDetailActivity)
                    builder.setTitle("Select PreCondition")
                        .setMessage(JSON.toJSONString(preC.expr))
                        .setPositiveButton(R.string.confirm) { _, which ->
                            if (which == DialogInterface.BUTTON_POSITIVE) {
                                editScene = if (editScene == null) {
                                    NormalScene()
                                } else {
                                    NormalScene(editScene!!)
                                }.apply {
                                    preConditions = listOf(preC)
                                }
                            }
                        }

                    val dialog: AlertDialog = builder.create()
                    dialog.show()
                }
            }
        }

        binding.tvMatchType.run {
            if (sceneId == null) {
                setOnClickListener {
                    val builder = AlertDialog.Builder(this@SceneDetailActivity)
                    builder.setTitle("Select an option")
                        .setItems(R.array.match_type_options_array) { _, which ->
                            val items = resources.getTextArray(R.array.match_type_options_array)
                            val matchType = if (items[which] == getString(R.string.scene_condition_type_or)) {
                                MatchType.ANY
                            } else {
                                MatchType.ALL
                            }
                            binding.tvMatchType.text = items[which]

                            editScene = if (editScene == null) {
                                NormalScene()
                            } else {
                                NormalScene(editScene!!)
                            }.apply {
                                this.matchType = matchType.type
                            }
                        }

                    val dialog = builder.create()
                    dialog.show()
                }
            }
        }

        supportFragmentManager.setFragmentResultListener(COND_REQ_KEY, this) { _, result ->
            val data: String? = result.getString(ConditionDialogFragment.KEY_DATA_KEY)
            val editCondition = JSONObject.parseObject(data, SceneCondition::class.java)
            val editConditions = editScene?.conditions ?: mutableListOf()
            editConditions.add(editCondition)

            editScene = if (editScene == null) {
                NormalScene()
            } else {
                NormalScene(editScene!!)
            }.apply {
                conditions = editConditions
            }

            editScene!!.conditions?.map { condition ->
                ConditionItemData(condition.id, condition.entityType, JSON.toJSONString(condition.expr))
            }.run {
                detailCondAdapter.submitList(this)
            }
        }

        supportFragmentManager.setFragmentResultListener(ACT_REQ_KEY, this) { _, result ->
            val data: String? = result.getString(ActionDialogFragment.KEY_DATA_KEY)
            val actionArray: List<SceneAction>? = JSON.parseArray(data, SceneAction::class.java)
            actionArray?.let {
                val editActions = editScene?.actions ?: mutableListOf()
                editActions.addAll(actionArray)

                editScene = if (editScene == null) {
                    NormalScene()
                } else {
                    NormalScene(editScene!!)
                }.apply {
                    actions = editActions
                }

                editScene!!.actions?.map { action ->
                    ActionItemData(action.id, action.actionExecutor, JSON.toJSONString(action.executorProperty))
                }.run {
                    detailActAdapter.submitList(this)
                }
            }
        }

        val disable = sceneId != null
        condAddAdapter = DetailAddAdapter(disable) { type ->
            if (type == COND) {
                ConditionDialogFragment.newInstance(COND_REQ_KEY).show(supportFragmentManager, ConditionDialogFragment::class.java.name)
            }
        }
        actAddAdapter = DetailAddAdapter(disable) { type ->
            if (type == ACT) {
                ActionDialogFragment.newInstance(ACT_REQ_KEY).show(supportFragmentManager, ActionDialogFragment::class.java.name)
            }
        }
        condTitleAdapter = DetailTitleAdapter()
        actTitleAdapter = DetailTitleAdapter()
        detailCondAdapter = DetailConditionAdapter()
        detailActAdapter = DetailActionAdapter()
        val concatAdapter = ConcatAdapter(
            condTitleAdapter,
            detailCondAdapter,
            condAddAdapter,
            actTitleAdapter,
            detailActAdapter,
            actAddAdapter
        )
        binding.rvSceneDetail.adapter = concatAdapter

        sceneId?.let {
            FamilyManagerCoreKit.getFamilyUseCase().getCurrentDefaultFamilyDetail(object : IFamilyDataCallback<BizResponseData<FamilyBean>> {
                override fun onSuccess(result: BizResponseData<FamilyBean>?) {
                    val msg = "getCurrentDefaultFamilyDetail onSuccess, result: $result"
                    Log.i(TAG, msg)
                    //successful return result。
                    val gid = result?.data?.homeId
                    if (gid == null) {
                        val msg1 = "gid is null"
                        Log.e(TAG, msg1)
                        Toast.makeText(this@SceneDetailActivity, msg1, Toast.LENGTH_LONG).show()
                        return
                    }
                    gid.let {
                        baseService?.getSceneDetail(it, sceneId!!, object : IResultCallback<NormalScene?> {
                            override fun onError(errorCode: String?, errorMessage: String?) {
                                val msg1 = "getSceneDetail, errCode: $errorCode, errMsg: $errorMessage"
                                Log.e(TAG, msg1)
                                Toast.makeText(this@SceneDetailActivity, msg1, Toast.LENGTH_LONG).show()
                            }

                            override fun onSuccess(result: NormalScene?) {
                                val msg1 = "getSceneDetail onSuccess, result: $result"
                                Log.i(TAG, msg1)
                                if (result?.sceneType() == SceneType.SCENE_TYPE_MANUAL) {
                                    result.conditions = listOf(SceneCondition().apply {
                                        entityType = CONDITION_TYPE_MANUAL
                                    })
                                }

                                result?.let {
                                    binding.btnSave.isVisible = false
                                    result.name?.let { name ->
                                        binding.toolbar.title = name
                                        binding.etSceneName.apply {
                                            setText(name)
                                            isEnabled = false
                                        }
                                    }

                                    result.conditions?.map { condition ->
                                        ConditionItemData(condition.id, condition.entityType, JSON.toJSONString(condition.expr))
                                    }.run {
                                        detailCondAdapter.submitList(this)
                                    }

                                    result.actions?.map { action ->
                                        ActionItemData(action.id, action.actionExecutor, JSON.toJSONString(action.executorProperty))
                                    }.run {
                                        detailActAdapter.submitList(this)
                                    }
                                }
                            }
                        })
                    }
                }

                override fun onError(errcode: String?, errMsg: String?) {
                    val msg = "getCurrentDefaultFamilyDetail, errCode: $errcode, errMsg: $errMsg"
                    Log.e(TAG, msg)
                    Toast.makeText(this@SceneDetailActivity, msg, Toast.LENGTH_LONG).show()
                }
            })
        }
    }

    override fun onResume() {
        super.onResume()
        condAddAdapter.submitList(listOf(COND))
        actAddAdapter.submitList(listOf(ACT))
        condTitleAdapter.submitList(listOf(R.string.title_condition))
        actTitleAdapter.submitList(listOf(R.string.title_action))
    }

    fun saveNormalScene() {
        editScene?.let { normalScene ->
            Log.i(TAG, "normalScene: ${JSON.toJSONString(normalScene)}")
            FamilyManagerCoreKit.getFamilyUseCase().getCurrentDefaultFamilyDetail(object : IFamilyDataCallback<BizResponseData<FamilyBean>> {
                override fun onSuccess(result: BizResponseData<FamilyBean>?) {
                    val msg = "getCurrentDefaultFamilyDetail onSuccess, result: $result"
                    Log.i(TAG, msg)
                    //successful return result。
                    val gid = result?.data?.homeId
                    gid?.let {
                        val cb = object : IResultCallback<NormalScene?> {
                            override fun onError(errorCode: String?, errorMessage: String?) {
                                val msg1 = "saveScene, errCode: $errorCode, errMsg: $errorMessage"
                                Log.e(TAG, msg1)
                                Toast.makeText(this@SceneDetailActivity, msg1, Toast.LENGTH_LONG).show()
                            }

                            override fun onSuccess(result: NormalScene?) {
                                val msg1 = "saveScene onSuccess, result: $result"
                                Log.i(TAG, msg1)
                                Toast.makeText(this@SceneDetailActivity, "saveScene succeed.", Toast.LENGTH_LONG).show()
                                finish()
                            }
                        }
                        if (sceneId == null) {
                            baseService?.saveScene(it, normalScene, cb)
                        } else {
                            baseService?.modifyScene(sceneId!!, normalScene, cb)
                        }
                    } ?: {
                        val msg1 = "gid is null"
                        Log.e(TAG, msg1)
                        Toast.makeText(this@SceneDetailActivity, msg1, Toast.LENGTH_LONG).show()
                    }
                }

                override fun onError(errcode: String?, errMsg: String?) {
                    val msg = "getCurrentDefaultFamilyDetail, errCode: $errcode, errMsg: $errMsg"
                    Log.e(TAG, msg)
                    Toast.makeText(this@SceneDetailActivity, msg, Toast.LENGTH_LONG).show()
                }
            })
        }
    }
}