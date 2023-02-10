package com.harbinger.touchproof

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
    private var lastClick = 0L
    private val clickInterval = 300L
    private var screenWidth = 0
    private var screenHeight = 0
    private var originalWidth = 0
    private var originalHeight = 0
    private var floatingOnTouchListener: FloatingOnTouchListener? = null

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
        originalWidth = AndroidUtils.dip2px(this, DEFAULT_WIDTH.toFloat())
        originalHeight = AndroidUtils.dip2px(this, DEFAULT_HEIGHT.toFloat())
        layoutParams.width = originalWidth
        layoutParams.height = originalHeight
        leftX = 0
        screenWidth = AndroidUtils.getScreeenWidth(this)
        screenHeight =
            AndroidUtils.getScreeenHeight(this) + AndroidUtils.dip2px(this, 50f)//懒得搞 凑活儿用
        layoutParams.x = (screenWidth - layoutParams.width / 2f).toInt()
        rightX = layoutParams.x
        layoutParams.y = (0.8f * screenHeight).toInt()
        floatingOnTouchListener = FloatingOnTouchListener(leftX, rightX)
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
        rootView?.setOnTouchListener(floatingOnTouchListener)
        rootView?.setOnClickListener {
            Log.d(TAG, "on click")
            lastClick++
            if (lastClick > 1) {
                Log.d(TAG, "on multiple($lastClick) click")
                if (layoutParams.width == originalWidth) {
                    layoutParams.width = screenWidth
                    layoutParams.height = screenHeight
                    layoutParams.x = 0
                    layoutParams.y = 0
                    rootView?.setBackgroundColor(resources.getColor(R.color.touch_proof))
                    rootView?.setOnTouchListener(null)
                    windowManager.updateViewLayout(rootView, layoutParams)
                } else {
                    layoutParams.width = originalWidth
                    layoutParams.height = originalHeight
                    layoutParams.x = Caches.x
                    layoutParams.y = Caches.y
                    rootView?.background = resources.getDrawable(R.drawable.touchproof)
                    rootView?.setOnTouchListener(floatingOnTouchListener)
                    windowManager.updateViewLayout(rootView, layoutParams)
                }
            }
            mH.postDelayed(Runnable {
                lastClick = 0
            }, clickInterval)
        }

        rootView?.setOnLongClickListener {
            if (layoutParams.width == originalWidth) {
                Log.d(TAG, "long click")
                android.os.Process.killProcess(android.os.Process.myPid())
            }
            return@setOnLongClickListener false
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
        private val LONG_CLICK_INTERVAL = 800L
        private val longClickHandler = Handler(Looper.getMainLooper())

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
                    longClickHandler.postDelayed(Runnable {
                        VibratorUtil.Vibrate(view.context,100)
                        view.performLongClick()
                    }, LONG_CLICK_INTERVAL)
                }
                MotionEvent.ACTION_MOVE -> {
                    longClickHandler.removeCallbacksAndMessages(null)
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
                    longClickHandler.removeCallbacksAndMessages(null)
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