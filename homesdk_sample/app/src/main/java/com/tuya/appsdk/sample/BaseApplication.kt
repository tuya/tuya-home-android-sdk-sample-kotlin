/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014-2021 Tuya Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tuya.appsdk.sample

import android.app.Application
import android.util.Log
import com.tuya.smart.android.demo.camera.CameraUtils
import com.tuya.smart.home.sdk.TuyaHomeSdk
import com.uuzuche.lib_zxing.activity.ZXingLibrary
import com.tuya.smart.api.service.RedirectService

import com.tuya.smart.api.router.UrlBuilder

import com.tuya.smart.api.MicroContext

import com.tuya.smart.commonbiz.bizbundle.family.api.AbsBizBundleFamilyService

import com.tuya.smart.wrapper.api.TuyaWrapper

import com.tuya.smart.optimus.sdk.TuyaOptimusSdk
import com.tuya.smart.api.service.ServiceEventListener

import com.tuya.smart.api.service.RouteEventListener







/**
 * Base Application
 *
 * @author qianqi <a href="mailto:developer@tuya.com"/>
 * @since 2021/1/6 11:50 AM
 */
class BaseApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        TuyaHomeSdk.init(this)
        TuyaHomeSdk.setDebugMode(true)
        ZXingLibrary.initDisplayOpinion(this)
        CameraUtils.init(this)
//        initData()
    }

    /**
     * 业务包初始化
     */
    private fun initData(){
        // 业务包初始化
        TuyaWrapper.init(this, { errorCode, urlBuilder -> // 路由未实现回调
            // 点击无反应表示路由未现实，需要在此实现， urlBuilder.target 目标路由， urlBuilder.params 路由参数
            Log.e("router not implement", urlBuilder.target + urlBuilder.params.toString())
        }) { serviceName -> // 服务未实现回调
            Log.e("service not implement", serviceName)
        }
        TuyaOptimusSdk.init(this)


        // 注册家庭服务，商城业务包可以不注册此服务
//        TuyaWrapper.registerService(AbsBizBundleFamilyService::class.java, BizBundleFamilyServiceImpl())
        //拦截已存在的路由，通过参数跳转至自定义实现页面
        val service = MicroContext.getServiceManager().findServiceByInterface<RedirectService>(
            RedirectService::class.java.name
        )
        service.registerUrlInterceptor { urlBuilder, interceptorCallback -> //Such as:
            //Intercept the event of clicking the panel right menu and jump to the custom page with the parameters of urlBuilder
            //例如：拦截点击面板右上角按钮事件，通过 urlBuilder 的参数跳转至自定义页面
            if (urlBuilder.target == "panelAction" && urlBuilder.params.getString("action") == "gotoPanelMore") {
                interceptorCallback.interceptor("interceptor")
                Log.e("interceptor", urlBuilder.params.toString())
            } else {
                interceptorCallback.onContinue(urlBuilder)
            }
        }
    }
}