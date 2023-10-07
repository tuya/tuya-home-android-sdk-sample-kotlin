package com.thingclips.smart.devicebiz.adapter;

import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.thingclips.smart.devicebiz.DeviceListActivity;
import com.thingclips.smart.devicebiz.R;

import java.util.List;


public class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.ViewHolder> {
    private List<DeviceListActivity.ItemBean> mData;

    private OnItemClickListener mOnItemClickListener;

    public DeviceListAdapter() {
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_device_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder viewHolder, int position) {
        final DeviceListActivity.ItemBean bean = mData.get(position);
        if (bean == null) {
            return;
        }
        if (!TextUtils.isEmpty(bean.getIconUrl())) {
            Uri uri = Uri.parse(bean.getIconUrl());
            viewHolder.icon.setImageURI(uri);

        }
        viewHolder.title.setText(bean.getTitle());

        viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onItemClick(bean, viewHolder.getAdapterPosition());
                }
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

    public void setData(List<DeviceListActivity.ItemBean> beans) {
        mData = beans;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        SimpleDraweeView icon;
        TextView title;

        public ViewHolder(View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.home_item_icon);
            title = itemView.findViewById(R.id.home_item_title);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(DeviceListActivity.ItemBean bean, int position);
    }
}
