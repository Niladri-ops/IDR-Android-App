package com.voltedge.mitra

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.util.Log

class MitraApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        setupUncaughtExceptionHandler()
        setupMainLooperProtection()
    }

    private fun setupUncaughtExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            if (isInputConnectionUnboundException(throwable) || isFrameworkPowerStatsException(throwable)) {
                Log.w(TAG, "Suppressed vendor/system framework exception on thread [${thread.name}]: ${throwable.message}", throwable)
            } else {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    private fun setupMainLooperProtection() {
        Handler(Looper.getMainLooper()).post {
            while (true) {
                try {
                    Looper.loop()
                } catch (t: Throwable) {
                    if (isInputConnectionUnboundException(t) || isFrameworkPowerStatsException(t)) {
                        Log.w(TAG, "Caught and recovered from main looper framework exception: ${t.message}", t)
                    } else {
                        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
                        if (defaultHandler != null) {
                            defaultHandler.uncaughtException(Thread.currentThread(), t)
                        } else {
                            throw t
                        }
                    }
                }
            }
        }
    }

    private fun isFrameworkPowerStatsException(throwable: Throwable?): Boolean {
        var current: Throwable? = throwable
        val visited = mutableSetOf<Throwable>()

        while ((current != null) && visited.add(current)) {
            val msg = "${current.javaClass.name}: ${current.message} ${current.localizedMessage} $current".lowercase()
            if (msg.contains("bluetoothactivityenergyinfo") ||
                msg.contains("bluetoothpowerstatscollector") ||
                msg.contains("cannot acquire bluetooth")
            ) {
                return true
            }

            for (element in current.stackTrace) {
                val className = element.className
                if (className.contains("BluetoothPowerStatsCollector") ||
                    className.contains("power.stats")
                ) {
                    return true
                }
            }
            current = current.cause
        }
        return false
    }

    private fun isInputConnectionUnboundException(throwable: Throwable?): Boolean {
        var current: Throwable? = throwable
        val visited = mutableSetOf<Throwable>()

        while ((current != null) && visited.add(current)) {
            val fullText = "${current.javaClass.name}: ${current.message} ${current.localizedMessage} $current".lowercase()
            if (fullText.contains("inputconnection") ||
                fullText.contains("input connection") ||
                fullText.contains("unbinded") ||
                fullText.contains("unbound") ||
                fullText.contains("getextractedtext") ||
                fullText.contains("honeyboard") ||
                fullText.contains("hbd") ||
                fullText.contains("v7.a") ||
                fullText.contains("w1.b") ||
                fullText.contains("v7") ||
                fullText.contains("w1")
            ) {
                return true
            }

            for (element in current.stackTrace) {
                val methodName = element.methodName.lowercase()
                val className = element.className.lowercase()

                if (methodName.contains("getextractedtext") ||
                    (methodName == "c") ||
                    (methodName == "f") ||
                    className.contains("inputconnection") ||
                    className.contains("v7") ||
                    className.contains("w1") ||
                    className.contains("honeyboard") ||
                    className.contains("hbd") ||
                    className.contains("inputmethod")
                ) {
                    return true
                }
            }

            for (suppressed in current.suppressed) {
                if (isInputConnectionUnboundException(suppressed)) {
                    return true
                }
            }

            current = current.cause
        }
        return false
    }

    companion object {
        private const val TAG = "MitraApplication"
    }
}
