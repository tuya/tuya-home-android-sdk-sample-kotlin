package com.thingclips.smart.devicebiz.biz.group;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.thingclips.group_usecase_api.relation.ThingGroupCoreKit;
import com.thingclips.smart.devicebiz.R;
import com.thingclips.smart.group.manager.GroupInitBuilder;
import com.thingclips.smart.group.manager.GroupOperateBuilder;
import com.thingclips.smart.group.manager.ThingGroupBizKit;
import com.thingclips.smart.group.manager.ThingGroupBizManager;
import com.thingclips.smart.sdk.bean.GroupBean;

import java.util.ArrayList;

public class GroupInfoActivity extends AppCompatActivity {

    private TextView groupInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_info);
        Toolbar toolbar = findViewById(R.id.toolbar_main);
        toolbar.setTitle("群组信息");
        long groupId = getIntent().getLongExtra("groupId", 0);
        long homeId = getIntent().getLongExtra("homeId",0);
        ArrayList<String> devIds = getIntent().getStringArrayListExtra("devIds");
        groupInfo = findViewById(R.id.tv_group_info);
        getGroupInfo(groupId,devIds);
        initDeleteGroup(groupId,homeId);
    }

    private void initDeleteGroup(long groupId,long homeId) {
        GroupInitBuilder initBuilder = new GroupInitBuilder.Builder()
                .setGroupId(groupId)
                .build();
        ThingGroupBizManager groupBizManager = ThingGroupBizKit.getGroupBizManager(homeId, initBuilder);
        GroupOperateBuilder operateBuilder = new GroupOperateBuilder.Builder()
                .setGroupId(groupId)
                .setSuccessCallback((groupId12, failDevices) -> {
                    Toast.makeText(GroupInfoActivity.this, "群组删除成功", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setFailureCallback((errorCode, errorMessage, groupId1, failDevices) -> {
                    Toast.makeText(GroupInfoActivity.this, "删除失败", Toast.LENGTH_SHORT).show();
                })
                .build();
        findViewById(R.id.deleteGroup).setOnClickListener(v -> groupBizManager.dismissGroup(operateBuilder));
    }

    private void getGroupInfo(Long groupId,ArrayList<String> devIds) {
        if (groupId <= 0) {
            Toast.makeText(this, "群组不存在", Toast.LENGTH_SHORT).show();
        }
        GroupBean groupBean = ThingGroupCoreKit.INSTANCE.getGroupBean(groupId);
        if (groupBean != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("群组ID：").append(groupBean.getId()).append("\n");
            sb.append("群组名称：").append(groupBean.getName()).append("\n");
            if (devIds != null) {
                sb.append("群组下的设备数量：").append(devIds.size()).append("\n");
                sb.append("群组下的各设备的ID：").append("\n");
                for (String devId : devIds) {
                    sb.append(devId).append("\n");
                }
            }
            groupInfo.setText(sb.toString());
        }
    }
}