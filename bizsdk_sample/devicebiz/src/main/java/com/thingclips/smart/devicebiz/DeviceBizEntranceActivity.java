package com.thingclips.smart.devicebiz;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.thingclips.smart.devicebiz.adapter.DeviceBizEntranceListAdapter;
import com.thingclips.smart.devicebiz.adapter.DeviceListAdapter;
import com.thingclips.smart.devicebiz.bean.DeviceBizBean;
import com.thingclips.smart.devicebiz.biz.deviceInfo.DeviceInfoActivity;
import com.thingclips.smart.devicebiz.biz.timer.TimerListActivity;
import com.thingclips.smart.devicebiz.utils.Constant;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class DeviceBizEntranceActivity extends AppCompatActivity {
    private String deviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_biz_entrance);
        Toolbar toolbar = findViewById(R.id.toolbar_main);
        toolbar.setTitle(getResources().getString(R.string.device_biz_list));
        deviceId = getIntent().getStringExtra("deviceId");
        initAdapter();
    }

    private void initAdapter() {
        RecyclerView homeRecycler = findViewById(R.id.rc_device_biz);
        homeRecycler.setLayoutManager(new LinearLayoutManager(this));
        homeRecycler.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        DeviceBizEntranceListAdapter mAdapter = new DeviceBizEntranceListAdapter();
        homeRecycler.setAdapter(mAdapter);
        mAdapter.setOnItemClickListener((bean, position) -> {
            switch (bean.getDeviceBizId()){
                case Constant.DEVICE_INFO:
                    gotoDeviceInfoPage(deviceId);
                    break;
                case Constant.DEVICE_TIMER:
                    gotoDeviceTimerPage(deviceId);
                    break;
            }
        });
        mAdapter.setData(getDeviceBizData());
    }

    private void gotoDeviceInfoPage(String devId){
        Intent intent = new Intent();
        intent.putExtra("deviceId",devId);
        intent.setClass(DeviceBizEntranceActivity.this, DeviceInfoActivity.class);
        startActivity(intent);
    }

    private void gotoDeviceTimerPage(String devId){
        Intent intent = new Intent();
        intent.putExtra("deviceId",devId);
        intent.setClass(DeviceBizEntranceActivity.this, TimerListActivity.class);
        startActivity(intent);
    }

    private List<DeviceBizBean> getDeviceBizData() {
        List<DeviceBizBean> deviceBizBeans;
        try {
            InputStream is = getAssets().open("deviceBiz.json");
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