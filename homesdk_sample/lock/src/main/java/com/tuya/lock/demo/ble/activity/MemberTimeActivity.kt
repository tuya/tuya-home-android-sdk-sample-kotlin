package com.tuya.lock.demo.ble.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import com.alibaba.fastjson.JSONObject
import com.thingclips.smart.home.sdk.callback.IThingResultCallback
import com.thingclips.smart.optimus.lock.api.IThingBleLockV2
import com.thingclips.smart.optimus.lock.api.IThingLockManager
import com.thingclips.smart.optimus.sdk.ThingOptimusSdk
import com.thingclips.smart.sdk.optimus.lock.bean.ble.MemberInfoBean
import com.thingclips.smart.sdk.optimus.lock.bean.ble.ScheduleBean
import com.thingclips.smart.sdk.optimus.lock.utils.LockUtil
import com.tuya.lock.demo.R
import com.tuya.lock.demo.common.constant.Constant
import com.tuya.lock.demo.common.utils.Utils.getDateDay
import com.tuya.lock.demo.common.utils.Utils.getStampTime

/**
 *
 * Created by HuiYao on 2024/2/29
 */
class MemberTimeActivity: AppCompatActivity(), CompoundButton.OnCheckedChangeListener {

    private var tuyaLockDevice: IThingBleLockV2? = null
    private var userBean: MemberInfoBean? = null
    private var scheduleBean: ScheduleBean? = null

    private var schedule_effective_time_hour = 0
    private var schedule_effective_time_minute = 0
    private var schedule_invalid_time_hour = 0
    private var schedule_invalid_time_minute = 0

    companion object{
        fun startActivity(
            context: Context, memberInfoBean: MemberInfoBean?,
            devId: String?
        ) {
            val intent = Intent(context, MemberTimeActivity::class.java)
            //设备id
            intent.putExtra(Constant.DEVICE_ID, devId)
            //编辑的密码数据
            intent.putExtra(Constant.USER_DATA, JSONObject.toJSONString(memberInfoBean))
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_member_time)
        val toolbar = findViewById<Toolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { v: View? -> finish() }
        val userData = intent.getStringExtra(Constant.USER_DATA)
        val mDevId = intent.getStringExtra(Constant.DEVICE_ID)
        try {
            userBean = JSONObject.parseObject(
                userData,
                MemberInfoBean::class.java
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(applicationContext, e.message, Toast.LENGTH_SHORT).show()
        }
        if (null == userBean) {
            userBean = MemberInfoBean()
        }
        val tuyaLockManager = ThingOptimusSdk.getManager(
            IThingLockManager::class.java
        )
        tuyaLockDevice = tuyaLockManager.getBleLockV2(mDevId)
        if (null != userBean!!.timeScheduleInfo.scheduleDetails && userBean!!.timeScheduleInfo.scheduleDetails.size > 0) {
            scheduleBean = userBean!!.timeScheduleInfo.scheduleDetails[0]
        } else {
            scheduleBean = ScheduleBean()
            userBean!!.timeScheduleInfo.scheduleDetails.add(scheduleBean)
        }
        val user_unlock_permanent_yes = findViewById<RadioButton>(R.id.user_unlock_permanent_yes)
        val user_unlock_permanent_no = findViewById<RadioButton>(R.id.user_unlock_permanent_no)
        if (userBean!!.timeScheduleInfo.isPermanent) {
            user_unlock_permanent_yes.isChecked = true
        } else {
            user_unlock_permanent_no.isChecked = true
        }
        val user_unlock_permanent = findViewById<RadioGroup>(R.id.user_unlock_permanent)
        user_unlock_permanent.setOnCheckedChangeListener { group: RadioGroup?, checkedId: Int ->
            if (checkedId == R.id.user_unlock_permanent_yes) {
                userBean!!.timeScheduleInfo.isPermanent = true
            } else if (checkedId == R.id.user_unlock_permanent_no) {
                userBean!!.timeScheduleInfo.isPermanent = false
            }
            showTimeMain(userBean!!.timeScheduleInfo.isPermanent)
        }
        showTimeMain(userBean!!.timeScheduleInfo.isPermanent)
        val user_effective_timestamp_content =
            findViewById<EditText>(R.id.user_effective_timestamp_content)
        if (userBean!!.timeScheduleInfo.effectiveTime > 0) {
            user_effective_timestamp_content.setText(
                getDateDay(
                    userBean!!.timeScheduleInfo.effectiveTime * 1000,
                    "yyyy-MM-dd HH:mm:ss"
                )
            )
        } else {
            user_effective_timestamp_content.setText(
                getDateDay(
                    System.currentTimeMillis(),
                    "yyyy-MM-dd HH:mm:ss"
                )
            )
            userBean!!.timeScheduleInfo.effectiveTime = System.currentTimeMillis()
        }
        user_effective_timestamp_content.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (!TextUtils.isEmpty(s)) {
                    userBean!!.timeScheduleInfo.effectiveTime =
                        getStampTime(s.toString(), "yyyy-MM-dd HH:mm:ss")
                    Log.i(
                        Constant.TAG,
                        "effectiveTimestamp select:" + userBean!!.timeScheduleInfo.effectiveTime
                    )
                }
            }

            override fun afterTextChanged(s: Editable) {}
        })
        val user_invalid_timestamp_content =
            findViewById<EditText>(R.id.user_invalid_timestamp_content)
        if (userBean!!.timeScheduleInfo.expiredTime > 0) {
            user_invalid_timestamp_content.setText(
                getDateDay(
                    userBean!!.timeScheduleInfo.expiredTime * 1000,
                    "yyyy-MM-dd HH:mm:ss"
                )
            )
        } else {
            user_invalid_timestamp_content.setText(
                getDateDay(
                    System.currentTimeMillis() + 31104000000L,
                    "yyyy-MM-dd HH:mm:ss"
                )
            )
            userBean!!.timeScheduleInfo.expiredTime = System.currentTimeMillis() + 31104000000L
        }
        user_invalid_timestamp_content.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (!TextUtils.isEmpty(s)) {
                    userBean!!.timeScheduleInfo.expiredTime =
                        getStampTime(s.toString(), "yyyy-MM-dd HH:mm:ss")
                    Log.i(
                        Constant.TAG,
                        "invalidTimestamp select:" + userBean!!.timeScheduleInfo.expiredTime
                    )
                }
            }

            override fun afterTextChanged(s: Editable) {}
        })
        val password_all_day_wrap = findViewById<RadioGroup>(R.id.password_all_day_wrap)
        password_all_day_wrap.setOnCheckedChangeListener { group: RadioGroup?, checkedId: Int ->
            if (checkedId == R.id.password_all_day_yes) {
                scheduleBean!!.allDay = false
            } else if (checkedId == R.id.password_all_day_no) {
                scheduleBean!!.allDay = true
            }
            showScheduleTimeMain()
        }
        password_all_day_wrap.check(if (scheduleBean!!.allDay) R.id.password_all_day_no else R.id.password_all_day_yes)
        showScheduleTimeMain()
        setScheduleEffectiveTime()
        setScheduleInvalidTime()
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
        scheduleBean!!.dayOfWeeks = LockUtil.parseWorkingDay(scheduleBean!!.workingDay)
        for (dayOfWeek in scheduleBean!!.dayOfWeeks) {
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
        val user_offline_unlock = findViewById<SwitchCompat>(R.id.user_offline_unlock)
        user_offline_unlock.isChecked = userBean!!.isOfflineUnlock
        user_offline_unlock.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            userBean!!.isOfflineUnlock = isChecked
        }
        val submitBtn = findViewById<Button>(R.id.edit_user_submit)
        submitBtn.setOnClickListener { v: View? -> updateLockUser() }
        submitBtn.text = "更新时效"
    }

    private fun showTimeMain(hide: Boolean) {
        val user_time_main = findViewById<View>(R.id.user_time_main)
        if (hide) {
            user_time_main.visibility = View.GONE
        } else {
            user_time_main.visibility = View.VISIBLE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tuyaLockDevice!!.onDestroy()
    }

    private fun showScheduleTimeMain() {
        val password_schedule_time_main = findViewById<View>(R.id.password_schedule_time_main)
        if (scheduleBean!!.allDay) {
            password_schedule_time_main.visibility = View.GONE
        } else {
            password_schedule_time_main.visibility = View.VISIBLE
        }
    }

    private fun setScheduleEffectiveTime() {
        val password_schedule_effective_time_hour =
            findViewById<EditText>(R.id.password_schedule_effective_time_hour)
        if (scheduleBean!!.effectiveTime == 0) {
            schedule_effective_time_hour = 0
            password_schedule_effective_time_hour.setText("0")
        } else {
            password_schedule_effective_time_hour.setText((scheduleBean!!.effectiveTime / 60).toString())
            schedule_effective_time_hour = scheduleBean!!.effectiveTime / 60
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
        if (scheduleBean!!.effectiveTime == 0) {
            schedule_effective_time_minute = 0
            password_schedule_effective_time_minute.setText("0")
        } else {
            password_schedule_effective_time_minute.setText((scheduleBean!!.effectiveTime % 60).toString())
            schedule_effective_time_minute = scheduleBean!!.effectiveTime % 60
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
        if (scheduleBean!!.invalidTime == 0) {
            schedule_invalid_time_hour = 23
            password_schedule_invalid_time_hour.setText("23")
        } else {
            password_schedule_invalid_time_hour.setText((scheduleBean!!.invalidTime / 60).toString())
            schedule_invalid_time_hour = scheduleBean!!.invalidTime / 60
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
        if (scheduleBean!!.invalidTime == 0) {
            schedule_invalid_time_minute = 59
            password_schedule_invalid_time_minute.setText("59")
        } else {
            password_schedule_invalid_time_minute.setText((scheduleBean!!.invalidTime % 60).toString())
            schedule_invalid_time_minute = scheduleBean!!.invalidTime % 60
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


    /**
     * 用户更新信息
     */
    private fun updateLockUser() {
        if (!scheduleBean!!.allDay) {
            scheduleBean!!.effectiveTime =
                schedule_effective_time_hour * 60 + schedule_effective_time_minute
            scheduleBean!!.invalidTime =
                schedule_invalid_time_hour * 60 + schedule_invalid_time_minute
            scheduleBean!!.workingDay =
                LockUtil.convertWorkingDay(scheduleBean!!.dayOfWeeks).toInt(16)
        } else {
            scheduleBean!!.effectiveTime = 0
            scheduleBean!!.invalidTime = 0
        }
        tuyaLockDevice!!.updateProLockMemberTime(userBean, object : IThingResultCallback<Boolean?> {
            override fun onSuccess(result: Boolean?) {
                Log.i(Constant.TAG, "update lock user time success")
                Toast.makeText(applicationContext, "add lock user success", Toast.LENGTH_SHORT)
                    .show()
                finish()
            }

            override fun onError(code: String, message: String) {
                Log.e(
                    Constant.TAG,
                    "update lock user time failed: code = $code  message = $message"
                )
                Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
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