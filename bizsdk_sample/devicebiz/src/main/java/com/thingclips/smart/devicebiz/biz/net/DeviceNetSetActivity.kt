package com.thingclips.smart.devicebiz.biz.net

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.thingclips.smart.devicebiz.biz.net.model.DeviceNetSetModel
import com.thingclips.smart.devicebiz.databinding.ActivityDeviceNetSetBinding
import com.thingclips.smart.devicebiz.utils.SHA256Utils
import com.thingclips.smart.sdk.api.wifibackup.api.bean.BackupWifiBean

class DeviceNetSetActivity : AppCompatActivity() {

    private var deviceId: String? = null
    private var backupWifiBeans: MutableList<BackupWifiBean?> = ArrayList()


    private lateinit var binding: ActivityDeviceNetSetBinding
    private val viewModel: DeviceNetSetModel by viewModels()


    private lateinit var adapter: DeviceNetworkListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceNetSetBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
        initObserve()
        initData()

    }

    private fun initView() {
        adapter = DeviceNetworkListAdapter()
        binding.rvWifiList.layoutManager = LinearLayoutManager(this)
        binding.rvWifiList.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        binding.rvWifiList.adapter = adapter
        adapter.setOnItemClickListener(object : DeviceNetworkListAdapter.OnItemClickListener {
            override fun onItemClick(bean: BackupWifiBean?, position: Int) {
                if (viewModel.canSwitchDeviceWiFi.value == true) {
                    showSwitchToBackupWifiDialog(bean)
                } else {
                    Toast.makeText(
                        this@DeviceNetSetActivity,
                        "设备不支持替换备用Wi-Fi",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })

        binding.deviceAddNewNet.setOnClickListener {
            showSwitchToNewWifiDialog("ABCD", "12345678")
        }

        binding.deviceAddNewBackupNet.setOnClickListener {
            showAddNewBackupWifiDialog("ABCD", "12345678")
        }

    }


    private fun initObserve() {
        viewModel.isSupportBackupNetwork.observe(this) {
            binding.deviceIsNoSupport.visibility = if (it) {
                viewModel.getDeviceCurrentWifi()
                viewModel.getDeviceBackupWiFiList()
                View.GONE
            } else {
                View.VISIBLE
            }
        }

        viewModel.mDeviceCurrentWifi.observe(this) {
            it?.let {
                binding.deviceCurrentNet.text = "当前设备网络为：${it.ssid}"
                viewModel.canSwitchDeviceWiFi(it)
                viewModel.canUpdateDeviceBackupWiFiList(it)
            }
        }

        viewModel.mDeviceBackupWiFiList.observe(this) {
            binding.backupWifiTip.text = if (it.isNullOrEmpty()) {
                binding.rvWifiList.visibility = View.GONE
                "当前设备无备用网络"
            } else {
                binding.rvWifiList.visibility = View.VISIBLE
                "备用网络"
            }
            backupWifiBeans.clear()
            backupWifiBeans.addAll(it)
            adapter.setData(it)
        }

        viewModel.canUpdateDeviceBackupWiFiList.observe(this) {
            binding.deviceAddNewBackupNet.visibility = if (it) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

        viewModel.canSwitchDeviceWiFi.observe(this) {
            binding.deviceAddNewNet.visibility = if (it) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

        viewModel.updateDeviceBackupWiFiListResult.observe(this) {
            if (it) {
                viewModel.getDeviceBackupWiFiList()
            } else {
                Toast.makeText(
                    this@DeviceNetSetActivity,
                    "设备更新备用Wi-Fi列表失败",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        viewModel.switchToBackupWifiResult.observe(this) {
            if (it) {
                viewModel.getDeviceCurrentWifi()
                viewModel.getDeviceBackupWiFiList()
            } else {
                Toast.makeText(
                    this@DeviceNetSetActivity,
                    "设备切换备用Wi-Fi失败",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        viewModel.switchToNewWifiResult.observe(this) {
            if (it) {
                viewModel.getDeviceCurrentWifi()
                viewModel.getDeviceBackupWiFiList()
            } else {
                Toast.makeText(
                    this@DeviceNetSetActivity,
                    "设备切换备用Wi-Fi失败",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    }

    private fun initData() {
        deviceId = intent.getStringExtra("deviceId")
        if (deviceId.isNullOrEmpty()) {
            binding.deviceIsNoSupport.visibility = View.GONE
        } else {
            viewModel.init(deviceId!!)
            viewModel.isSupportBackupNetwork()
        }
    }

    private fun showSwitchToBackupWifiDialog(bean: BackupWifiBean?) {
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle("提示")
        alertDialogBuilder.setMessage("是否将设备的Wi-Fi切换成 ${bean?.ssid}")
        alertDialogBuilder.setPositiveButton("确定") { dialog, which ->
            viewModel.switchToBackupWifi(bean?.hash)
        }

        alertDialogBuilder.setNegativeButton("取消") { dialog, which ->

        }

        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }


    private fun showSwitchToNewWifiDialog(ssid: String, pwd: String) {
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle("提示")
        alertDialogBuilder.setMessage("是否将设备的Wi-Fi切换成 $ssid")
        alertDialogBuilder.setPositiveButton("确定") { dialog, which ->
            viewModel.switchToNewWifi(ssid, pwd)
        }

        alertDialogBuilder.setNegativeButton("取消") { dialog, which ->

        }

        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }

    private fun showAddNewBackupWifiDialog(ssid: String, pwd: String) {
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle("提示")
        alertDialogBuilder.setMessage("是否为设备添加新的备用网络：$ssid")
        alertDialogBuilder.setPositiveButton("确定") { dialog, which ->
            val backupWifiBean = BackupWifiBean()
            backupWifiBean.ssid = ssid
            backupWifiBean.passwd = pwd
            backupWifiBean.hash =
                SHA256Utils.getBase64Hash(viewModel.deviceBean?.getLocalKey() + ssid + pwd)
            backupWifiBeans.add(backupWifiBean)
            viewModel.updateDeviceBackupWiFiList(backupWifiBeans as List<BackupWifiBean>)
        }

        alertDialogBuilder.setNegativeButton("取消") { dialog, which ->

        }

        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }


}