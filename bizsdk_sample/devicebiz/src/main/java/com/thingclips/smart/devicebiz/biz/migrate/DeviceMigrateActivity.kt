package com.thingclips.smart.devicebiz.biz.migrate

import android.os.Bundle
import android.os.PersistableBundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.thingclips.smart.device.migration.IMigratedStateListener
import com.thingclips.smart.device.migration.bean.MigrationInfo
import com.thingclips.smart.devicebiz.biz.migrate.model.DeviceMigrateModel
import com.thingclips.smart.devicebiz.biz.net.model.DeviceNetSetModel
import com.thingclips.smart.devicebiz.databinding.ActivityDeviceMigrateBinding
import com.thingclips.smart.devicebiz.databinding.ActivityDeviceNetSetBinding

class DeviceMigrateActivity : AppCompatActivity() {

    private val viewModel: DeviceMigrateModel by viewModels()
    private lateinit var binding: ActivityDeviceMigrateBinding
    private var deviceId: String? = null
    private var gid: Long = 0

    private val listener = object : IMigratedStateListener {
        override fun onMigratedStateChange(migrationInfo: MigrationInfo?) {
            Toast.makeText(
                this@DeviceMigrateActivity,
                "迁移状态：${migrationInfo?.status}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceMigrateBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
        initObserve()
        initData()

    }


    private fun initView() {
        binding.btnStartMigrate.setOnClickListener {
            if (binding.deviceIDOrSN.text.isNullOrEmpty()) {
                Toast.makeText(this, "请输入设备ID或SN", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.startMigrateGateway(binding.deviceIDOrSN.text.toString(), deviceId!!, gid)
        }
    }

    private fun initObserve() {
        viewModel.isSupportMigrate.observe(this) {
            binding.isSupportMigrate.visibility = if (it) {
                viewModel.getEnableMigratedGatewayList(deviceId!!, gid)
                View.GONE
            } else {
                View.VISIBLE
            }
        }

        viewModel.mMigrationDeviceList.observe(this) {
            if (it.isNullOrEmpty()) {
                binding.deviceIDOrSN.visibility = View.GONE
                binding.btnStartMigrate.visibility = View.GONE
                binding.migrationDeviceList.text = "无可迁移设备"
                return@observe
            }
            binding.deviceIDOrSN.visibility = View.VISIBLE
            binding.btnStartMigrate.visibility = View.VISIBLE
            val sb = StringBuffer()
            for (item in it) {
                sb.append("ID/SN: ").append(item).append("\n")
            }
            binding.migrationDeviceList.text = sb.toString()
        }

        viewModel.migrationState.observe(this) {
            Toast.makeText(this, "迁移状态：${it?.status}", Toast.LENGTH_SHORT).show()
        }

        viewModel.isStartMigrateSuccess.observe(this) {
            if (it) {
                // 查询迁移状态
                viewModel.getMigratedGwState(deviceId!!)
                Toast.makeText(this, "开始迁移", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "迁移失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initData() {
        deviceId = intent.getStringExtra("deviceId")
        gid = intent.getLongExtra("homeId", 0)
        if (deviceId.isNullOrEmpty()) {
            binding.isSupportMigrate.visibility = View.VISIBLE
        } else {
            viewModel.isSupportedMigrationWithGwId(deviceId!!)
            viewModel.addMigratedStateListener(listener)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.removeMigratedStateListener(listener)
        viewModel.onDestroy()
    }

}