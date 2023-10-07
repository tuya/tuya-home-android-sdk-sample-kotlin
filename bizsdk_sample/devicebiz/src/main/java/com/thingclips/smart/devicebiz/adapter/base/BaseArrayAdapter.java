package com.thingclips.smart.devicebiz.adapter.base;

import android.content.Context;
import android.view.LayoutInflater;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public abstract class BaseArrayAdapter<T, E extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<E> {
    protected Context mContext;
    protected int mResource;
    protected LayoutInflater mInflater;
    protected List<T> mData;

    public BaseArrayAdapter(Context context, int resource, List<T> data) {
        this.mContext = context;
        this.mResource = resource;
        this.mData = data;
        this.mInflater = (LayoutInflater)this.mContext.getSystemService("layout_inflater");
    }

    public int getItemCount() {
        return this.mData == null ? 0 : this.mData.size();
    }

    public List<T> getData() {
        return this.mData;
    }

    public String getString(int id) {
        return this.mContext.getString(id);
    }

    public void setData(List<T> data) {
        this.mData.clear();
        this.mData.addAll(data);
    }

    public void insertData(T data, int position) {
        this.mData.add(position, data);
        this.notifyItemInserted(position);
    }

    public void deleteData(T data) {
        int position = this.mData.indexOf(data);
        if (position != -1) {
            this.mData.remove(data);
            this.notifyItemRemoved(position);
        }

    }
}
