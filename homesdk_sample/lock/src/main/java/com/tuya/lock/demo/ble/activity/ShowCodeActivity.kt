package com.tuya.lock.demo.ble.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.tuya.lock.demo.R
import com.tuya.lock.demo.common.constant.Constant
import com.tuya.lock.demo.common.utils.JSONFormat

/**
 *
 * Created by HuiYao on 2024/2/29
 */
class ShowCodeActivity : AppCompatActivity() {

    companion object {
        fun startActivity(context: Context?, code: String?) {
            val intent = Intent(context, ShowCodeActivity::class.java)
            intent.putExtra(Constant.CODE_DATA, code)
            context?.startActivity(intent)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_code)

        val toolbar = findViewById<Toolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { v: View? -> finish() }

        val codeData = intent.getStringExtra(Constant.CODE_DATA)

        val codeView = findViewById<TextView>(R.id.code_view)
        val endCode: String? = JSONFormat.format(codeData)
        codeView.text = endCode
    }

}