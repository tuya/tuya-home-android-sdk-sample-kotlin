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

package com.tuya.appsdk.sample.home.main

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.thingclips.sdk.aistream.ai.AiChatActivity
import com.thingclips.smart.home.sdk.ThingHomeSdk
import com.thingclips.smart.home.sdk.bean.HomeBean
import com.thingclips.smart.home.sdk.callback.IThingHomeResultCallback
import com.tuya.appsdk.sample.home.list.HomeListActivity
import com.tuya.appsdk.sample.home.list.enum.HomeListPageType
import com.tuya.appsdk.sample.home.newHome.NewHomeActivity
import com.tuya.appsdk.sample.resource.HomeModel
import com.tuya.appsdk.sample.user.R

/**
 * Home Management Widget
 *
 * @author qianqi <a href="mailto:developer@tuya.com"/>
 * @since 2021/1/9 5:06 PM
 */
class HomeFuncWidget {
    lateinit var tvCurrentHomeName: TextView
    lateinit var mContext: Context

    fun render(context: Context): View {
        mContext = context
        val rootView =
            LayoutInflater.from(context).inflate(R.layout.home_view_func, null, false)
        initView(rootView)
        return rootView
    }

    private fun initView(rootView: View) {
        // Create Home
        rootView.findViewById<TextView>(R.id.tvNewHome).setOnClickListener {
            it.context.startActivity(Intent(it.context, NewHomeActivity::class.java))
        }

        // Switch Home
        val tvCurrentHome = rootView.findViewById<TextView>(R.id.tvCurrentHome)
        tvCurrentHome.setOnClickListener {
            val intent = Intent(it.context, HomeListActivity::class.java)
            intent.putExtra("type", HomeListPageType.SWITCH)
            it.context.startActivity(intent)
        }
        tvCurrentHomeName = rootView.findViewById<TextView>(R.id.tvCurrentHomeName)

        // Get Home List And Home Detail
        rootView.findViewById<TextView>(R.id.tvHomeList).setOnClickListener {
            val intent = Intent(it.context, HomeListActivity::class.java)
            intent.putExtra("type", HomeListPageType.LIST)
            it.context.startActivity(intent)
        }
        rootView.findViewById<TextView>(R.id.tvAiAssistant).setOnClickListener {
            val currentHomeId = HomeModel.INSTANCE.getCurrentHome(tvCurrentHomeName.context)
            if (currentHomeId == 0L) {
                // No home selected, show error or prompt
                Toast.makeText(it.context, "Please select a home", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val aiSolutionCode = getMetaDataValue(it.context, "AI_SOLUTION_CODE")
            val miniProgramId = getMetaDataValue(it.context, "MINI_PROGRAM_ID")
            if (aiSolutionCode.isNullOrEmpty() || miniProgramId.isNullOrEmpty()) {
                Toast.makeText(
                    it.context,
                    "AI solution code or mini program ID not found",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            val intent = Intent(it.context, AiChatActivity::class.java)
            intent.putExtra("ownerId", currentHomeId.toString())
            intent.putExtra("aiSolutionCode", aiSolutionCode)
            intent.putExtra("miniProgramId", miniProgramId)
            it.context.startActivity(intent)
        }

    }

    fun getMetaDataValue(context: Context, key: String): String? {
        try {
            val appInfo = context.packageManager.getApplicationInfo(
                context.packageName,
                PackageManager.GET_META_DATA
            )
            return appInfo.metaData?.getString(key)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun refresh() {
        val currentHomeId = HomeModel.INSTANCE.getCurrentHome(tvCurrentHomeName.context)
        if (currentHomeId != 0L) {
            ThingHomeSdk.newHomeInstance(currentHomeId)
                .getHomeDetail(object : IThingHomeResultCallback {
                    override fun onSuccess(bean: HomeBean?) {
                        bean?.let {
                            tvCurrentHomeName.text = it.name
                            if (it.name == null) {
                                HomeModel.INSTANCE.clear(mContext)
                            }
                        }


                    }

                    override fun onError(errorCode: String?, errorMsg: String?) {

                    }

                })
        }
    }


}