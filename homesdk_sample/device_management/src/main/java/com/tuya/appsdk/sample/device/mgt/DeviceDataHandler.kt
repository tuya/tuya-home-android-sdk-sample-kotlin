package com.tuya.appsdk.sample.device.mgt

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.thingclips.sdk.core.PluginManager
import com.thingclips.smart.dp.parser.api.IDeviceDpParser
import com.thingclips.smart.dp.parser.api.IDpParser
import com.thingclips.smart.dp.parser.api.ISwitch
import com.thingclips.smart.interior.api.IAppDpParserPlugin
import com.thingclips.smart.sdk.bean.DeviceBean
import com.tuya.appsdk.sample.device.mgt.list.adapter.DeviceMgtAdapter
import java.util.concurrent.Executors

/**
 * create by dongdaqing[mibo] 2023/9/20 14:23
 */
class DeviceDataHandler(lifecycleOwner: LifecycleOwner, private val adapter: DeviceMgtAdapter) :
    LifecycleEventObserver {
    private val plugin by lazy {
        PluginManager.service(IAppDpParserPlugin::class.java)
    }

    private val handler by lazy {
        Handler(Looper.getMainLooper())
    }

    private val mExecutor by lazy {
        Executors.newSingleThreadExecutor()
    }

    init {
        lifecycleOwner.lifecycle.addObserver(this)
    }

    fun execute(runnable: Runnable) {
        mExecutor.execute(runnable)
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (event == Lifecycle.Event.ON_DESTROY) {
            mExecutor.shutdownNow()
        }
    }

    fun updateAdapter(list: List<DeviceBean>) {
        val data = java.util.ArrayList<SimpleDevice>()
        for (deviceBean in list) {
            val parser: IDeviceDpParser = plugin.update(deviceBean)
            val switch = parser.getSwitchDp()
            val simpleDevice = SimpleDevice(
                deviceBean.devId,
                deviceBean.getIconUrl(),
                deviceBean.getName(),
                deviceBean.isOnline,
                deviceBean.productBean.category,
                convert(deviceBean.devId, parser.getDisplayDp(), true),
                convert(deviceBean.devId, parser.getOperableDp(), false),
                if (switch == null) null else SimpleSwitch(switch.getSwitchStatus() == ISwitch.SWITCH_STATUS_ON)
            )
            data.add(simpleDevice)
        }
        handler.post { adapter.update(data) }
    }

    private fun convert(
        devId: String,
        list: List<IDpParser<Any>>,
        display: Boolean
    ): List<SimpleDp> {
        val data = java.util.ArrayList<SimpleDp>()
        for (dp in list) {
            val simpleDp = SimpleDp(
                devId,
                dp.getDpId(),
                dp.getIconFont(),
                if (display) dp.getDisplayStatus() else dp.getDisplayStatusForQuickOp(),
                dp.getDisplayTitle(),
                dp.getType()
            )
            data.add(simpleDp)
        }
        return data
    }
}