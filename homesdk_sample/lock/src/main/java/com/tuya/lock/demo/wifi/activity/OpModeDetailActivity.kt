package com.tuya.lock.demo.wifi.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.alibaba.fastjson.JSONObject
import com.thingclips.smart.optimus.lock.api.bean.UnlockRelation
import com.tuya.lock.demo.R
import com.tuya.lock.demo.common.constant.Constant

/**
 *
 * Created by HuiYao on 2024/2/29
 */
class OpModeDetailActivity: AppCompatActivity() {

    private var dpCode: String? = null
    private var sn = 0
    private var mFrom = 0
    private var add_password_sn: EditText? = null
    private var addView: Button? = null

    private var inputSn = 0

    companion object{
        const val REQUEST_CODE = 9999
        fun startActivity(activity: Activity, sn: Int, dpCode: String?) {
            val intent = Intent(activity, OpModeDetailActivity::class.java)
            intent.putExtra(Constant.DP_CODE, dpCode)
            intent.putExtra("sn", sn)
            activity.startActivityForResult(intent, REQUEST_CODE)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wifi_unlock_add)
        val toolbar = findViewById<Toolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { v: View? -> onBackPressed() }
        dpCode = intent.getStringExtra(Constant.DP_CODE)
        sn = intent.getIntExtra("sn", 0)
        if (sn > 0) {
            mFrom = 1
        }
        initView()
        initData()
    }

    fun initView() {
        add_password_sn = findViewById<EditText>(R.id.add_password_sn)
        addView = findViewById<Button>(R.id.unlock_mode_add)
    }

    override fun onResume() {
        super.onResume()
    }

    fun initData() {
        add_password_sn!!.setText(sn.toString())
        add_password_sn!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (!TextUtils.isEmpty(s)) {
                    inputSn = s.toString().toInt()
                }
            }

            override fun afterTextChanged(s: Editable) {}
        })
        val addString: String
        addString = if (mFrom == 1) {
            resources.getString(R.string.submit_edit)
        } else {
            resources.getString(R.string.submit_add)
        }
        addView!!.text = addString
        addView!!.setOnClickListener { v: View? ->
            if (inputSn != sn && inputSn > 0) {
                setResult(0, getOneIntent())
            }
            finish()
        }
    }

    override fun onBackPressed() {
        if (inputSn != sn && inputSn > 0) {
            setResult(0, getOneIntent())
        }
        finish()
        super.onBackPressed()
    }

    private fun getOneIntent(): Intent? {
        val relation = UnlockRelation()
        relation.unlockType = dpCode
        relation.passwordNumber = inputSn
        val intent = Intent()
        intent.putExtra(Constant.UNLOCK_INFO, JSONObject.toJSONString(relation))
        return intent
    }
}