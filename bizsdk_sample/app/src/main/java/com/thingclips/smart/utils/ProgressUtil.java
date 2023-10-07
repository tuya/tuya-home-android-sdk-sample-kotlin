package com.thingclips.smart.utils;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.Gravity;
import android.view.Window;
import android.widget.TextView;
import com.thing.appbizsdk.sample.R;


public class ProgressUtil {

    private static Dialog progressDialog;


    public static void showLoading(Context context) {
        showLoading(context, "");
    }

    public static void showLoading(Context context, String message) {
        if (progressDialog == null) {
            progressDialog = getSimpleProgressDialog(context, "", new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    progressDialog = null;
                }
            });
        }
        ((TextView) progressDialog.findViewById(R.id.progress_dialog_message)).setText(message);
        if (!isShowLoading()) {
            progressDialog.show();
        }
    }
    public static void showLoading(Context context, int resS) {
        showLoading(context,context.getText(resS).toString());
    }
    public static boolean isShowLoading() {
        if (progressDialog == null) {
            return false;
        }
        return progressDialog.isShowing();
    }

    public static void hideLoading() {
        if (progressDialog != null && progressDialog.getContext() != null) {
            progressDialog.hide();
            try {
                progressDialog.dismiss();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        progressDialog = null;
    }

    /**
     * 简单菊花进度
     *
     * @param mContext
     * @param msg
     * @return
     */
    public static Dialog getSimpleProgressDialog(Context mContext, String msg, DialogInterface.OnCancelListener listener) {
        Dialog dialog = new Dialog(mContext, R.style.Progress_Dialog);
        dialog.setContentView(R.layout.thing_progress_dialog_h);
        ((TextView) dialog.findViewById(R.id.progress_dialog_message)).setText(msg);
        Window win = dialog.getWindow();
        win.setGravity(Gravity.CENTER);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(false);
        if (listener != null) {
            dialog.setOnCancelListener(listener);
        }
        return dialog;
    }


}
