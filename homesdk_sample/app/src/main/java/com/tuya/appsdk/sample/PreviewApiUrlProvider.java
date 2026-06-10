package com.tuya.appsdk.sample;

import static com.thingclips.smart.android.network.ThingSmartNetWork.DOMAIN_AY;

import android.content.Context;

import com.alibaba.fastjson.JSONObject;
import com.thingclips.smart.android.base.provider.ApiUrlProvider;
import com.thingclips.smart.android.network.ThingSmartNetWork;
import com.thingclips.smart.android.user.bean.Domain;

/**
 * 默认预发环境的 ApiUrlProvider 实现，使用中立域名，不依赖 IYuPlugin、SpHelper、ApiConfig
 */
public class PreviewApiUrlProvider extends ApiUrlProvider {

    private static final String PREVIEW_DOMAIN_NEUTRAL = "{\n" +
            "    \"AY\": {\n" +
            "        \"mobileApiUrl\": \"https://a1-cn.wgine.com/api.json\",\n" +
            "        \"dnsUrl\": \"https://h1-cn.iot-dns.com\",\n" +
            "        \"dns2Url\": \"https://h2-cn.iot-dns.com\",\n" +
            "        \"thingAppUrl\": \"app-support.thingcn.com\",\n" +
            "        \"dnsIps\": [\n" +
            "          \"162.14.14.134\"\n" +
            "        ]\n" +
            "    },\n" +
            "    \"AZ\": {\n" +
            "        \"mobileApiUrl\": \"https://a1-us-pre.iot-888.com/api.json\",\n" +
            "        \"thingAppUrl\": \"app-support.thingeu.com\",\n" +
            "        \"dnsUrl\": \"https://h1-us.iot-dns.com\",\n" +
            "        \"dns2Url\": \"https://h2-us.iot-dns.com\",\n" +
            "        \"dnsIps\": [\n" +
            "          \"35.167.213.203\",\n" +
            "          \"52.27.85.79\"\n" +
            "        ]\n" +
            "    },\n" +
            "    \"EU\": {\n" +
            "        \"mobileApiUrl\": \"https://a1-eu-pre.iot-888.com/api.json\",\n" +
            "        \"dnsUrl\": \"https://h1-eu.iot-dns.com\",\n" +
            "        \"dns2Url\": \"https://h2-eu.iot-dns.com\",\n" +
            "        \"thingAppUrl\": \"app-h5-pre-eu.iot-888.com/developerapp/support\",\n" +
            "        \"dnsIps\": [\n" +
            "          \"52.29.0.171\",\n" +
            "          \"35.156.160.91\"\n" +
            "        ]\n" +
            "    },\n" +
            "    \"RU\": {\n" +
            "        \"mobileApiUrl\": \"https://a1-rus.wgine.com/api.json\",\n" +
            "        \"thingAppUrl\": \"app-h5-pre-eu.iot-888.com/developerapp/support\",\n" +
            "    },\n" +
            "    \"IN\": {\n" +
            "        \"mobileApiUrl\": \"https://a1-in-pre.iot-888.com/api.json\",\n" +
            "        \"dnsUrl\": \"https://h1-in.iot-dns.com\",\n" +
            "        \"dns2Url\": \"https://h2-in.iot-dns.com\",\n" +
            "        \"thingAppUrl\": \"app-h5-pre-in.iot-888.com/developerapp/support\",\n" +
            "        \"dnsIps\": [\n" +
            "          \"13.234.164.70\",\n" +
            "          \"13.234.89.49\"\n" +
            "        ]\n" +
            "    }\n" +
            "}";

    public PreviewApiUrlProvider(Context cxt) {
        super(cxt);
        setDomain(cxt, PREVIEW_DOMAIN_NEUTRAL);
    }

    private void setDomain(Context context, String domain) {
        String region = getDefaultRegion(context);
        JSONObject previewDomains = JSONObject.parseObject(domain);
        Domain defaultDomain = previewDomains.getObject(region, Domain.class);
        setDefaultDomain(defaultDomain);
        setDomainJson(domain);
    }

    @Override
    protected String getOldApiUrl() {
        return mRegion == DOMAIN_AY
                ? "https://a1.mb.cn.wgine.com/api.json"
                : "https://a1.mb.us.wgine.com/api.json";
    }

    @Override
    protected String[] getOldMqttUrl(String region) {
        return mRegion == DOMAIN_AY
                ? new String[]{"mq.mb.cn.wgine.com", "mq.mb.cn.wgine.com"}
                : new String[]{"mq.mb.us.wgine.com", "mq.mb.us.wgine.com"};
    }

    @Override
    protected String getOldGwApiUrl() {
        return "";
    }

    @Override
    protected String[] getOldGwMqttUrl() {
        return mRegion == DOMAIN_AY
                ? new String[]{"mq.gw.cn.wgine.com", "mq.gw.cn.wgine.com"}
                : new String[]{"mq.gw.us.wgine.com", "mq.gw.us.wgine.com"};
    }
}