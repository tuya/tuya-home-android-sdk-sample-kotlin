package com.tuya.lock.demo.video.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.alibaba.fastjson.JSONObject
import com.thingclips.thinglock.videolock.bean.LogsListBean.MediaInfo
import com.tuya.lock.demo.R
import com.tuya.lock.demo.common.constant.Constant
import com.tuya.lock.demo.common.view.EncryptImageView

/**
 *
 * Created by HuiYao on 2024/2/29
 */
class LogRecordDetailActivity: AppCompatActivity() {

    private var decrypt_view: EncryptImageView? = null

    private var mediaInfo: MediaInfo? = null

    companion object{
        fun startActivity(context: Context, mediaInfo: MediaInfo?) {
            val intent = Intent(context, LogRecordDetailActivity::class.java)
            intent.putExtra(Constant.CODE_DATA, JSONObject.toJSONString(mediaInfo))
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_record_detail)
        val toolbar = findViewById<Toolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { v: View? -> finish() }
        toolbar.title = getString(R.string.lock_log_list)
        val data = intent.getStringExtra(Constant.CODE_DATA)
        if (null != data) {
            mediaInfo = JSONObject.parseObject(data, MediaInfo::class.java)
        }
        decrypt_view = findViewById(R.id.decrypt_view)
        decrypt_view!!.setEncryptImageViewLoadListener(object :
            EncryptImageView.EncryptImageViewLoadListener {
            override fun success(url: String?, width: Int, height: Int) {
                Log.e("setImageURI", "success width:$width, height:$height")
            }

            override fun failure(url: String?, error: String) {
                Log.e("setImageURI", "failure error:$error")
            }
        })
    }

    override fun onResume() {
        super.onResume()
        decrypt_view!!.setImageURI(mediaInfo!!.fileUrl, mediaInfo!!.fileKey.toByteArray())
    }
}