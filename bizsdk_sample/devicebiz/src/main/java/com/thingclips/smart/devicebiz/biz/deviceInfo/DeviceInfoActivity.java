package com.thingclips.smart.devicebiz.biz.deviceInfo;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.thingclips.sdk.core.PluginManager;
import com.thingclips.smart.device.info.sdk.ThingDeviceInfoKit;
import com.thingclips.smart.device.info.sdk.api.IDeviceDetailInfoManager;
import com.thingclips.smart.devicebiz.R;
import com.thingclips.smart.interior.api.IThingDevicePlugin;
import com.thingclips.smart.sdk.api.IThingDevice;
import com.thingclips.smart.sdk.api.WifiSignalListener;
import com.thingclips.smart.thingdevicedetailkit.ThingDeviceDetailKit;

public class DeviceInfoActivity extends AppCompatActivity {

    private TextView deviceInfo;
    IDeviceDetailInfoManager newDeviceDetailInfoManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_info);
        Toolbar toolbar = findViewById(R.id.toolbar_main);
        toolbar.setTitle(getResources().getString(R.string.device_info));
        String deviceId = getIntent().getStringExtra("deviceId");
        deviceInfo = findViewById(R.id.tv_device_info);
        getDeviceInfo(deviceId);
    }

    private void getDeviceInfo(String deviceId) {
        IThingDevicePlugin service = PluginManager.service(IThingDevicePlugin.class);
        IThingDevice iThingDevice = service.newDeviceInstance(deviceId);
        iThingDevice.requestWifiSignal(new WifiSignalListener() {
            @Override
            public void onSignalValueFind(String signal) {
                System.out.println(signal);
            }

            @Override
            public void onError(String errorCode, String errorMsg) {
                System.out.println(errorCode);
            }
        });
        newDeviceDetailInfoManager = ThingDeviceDetailKit.getInstance().getDeviceInfoManager();
        newDeviceDetailInfoManager.getDeviceDetailInfo(deviceId, deviceDetailInfo -> {
            StringBuilder sb = new StringBuilder();
            if (deviceDetailInfo != null) {
                sb.append("Device ID：").append(deviceDetailInfo.devId).append("\n");
                sb.append("Device ICCID：").append(deviceDetailInfo.iccid).append("\n");
                sb.append("Device NetStrength：").append(deviceDetailInfo.netStrength).append("\n");
                sb.append("Lan Ip：").append(deviceDetailInfo.lanIp).append("\n");
                sb.append("Ip：").append(deviceDetailInfo.ip).append("\n");
                sb.append("Mac：").append(deviceDetailInfo.mac).append("\n");
                sb.append("Timezone Id：").append(deviceDetailInfo.timezone).append("\n");
                sb.append("Device channel：").append(deviceDetailInfo.channel).append("\n");
                sb.append("Device Connect Ability：").append(deviceDetailInfo.connectAbility.name()).append("\n");
                sb.append("rsrp：").append(deviceDetailInfo.rsrp).append("\n");
                sb.append("Wi-Fi signal：").append(deviceDetailInfo.wifiSignal).append("\n");
                sb.append("vendorName：").append(deviceDetailInfo.vendorName).append("\n");
            } else {
                sb.append("not info");
            }
            deviceInfo.setText(sb.toString());
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        newDeviceDetailInfoManager.onDestroy();
    }
}