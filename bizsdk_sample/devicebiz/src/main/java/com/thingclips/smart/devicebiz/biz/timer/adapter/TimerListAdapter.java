package com.thingclips.smart.devicebiz.biz.timer.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import androidx.annotation.Nullable;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.thingclips.smart.android.device.bean.AlarmTimerBean;
import com.thingclips.smart.android.device.bean.SchemaBean;
import com.thingclips.smart.devicebiz.utils.CommonUtils;
import com.thingclips.smart.devicebiz.utils.DpDescUtil;
import com.thingclips.smart.devicebiz.R;
import com.thingclips.smart.devicebiz.adapter.base.BaseArrayAdapter;
import com.thingclips.smart.devicebiz.adapter.base.OnItemClickListener;
import com.thingclips.smart.devicebiz.adapter.base.OnItemLongClickListener;
import com.thingclips.smart.devicebiz.bean.AlarmDpBean;
import com.thingclips.smart.sdk.bean.DeviceBean;
import com.thingclips.smart.sdk.bean.GroupBean;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class TimerListAdapter extends BaseArrayAdapter<AlarmTimerBean, TimerListViewHolder> {

    private static final String TAG = "AlarmListAdapter";

    private OnItemClickListener mOnItemClickListener;

    private OnItemLongClickListener mOnItemLongClickListener;

    private Map<String, AlarmDpBean> mAlarmDpBeanMap;

    private String mDevId;

    private long mGroupId;

    private boolean mIsTimeMode12 = false;

    private boolean enableFilter = false;

    public TimerListAdapter(Context context, int resource, List<AlarmTimerBean> data, boolean isTimeMode12, boolean enableFilter) {
        super(context, resource, data);
        mIsTimeMode12 = isTimeMode12;
        this.enableFilter = enableFilter;
    }

    @Override
    public TimerListViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        return new TimerListViewHolder(mInflater.inflate(mResource, null, false), mOnItemClickListener, mOnItemLongClickListener);
    }


    private String getAlarmTime(AlarmTimerBean alarmTimerBean) {
        String time = alarmTimerBean.getTime();

        if (mIsTimeMode12) {
            String[] a = time.split(":");
            int hour = Integer.parseInt(a[0]);
            String minute = time.substring(time.indexOf(":"));

            if (hour >= 12) {
                //下午:
                time = mContext.getString(R.string.timer_pm);
                hour = hour == 12 ? 12 : hour - 12;
            } else {//hour >= 0 && hour < 12
                //上午:
                time = mContext.getString(R.string.timer_am);
                hour = hour == 0 ? 12 : hour;
            }

            time += " " + hour + minute;
        }

        return time;
    }

    @Override
    public void onBindViewHolder(TimerListViewHolder timerListViewHolder, int position) {
        final AlarmTimerBean alarmTimerBean = mData.get(position);
        timerListViewHolder.setOpen(alarmTimerBean.getStatus() == 1);
        timerListViewHolder.getOpenOffTime().setText(getAlarmTime(alarmTimerBean));
        timerListViewHolder.getOpenDay().setText(getOpenDay(alarmTimerBean.getLoops()));
        timerListViewHolder.getSwitchButton().setChecked(alarmTimerBean.getStatus() == AlarmTimerBean.ENABLED);
        if (TextUtils.isEmpty(alarmTimerBean.getAliasName())) {
            timerListViewHolder.setRemark("");
        } else {
            timerListViewHolder.setRemark(alarmTimerBean.getAliasName());
        }

        // dp描述
        String dpDesc = "";
        try {
            dpDesc = DpDescUtil.INSTANCE.getDpDesc(alarmTimerBean.getValue(), mAlarmDpBeanMap);
        } catch (Exception e) {
            e.printStackTrace();
        }
        timerListViewHolder.getDescDp().setText(dpDesc);

        timerListViewHolder.getSwitchButton().setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mOnModifyTimerListener != null) {
                    mOnModifyTimerListener.modifyTimer(alarmTimerBean, (isChecked ? AlarmTimerBean.ENABLED : AlarmTimerBean.UNENABLED));
                }

            }
        });
    }

    public void setOnItemClickListener(OnItemClickListener mOnItemClickListener) {
        this.mOnItemClickListener = mOnItemClickListener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener onItemLongClickListener) {
        this.mOnItemLongClickListener = onItemLongClickListener;
    }

    public void setDpBean(List<AlarmDpBean> dpList) {
        mAlarmDpBeanMap = new HashMap<String, AlarmDpBean>(dpList.size());
        for (AlarmDpBean dp : dpList) {
            mAlarmDpBeanMap.put(dp.getDpId(), dp);
        }
    }

    @Override
    public void setData(List<AlarmTimerBean> data) {
        super.setData(getEffectiveData(data));
    }

    private List<AlarmTimerBean> getEffectiveData(List<AlarmTimerBean> data) {
        List<AlarmTimerBean> alarmTimerBeans = new ArrayList<>();
        for (AlarmTimerBean alarmTimerBean : data) {
            Map<String, Object> valueMap = JSONObject.parseObject(alarmTimerBean.getValue(), new TypeReference<Map<String, Object>>() {
            });
            boolean isEffective = true;
            if (enableFilter) {
                if (valueMap.size() != mAlarmDpBeanMap.size()) {
                    isEffective = false;
                } else {
                    for (Map.Entry<String, Object> entry : valueMap.entrySet()) {
                        if (mAlarmDpBeanMap.get(entry.getKey()) == null) {
                            isEffective = false;
                            break;
                        }
                    }
                }
            }
            if (isEffective) {
                alarmTimerBeans.add(alarmTimerBean);
            }
        }
        return alarmTimerBeans;
    }

    private String getOpenDay(String loops) {
        StringBuilder stringBuffer = new StringBuilder();
        switch (loops) {
            case AlarmTimerBean.MODE_REPEAT_ONCE:
                stringBuffer.append(getString(R.string.clock_timer_once));

                break;
            case AlarmTimerBean.MODE_REPEAT_EVEVRYDAY:
                stringBuffer.append(getString(R.string.clock_timer_everyday));

                break;
            case AlarmTimerBean.MODE_REPEAT_WEEKDAY:
                stringBuffer.append(getString(R.string.clock_timer_weekday));

                break;
            case AlarmTimerBean.MODE_REPEAT_WEEKEND:
                stringBuffer.append(getString(R.string.clock_timer_weekEND));
                break;
            default:
                stringBuffer.append(CommonUtils.getRepeatString(mContext, loops));
                break;
        }
        return stringBuffer.toString();
    }

    public void setOnModifyTimerListener(OnModifyTimerListener mOnModifyTimerListener) {
        this.mOnModifyTimerListener = mOnModifyTimerListener;
    }

    public void setSwitchButtonChecked(AlarmTimerBean alarmTimerBean) {
        notifyDataSetChanged();
    }


    public void setDevId(String mDevId) {
        this.mDevId = mDevId;
    }

    public void setGroupId(long groupId) {
        this.mGroupId = groupId;
    }

    @Deprecated
    @Nullable
    private Map<String, SchemaBean> getDpSchema() {
        Map<String, SchemaBean> schema;
        if (mGroupId > 0) {
            GroupBean groupBean = mOnModifyTimerListener.getGroupBean(mGroupId);
            if (groupBean != null && groupBean.getGroupType() != GroupBean.TYPE_BLE_MESH && groupBean.getGroupType() != 3) {
                DeviceBean deviceBean = CommonUtils.getDeviceBeanInstance(groupBean);
                schema = deviceBean.getSchemaMap();
            } else {
                schema = mOnModifyTimerListener.getSchema(mDevId);
            }
        } else {
            schema = mOnModifyTimerListener.getSchema(mDevId);
        }
        return schema;
    }

    private OnModifyTimerListener mOnModifyTimerListener;

    public interface OnModifyTimerListener {
        void modifyTimer(AlarmTimerBean alarmTimerBean, int status);

        GroupBean getGroupBean(long id);

        Map<String, SchemaBean> getSchema(String id);
    }
}
