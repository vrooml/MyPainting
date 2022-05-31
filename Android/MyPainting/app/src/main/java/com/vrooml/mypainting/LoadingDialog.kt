package com.vrooml.mypainting

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import com.vrooml.mypainting.LoadingDialog
import android.view.LayoutInflater
import android.view.View
import com.vrooml.mypainting.R
import android.widget.TextView

class LoadingDialog : Dialog {
    constructor(context: Context) : super(context) {}
    constructor(context: Context, themeResId: Int) : super(context, themeResId) {}
    protected constructor(
        context: Context,
        cancelable: Boolean,
        cancelListener: DialogInterface.OnCancelListener?
    ) : super(context, cancelable, cancelListener) {
    }

    //2,创建静态内部类Builder，将dialog的部分属性封装进该类
    class Builder(private val context: Context) {
        //提示信息
        private var message: String? = null

        //是否展示提示信息
        private var isShowMessage = true

        //是否按返回键取消
        private var isCancelable = true

        //是否取消
        private var isCancelOutside = false

        /**
         * 设置提示信息
         * @param message
         * @return
         */
        fun setMessage(message: String?): Builder {
            this.message = message
            return this
        }

        /**
         * 设置是否显示提示信息
         * @param isShowMessage
         * @return
         */
        fun setShowMessage(isShowMessage: Boolean): Builder {
            this.isShowMessage = isShowMessage
            return this
        }

        /**
         * 设置是否可以按返回键取消
         * @param isCancelable
         * @return
         */
        fun setCancelable(isCancelable: Boolean): Builder {
            this.isCancelable = isCancelable
            return this
        }

        /**
         * 设置是否可以取消
         * @param isCancelOutside
         * @return
         */
        fun setCancelOutside(isCancelOutside: Boolean): Builder {
            this.isCancelOutside = isCancelOutside
            return this
        }

        //创建Dialog
        fun create(): LoadingDialog {
            val inflater = LayoutInflater.from(context)
            val view: View = inflater.inflate(R.layout.loading_dialog, null)
            //设置带自定义主题的dialog
            val loadingDailog = LoadingDialog(context, R.style.dialog)
            val msgText = view.findViewById<View>(R.id.loading_text) as TextView
            if (isShowMessage) {
                msgText.text = message
            } else {
                msgText.visibility = View.GONE
            }
            loadingDailog.setContentView(view)
            loadingDailog.setCancelable(isCancelable)
            loadingDailog.setCanceledOnTouchOutside(isCancelOutside)
            return loadingDailog
        }
    }
}