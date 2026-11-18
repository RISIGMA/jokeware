package com.jokeware.nuisance

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Controller untuk mengendalikan lampu kilat kamera (LED Flashlight Strobe).
 */
class FlashlightController(private val context: Context) {

    private val cameraManager: CameraManager? = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private var strobeJob: Job? = null
    private var isStrobing = false
    private var targetCameraId: String? = null

    companion object {
        private const val TAG = "FlashlightController"
        private const val STROBE_ON_MS = 100L
        private const val STROBE_OFF_MS = 100L
    }

    init {
        findFlashCameraId()
    }

    private fun findFlashCameraId() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        try {
            cameraManager?.let { cm ->
                for (id in cm.cameraIdList) {
                    val characteristics = cm.getCameraCharacteristics(id)
                    val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                    val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                    if (hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK) {
                        targetCameraId = id
                        Log.d(TAG, "Selected back camera with flash: $id")
                        break
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding flash camera id", e)
        }
    }

    /**
     * Memulai kedip-kedip lampu flash (strobe).
     */
    fun startStrobe(scope: CoroutineScope) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Log.w(TAG, "Torch mode requires API 23+")
            return
        }
        val camId = targetCameraId ?: run {
            Log.w(TAG, "No suitable camera with flash found")
            return
        }

        if (isStrobing) return
        isStrobing = true

        strobeJob = scope.launch(Dispatchers.IO) {
            var state = false
            try {
                while (isActive && isStrobing) {
                    state = !state
                    cameraManager?.setTorchMode(camId, state)
                    delay(if (state) STROBE_ON_MS else STROBE_OFF_MS)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in strobe loop", e)
            } finally {
                turnOffTorch(camId)
            }
        }
        Log.d(TAG, "Flashlight strobe started")
    }

    /**
     * Menghentikan strobe dan memastikan senter mati.
     */
    fun stopStrobe() {
        isStrobing = false
        strobeJob?.cancel()
        targetCameraId?.let { turnOffTorch(it) }
        Log.d(TAG, "Flashlight strobe stopped")
    }

    private fun turnOffTorch(camId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                cameraManager?.setTorchMode(camId, false)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to turn off torch", e)
            }
        }
    }
}
