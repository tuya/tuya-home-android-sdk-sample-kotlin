package com.tuya.lock.demo.ble.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.alibaba.fastjson.JSONObject
import com.thingclips.smart.home.sdk.callback.IThingResultCallback
import com.thingclips.smart.optimus.lock.api.IThingBleLockV2
import com.thingclips.smart.optimus.lock.api.IThingLockManager
import com.thingclips.smart.optimus.sdk.ThingOptimusSdk
import com.thingclips.smart.sdk.optimus.lock.bean.ble.PasswordRequest
import com.thingclips.smart.sdk.optimus.lock.bean.ble.ScheduleBean
import com.thingclips.smart.sdk.optimus.lock.bean.ble.TempPasswordBeanV3
import com.thingclips.smart.sdk.optimus.lock.utils.LockUtil
import com.tuya.lock.demo.R
import com.tuya.lock.demo.common.constant.Constant
import com.tuya.lock.demo.common.utils.Utils.getDateDay
import com.tuya.lock.demo.common.utils.Utils.getStampTime

/**
 *
 * Created by HuiYao on 2024/2/29
 */
class PasswordOldOnlineDetailActivity: AppCompatActivity(), CompoundButton.OnCheckedChangeListener {
    companion object {
        fun startActivity(
            context: Context, passwordItem: TempPasswordBeanV3?,
            devId: String?, from: Int, availTimes: Int
        ) {
            val intent = Intent(
                context,
                PasswordOldOnlineDetailActivity::class.java
            )
            //设备id
            intent.putExtra(Constant.DEVICE_ID, devId)
            //创建还是编辑
            intent.putExtra(Constant.FROM, from)
            //编辑的密码数据
            intent.putExtra(Constant.PASSWORD_DATA, JSONObject.toJSONString(passwordItem))
            intent.putExtra(Constant.AVAIL_TIMES, availTimes)
            context.startActivity(intent)
        }
    }

    private var tuyaLockDevice: IThingBleLockV2? = null

    private var scheduleBean = ScheduleBean()

    private var schedule_effective_time_hour = 0
    private var schedule_effective_time_minute = 0
    private var schedule_invalid_time_hour = 0
    private var schedule_invalid_time_minute = 0

    private var mFrom = 0 //0是创建、1是编辑

    private var mPasswordData: TempPasswordBeanV3? = null

    private var passwordValue: String? = null
    private var availTimes = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_password_temp_add)
        val toolbar = findViewById<Toolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { v: View? -> finish() }
        val mDevId = intent.getStringExtra(Constant.DEVICE_ID)
        mFrom = intent.getIntExtra(Constant.FROM, 0)
        mPasswordData = JSONObject.parseObject(
            intent.getStringExtra(Constant.PASSWORD_DATA),
            TempPasswordBeanV3::class.java
        )
        availTimes = intent.getIntExtra(Constant.AVAIL_TIMES, 0)
        if (availTimes == 1) {
            toolbar.title = resources.getString(R.string.password_one_time)
        } else {
            toolbar.title = resources.getString(R.string.password_periodic)
        }
        if (null == mPasswordData) {
            mPasswordData = TempPasswordBeanV3()
        } else {
            if (null != mPasswordData!!.scheduleDetails && null != mPasswordData!!.scheduleDetails[0]) {
                scheduleBean = mPasswordData!!.scheduleDetails[0]
            }
        }
        val tuyaLockManager = ThingOptimusSdk.getManager(
            IThingLockManager::class.java
        )
        tuyaLockDevice = tuyaLockManager.getBleLockV2(mDevId)
        showScheduleMain()
        val password_day_weeks_1 = findViewById<CheckBox>(R.id.password_day_weeks_1)
        val password_day_weeks_2 = findViewById<CheckBox>(R.id.password_day_weeks_2)
        val password_day_weeks_3 = findViewById<CheckBox>(R.id.password_day_weeks_3)
        val password_day_weeks_4 = findViewById<CheckBox>(R.id.password_day_weeks_4)
        val password_day_weeks_5 = findViewById<CheckBox>(R.id.password_day_weeks_5)
        val password_day_weeks_6 = findViewById<CheckBox>(R.id.password_day_weeks_6)
        val password_day_weeks_7 = findViewById<CheckBox>(R.id.password_day_weeks_7)
        password_day_weeks_1.setOnCheckedChangeListener(this)
        password_day_weeks_2.setOnCheckedChangeListener(this)
        password_day_weeks_3.setOnCheckedChangeListener(this)
        password_day_weeks_4.setOnCheckedChangeListener(this)
        password_day_weeks_5.setOnCheckedChangeListener(this)
        password_day_weeks_6.setOnCheckedChangeListener(this)
        password_day_weeks_7.setOnCheckedChangeListener(this)
        if (mFrom == 0) {
            scheduleBean.dayOfWeeks.add(ScheduleBean.DayOfWeek.FRIDAY)
            scheduleBean.dayOfWeeks.add(ScheduleBean.DayOfWeek.MONDAY)
            scheduleBean.dayOfWeeks.add(ScheduleBean.DayOfWeek.SATURDAY)
            scheduleBean.dayOfWeeks.add(ScheduleBean.DayOfWeek.SUNDAY)
            scheduleBean.dayOfWeeks.add(ScheduleBean.DayOfWeek.THURSDAY)
            scheduleBean.dayOfWeeks.add(ScheduleBean.DayOfWeek.TUESDAY)
            scheduleBean.dayOfWeeks.add(ScheduleBean.DayOfWeek.WEDNESDAY)
            password_day_weeks_1.isChecked = true
            password_day_weeks_2.isChecked = true
            password_day_weeks_3.isChecked = true
            password_day_weeks_4.isChecked = true
            password_day_weeks_5.isChecked = true
            password_day_weeks_6.isChecked = true
            password_day_weeks_7.isChecked = true
        } else {
            scheduleBean.dayOfWeeks = LockUtil.parseWorkingDay(scheduleBean.workingDay)
            for (dayOfWeek in scheduleBean.dayOfWeeks) {
                if (dayOfWeek == ScheduleBean.DayOfWeek.MONDAY) {
                    password_day_weeks_1.isChecked = true
                } else if (dayOfWeek == ScheduleBean.DayOfWeek.TUESDAY) {
                    password_day_weeks_2.isChecked = true
                } else if (dayOfWeek == ScheduleBean.DayOfWeek.WEDNESDAY) {
                    password_day_weeks_3.isChecked = true
                } else if (dayOfWeek == ScheduleBean.DayOfWeek.THURSDAY) {
                    password_day_weeks_4.isChecked = true
                } else if (dayOfWeek == ScheduleBean.DayOfWeek.FRIDAY) {
                    password_day_weeks_5.isChecked = true
                } else if (dayOfWeek == ScheduleBean.DayOfWeek.SATURDAY) {
                    password_day_weeks_6.isChecked = true
                } else if (dayOfWeek == ScheduleBean.DayOfWeek.SUNDAY) {
                    password_day_weeks_7.isChecked = true
                }
            }
        }
        val password_all_day_wrap = findViewById<RadioGroup>(R.id.password_all_day_wrap)
        password_all_day_wrap.setOnCheckedChangeListener { group: RadioGroup?, checkedId: Int ->
            if (checkedId == R.id.password_all_day_yes) {
                scheduleBean.allDay = false
            } else if (checkedId == R.id.password_all_day_no) {
                scheduleBean.allDay = true
            }
            showScheduleTimeMain()
        }
        if (scheduleBean.allDay) {
            password_all_day_wrap.check(R.id.password_all_day_no)
        } else {
            password_all_day_wrap.check(R.id.password_all_day_yes)
        }
        showScheduleTimeMain()
        setScheduleEffectiveTime()
        setScheduleInvalidTime()
        val password_name = findViewById<EditText>(R.id.password_name)
        password_name.setText(mPasswordData!!.name)
        password_name.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (!TextUtils.isEmpty(s)) {
                    mPasswordData!!.name = s.toString()
                }
            }

            override fun afterTextChanged(s: Editable) {}
        })
        val password_content = findViewById<EditText>(R.id.password_content)
        password_content.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (!TextUtils.isEmpty(s)) {
                    passwordValue = s.toString()
                }
            }

            override fun afterTextChanged(s: Editable) {}
        })
        val password_effective_time = findViewById<EditText>(R.id.password_effective_time)
        if (mPasswordData!!.effectiveTime == 0L) {
            mPasswordData!!.effectiveTime = System.currentTimeMillis()
        }
        var effectiveTime = mPasswordData!!.effectiveTime
        if (effectiveTime.toString().length == 10) {
            effectiveTime = effectiveTime * 1000
        }
        password_effective_time.setText(getDateDay(effectiveTime))
        password_effective_time.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (!TextUtils.isEmpty(s)) {
                    mPasswordData!!.effectiveTime = getStampTime(s.toString())
                }
            }

            override fun afterTextChanged(s: Editable) {}
        })
        val password_invalid_time = findViewById<EditText>(R.id.password_invalid_time)
        if (mPasswordData!!.invalidTime == 0L) {
            mPasswordData!!.invalidTime = System.currentTimeMillis() + 7 * 86400000L
        }
        var invalidTime = mPasswordData!!.invalidTime
        if (invalidTime.toString().length == 10) {
            invalidTime = invalidTime * 1000
        }
        password_invalid_time.setText(getDateDay(invalidTime))
        password_invalid_time.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (!TextUtils.isEmpty(s)) {
                    mPasswordData!!.invalidTime = getStampTime(s.toString())
                }
            }

            override fun afterTextChanged(s: Editable) {}
        })
        if (mFrom == 1) {
            findViewById<View>(R.id.password_content_wrap).visibility = View.GONE
            findViewById<View>(R.id.password_content_line).visibility = View.GONE
        } else {
            findViewById<View>(R.id.password_content_wrap).visibility = View.VISIBLE
            findViewById<View>(R.id.password_content_line).visibility = View.VISIBLE
        }
        findViewById<View>(R.id.password_add).setOnClickListener { v: View? -> createPassword() }
    }

    private fun setScheduleEffectiveTime() {
        val password_schedule_effective_time_hour =
            findViewById<EditText>(R.id.password_schedule_effective_time_hour)
        if (scheduleBean.effectiveTime == 0) {
            schedule_effective_time_hour = 0
            password_schedule_effective_time_hour.setText("0")
        } else {
            password_schedule_effective_time_hour.setText((scheduleBean.effectiveTime / 60).toString())
            schedule_effective_time_hour = scheduleBean.effectiveTime / 60
        }
        password_schedule_effective_time_hour.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                schedule_effective_time_hour = if (!TextUtils.isEmpty(s)) {
                    s.toString().toInt()
                } else {
                    0
                }
            }

            override fun afterTextChanged(s: Editable) {}
        })
        val password_schedule_effective_time_minute =
            findViewById<EditText>(R.id.password_schedule_effective_time_minute)
        if (scheduleBean.effectiveTime == 0) {
            schedule_effective_time_minute = 0
            password_schedule_effective_time_minute.setText("0")
        } else {
            password_schedule_effective_time_minute.setText((scheduleBean.effectiveTime % 60).toString())
            schedule_effective_time_minute = scheduleBean.effectiveTime % 60
        }
        password_schedule_effective_time_minute.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                schedule_effective_time_minute = if (!TextUtils.isEmpty(s)) {
                    s.toString().toInt()
                } else {
                    0
                }
            }

            override fun afterTextChanged(s: Editable) {}
        })
    }

    private fun setScheduleInvalidTime() {
        val password_schedule_invalid_time_hour =
            findViewById<EditText>(R.id.password_schedule_invalid_time_hour)
        if (scheduleBean.invalidTime == 0) {
            schedule_invalid_time_hour = 23
            password_schedule_invalid_time_hour.setText("23")
        } else {
            password_schedule_invalid_time_hour.setText((scheduleBean.invalidTime / 60).toString())
            schedule_invalid_time_hour = scheduleBean.invalidTime / 60
        }
        password_schedule_invalid_time_hour.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                schedule_invalid_time_hour = if (!TextUtils.isEmpty(s)) {
                    s.toString().toInt()
                } else {
                    0
                }
            }

            override fun afterTextChanged(s: Editable) {}
        })
        val password_schedule_invalid_time_minute =
            findViewById<EditText>(R.id.password_schedule_invalid_time_minute)
        if (scheduleBean.invalidTime == 0) {
            schedule_invalid_time_minute = 59
            password_schedule_invalid_time_minute.setText("59")
        } else {
            password_schedule_invalid_time_minute.setText((scheduleBean.invalidTime % 60).toString())
            schedule_invalid_time_minute = scheduleBean.invalidTime % 60
        }
        password_schedule_invalid_time_minute.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                schedule_invalid_time_minute = if (!TextUtils.isEmpty(s)) {
                    s.toString().toInt()
                } else {
                    0
                }
            }

            override fun afterTextChanged(s: Editable) {}
        })
    }

    private fun showScheduleMain() {
        val password_schedule_main = findViewById<View>(R.id.password_schedule_main)
        if (availTimes == 1) {
            password_schedule_main.visibility = View.GONE
        } else {
            password_schedule_main.visibility = View.VISIBLE
        }
    }

    private fun showScheduleTimeMain() {
        val password_schedule_time_main = findViewById<View>(R.id.password_schedule_time_main)
        val password_day_weeks_main = findViewById<View>(R.id.password_day_weeks_main)
        if (scheduleBean.allDay) {
            password_schedule_time_main.visibility = View.GONE
            password_day_weeks_main.visibility = View.GONE
        } else {
            password_schedule_time_main.visibility = View.VISIBLE
            password_day_weeks_main.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        tuyaLockDevice!!.onDestroy()
    }

    private fun createPassword() {
        if (!scheduleBean.allDay) {
            scheduleBean.effectiveTime =
                schedule_effective_time_hour * 60 + schedule_effective_time_minute
            scheduleBean.invalidTime =
                schedule_invalid_time_hour * 60 + schedule_invalid_time_minute
            scheduleBean.workingDay = LockUtil.convertWorkingDay(scheduleBean.dayOfWeeks).toInt(16)
        } else {
            scheduleBean.effectiveTime = 0
            scheduleBean.invalidTime = 0
        }
        val passwordRequest = PasswordRequest()
        passwordRequest.password = passwordValue
        passwordRequest.name = mPasswordData!!.name
        passwordRequest.setSchedule(scheduleBean)
        passwordRequest.effectiveTime = mPasswordData!!.effectiveTime
        passwordRequest.invalidTime = mPasswordData!!.invalidTime
        passwordRequest.availTime = availTimes
        if (mFrom == 1) {
            passwordRequest.id = mPasswordData!!.passwordId.toString()
        }
        Log.i(Constant.TAG, "request:$passwordRequest")
        if (mFrom == 0) {
            tuyaLockDevice!!.getCustomOnlinePassword(
                passwordRequest,
                object : IThingResultCallback<String?> {
                    override fun onSuccess(result: String?) {
                        Toast.makeText(applicationContext, "onSuccess", Toast.LENGTH_SHORT).show()
                        finish()
                    }

                    override fun onError(errorCode: String, errorMessage: String) {
                        Toast.makeText(applicationContext, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                })
        } else {
            passwordRequest.id = mPasswordData!!.passwordId.toString()
            passwordRequest.sn = mPasswordData!!.sn
            tuyaLockDevice!!.updateOnlinePassword(
                passwordRequest,
                object : IThingResultCallback<String?> {
                    override fun onSuccess(result: String?) {
                        Toast.makeText(applicationContext, "onSuccess", Toast.LENGTH_SHORT).show()
                        finish()
                    }

                    override fun onError(errorCode: String, errorMessage: String) {
                        Toast.makeText(applicationContext, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        if (null == scheduleBean) {
            scheduleBean = ScheduleBean()
        }
        val id = buttonView.id
        if (id == R.id.password_day_weeks_1) {
            if (isChecked) {
                scheduleBean.dayOfWeeks.add(ScheduleBean.DayOfWeek.MONDAY)
            } else {
                scheduleBean.dayOfWeeks.remove(ScheduleBean.DayOfWeek.MONDAY)
            }
        } else if (id == R.id.password_day_weeks_2) {
            if (isChecked) {
                scheduleBean.dayOfWeeks.add(ScheduleBean.DayOfWeek.TUESDAY)
            } else {
                scheduleBean.dayOfWeeks.remove(ScheduleBean.DayOfWeek.TUESDAY)
            }
        } else if (id == R.id.password_day_weeks_3) {
            if (isChecked) {
                scheduleBean.dayOfWeeks.add(ScheduleBean.DayOfWeek.WEDNESDAY)
            } else {
                scheduleBean.dayOfWeeks.remove(ScheduleBean.DayOfWeek.WEDNESDAY)
            }
        } else if (id == R.id.password_day_weeks_4) {
            if (isChecked) {
                scheduleBean.dayOfWeeks.add(ScheduleBean.DayOfWeek.THURSDAY)
            } else {
                scheduleBean.dayOfWeeks.remove(ScheduleBean.DayOfWeek.THURSDAY)
            }
        } else if (id == R.id.password_day_weeks_5) {
            if (isChecked) {
                scheduleBean.dayOfWeeks.add(ScheduleBean.DayOfWeek.FRIDAY)
            } else {
                scheduleBean.dayOfWeeks.remove(ScheduleBean.DayOfWeek.FRIDAY)
            }
        } else if (id == R.id.password_day_weeks_6) {
            if (isChecked) {
                scheduleBean.dayOfWeeks.add(ScheduleBean.DayOfWeek.SATURDAY)
            } else {
                scheduleBean.dayOfWeeks.remove(ScheduleBean.DayOfWeek.SATURDAY)
            }
        } else if (id == R.id.password_day_weeks_7) {
            if (isChecked) {
                scheduleBean.dayOfWeeks.add(ScheduleBean.DayOfWeek.SUNDAY)
            } else {
                scheduleBean.dayOfWeeks.remove(ScheduleBean.DayOfWeek.SUNDAY)
            }
        }
    }
}