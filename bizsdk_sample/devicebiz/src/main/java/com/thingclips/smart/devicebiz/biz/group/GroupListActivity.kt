package com.thingclips.smart.devicebiz.biz.group

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.thingclips.group_usecase_api.bean.GroupDeviceDetailBean
import com.thingclips.smart.devicebiz.R
import com.thingclips.smart.devicebiz.utils.NoShakeBtnUtil
import com.thingclips.smart.devicebiz.utils.toGroupDeviceDetail
import com.thingclips.smart.group.manager.GroupInitBuilder
import com.thingclips.smart.group.manager.GroupOperateBuilder
import com.thingclips.smart.group.manager.IThingGroupManager
import com.thingclips.smart.group.manager.ThingGroupBizKit
import com.thingclips.smart.group.manager.bean.GroupInfo
import com.thingclips.smart.group.manager.bean.GroupResult
import com.thingclips.smart.group.manager.callback.FailureCallback
import com.thingclips.smart.group.manager.callback.ProcessCallback
import com.thingclips.smart.group.manager.callback.QueryDeviceCallback
import com.thingclips.smart.group.manager.callback.SuccessCallback


open class GroupListActivity : AppCompatActivity() {

    private val mAddedAdapter by lazy {
        AddedDeviceAdapter(this, arrayListOf())
    }
    private val mNotAddAdapter by lazy {
        NotAddedDeviceAdapter(this, arrayListOf())
    }

    private var homeId = 0L
    private var deviceId: String? = null
    private var groupId: Long = 0
    private var isGroup = false
    private var addBeanList: ArrayList<GroupDeviceDetailBean> = ArrayList()
    private var foundBeanList: ArrayList<GroupDeviceDetailBean> = ArrayList()
    var thingGroupBizManager: IThingGroupManager? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_list)

        initView()
        initData()
    }

    private fun initView() {
        initRecyclerView()
        findViewById<TextView>(R.id.tvCancel).setOnClickListener {
            finish()
        }

        findViewById<TextView>(R.id.tvSave).setOnClickListener {
            val selectedDevice: ArrayList<String> = ArrayList()
            mAddedAdapter.getData().forEach {
                selectedDevice.add(it.deviceBean.devId)
            }

            if (isGroup) {
                val operateBuilder = GroupOperateBuilder.Builder()
                    .setAllSelectDeviceList(selectedDevice)
                    .setSuccessCallback(object : SuccessCallback {
                        override fun result(groupId: Long, failDevices: List<GroupResult>?) {
                            Toast.makeText(this@GroupListActivity, if (isGroup) {
                                "编辑成功"
                            } else {
                                "创建成功"
                            }, Toast.LENGTH_SHORT).show()
                            finish()
                        }

                    })
                    .setProcessCallback(object : ProcessCallback {
                        override fun result(process: Int, size: Int) {

                        }
                    })
                    .setFailureCallback(object : FailureCallback {
                        override fun onError(
                            errorCode: String?,
                            errorMessage: String?,
                            groupId: Long,
                            failDevices: List<GroupResult>?
                        ) {
                            Toast.makeText(this@GroupListActivity, if (isGroup) {
                                "编辑失败"
                            } else {
                                "创建失败"
                            }, Toast.LENGTH_SHORT).show()
                        }

                    })
                    .build()
                thingGroupBizManager?.updateGroup(operateBuilder)
            } else {
                val operateBuilder = GroupOperateBuilder.Builder()
                    .setGroupName("群组1")
                    .setAllSelectDeviceList(selectedDevice)
                    .setSuccessCallback(object : SuccessCallback {
                        override fun result(groupId: Long, failDevices: List<GroupResult>?) {
                            Toast.makeText(this@GroupListActivity, if (isGroup) {
                                "编辑成功"
                            } else {
                                "创建成功"
                            }, Toast.LENGTH_SHORT).show()
                            finish()
                        }

                    })
                    .setProcessCallback(object : ProcessCallback {
                        override fun result(process: Int, size: Int) {

                        }
                    })
                    .setFailureCallback(object : FailureCallback {
                        override fun onError(
                            errorCode: String?,
                            errorMessage: String?,
                            groupId: Long,
                            failDevices: List<GroupResult>?
                        ) {
                            Toast.makeText(this@GroupListActivity, if (isGroup) {
                                "编辑失败"
                            } else {
                                "创建失败"
                            }, Toast.LENGTH_SHORT).show()
                        }

                    })
                    .build()
                thingGroupBizManager?.createGroup(operateBuilder)
            }

        }

    }


    private fun initRecyclerView() {
        val rvAddedView = findViewById<RecyclerView>(R.id.rvAddedView)
        val rvNotAddView = findViewById<RecyclerView>(R.id.rvNotAddView)
        // added
        rvAddedView.layoutManager = LinearLayoutManager(this)
        rvAddedView.adapter = mAddedAdapter
        mAddedAdapter.setOnItemClickListener {
            if (!it.isOnline) {
                Toast.makeText(this, "设备离线无法移除", Toast.LENGTH_SHORT).show()
                return@setOnItemClickListener
            }
            if (NoShakeBtnUtil.isFastDoubleClick(it.hashCode())) {
                return@setOnItemClickListener
            }
            mAddedAdapter.removeData(it)
            it.isChecked = !it.isChecked
            mNotAddAdapter.addData(it)
            foundBeanList.add(it)
            updateRightButton()


        }
        // not Add
        rvNotAddView.layoutManager = LinearLayoutManager(this)
        rvNotAddView.adapter = mNotAddAdapter
        mNotAddAdapter.setOnItemClickListener {
            if (!it.isOnline) {
                Toast.makeText(this, "设备离线无法添加", Toast.LENGTH_SHORT).show()
                return@setOnItemClickListener
            }
            if (NoShakeBtnUtil.isFastDoubleClick(it.hashCode())) {
                return@setOnItemClickListener
            }
            mNotAddAdapter.removeData(it)
            foundBeanList.remove(it)
            it.isChecked = !it.isChecked
            mAddedAdapter.addData(it)
            updateRightButton()
        }
    }

    private fun initData() {
        homeId = intent.getLongExtra("homeId", 0)
        deviceId = intent.getStringExtra("deviceId")
        groupId = intent.getLongExtra("groupId", 0)
        val tvGroupTitle = findViewById<TextView>(R.id.tvGroupTitle)
        isGroup = deviceId.isNullOrEmpty() && groupId > 0
        val initBuilder = if (isGroup) {
            tvGroupTitle.text = "编辑群组"
            GroupInitBuilder.Builder()
                .setGroupId(groupId)
                .build()
        } else {
            tvGroupTitle.text = "创建群组"
            GroupInitBuilder.Builder()
                .setDevId(deviceId)
                .build()
        }
        thingGroupBizManager = ThingGroupBizKit.getGroupBizManager(homeId, initBuilder)
        val operateBuilder = GroupOperateBuilder.Builder()
            .setQueryDeviceCallback(object : QueryDeviceCallback {
                override fun result(list: List<GroupInfo>) {
                    var toGroupDeviceDetail = list.toGroupDeviceDetail()
                    toGroupDeviceDetail?.let {
                        for (groupBean in toGroupDeviceDetail) {
                            if (isChecked(groupBean)) addBeanList.add(groupBean) else foundBeanList.add(
                                groupBean
                            )
                        }
                    }
                    mAddedAdapter.setDataList(addBeanList)
                    mNotAddAdapter.setDataList(foundBeanList)
                    updateRightButton()
                }

            })
            .setFailureCallback(object : FailureCallback {
                override fun onError(
                    errorCode: String?,
                    errorMessage: String?,
                    groupId: Long,
                    failDevices: List<GroupResult>?
                ) {

                }

            })
            .build()
        thingGroupBizManager?.fetchDeviceList(operateBuilder)
    }


    open protected fun updateRightButton() {
        val tvSave = findViewById<TextView>(R.id.tvSave)
        if (mAddedAdapter.getData().size > 0) {
            tvSave?.isEnabled = true
            tvSave?.setTextColor(resources.getColor(R.color.thing_theme_color_m1))
        } else {
            tvSave?.isEnabled = false
            tvSave?.setTextColor(resources.getColor(R.color.thing_theme_color_m1_1))
        }
    }

    fun isChecked(groupBean: GroupDeviceDetailBean): Boolean {
        val deviceBean = groupBean.deviceBean
        val isChecked =
            if (!isGroup) {
                deviceBean.devId == deviceId && deviceBean.isOnline
            } else {
                groupBean.isChecked
            }
        return if (isChecked) {
            groupBean.isChecked = true
            true
        } else {
            groupBean.isChecked = false
            false
        }
    }

}