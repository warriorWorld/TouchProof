package com.harbinger.touchproof

import android.content.Context


class AndroidUtils {
    companion object {
        fun getScreeenWidth(context: Context): Int {
            val resources = context.getResources()
            val dm = resources.getDisplayMetrics()
            return dm.widthPixels
        }

        fun getScreeenHeight(context: Context): Int {
            val resources = context.getResources()
            val dm = resources.getDisplayMetrics()
            return dm.heightPixels
        }

        /**
         * 将dip或dp值转换为px值，保证尺寸大小不变
         *
         * @param dipValue
         * @return
         */
        fun dip2px(context: Context, dipValue: Float): Int {
            val scale = context.resources.displayMetrics.density
            return (dipValue * scale + 0.5f).toInt()
        }
    }
}