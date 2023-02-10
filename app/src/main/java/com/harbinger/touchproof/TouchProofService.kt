package com.harbinger.touchproof

import android.animation.ObjectAnimator
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import kotlin.math.abs

/**
 * Created by acorn on 2023/2/6.
 */
class TouchProofService : Service() {
    companion object {
        const val DEFAULT_WIDTH = 50
        const val DEFAULT_HEIGHT = 50
        const val TAG = "TouchProof"
    }

    private lateinit var windowManager: WindowManager
    private lateinit var layoutParams: WindowManager.LayoutParams
    private var leftX: Int = 0
    private var rightX: Int = 0

    var rootView: View? = null
    private val mH = Handler(Looper.getMainLooper())


    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        layoutParams = WindowManager.LayoutParams()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE
        }
        layoutParams.format = PixelFormat.RGBA_8888
        layoutParams.gravity = Gravity.LEFT or Gravity.TOP
        layoutParams.flags =
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        layoutParams.width = AndroidUtils.dip2px(this, DEFAULT_WIDTH.toFloat())
        layoutParams.height = AndroidUtils.dip2px(this, DEFAULT_HEIGHT.toFloat())
        leftX =0
        layoutParams.x = (AndroidUtils.getScreeenWidth(this) - layoutParams.width / 2f).toInt()
        rightX = layoutParams.x
        layoutParams.y = (0.8f * AndroidUtils.getScreeenHeight(this)).toInt()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        showFloatingWindow()
        return super.onStartCommand(intent, flags, startId)
    }

    private fun showFloatingWindow() {
        rootView = View(this)
        rootView?.background = resources.getDrawable(R.drawable.touchproof)

        windowManager.addView(rootView, layoutParams)
        mH.postDelayed(Runnable {
            if (Caches.x > 0) {
                layoutParams.x = Caches.x
            }
            if (Caches.y > 0) {
                layoutParams.y = Caches.y
            }
            windowManager.updateViewLayout(rootView, layoutParams)
        }, 500)
        rootView?.setOnTouchListener(
            FloatingOnTouchListener(leftX, rightX)
        )
        rootView?.setOnClickListener {
            Toast.makeText(this, "test", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "on click")
        }
    }

    //设置触摸listenner
    private inner class FloatingOnTouchListener(val leftX: Int, val rightX: Int) :
        View.OnTouchListener {
        private var x: Int = 0
        private var y: Int = 0
        private var movedX = 0
        private var movedY = 0
        private val CLICK_THRESHOLD = 1

        private fun toEdge(view: View) {
            if (Caches.x != leftX || Caches.x != rightX) {
                val center: Float = (rightX - leftX) / 2f
                if (Caches.x > center) {
                    //向右
                    layoutParams.x = rightX
                } else {
                    //向左
                    layoutParams.x = leftX
                }
                Caches.x = layoutParams.x
                windowManager.updateViewLayout(view, layoutParams)
            }
        }

        override fun onTouch(view: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    x = event.rawX.toInt()
                    y = event.rawY.toInt()
                    movedX = 0
                    movedY = 0
                }
                MotionEvent.ACTION_MOVE -> {
                    val nowX = event.rawX.toInt()
                    val nowY = event.rawY.toInt()
                    movedX = nowX - x
                    movedY = nowY - y
                    x = nowX
                    y = nowY
                    layoutParams.x = layoutParams.x + movedX
                    layoutParams.y = layoutParams.y + movedY
//                    Log.d("FLOAT", "position:${layoutParams.x},${layoutParams.y}")
                    windowManager.updateViewLayout(view, layoutParams)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    Caches.x = layoutParams.x
                    Caches.y = layoutParams.y
                    Log.d("FLOAT", "moved x:$movedX ,moved y:$movedY")
                    if (abs(movedX) < CLICK_THRESHOLD && abs(movedY) < CLICK_THRESHOLD) {
                        view.performClick()
                    }
                    toEdge(view)
                }
                else -> {
                }
            }
            return true
        }
    }
}