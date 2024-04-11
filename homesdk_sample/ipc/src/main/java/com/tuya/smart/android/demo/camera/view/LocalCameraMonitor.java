package com.tuya.smart.android.demo.camera.view;

import android.content.Context;
import android.util.AttributeSet;

import com.thingclips.smart.camera.camerasdk.bean.ThingAudioFrameInfo;
import com.thingclips.smart.camera.camerasdk.bean.ThingVideoFrameInfo;
import com.thingclips.smart.camera.camerasdk.thingplayer.callback.IRegistorIOTCListener;
import com.thingclips.smart.ipc.yuv.monitor.YUVMonitorTextureView;

import java.nio.ByteBuffer;

/**
 * 本地相机采集画面显示
 */
public class LocalCameraMonitor extends YUVMonitorTextureView implements IRegistorIOTCListener {

    public LocalCameraMonitor(Context context) {
        this(context, null);
    }

    public LocalCameraMonitor(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void receiveFrameYUVData(int sessionId, ByteBuffer y, ByteBuffer u, ByteBuffer v, ThingVideoFrameInfo videoFrameInfo, Object camera) {
    }

    @Override
    public void receiveLocalVideoFrame(int sessionId, ByteBuffer y, ByteBuffer u, ByteBuffer v, int width, int height) {
        updateFrameYUVData(y, u, v, width, height);
    }

    @Override
    public void receivePCMData(int sessionId, ByteBuffer pcm, ThingAudioFrameInfo audioFrameInfo, Object camera) {

    }

    @Override
    public void onSessionStatusChanged(Object camera, int sessionId, int sessionStatus) {

    }
}
