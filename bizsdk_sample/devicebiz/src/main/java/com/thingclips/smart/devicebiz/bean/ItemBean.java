package com.thingclips.smart.devicebiz.bean;

import java.util.ArrayList;
import java.util.List;

public class ItemBean {

    private String devId;
    private long groupId;
    private String iconUrl;
    private String title;

    private ArrayList<String> devIds;

    public String getDevId() {
        return devId;
    }

    public void setDevId(String devId) {
        this.devId = devId;
    }

    public long getGroupId() {
        return groupId;
    }

    public void setGroupId(long groupId) {
        this.groupId = groupId;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public ArrayList<String> getDevIds() {
        return devIds;
    }

    public void setDevIds(ArrayList<String> devIds) {
        this.devIds = devIds;
    }
}
