package com.tuya.lock.demo.common.utils

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.NumberPicker
import android.widget.TextView
import com.thingclips.smart.optimus.lock.api.zigbee.response.PasswordBean
import com.tuya.lock.demo.R

/**
 *
 * Created by HuiYao on 2024/2/29
 */
object DialogUtils {

    @JvmStatic
    fun showClear(context: Context?, listener: DialogInterface.OnClickListener?) {
        showDelete(context, "是否确认清除", listener)
    }

    @JvmStatic
    fun showDelete(context: Context?, listener: DialogInterface.OnClickListener?) {
        showDelete(context, "是否确认删除", listener)
    }

    @JvmStatic
    fun showDelete(
        context: Context?,
        message: String?,
        listener: DialogInterface.OnClickListener?
    ) {
        val dialog = AlertDialog.Builder(context)
        dialog.setTitle("提示")
        dialog.setMessage(message)
        dialog.setPositiveButton("确认", listener)
        dialog.setNegativeButton(
            "取消"
        ) { dialog12: DialogInterface, _: Int -> dialog12.dismiss() }
        dialog.show()
    }

    @JvmStatic
    fun showNumberEdit(context: Context?, timeSelect: Int, callback: NumberCallback?) {
        val select = intArrayOf(0)
        val dialog = AlertDialog.Builder(context)
        dialog.setTitle("提示")
        val layout: View = LayoutInflater.from(context).inflate(R.layout.dialog_number, null)
        dialog.setView(layout)
        val numberPicker = layout.findViewById<NumberPicker>(R.id.number_picker)
        numberPicker.minValue = 1
        numberPicker.maxValue = 60
        numberPicker.value = timeSelect
        numberPicker.setOnValueChangedListener { _: NumberPicker?, _: Int, newVal: Int ->
            select[0] = newVal
        }
        dialog.setPositiveButton(
            "确认"
        ) { _: DialogInterface?, _: Int ->
            callback?.select(select[0])
        }
        dialog.setNegativeButton(
            "取消"
        ) { dialog12: DialogInterface, _: Int -> dialog12.dismiss() }
        dialog.show()
    }


    fun interface NumberCallback {
        fun select(number: Int)
    }


    @JvmStatic
    fun showInputEdit(context: Context, callback: InputCallback?) {
        val dialog = AlertDialog.Builder(context)
        dialog.setTitle("Tips")
        val layout = LayoutInflater.from(context).inflate(R.layout.dialog_zigbee_input, null)
        val edittext_view = layout.findViewById<EditText>(R.id.edittext_view)
        dialog.setView(layout)
        dialog.setPositiveButton(
            context.getString(R.string.submit)
        ) { _: DialogInterface?, _: Int ->
            callback?.input(edittext_view.text.toString())
        }
        dialog.setNegativeButton(
            context.getString(R.string.zigbee_cancel)
        ) { dialog12: DialogInterface, _: Int ->
            callback?.close()
            dialog12.dismiss()
        }
        dialog.show()
    }

    interface InputCallback {
        fun input(password: String)
        fun close()
    }


    fun showPassword(context: Context, bean: PasswordBean.DataBean, callback: Callback?) {
        val dialog = AlertDialog.Builder(context).create()
        dialog.setTitle(context.getString(R.string.zigbee_select_function))
        val layout = LayoutInflater.from(context).inflate(R.layout.zigbee_dialog_list, null)
        val isOneTime = bean.oneTime == 1
        val editView = layout.findViewById<TextView>(R.id.edit_view)
        editView.setOnClickListener { v: View? ->
            callback?.edit(bean)
            dialog.dismiss()
        }
        layout.findViewById<View>(R.id.delete_view).setOnClickListener { v: View? ->
            callback?.delete(bean)
            dialog.dismiss()
        }
        val freezeView = layout.findViewById<TextView>(R.id.freeze_view)
        if (isOneTime) {
            freezeView.visibility = View.GONE
            layout.findViewById<View>(R.id.freeze_view_line).visibility = View.GONE
        } else {
            freezeView.visibility = View.VISIBLE
            layout.findViewById<View>(R.id.freeze_view_line).visibility = View.VISIBLE
        }
        val isFreeze: Boolean
        if (bean.phase == 3) {
            freezeView.text = context.getString(R.string.zigbee_unfreeze)
            editView.visibility = View.GONE
            isFreeze = false
        } else {
            freezeView.text = context.getString(R.string.zigbee_freeze)
            isFreeze = true
            if (isOneTime) {
                editView.visibility = View.GONE
            } else {
                editView.visibility = View.VISIBLE
            }
        }
        freezeView.setOnClickListener { v: View? ->
            callback?.freeze(bean, isFreeze)
            dialog.dismiss()
        }
        layout.findViewById<View>(R.id.rename_view).setOnClickListener { v: View? ->
            callback?.rename(bean)
            dialog.dismiss()
        }
        layout.findViewById<View>(R.id.showCode_view).setOnClickListener { v: View? ->
            callback?.showCode(bean)
            dialog.dismiss()
        }
        dialog.setView(layout)
        dialog.show()
    }


    interface Callback {
        fun edit(bean: PasswordBean.DataBean?)
        fun delete(bean: PasswordBean.DataBean?)
        fun rename(bean: PasswordBean.DataBean?)

        /**
         * @param bean     数据
         * @param isFreeze 冻结或者解冻
         */
        fun freeze(bean: PasswordBean.DataBean?, isFreeze: Boolean)
        fun showCode(bean: PasswordBean.DataBean?)
    }

}