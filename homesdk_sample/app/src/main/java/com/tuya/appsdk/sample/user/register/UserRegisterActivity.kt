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

package com.tuya.appsdk.sample.user.register

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.tuya.appsdk.sample.R
import com.thingclips.smart.android.user.api.IRegisterCallback
import com.thingclips.smart.android.user.api.IValidateCallback
import com.thingclips.smart.android.user.bean.User
import com.thingclips.smart.home.sdk.ThingHomeSdk
import com.thingclips.smart.sdk.api.IResultCallback
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * User Register Example
 *
 * @author qianqi <a href="mailto:developer@tuya.com">Contact me.</a>
 * @since 2021/1/5 5:13 PM
 */
class UserRegisterActivity : AppCompatActivity(), View.OnClickListener {
    private val check =
        "^([a-z0-9A-Z]+[-|\\.]?)+[a-z0-9A-Z]@([a-z0-9A-Z]+(-[a-z0-9A-Z]+)?\\.)+[a-zA-Z]{2,}$"
    private val regex: Pattern = Pattern.compile(check)
    private val mRegisterType = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user_activity_register)

        val toolbar: Toolbar = findViewById<View>(R.id.topAppBar) as Toolbar
        toolbar.setNavigationOnClickListener {
            finish()
        }

        findViewById<Button>(R.id.btnRegister).setOnClickListener(this)
        findViewById<Button>(R.id.btnCode).setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        val strAccount = findViewById<EditText>(R.id.etAccount).text.toString()
        val strCountryCode = findViewById<EditText>(R.id.etCountryCode).text.toString()
        val strPassword = findViewById<EditText>(R.id.etPassword).text.toString()
        val strCode = findViewById<EditText>(R.id.etCode).text.toString()

        val matcher: Matcher = regex.matcher(strAccount)
        val isEmail: Boolean = matcher.matches()

        v?.id?.let {
            if (it == R.id.btnRegister) {
                val callback = object : IRegisterCallback {
                    override fun onSuccess(user: User?) {
                        Toast.makeText(
                            this@UserRegisterActivity,
                            "Register success",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    override fun onError(code: String?, error: String?) {
                        Toast.makeText(
                            this@UserRegisterActivity,
                            "Register error->$error",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                if (isEmail) {
                    // Register by email
                    ThingHomeSdk.getUserInstance().registerAccountWithEmail(
                        strCountryCode,
                        strAccount,
                        strPassword,
                        strCode,
                        callback
                    )
                } else {
                    // Register by phone
                    ThingHomeSdk.getUserInstance().registerAccountWithPhone(
                        strCountryCode,
                        strAccount,
                        strPassword,
                        strCode,
                        callback
                    )
                }

            } else if (it == R.id.btnCode) {
                // Get verification code code
                ThingHomeSdk.getUserInstance().sendVerifyCodeWithUserName(
                    strAccount,
                    "",
                    strCountryCode,
                    mRegisterType,
                    object : IResultCallback {
                        override fun onSuccess() {
                            Toast.makeText(
                                this@UserRegisterActivity,
                                "Got validateCode",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                        override fun onError(code: String?, error: String?) {
                            Toast.makeText(
                                this@UserRegisterActivity,
                                "getValidateCode error->$error",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                    })
            }
        }
    }
}