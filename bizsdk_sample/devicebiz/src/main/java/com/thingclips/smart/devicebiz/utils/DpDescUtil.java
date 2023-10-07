package com.thingclips.smart.devicebiz.utils;


import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.thingclips.smart.android.device.bean.BoolSchemaBean;
import com.thingclips.smart.android.device.bean.EnumSchemaBean;
import com.thingclips.smart.android.device.bean.SchemaBean;
import com.thingclips.smart.android.device.bean.StringSchemaBean;
import com.thingclips.smart.android.device.bean.ValueSchemaBean;
import com.thingclips.smart.devicebiz.bean.AlarmDpBean;

import java.util.List;
import java.util.Map;

public enum DpDescUtil {
    INSTANCE;

    public static final String FILL = "  ";


    public String getDpDesc(String dps, Map<String, AlarmDpBean> alarmDpBeanMap) {
        StringBuilder dpBuffer = new StringBuilder();

        Map<String, Object> valueMap = JSONObject.parseObject(dps, new TypeReference<Map<String, Object>>() {
        });

        for (Map.Entry<String, Object> entry : valueMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            AlarmDpBean alarmDpBean = alarmDpBeanMap.get(key);
            List<Object> rangeKeys = alarmDpBean.getRangeKeys();
            List<String> rangeValues = alarmDpBean.getRangeValues();

            try {
                int index = rangeKeys.indexOf(value);
                composeDpBuffer(dpBuffer, alarmDpBean, rangeValues, index);
            }
            catch (Exception e) {
                composeDpBuffer(dpBuffer, alarmDpBean, rangeValues, 0);
            }

    }
        return dpBuffer.toString();
    }

    @Deprecated
    public String getDpDesc(String dps, Map<String, AlarmDpBean> mAlarmDpBeanMap, Map<String, SchemaBean> schema) {
        StringBuilder dpBuffer = new StringBuilder();

        Map<String, Object> valueMap = JSONObject.parseObject(dps, new TypeReference<Map<String, Object>>() {
        });

        for (Map.Entry<String, Object> entry : valueMap.entrySet()) {
            String key = entry.getKey();
            SchemaBean schemaBean = schema.get(key);
            Object value = entry.getValue();

            AlarmDpBean alarmDpBean = mAlarmDpBeanMap.get(key);
            List<Object> rangeKeys = alarmDpBean.getRangeKeys();
            List<String> rangeValues = alarmDpBean.getRangeValues();

            if (BoolSchemaBean.type.equals(schemaBean.getSchemaType())) {
                //bool
                boolean realValue = (boolean) value;
                for (int i = 0; i < rangeKeys.size(); i++) {
                    Boolean rangeKey = (Boolean) rangeKeys.get(i);
                    if (rangeKey != null && rangeKey == realValue) {
                        composeDpBuffer(dpBuffer, alarmDpBean, rangeValues, i);
                    }
                }
            } else if (EnumSchemaBean.type.equals(schemaBean.getSchemaType()) || StringSchemaBean.type.equals(schemaBean.getSchemaType())) {
                //enums | string
                String realValue = (String) value;
                for (int i = 0; i < rangeKeys.size(); i++) {
                    String rangeKey = (String) rangeKeys.get(i);
                    if (realValue.equals(rangeKey)) {
                        composeDpBuffer(dpBuffer, alarmDpBean, rangeValues, i);
                    }
                }
            } else if (ValueSchemaBean.type.equals(schemaBean.getSchemaType())) {
                int v;
                if (value instanceof String) {
                    v = Integer.parseInt((String) value);
                } else {
                    v = (int) value;
                }
                int realValue = v;
                for (int i = 0; i < rangeKeys.size(); i++) {
                    Integer rangeKey = (Integer) rangeKeys.get(i);
                    if (rangeKey != null && rangeKey == realValue) {
                        composeDpBuffer(dpBuffer, alarmDpBean, rangeValues, i);
                    }
                }
            }

        }
        return dpBuffer.toString();
    }

    private void composeDpBuffer(StringBuilder dpBuffer, AlarmDpBean alarmDpBean, List<String> rangeValues, int position){
        dpBuffer.append(alarmDpBean.getDpName());
        dpBuffer.append(":");
        dpBuffer.append(rangeValues.get(position));
        dpBuffer.append(FILL);
    }


}
