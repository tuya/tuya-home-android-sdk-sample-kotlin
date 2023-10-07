package com.thingclips.smart.devicebiz.biz.timer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.thingclips.smart.android.device.bean.AlarmTimerBean;
import com.thingclips.smart.android.device.bean.SchemaBean;
import com.thingclips.smart.devicebiz.R;
import com.thingclips.smart.devicebiz.biz.timer.adapter.TimerListAdapter;
import com.thingclips.smart.sdk.bean.GroupBean;
import com.thingclips.smart.thingdevicedetailkit.ThingDeviceDetailKit;
import com.thingclips.smart.timer.manager.ThingTimerBizKit;
import com.thingclips.smart.timer.manager.api.IThingTimerManager;
import com.thingclips.smart.timer.sdk.callback.IThingTimerCallBack;
import com.thingclips.smart.timer.sdk.model.TimerManagerBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TimerListActivity extends AppCompatActivity {
    String deviceId;
    IThingTimerManager newThingTimerManager;
    TimerListAdapter timerListAdapter;
    List<AlarmTimerBean> timerList;
    TimerManagerBuilder builder;
    AlertDialog alertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer_list);
        Toolbar toolbar = findViewById(R.id.toolbar_main);
        toolbar.inflateMenu(R.menu.panel_menu_list);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.addTimer) {
                Intent intent = new Intent();
                intent.putExtra("deviceId", deviceId);
                intent.putExtra("isEdit", false);
                intent.setClass(TimerListActivity.this, TimerSettingActivity.class);
                startActivityForResult(intent, 1001);
                return true;
            }
            return false;
        });
        toolbar.setTitle(getResources().getString(R.string.timer_list));

        deviceId = getIntent().getStringExtra("deviceId");
        newThingTimerManager = ThingDeviceDetailKit.getInstance().getDeviceTimerManager();
        builder = new TimerManagerBuilder.Builder()
                .setDeviceId(deviceId)
                .setCallback(new IThingTimerCallBack() {
                    @Override
                    public void successful(@Nullable List<? extends AlarmTimerBean> list) {
                        if (list != null) {
                            timerList = (List<AlarmTimerBean>) list;
                            timerListAdapter.setData(timerList);
                            timerListAdapter.notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void fail(int i, @NonNull String s) {
                        System.out.println(s);
                    }
                })
                .build();
        initAdapter(new ArrayList<>());
        initData();
    }

    private void initData() {
        newThingTimerManager.getTimerList(builder);
    }

    private void initAdapter(List<AlarmTimerBean> list) {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        RecyclerView rcTimer = findViewById(R.id.rv_lv_clock_time);
        rcTimer.setLayoutManager(linearLayoutManager);
        timerListAdapter = new TimerListAdapter(this, R.layout.item_device_timer_detail, list, false, false);
        timerListAdapter.setDevId(deviceId);
        //点击编辑
        timerListAdapter.setOnItemClickListener((view, position) -> {
            Intent intent = new Intent();
            intent.putExtra("deviceId", deviceId);
            intent.putExtra("isEdit", true);
            intent.putExtra("timerDetail", timerList.get(position));
            intent.setClass(TimerListActivity.this, TimerSettingActivity.class);
            startActivityForResult(intent, 1001);

        });
        // 长按删除
        timerListAdapter.setOnItemLongClickListener((view, position) -> {
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(TimerListActivity.this);
            alertBuilder.setMessage("Are you sure to delete it?");
            alertBuilder.setPositiveButton("Yes", (dialog, which) -> {
                TimerManagerBuilder builder = new TimerManagerBuilder.Builder()
                        .setDeviceId(deviceId)
                        .setTimerId(timerList.get(position).getGroupId())
                        .setCallback(new IThingTimerCallBack() {
                            @Override
                            public void successful(@Nullable List<? extends AlarmTimerBean> list) {
                                Toast.makeText(TimerListActivity.this, "delete success", Toast.LENGTH_SHORT).show();
                                initData();
                                alertDialog.dismiss();
                            }

                            @Override
                            public void fail(int i, @NonNull String s) {
                                Toast.makeText(TimerListActivity.this, "delete fail：" + s, Toast.LENGTH_SHORT).show();
                                alertDialog.dismiss();
                            }
                        })
                        .build();
                newThingTimerManager.deleteTimer(builder);
            });
            alertBuilder.setNegativeButton("No", (dialog, which) -> {
                if (alertDialog != null) {
                    alertDialog.dismiss();
                }
            });
            alertDialog = alertBuilder.create();
            alertDialog.show();
        });

        timerListAdapter.setOnModifyTimerListener(new TimerListAdapter.OnModifyTimerListener() {
            @Override
            public void modifyTimer(AlarmTimerBean alarmTimerBean, int status) {
                TimerManagerBuilder builder = new TimerManagerBuilder.Builder()
                        .setDeviceId(deviceId)
                        .isOpen(status == 1)
                        .setTimerId(alarmTimerBean.getGroupId())
                        .setCallback(new IThingTimerCallBack() {
                            @Override
                            public void successful(@Nullable List<? extends AlarmTimerBean> list) {
                                Toast.makeText(TimerListActivity.this, "更新成功", Toast.LENGTH_SHORT).show();
                                initData();
                            }

                            @Override
                            public void fail(int i, @NonNull String s) {
                                Toast.makeText(TimerListActivity.this, "更新失败：" + s, Toast.LENGTH_SHORT).show();
                            }
                        })
                        .build();
                newThingTimerManager.updateTimerStatus(builder);
            }

            @Override
            public GroupBean getGroupBean(long id) {
                return null;
            }

            @Override
            public Map<String, SchemaBean> getSchema(String id) {
                return null;
            }
        });
        rcTimer.setAdapter(timerListAdapter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001) {
            initData();
        }
    }
}