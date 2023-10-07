package com.thingclips.smart.devicebiz.bean;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class AlarmDpBean implements Parcelable {

    private String dpId;

    private String dpName;

    private int selected;

    private int realSelected;

    private List<Object> rangeKeys = new ArrayList<>();

    private List<String> rangeValues = new ArrayList<>();

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(dpId);
        dest.writeString(dpName);
        dest.writeInt(selected);
        dest.writeInt(realSelected);
        dest.writeList(rangeKeys);
        dest.writeStringList(rangeValues);
    }

    public AlarmDpBean() {
    }

    public AlarmDpBean(Parcel dest) {
        dpId = dest.readString();
        dpName = dest.readString();
        selected = dest.readInt();
        realSelected = dest.readInt();
        dest.readList(rangeKeys, AlarmDpBean.class.getClassLoader());
        dest.readStringList(rangeValues);
    }

    public static final Creator<AlarmDpBean> CREATOR = new Creator<AlarmDpBean>() {
        @Override
        public AlarmDpBean createFromParcel(Parcel source) {
            return new AlarmDpBean(source);
        }

        @Override
        public AlarmDpBean[] newArray(int size) {
            return new AlarmDpBean[size];
        }
    };

    public String getDpId() {
        return dpId;
    }

    public void setDpId(String dpId) {
        this.dpId = dpId;
    }

    public String getDpName() {
        return dpName;
    }

    public void setDpName(String dpName) {
        this.dpName = dpName;
    }

    public int getSelected() {
        return selected;
    }

    public void setSelected(int selected) {
        this.selected = selected;
    }

    public int getRealSelected() {
        return realSelected;
    }

    public void setRealSelected(int realSelected) {
        this.realSelected = realSelected;
    }

    public List<Object> getRangeKeys() {
        return rangeKeys;
    }

    public void setRangeKeys(List<Object> rangeKeys) {
        this.rangeKeys = rangeKeys;
    }

    public List<String> getRangeValues() {
        return rangeValues;
    }

    public void setRangeValues(List<String> rangeValues) {
        this.rangeValues = rangeValues;
    }

}
