package com.thingclips.smart.devicebiz;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.thingclips.smart.devicebiz.adapter.DeviceListAdapter;
import com.thingclips.smart.devicebiz.bean.ItemBean;
import com.thingclips.smart.devicebiz.biz.timer.TimerListActivity;
import com.thingclips.smart.home.sdk.ThingHomeSdk;
import com.thingclips.smart.home.sdk.bean.HomeBean;
import com.thingclips.smart.home.sdk.callback.IThingHomeResultCallback;
import com.thingclips.smart.sdk.bean.DeviceBean;
import com.thingclips.smart.sdk.bean.GroupBean;

import java.util.ArrayList;
import java.util.List;

public class DeviceListActivity extends AppCompatActivity {

    private DeviceListAdapter mAdapter;
    private long homeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_panel);
        Toolbar toolbar = findViewById(R.id.toolbar_main);
        toolbar.setTitle(getResources().getString(R.string.device_list));
        homeId = getIntent().getLongExtra("homeId", 0);
        RecyclerView homeRecycler = findViewById(R.id.home_recycler);
        homeRecycler.setLayoutManager(new LinearLayoutManager(this));
        homeRecycler.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        mAdapter = new DeviceListAdapter();
        homeRecycler.setAdapter(mAdapter);
        mAdapter.setOnItemClickListener((bean, position) -> {
            Intent intent = new Intent();
            intent.putExtra("deviceId", bean.getDevId());
            intent.putExtra("groupId", bean.getGroupId());
            intent.putExtra("devIds", bean.getDevIds());
            intent.putExtra("homeId", homeId);
            intent.setClass(DeviceListActivity.this, DeviceBizEntranceActivity.class);
            startActivity(intent);
        });
        getCurrentHomeDetail();
    }

    /**
     * you must implementation AbsBizBundleFamilyService
     *
     * @return AbsBizBundleFamilyService
     */

    private void getCurrentHomeDetail() {
        ThingHomeSdk.newHomeInstance(homeId).getHomeDetail(new IThingHomeResultCallback() {
            @Override
            public void onSuccess(HomeBean homeBean) {
                List<ItemBean> beans = new ArrayList<>(8);
                for (GroupBean groupBean : homeBean.getGroupList()) {
                    beans.add(getItemBeanFromGroup(groupBean));
                }
                for (DeviceBean deviceBean : homeBean.getDeviceList()) {
                    beans.add(getItemBeanFromDevice(deviceBean));
                }
                mAdapter.setData(beans);
            }

            @Override
            public void onError(String s, String s1) {
                Toast.makeText(DeviceListActivity.this, s + "\n" + s1, Toast.LENGTH_LONG).show();
            }
        });
    }

    private ItemBean getItemBeanFromGroup(GroupBean groupBean) {
        ItemBean itemBean = new ItemBean();
        itemBean.setGroupId(groupBean.getId());
        itemBean.setTitle(groupBean.getName());
        itemBean.setIconUrl(groupBean.getIconUrl());
        itemBean.setDevIds((ArrayList<String>) groupBean.getDevIds());
        return itemBean;
    }

    private ItemBean getItemBeanFromDevice(DeviceBean deviceBean) {
        ItemBean itemBean = new ItemBean();
        itemBean.setDevId(deviceBean.getDevId());
        itemBean.setIconUrl(deviceBean.getIconUrl());
        itemBean.setTitle(deviceBean.getName());
        return itemBean;
    }


}
