package com.thingclips.smart.devicebiz.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.thingclips.smart.devicebiz.R;
import com.thingclips.smart.devicebiz.bean.DeviceBizBean;

import java.util.List;


public class DeviceBizEntranceListAdapter extends RecyclerView.Adapter<DeviceBizEntranceListAdapter.ViewHolder> {
    private List<DeviceBizBean> mData;

    private OnItemClickListener mOnItemClickListener;

    public DeviceBizEntranceListAdapter() {
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_device_biz_entrance, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder viewHolder, int position) {
        final DeviceBizBean bean = mData.get(position);
        if (bean == null) {
            return;
        }
        viewHolder.name.setText(bean.getDeviceBizName());

        viewHolder.itemView.setOnClickListener(v -> {
            if (mOnItemClickListener != null) {
                mOnItemClickListener.onItemClick(bean, viewHolder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return mData != null && !mData.isEmpty() ? mData.size() : 0;
    }

    public void setOnItemClickListener(OnItemClickListener l) {
        this.mOnItemClickListener = l;
    }

    public void setData(List<DeviceBizBean> beans) {
        mData = beans;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name;

        public ViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.device_biz_name);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(DeviceBizBean bean, int position);
    }
}
