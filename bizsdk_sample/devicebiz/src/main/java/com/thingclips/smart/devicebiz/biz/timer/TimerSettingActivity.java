package com.thingclips.smart.devicebiz.biz.timer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.thingclips.smart.android.device.bean.AlarmTimerBean;
import com.thingclips.smart.devicebiz.R;
import com.thingclips.smart.thingdevicedetailkit.ThingDeviceDetailKit;
import com.thingclips.smart.timer.manager.ThingTimerBizKit;
import com.thingclips.smart.timer.manager.api.IThingTimerManager;
import com.thingclips.smart.timer.sdk.callback.IThingTimerCallBack;
import com.thingclips.smart.timer.sdk.model.TimerManagerBuilder;

import java.util.HashMap;
import java.util.List;

public class TimerSettingActivity extends AppCompatActivity implements View.OnClickListener {
    IThingTimerManager newThingTimerManager;
    EditText time;
    EditText loop;
    EditText alias;
    EditText dps;
    String deviceId;
    boolean isEdit;
    AlarmTimerBean timerDetail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer_detail);
        findViewById(R.id.saveTime).setOnClickListener(this);
        time = findViewById(R.id.time);
        loop = findViewById(R.id.loop);
        alias = findViewById(R.id.alias);
        dps = findViewById(R.id.dps);
        newThingTimerManager = ThingDeviceDetailKit.getInstance().getDeviceTimerManager();
        deviceId = getIntent().getStringExtra("deviceId");
        isEdit = getIntent().getBooleanExtra("isEdit", false);
        timerDetail = getIntent().getParcelableExtra("timerDetail");
        if (timerDetail != null) {
            time.setText(timerDetail.getTime());
            loop.setText(timerDetail.getLoops());
            alias.setText(timerDetail.getAliasName());
            dps.setText(timerDetail.getValue());
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.saveTime) {
            HashMap<String, Object> map = new HashMap<>();
            if (isEdit) {
                // dp points of different devices are different. Select an appropriate dp point for scheduled operations
                map.put("1", false);
                TimerManagerBuilder builder = new TimerManagerBuilder.Builder()
                        .setDeviceId(deviceId)
                        .setTimerId(timerDetail.getGroupId())
                        .setTime(time.getText().toString().trim())
                        .setDps(map)
                        .setLoops(loop.getText().toString().trim())
                        .setAliasName(alias.getText().toString().trim())
                        .setCallback(new IThingTimerCallBack() {
                            @Override
                            public void successful(@Nullable List<? extends AlarmTimerBean> list) {
                                Toast.makeText(TimerSettingActivity.this, "修改成功", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent();
                                setResult(Activity.RESULT_OK, intent);
                                finish();
                            }

                            @Override
                            public void fail(int i, @NonNull String s) {
                                Toast.makeText(TimerSettingActivity.this, "修改失败：" + s, Toast.LENGTH_SHORT).show();
                            }
                        })
                        .build();
                newThingTimerManager.editTimer(builder);
            } else {
                // dp points of different devices are different. Select an appropriate dp point for scheduled operations
                map.put("1", true);
                TimerManagerBuilder builder = new TimerManagerBuilder.Builder()
                        .setDeviceId(deviceId)
                        .setCategory("schedule")
                        .setTime(time.getText().toString().trim())
                        .setDps(map)
                        .setLoops(loop.getText().toString().trim())
                        .setAliasName(alias.getText().toString().trim())
                        .isOpen(true)
                        .isAppPush(true)
                        .setCallback(new IThingTimerCallBack() {
                            @Override
                            public void successful(@Nullable List<? extends AlarmTimerBean> list) {
                                Toast.makeText(TimerSettingActivity.this, "Add success", Toast.LENGTH_SHORT).show();
                                finish();
                            }

                            @Override
                            public void fail(int i, @NonNull String s) {
                                Toast.makeText(TimerSettingActivity.this, "Add fail：" + s, Toast.LENGTH_SHORT).show();
                            }
                        })
                        .build();
                newThingTimerManager.addTimer(builder);
            }

        }
    }
}