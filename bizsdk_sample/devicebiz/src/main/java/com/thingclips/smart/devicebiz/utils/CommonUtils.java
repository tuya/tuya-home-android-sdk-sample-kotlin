package com.thingclips.smart.devicebiz.utils;

import android.content.Context;

import com.thingclips.smart.devicebiz.R;
import com.thingclips.smart.home.sdk.ThingHomeSdk;
import com.thingclips.smart.sdk.bean.DeviceBean;
import com.thingclips.smart.sdk.bean.GroupBean;
import com.thingclips.smart.sdk.bean.ProductBean;

public class CommonUtils {

    public static DeviceBean getDeviceBeanInstance(GroupBean data) {
        if (data == null) {
            return null;
        }
        DeviceBean deviceBean = new DeviceBean();
        ProductBean productBean = ThingHomeSdk.getDataInstance().getProductBeanByVer(data.getProductId(),data.getProductVer());
        if (productBean!=null){
            deviceBean.setPanelConfig(productBean.getPanelConfig());
            deviceBean.setProductBean(productBean);
        }
        deviceBean.setDevId(data.getId() + "");
        deviceBean.setDps(data.getDps());
        deviceBean.setIsShare(data.isShare());
        deviceBean.setLocalKey(data.getLocalKey());
        deviceBean.setProductId(data.getProductId());
        deviceBean.setProductVer(data.getProductVer());
        deviceBean.setTime(data.getTime());
        deviceBean.setPv(data.getPv());
        deviceBean.setIsOnline(true);
        deviceBean.setName(data.getName());
        deviceBean.setIconUrl(data.getIconUrl());
        deviceBean.setDpCodes(data.getDpCodes());
        return deviceBean;
    }

    public static String getRepeatString(Context context, String mode) {
        String sufix = ", ";
        StringBuilder stringBuilder = new StringBuilder();
        int[] res = new int[]{R.string.sunday, R.string.monday, R.string.tuesday, R.string.wednesday, R.string.thursday, R.string.friday, R.string.saturday};

        for(int i = 0; i < mode.length(); ++i) {
            if (mode.charAt(i) == '1') {
                stringBuilder.append(context.getString(res[i]));
                stringBuilder.append(", ");
            }
        }

        if (stringBuilder.length() > 0) {
            stringBuilder.delete(stringBuilder.length() - ", ".length(), stringBuilder.length());
        }

        return stringBuilder.toString();
    }

}
