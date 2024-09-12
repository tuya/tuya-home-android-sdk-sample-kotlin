package com.thingclips.smart.devicebiz.biz.ota

import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import android.widget.Toast.makeText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.thingclips.sdk.core.PluginManager
import com.thingclips.sdk.device.enums.DevUpgradeStatusEnum
import com.thingclips.smart.android.common.utils.L
import com.thingclips.smart.android.device.bean.UpgradeInfoBean
import com.thingclips.smart.android.network.Business
import com.thingclips.smart.android.network.http.BusinessResponse
import com.thingclips.smart.android.user.bean.User
import com.thingclips.smart.device.DeviceBusinessDataManager
import com.thingclips.smart.device.bean.ThingDevUpgradeStatusBean
import com.thingclips.smart.device.ota.bean.UpgradeDevListBean
import com.thingclips.smart.devicebiz.R
import com.thingclips.smart.devicebiz.bean.OtaInfoItemBean
import com.thingclips.smart.devicebiz.databinding.ActivityOtaBinding
import com.thingclips.smart.family.FamilyManagerCoreKit
import com.thingclips.smart.family.base.BizResponseData
import com.thingclips.smart.family.bean.MemberBean
import com.thingclips.smart.family.callback.IFamilyDataCallback
import com.thingclips.smart.family.callback.IFamilyMemberDataCallback
import com.thingclips.smart.family.usecase.interf.IFamilyUseCase
import com.thingclips.smart.family.usecase.interf.IMemberUseCase
import com.thingclips.smart.home.sdk.ThingHomeSdk
import com.thingclips.smart.interior.api.IThingUserPlugin
import com.thingclips.smart.sdk.api.IDevOTAListener
import com.thingclips.smart.sdk.api.IGetOtaInfoCallback


class OtaActivity : AppCompatActivity() {

    private var homeId: Long = 0
    private var memberId: Long = 0
    private lateinit var adapter: OtaListAdapter
    private lateinit var binding: ActivityOtaBinding
    private val updateDevList: MutableList<OtaInfoItemBean> = ArrayList(8)
    private val listenerList: MutableList<IDevOTAListener> = ArrayList(8)
    private val userPlugin = PluginManager.service(
        IThingUserPlugin::class.java
    )

    companion object {
        const val TAG = "OtaActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOtaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbarOta.title = resources.getString(R.string.device_list)
        homeId = intent.getLongExtra("homeId", 0)
        initView()
        getMemberId()
    }

    private fun initView() {
        adapter = OtaListAdapter()
        binding.otaRecycler.layoutManager = LinearLayoutManager(this)
        binding.otaRecycler.addItemDecoration(
            DividerItemDecoration(
                this,
                DividerItemDecoration.VERTICAL
            )
        )
        binding.otaRecycler.adapter = adapter
        adapter.setOnItemClickListener(object : OtaListAdapter.OnItemClickListener {
            override fun onItemClick(bean: OtaInfoItemBean?, position: Int) {

            }

            override fun onButtonClick(position: Int) {
                registerOtaStatus(position)
                startOta(position)
            }
        })
    }

    private fun isHomeManager() {
        var mMemberUseCase: IMemberUseCase = FamilyManagerCoreKit.getMemberUseCase()
        mMemberUseCase.getMemberInfo(
            homeId,
            memberId,
            object : IFamilyMemberDataCallback<MemberBean> {
                override fun onSuccess(result: MemberBean?) {
                    L.i(TAG, "isHomeManager onSuccess")
                    /**
                     * 0：家庭所有者 MemberRole.ROLE_OWNER
                     * 1：家庭管理员 MemberRole.ROLE_ADMIN
                     * 2：家庭普通成员 MemberRole.ROLE_MEMBER
                     */
                    val role = result?.role
                    if (role != null && role < 2) {
                        getOtaDevList()
                    }else{
                        showTips("isn't home manager")
                    }
                }

                override fun onError(errorCode: String?, errorMessage: String?) {

                }

            })
    }

    private fun getMemberId() {
        var mFamilyUseCase: IFamilyUseCase = FamilyManagerCoreKit.getFamilyUseCase()
        mFamilyUseCase.getFamilyMemberList(homeId,
            object : IFamilyDataCallback<BizResponseData<List<MemberBean>>> {
                override fun onSuccess(result: BizResponseData<List<MemberBean>>?) {
                    L.i(TAG, "getMemberId onSuccess")
                    val members: ArrayList<MemberBean> = arrayListOf()
                    result?.data?.let { members.addAll(it) }
                    if (members.size == 1) {
                        memberId = members.get(0).memberId
                    } else {
                        val user: User? = userPlugin.getUserInstance().getUser()
                        val uid = user?.uid ?: ""
                        for (j in members.indices) {
                            val bean: MemberBean = members.get(j)
                            if (TextUtils.equals(uid, bean.uid)) {
                                memberId = bean.memberId
                            }
                        }
                    }
                    isHomeManager()
                }

                override fun onError(errorCode: String?, errorMessage: String?) {

                }
            })
    }

    private fun getOtaDevList() {
        val otaManager = DeviceBusinessDataManager.getInstance().getDeviceOTAManager()
        otaManager.queryHasUpgradeInfoDeviceList(
            homeId.toString(),
            object : Business.ResultListener<UpgradeDevListBean> {
                override fun onFailure(
                    p0: BusinessResponse?,
                    p1: UpgradeDevListBean?,
                    p2: String?
                ) {
                    //fail
                    L.i(TAG, "getOtaDevList onFailure")
                }

                override fun onSuccess(
                    p0: BusinessResponse?,
                    p1: UpgradeDevListBean?,
                    p2: String?
                ) {
                    L.i(TAG, "getOtaDevList onSuccess")
                    if (p1?.isStatus == true) {
                        L.i(TAG, "getOtaDevList canUpdate")
                        for (otaBean in p1.list) {
                            updateDevList.add(getItemFromUpgradeDev(otaBean))
                        }
                        adapter.setData(updateDevList,null)
                        L.i(TAG, "getOtaDevList setData")
                    } else {
                        // fail
                        L.i(TAG, "getOtaDevList can't update")
                    }
                }
            })
    }

    private fun getItemFromUpgradeDev(bean: UpgradeDevListBean.UpgradeDeviceBean): OtaInfoItemBean {
        return OtaInfoItemBean(bean.icon, bean.name, bean.devId, DevUpgradeStatusEnum.READY,true)
    }

    private fun startOta(index: Int) {
        val iThingOTAService = ThingHomeSdk.newOTAServiceInstance(updateDevList[index].devId)
        iThingOTAService?.getFirmwareUpgradeInfo(object : IGetOtaInfoCallback {
            override fun onSuccess(upgradeInfoBeans: MutableList<UpgradeInfoBean>?) {
                iThingOTAService.startFirmwareUpgrade(upgradeInfoBeans)
            }

            override fun onFailure(code: String?, error: String?) {
                showTips("ota fail")
            }
        })
    }

    private fun registerOtaStatus(index: Int) {
        val iThingOTAService = ThingHomeSdk.newOTAServiceInstance(updateDevList[index].devId)
        iThingOTAService?.registerDevOTAListener(object : IDevOTAListener {
            override fun firmwareUpgradeStatus(upgradeStatusBean: ThingDevUpgradeStatusBean) {
                for (otaDev in updateDevList){
                    if (otaDev.devId == upgradeStatusBean.devId){
                        otaDev.status = upgradeStatusBean.status
                        break
                    }
                }
                adapter.setData(updateDevList,index)
            }
        });
    }



    private fun showTips(tip:String){
        makeText(this@OtaActivity,tip,Toast.LENGTH_LONG).show()
    }

}