package com.tuya.lock.demo.zigbee.activity

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
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.alibaba.fastjson.JSONObject
import com.thingclips.smart.home.sdk.callback.IThingResultCallback
import com.thingclips.smart.optimus.lock.api.IThingLockManager
import com.thingclips.smart.optimus.lock.api.IThingZigBeeLock
import com.thingclips.smart.optimus.lock.api.zigbee.request.PasswordRequest
import com.thingclips.smart.optimus.lock.api.zigbee.request.ScheduleBean
import com.thingclips.smart.optimus.lock.api.zigbee.response.PasswordBean
import com.thingclips.smart.optimus.sdk.ThingOptimusSdk
import com.thingclips.smart.sdk.optimus.lock.utils.ZigbeeLockUtils
import com.tuya.lock.demo.R
import com.tuya.lock.demo.common.constant.Constant
import com.tuya.lock.demo.common.utils.Utils

/**
 *
 * Created by HuiYao on 2024/3/1
 */
class PasswordDetailActivity : AppCompatActivity(), CompoundButton.OnCheckedChangeListener {

    private var zigBeeLock: IThingZigBeeLock? = null

    private var scheduleBean: ScheduleBean? = ScheduleBean()

    private var schedule_effective_time_hour = "00"
    private var schedule_effective_time_minute = "00"
    private var schedule_invalid_time_hour = "00"
    private var schedule_invalid_time_minute = "00"

    private var dataBean: PasswordBean.DataBean? = null

    private var passwordValue: String? = null
    private var oneTime = 0
    private var mFrom = 0


    companion object{
        fun startActivity(context: Context, devId: String?, times: Int) {
            val intent = Intent(context, PasswordDetailActivity::class.java)
            intent.putExtra(Constant.DEVICE_ID, devId)
            intent.putExtra(Constant.AVAIL_TIMES, times)
            context.startActivity(intent)
        }

        fun startEditActivity(context: Context, devId: String?, bean: PasswordBean.DataBean?) {
            val intent = Intent(context, PasswordDetailActivity::class.java)
            intent.putExtra(Constant.DEVICE_ID, devId)
            intent.putExtra(Constant.PASSWORD_DATA, JSONObject.toJSONString(bean))
            intent.putExtra(Constant.FROM, 1)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_zigbee_password_temp_add)
        val toolbar = findViewById<Toolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { v: View? -> finish() }
        val mDevId = intent.getStringExtra(Constant.DEVICE_ID)
        mFrom = intent.getIntExtra(Constant.FROM, 0)
        dataBean = JSONObject.parseObject<PasswordBean.DataBean>(
            intent.getStringExtra(Constant.PASSWORD_DATA),
            PasswordBean.DataBean::class.java
        )
        oneTime = intent.getIntExtra(Constant.AVAIL_TIMES, 0)
        if (oneTime == 1) {
            toolbar.title = resources.getString(R.string.password_one_time)
        } else {
            toolbar.title = resources.getString(R.string.password_periodic)
        }
        if (null == dataBean) {
            dataBean = PasswordBean.DataBean()
        } else {
            if (null != dataBean!!.modifyData.scheduleList && dataBean!!.modifyData.scheduleList.size > 0 && null != dataBean!!.modifyData.scheduleList[0]) {
                scheduleBean = dataBean!!.modifyData.scheduleList[0]
            }
        }
        val tuyaLockManager = ThingOptimusSdk.getManager(
            IThingLockManager::class.java
        )
        zigBeeLock = tuyaLockManager.getZigBeeLock(mDevId)
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
            scheduleBean!!.dayOfWeeks.add(ScheduleBean.DayOfWeek.FRIDAY)
            scheduleBean!!.dayOfWeeks.add(ScheduleBean.DayOfWeek.MONDAY)
            scheduleBean!!.dayOfWeeks.add(ScheduleBean.DayOfWeek.SATURDAY)
            scheduleBean!!.dayOfWeeks.add(ScheduleBean.DayOfWeek.SUNDAY)
            scheduleBean!!.dayOfWeeks.add(ScheduleBean.DayOfWeek.THURSDAY)
            scheduleBean!!.dayOfWeeks.add(ScheduleBean.DayOfWeek.TUESDAY)
            scheduleBean!!.dayOfWeeks.add(ScheduleBean.DayOfWeek.WEDNESDAY)
            scheduleBean!!.isAllDay = true
            password_day_weeks_1.isChecked = true
            password_day_weeks_2.isChecked = true
            password_day_weeks_3.isChecked = true
            password_day_weeks_4.isChecked = true
            password_day_weeks_5.isChecked = true
            password_day_weeks_6.isChecked = true
            password_day_weeks_7.isChecked = true
        } else {
            scheduleBean!!.dayOfWeeks = ZigbeeLockUtils.parseWorkingDay(scheduleBean!!.workingDay)
            for (dayOfWeek in scheduleBean!!.dayOfWeeks) {
                when (dayOfWeek!!) {
                    ScheduleBean.DayOfWeek.MONDAY -> {
                        password_day_weeks_1.isChecked = true
                    }
                    ScheduleBean.DayOfWeek.TUESDAY -> {
                        password_day_weeks_2.isChecked = true
                    }
                    ScheduleBean.DayOfWeek.WEDNESDAY -> {
                        password_day_weeks_3.isChecked = true
                    }
                    ScheduleBean.DayOfWeek.THURSDAY -> {
                        password_day_weeks_4.isChecked = true
                    }
                    ScheduleBean.DayOfWeek.FRIDAY -> {
                        password_day_weeks_5.isChecked = true
                    }
                    ScheduleBean.DayOfWeek.SATURDAY -> {
                        password_day_weeks_6.isChecked = true
                    }
                    ScheduleBean.DayOfWeek.SUNDAY -> {
                        password_day_weeks_7.isChecked = true
                    }
                }
            }
        }
        val password_all_day_wrap = findViewById<RadioGroup>(R.id.password_all_day_wrap)
        password_all_day_wrap.setOnCheckedChangeListener { group: RadioGroup?, checkedId: Int -> showScheduleTimeMain() }
        if (scheduleBean!!.isAllDay || scheduleBean!!.invalidTime == 0) {
            password_all_day_wrap.check(R.id.password_all_day_no)
        } else {
            password_all_day_wrap.check(R.id.password_all_day_yes)
        }
        showScheduleTimeMain()
        setScheduleEffectiveTime()
        setScheduleInvalidTime()
        val password_name = findViewById<EditText>(R.id.password_name)
        password_name.setText(dataBean!!.name)
        password_name.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (!TextUtils.isEmpty(s)) {
                    dataBean!!.name = s.toString()
                }
            }

            override fun afterTextChanged(s: Editable) {}
        })
        val password_content = findViewById<EditText>(R.id.password_content)
        password_content.setText(dataBean!!.password)
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
        if (dataBean!!.effectiveTime == 0L) {
            dataBean!!.effectiveTime = System.currentTimeMillis()
        }
        var effectiveTime = dataBean!!.effectiveTime
        if (effectiveTime.toString().length == 10) {
            effectiveTime *= 1000
        }
        password_effective_time.setText(Utils.getDateDay(effectiveTime))
        password_effective_time.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (!TextUtils.isEmpty(s)) {
                    dataBean!!.effectiveTime = Utils.getStampTime(s.toString())
                }
            }

            override fun afterTextChanged(s: Editable) {}
        })
        val password_invalid_time = findViewById<EditText>(R.id.password_invalid_time)
        if (dataBean!!.invalidTime == 0L) {
            dataBean!!.invalidTime = System.currentTimeMillis() + 7 * 86400000L
        }
        var invalidTime = dataBean!!.invalidTime
        if (invalidTime.toString().length == 10) {
            invalidTime *= 1000
        }
        password_invalid_time.setText(Utils.getDateDay(invalidTime))
        password_invalid_time.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (!TextUtils.isEmpty(s)) {
                    dataBean!!.invalidTime = Utils.getStampTime(s.toString())
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
        if (oneTime == 1) {
            findViewById<View>(R.id.password_effective_time_main).visibility = View.GONE
            findViewById<View>(R.id.password_effective_time_line).visibility = View.GONE
            findViewById<View>(R.id.password_invalid_time_main).visibility = View.GONE
            findViewById<View>(R.id.password_invalid_time_line).visibility = View.GONE
        } else {
            findViewById<View>(R.id.password_effective_time_main).visibility = View.VISIBLE
            findViewById<View>(R.id.password_effective_time_line).visibility = View.VISIBLE
            findViewById<View>(R.id.password_invalid_time_main).visibility = View.VISIBLE
            findViewById<View>(R.id.password_invalid_time_line).visibility = View.VISIBLE
        }
        findViewById<View>(R.id.password_add).setOnClickListener { v: View? -> createPassword() }
    }

    private fun setScheduleEffectiveTime() {
        val password_schedule_effective_time_hour =
            findViewById<EditText>(R.id.password_schedule_effective_time_hour)
        var effectiveTime = scheduleBean!!.effectiveTime.toString()
        if (effectiveTime.length == 3) {
            effectiveTime = "0$effectiveTime"
        }
        if (scheduleBean!!.effectiveTime == 0) {
            schedule_effective_time_hour = "00"
            password_schedule_effective_time_hour.setText(schedule_effective_time_hour)
        } else {
            password_schedule_effective_time_hour.setText(effectiveTime.substring(0, 2))
            schedule_effective_time_hour = effectiveTime.substring(0, 2)
        }
        password_schedule_effective_time_hour.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (!TextUtils.isEmpty(s)) {
                    var endStr = s.toString()
                    if (endStr.length == 1) {
                        endStr = "0$endStr"
                    }
                    schedule_effective_time_hour = endStr
                } else {
                    schedule_effective_time_hour = "00"
                }
            }

            override fun afterTextChanged(s: Editable) {}
        })
        val password_schedule_effective_time_minute =
            findViewById<EditText>(R.id.password_schedule_effective_time_minute)
        if (scheduleBean!!.effectiveTime == 0) {
            schedule_effective_time_minute = "00"
            password_schedule_effective_time_minute.setText(schedule_effective_time_minute)
        } else {
            password_schedule_effective_time_minute.setText(effectiveTime.substring(2, 4))
            schedule_effective_time_minute = effectiveTime.substring(2, 4)
        }
        password_schedule_effective_time_minute.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (!TextUtils.isEmpty(s)) {
                    var endStr = s.toString()
                    if (endStr.length == 1) {
                        endStr = "0$endStr"
                    }
                    schedule_effective_time_minute = endStr
                } else {
                    schedule_effective_time_minute = "00"
                }
            }

            override fun afterTextChanged(s: Editable) {}
        })
    }

    private fun setScheduleInvalidTime() {
        val password_schedule_invalid_time_hour =
            findViewById<EditText>(R.id.password_schedule_invalid_time_hour)
        var invalidTime = scheduleBean!!.invalidTime.toString()
        if (invalidTime.length == 3) {
            invalidTime = "0$invalidTime"
        }
        if (scheduleBean!!.invalidTime == 0) {
            if (mFrom == 0) {
                schedule_invalid_time_hour = "23"
                password_schedule_invalid_time_hour.setText(schedule_invalid_time_hour)
            } else {
                schedule_invalid_time_hour = "00"
                password_schedule_invalid_time_hour.setText(schedule_invalid_time_hour)
            }
        } else {
            password_schedule_invalid_time_hour.setText(invalidTime.substring(0, 2))
            schedule_invalid_time_hour = invalidTime.substring(0, 2)
        }
        password_schedule_invalid_time_hour.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (!TextUtils.isEmpty(s)) {
                    var endStr = s.toString()
                    if (endStr.length == 1) {
                        endStr = "0$endStr"
                    }
                    schedule_invalid_time_hour = endStr
                } else {
                    schedule_invalid_time_hour = "00"
                }
            }

            override fun afterTextChanged(s: Editable) {}
        })
        val password_schedule_invalid_time_minute =
            findViewById<EditText>(R.id.password_schedule_invalid_time_minute)
        if (scheduleBean!!.invalidTime == 0) {
            if (mFrom == 0) {
                schedule_invalid_time_minute = "59"
                password_schedule_invalid_time_minute.setText(schedule_invalid_time_minute)
            } else {
                schedule_invalid_time_minute = "00"
                password_schedule_invalid_time_minute.setText(schedule_invalid_time_minute)
            }
        } else {
            password_schedule_invalid_time_minute.setText(invalidTime.substring(2, 4))
            schedule_invalid_time_minute = invalidTime.substring(2, 4)
        }
        password_schedule_invalid_time_minute.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (!TextUtils.isEmpty(s)) {
                    var endStr = s.toString()
                    if (endStr.length == 1) {
                        endStr = "0$endStr"
                    }
                    schedule_invalid_time_minute = endStr
                } else {
                    schedule_invalid_time_minute = "00"
                }
            }

            override fun afterTextChanged(s: Editable) {}
        })
    }

    private fun showScheduleMain() {
        val password_schedule_main = findViewById<View>(R.id.password_schedule_main)
        if (oneTime == 1) {
            password_schedule_main.visibility = View.GONE
        } else {
            password_schedule_main.visibility = View.VISIBLE
        }
    }

    private fun showScheduleTimeMain() {
        val password_schedule_time_main = findViewById<View>(R.id.password_schedule_time_main)
        val password_day_weeks_main = findViewById<View>(R.id.password_day_weeks_main)
        val password_all_no = findViewById<RadioButton>(R.id.password_all_day_no)
        if (password_all_no.isChecked) {
            scheduleBean!!.isAllDay = true
            password_schedule_time_main.visibility = View.GONE
            password_day_weeks_main.visibility = View.GONE
        } else {
            scheduleBean!!.isAllDay = false
            password_schedule_time_main.visibility = View.VISIBLE
            password_day_weeks_main.visibility = View.VISIBLE
        }
    }

    private fun createPassword() {
        if (!scheduleBean!!.isAllDay) {
            scheduleBean!!.effectiveTime =
                (schedule_effective_time_hour + schedule_effective_time_minute).toInt()
            scheduleBean!!.invalidTime =
                (schedule_invalid_time_hour + schedule_invalid_time_minute).toInt()
            scheduleBean!!.workingDay =
                ZigbeeLockUtils.convertWorkingDay(scheduleBean!!.dayOfWeeks).toInt(16)
        } else {
            scheduleBean!!.effectiveTime = 0
            scheduleBean!!.invalidTime = 0
        }
        val passwordRequest = PasswordRequest()
        passwordRequest.name = dataBean!!.name
        if (!TextUtils.isEmpty(passwordValue)) {
            passwordRequest.password = passwordValue
        }
        if (oneTime == 0) {
            passwordRequest.setSchedule(scheduleBean)
            passwordRequest.effectiveTime = dataBean!!.effectiveTime
            passwordRequest.invalidTime = dataBean!!.invalidTime
        }
        passwordRequest.oneTime = oneTime
        if (mFrom == 1) {
            passwordRequest.id = dataBean!!.id
        }
        Log.i(Constant.TAG, "request:$passwordRequest")
        if (mFrom == 1) {
            //编辑临时密码
            zigBeeLock!!.modifyTemporaryPassword(
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
            return
        }
        zigBeeLock!!.addTemporaryPassword(passwordRequest, object : IThingResultCallback<String?> {
            override fun onSuccess(result: String?) {
                Toast.makeText(applicationContext, "onSuccess", Toast.LENGTH_SHORT).show()
                finish()
            }

            override fun onError(errorCode: String, errorMessage: String) {
                Toast.makeText(applicationContext, errorMessage, Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        if (null == scheduleBean) {
            scheduleBean = ScheduleBean()
        }
        val id = buttonView.id
        if (id == R.id.password_day_weeks_1) {
            if (isChecked) {
                scheduleBean!!.dayOfWeeks.add(ScheduleBean.DayOfWeek.MONDAY)
            } else {
                scheduleBean!!.dayOfWeeks.remove(ScheduleBean.DayOfWeek.MONDAY)
            }
        } else if (id == R.id.password_day_weeks_2) {
            if (isChecked) {
                scheduleBean!!.dayOfWeeks.add(ScheduleBean.DayOfWeek.TUESDAY)
            } else {
                scheduleBean!!.dayOfWeeks.remove(ScheduleBean.DayOfWeek.TUESDAY)
            }
        } else if (id == R.id.password_day_weeks_3) {
            if (isChecked) {
                scheduleBean!!.dayOfWeeks.add(ScheduleBean.DayOfWeek.WEDNESDAY)
            } else {
                scheduleBean!!.dayOfWeeks.remove(ScheduleBean.DayOfWeek.WEDNESDAY)
            }
        } else if (id == R.id.password_day_weeks_4) {
            if (isChecked) {
                scheduleBean!!.dayOfWeeks.add(ScheduleBean.DayOfWeek.THURSDAY)
            } else {
                scheduleBean!!.dayOfWeeks.remove(ScheduleBean.DayOfWeek.THURSDAY)
            }
        } else if (id == R.id.password_day_weeks_5) {
            if (isChecked) {
                scheduleBean!!.dayOfWeeks.add(ScheduleBean.DayOfWeek.FRIDAY)
            } else {
                scheduleBean!!.dayOfWeeks.remove(ScheduleBean.DayOfWeek.FRIDAY)
            }
        } else if (id == R.id.password_day_weeks_6) {
            if (isChecked) {
                scheduleBean!!.dayOfWeeks.add(ScheduleBean.DayOfWeek.SATURDAY)
            } else {
                scheduleBean!!.dayOfWeeks.remove(ScheduleBean.DayOfWeek.SATURDAY)
            }
        } else if (id == R.id.password_day_weeks_7) {
            if (isChecked) {
                scheduleBean!!.dayOfWeeks.add(ScheduleBean.DayOfWeek.SUNDAY)
            } else {
                scheduleBean!!.dayOfWeeks.remove(ScheduleBean.DayOfWeek.SUNDAY)
            }
        }
    }
}