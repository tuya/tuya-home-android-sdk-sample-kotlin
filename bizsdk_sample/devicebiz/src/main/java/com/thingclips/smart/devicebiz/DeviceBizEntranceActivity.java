package com.thingclips.smart.devicebiz;

import static com.thingclips.smart.devicebiz.utils.Constant.DEVICE_RESTART;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.thingclips.smart.devicebiz.adapter.DeviceBizEntranceListAdapter;
import com.thingclips.smart.devicebiz.adapter.DeviceListAdapter;
import com.thingclips.smart.devicebiz.bean.DeviceBizBean;
import com.thingclips.smart.devicebiz.biz.deviceInfo.DeviceInfoActivity;
import com.thingclips.smart.devicebiz.biz.group.GroupInfoActivity;
import com.thingclips.smart.devicebiz.biz.group.GroupListActivity;
import com.thingclips.smart.devicebiz.biz.migrate.DeviceMigrateActivity;
import com.thingclips.smart.devicebiz.biz.net.DeviceNetSetActivity;
import com.thingclips.smart.devicebiz.biz.offline.DeviceOfflineRemindSettingActivity;
import com.thingclips.smart.devicebiz.biz.preventTouch.PreventAccidentalTouchActivity;
import com.thingclips.smart.devicebiz.biz.restart.DeviceRestartActivity;
import com.thingclips.smart.devicebiz.biz.timer.TimerListActivity;
import com.thingclips.smart.devicebiz.utils.Constant;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class DeviceBizEntranceActivity extends AppCompatActivity {
    private String deviceId;
    private Long groupId;
    private Long homeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_biz_entrance);
        Toolbar toolbar = findViewById(R.id.toolbar_main);
        toolbar.setTitle(getResources().getString(R.string.device_biz_list));
        deviceId = getIntent().getStringExtra("deviceId");
        groupId = getIntent().getLongExtra("groupId", 0);
        homeId = getIntent().getLongExtra("homeId", 0);
        toolbar.setTitle((TextUtils.isEmpty(deviceId) && groupId > 0) ? "群组业务列表" : "设备业务列表");
        initAdapter();
    }

    private void initAdapter() {
        RecyclerView homeRecycler = findViewById(R.id.rc_device_biz);
        homeRecycler.setLayoutManager(new LinearLayoutManager(this));
        homeRecycler.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        DeviceBizEntranceListAdapter mAdapter = new DeviceBizEntranceListAdapter();
        homeRecycler.setAdapter(mAdapter);
        mAdapter.setOnItemClickListener((bean, position) -> {
            switch (bean.getDeviceBizId()) {
                case Constant.DEVICE_INFO:
                    gotoDeviceInfoPage();
                    break;
                case Constant.DEVICE_TIMER:
                    gotoDeviceTimerPage();
                    break;
                case Constant.DEVICE_OFFLINE_REMIND:
                    gotoDeviceOfflinePage();
                    break;
                case Constant.DEVICE_NET_SETTING:
                    gotoDeviceNetSettingPage();
                    break;
                case Constant.DEVICE_MIGRATE:
                    gotoDeviceMigratePage();
                    break;
                case Constant.CREATE_GROUP:
                case Constant.GROUP_EDIT:
                    gotoGroupPage();
                    break;
                case Constant.GROUP_INFO:
                    gotoGroupInfoPage();
                    break;
                case Constant.DEVICE_PREVENT_ACCIDENTAL_TOUCH:
                    gotoPreventPage();
                    break;
                case DEVICE_RESTART:
                    gotoRestartPage();
                    break;
            }
        });
        mAdapter.setData(getDeviceBizData());
    }

    private void gotoDeviceInfoPage() {
        Intent intent = new Intent();
        intent.putExtra("deviceId", deviceId);
        intent.putExtra("homeId", homeId);
        intent.setClass(DeviceBizEntranceActivity.this, DeviceInfoActivity.class);
        startActivity(intent);
    }

    private void gotoDeviceTimerPage() {
        Intent intent = new Intent();
        intent.putExtra("deviceId", deviceId);
        intent.putExtra("homeId", homeId);
        intent.setClass(DeviceBizEntranceActivity.this, TimerListActivity.class);
        startActivity(intent);
    }

    private void gotoDeviceOfflinePage() {
        Intent intent = new Intent();
        intent.putExtra("deviceId", deviceId);
        intent.setClass(DeviceBizEntranceActivity.this, DeviceOfflineRemindSettingActivity.class);
        startActivity(intent);
    }

    private void gotoDeviceNetSettingPage() {
        Intent intent = new Intent();
        intent.putExtra("deviceId", deviceId);
        intent.setClass(DeviceBizEntranceActivity.this, DeviceNetSetActivity.class);
        startActivity(intent);
    }

    private void gotoDeviceMigratePage() {
        Intent intent = new Intent();
        intent.putExtra("deviceId", deviceId);
        intent.putExtra("homeId", homeId);
        intent.setClass(DeviceBizEntranceActivity.this, DeviceMigrateActivity.class);
        startActivity(intent);
    }

    private void gotoGroupPage() {
        Intent intent = new Intent();
        intent.putExtra("deviceId", deviceId);
        intent.putExtra("groupId", groupId);
        intent.putExtra("homeId", homeId);
        intent.setClass(DeviceBizEntranceActivity.this, GroupListActivity.class);
        startActivity(intent);
    }

    private void gotoGroupInfoPage() {
        Intent intent = new Intent();
        intent.putExtra("deviceId", deviceId);
        intent.putExtra("groupId", groupId);
        intent.putExtra("homeId", homeId);
        intent.putExtra("devIds", getIntent().getStringArrayListExtra("devIds"));
        intent.setClass(DeviceBizEntranceActivity.this, GroupInfoActivity.class);
        startActivity(intent);
    }

    private void gotoPreventPage() {
        Intent intent = new Intent();
        intent.putExtra("deviceId", deviceId);
        intent.setClass(DeviceBizEntranceActivity.this, PreventAccidentalTouchActivity.class);
        startActivity(intent);
    }

    private void gotoRestartPage() {
        Intent intent = new Intent();
        intent.putExtra("deviceId", deviceId);
        intent.setClass(DeviceBizEntranceActivity.this, DeviceRestartActivity.class);
        startActivity(intent);
    }

    private List<DeviceBizBean> getDeviceBizData() {
        List<DeviceBizBean> deviceBizBeans;
        try {
            InputStream is = getAssets().open((TextUtils.isEmpty(deviceId) && groupId > 0) ? "groupBiz.json" : "deviceBiz.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, StandardCharsets.UTF_8);
            // 解析JSON数据并创建Java对象
            JSONObject jsonObject = JSON.parseObject(json);
            JSONArray deviceBiz = jsonObject.getJSONArray("deviceBiz");
            deviceBizBeans = JSON.parseArray(deviceBiz.toJSONString(), DeviceBizBean.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return deviceBizBeans == null ? new ArrayList<>() : deviceBizBeans;
    }
}