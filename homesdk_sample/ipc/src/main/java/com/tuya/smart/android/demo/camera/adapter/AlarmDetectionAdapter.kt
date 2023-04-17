package com.tuya.smart.android.demo.camera.adapter

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.backends.pipeline.Fresco
import com.thingclips.drawee.view.DecryptImageView
import com.thingclips.smart.android.camera.sdk.ThingIPCSdk
import com.tuya.smart.android.demo.camera.R
import com.tuya.smart.android.demo.camera.databinding.CameraNewuiMoreMotionRecycleItemBinding
import com.tuya.smart.android.demo.camera.utils.ToastUtil
import com.tuya.smart.android.demo.camera.utils.BitmapUtils
import com.thingclips.smart.home.sdk.callback.IThingResultCallback
import com.thingclips.smart.ipc.messagecenter.bean.CameraMessageBean
import java.io.File
import java.lang.Exception
import java.util.*

/**
 * @author houqing <a href="mailto:developer@tuya.com"/>
 * @since 2021/7/27 3:09 PM
 */
class AlarmDetectionAdapter(context: Context, cameraMessageBeans: MutableList<CameraMessageBean>) : RecyclerView.Adapter<AlarmDetectionAdapter.MyViewHolder>() {
    private var cameraMessageBeans: MutableList<CameraMessageBean> = cameraMessageBeans
    private var listener: OnItemListener? = null
    private val context: Context = context
    fun setListener(listener: OnItemListener) {
        this.listener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding =  CameraNewuiMoreMotionRecycleItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
       return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val ipcVideoBean = cameraMessageBeans[position]
        holder.mTvStartTime.text = ipcVideoBean.dateTime
        holder.mTvDescription.text = ipcVideoBean.msgTypeContent
        holder.itemView.setOnLongClickListener {
                listener?.onLongClick(ipcVideoBean)
            false
        }
        holder.itemView.setOnClickListener {
                listener?.onItemClick(ipcVideoBean)
        }
        holder.showPicture(context,ipcVideoBean)
    }

    override fun getItemCount(): Int {
        return cameraMessageBeans.size
    }

    fun updateAlarmDetectionMessage(messageBeans: MutableList<CameraMessageBean>) {
        cameraMessageBeans.clear()
        cameraMessageBeans.addAll(messageBeans)
        notifyDataSetChanged()
    }

   inner class MyViewHolder(binding:CameraNewuiMoreMotionRecycleItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val mTvStartTime: TextView = binding.tvTimeRangeStartTime
        val mTvDescription: TextView = binding.tvAlarmDetectionDescription
        //don't forget Fresco.initialize(context, config)
        private val mSnapshot: DecryptImageView = binding.ivTimeRangeSnapshot
        private val mBtn: Button = binding.btnDownloadImg

        fun showPicture(context :Context,cameraMessageBean: CameraMessageBean) {
            val attachPics = cameraMessageBean.attachPics
            mSnapshot.visibility = View.VISIBLE
            if (attachPics.contains("@")) {
                val index = attachPics.lastIndexOf("@")
                try {
                    val decryption = attachPics.substring(index + 1)
                    val imageUrl = attachPics.substring(0, index)
                    mSnapshot.setImageURI(imageUrl, decryption.toByteArray())
                    //show download encryptedImg button
                    mBtn.visibility = View.VISIBLE
                    mBtn.setOnClickListener {
                        ThingIPCSdk.getTool()?.downloadEncryptedImg(imageUrl, decryption, object : IThingResultCallback<Bitmap?> {
                                override fun onSuccess(result: Bitmap?) {
                                    //                                        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Camera/";
                                    val path = Objects.requireNonNull<File>(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)).path + "/Camera"
                                    val file = File(path)
                                    if (!file.exists()) {
                                        file.mkdirs()
                                    }
                                    if (BitmapUtils.savePhotoToSDCard(result, path)) {
                                        ToastUtil.shortToast(context, context.getString(R.string.download_suc))
                                    }
                                }
                                override fun onError(errorCode: String, errorMessage: String) {
                                    Log.e("AlarmDetectionAdapter", "download encrypted img err: $errorCode$errorMessage")
                                }
                            })
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                var uri: Uri? = null
                try {
                    uri = Uri.parse(attachPics)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                mSnapshot.controller = Fresco.newDraweeControllerBuilder().setUri(uri).build()
            }
        }
    }
    interface OnItemListener {
        fun onLongClick(o: CameraMessageBean)
        fun onItemClick(o: CameraMessageBean)
    }
}