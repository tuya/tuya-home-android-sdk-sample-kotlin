package com.tuya.appbizsdk.scenebiz.main

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.thingclips.smart.family.FamilyManagerCoreKit
import com.thingclips.smart.family.base.BizResponseData
import com.thingclips.smart.family.bean.FamilyBean
import com.thingclips.smart.family.callback.IFamilyDataCallback
import com.thingclips.smart.home.sdk.ThingHomeSdk
import com.thingclips.smart.scene.api.IResultCallback
import com.thingclips.smart.scene.api.service.IBaseService
import com.thingclips.smart.scene.model.NormalScene
import com.tuya.appbizsdk.scenebiz.databinding.ActivitySceneMainBinding
import com.tuya.appbizsdk.scenebiz.detail.SceneDetailActivity

class SceneMainActivity : AppCompatActivity() {
    companion object {
        const val TAG = "SceneMainActivity"
    }

    private lateinit var binding: ActivitySceneMainBinding
    private lateinit var sceneListAdapter: SceneListAdapter

    private val baseService: IBaseService? = ThingHomeSdk.getSceneServiceInstance()?.baseService()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySceneMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        binding.toolbar.run {
            setNavigationOnClickListener {
                finish()
            }
        }

        sceneListAdapter = SceneListAdapter { sceneId ->
            Intent(this@SceneMainActivity, SceneDetailActivity::class.java).run {
                this.putExtra(SceneDetailActivity.KEY_SCENE_ID, sceneId)
                this@SceneMainActivity.startActivity(this)
            }
        }
        binding.contentMain.rvSceneList.adapter = sceneListAdapter

        binding.fabAddScene.setOnClickListener {
            Intent(this@SceneMainActivity, SceneDetailActivity::class.java).run {
                this@SceneMainActivity.startActivity(this)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        FamilyManagerCoreKit.getFamilyUseCase().getCurrentDefaultFamilyDetail(object : IFamilyDataCallback<BizResponseData<FamilyBean>> {
            override fun onSuccess(result: BizResponseData<FamilyBean>?) {
                val msg = "getCurrentDefaultFamilyDetail onSuccess, result: $result"
                Log.i(TAG, msg)
                //successful return result。
                val gid = result?.data?.homeId
                if (gid == null) {
                    val msg1 = "gid is null"
                    Log.e(TAG, msg1)
                    Toast.makeText(this@SceneMainActivity, msg1, Toast.LENGTH_LONG).show()
                    return
                }
                gid?.let {
                    baseService?.getSimpleSceneAll(it, object : IResultCallback<List<NormalScene>?> {
                        override fun onError(errorCode: String?, errorMessage: String?) {
                            val msg1 = "getSimpleSceneAll, errCode: $errorCode, errMsg: $errorMessage"
                            Log.e(TAG, msg1)
                            Toast.makeText(this@SceneMainActivity, msg1, Toast.LENGTH_LONG).show()
                        }

                        override fun onSuccess(result: List<NormalScene>?) {
                            val msg1 = "getSimpleSceneAll onSuccess, result.size: ${result?.size}"
                            Log.i(TAG, msg1)
                            result?.let {
                                result.map { normalScene ->
                                    SceneItemData(normalScene.id, normalScene.sceneType(), normalScene.name)
                                }.run {
                                    sceneListAdapter.submitList(this)
                                }
                            }
                        }
                    })
                }
            }

            override fun onError(errcode: String?, errMsg: String?) {
                val msg = "getCurrentDefaultFamilyDetail, errCode: $errcode, errMsg: $errMsg"
                Log.e(TAG, msg)
                Toast.makeText(this@SceneMainActivity, msg, Toast.LENGTH_LONG).show()
            }
        })
    }
}