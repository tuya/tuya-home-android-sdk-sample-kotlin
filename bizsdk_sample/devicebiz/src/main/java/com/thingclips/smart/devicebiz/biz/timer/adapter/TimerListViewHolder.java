package com.thingclips.smart.devicebiz.biz.timer.adapter;

import android.text.TextUtils;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.thingclips.smart.devicebiz.R;
import com.thingclips.smart.devicebiz.adapter.base.OnItemClickListener;
import com.thingclips.smart.devicebiz.adapter.base.OnItemLongClickListener;

public class TimerListViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

    private TextView openOffTime;

    private TextView openDay;

    private TextView descDp;

    private Switch switchButton;

    private final OnItemClickListener mListener;

    private final OnItemLongClickListener mLongListener;
    private final TextView tv_remark;

    private final View mVCloseMask;

    public TimerListViewHolder(View contentView, OnItemClickListener listener, OnItemLongClickListener longListener) {
        super(contentView);
        mListener = listener;
        mLongListener = longListener;
        tv_remark = contentView.findViewById(R.id.tv_remark);
        openOffTime = contentView.findViewById(R.id.tv_show_open_off_time);
        openDay = contentView.findViewById(R.id.tv_day_time);
        descDp = contentView.findViewById(R.id.tv_desc_dp);
        switchButton = contentView.findViewById(R.id.sb_open_clock_time);
        mVCloseMask = contentView.findViewById(R.id.v_offline);
        mVCloseMask.setVisibility(View.INVISIBLE);
        contentView.setOnClickListener(this);
        contentView.setOnLongClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (mListener != null) {
            mListener.onItemClick(v, getAdapterPosition());
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (mLongListener != null) {
            mLongListener.onItemLongClick(v, getAdapterPosition());
        }
        return false;
    }

    public TextView getOpenOffTime() {
        return openOffTime;
    }

    public void setOpenOffTime(TextView openOffTime) {
        this.openOffTime = openOffTime;
    }

    public TextView getOpenDay() {
        return openDay;
    }

    public void setOpenDay(TextView openDay) {
        this.openDay = openDay;
    }

    public TextView getDescDp() {
        return descDp;
    }

    public void setDescDp(TextView descDp) {
        this.descDp = descDp;
    }

    public Switch getSwitchButton() {
        return switchButton;
    }

    public void setSwitchButton(Switch switchButton) {
        this.switchButton = switchButton;
    }

    public void setOpen(boolean isOpen) {
        mVCloseMask.setVisibility(isOpen ? View.INVISIBLE : View.VISIBLE);
    }

    public void setRemark(String remark) {
        if (TextUtils.isEmpty(remark)) {
            tv_remark.setVisibility(View.GONE);
        }else {
            tv_remark.setVisibility(View.VISIBLE);
            tv_remark.setText(remark);
        }
    }
}
