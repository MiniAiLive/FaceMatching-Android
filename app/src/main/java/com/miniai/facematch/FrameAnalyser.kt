package com.miniai.facematch;

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.fm.face.FaceBox
import com.fm.face.FaceSDK
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Analyser class to process frames and produce detections.
class FrameAnalyser(
    private var context: Context,
    private var boundingBoxOverlay: BoundingBoxOverlay,
    private var viewBackgroundOfMessage: View,
    private var textViewMessage: TextView,
    private var receivedImageID: Int,
) : ImageAnalysis.Analyzer {

    companion object {
        private val TAG = FrameAnalyser::class.simpleName
    }

    enum class PROC_MODE {
        VERIFY, REGISTER,
    }

    var mode = PROC_MODE.VERIFY

    private var isRunning = false
    private var isProcessing = false
    private var isRegistering = false
    public var frameInterface: FrameInferface? = null

    fun setRunning(running: Boolean) {
        isRunning = running
        viewBackgroundOfMessage.alpha = 0f
        textViewMessage.alpha = 0f
        boundingBoxOverlay.faceBoundingBoxes = null
        boundingBoxOverlay.invalidate()
    }

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(image: ImageProxy) {

        var faceImage: Bitmap? = null
        var featData: ByteArray? = null

        if(!isRunning) {
            boundingBoxOverlay.faceBoundingBoxes = null
            boundingBoxOverlay.invalidate()
            image.close()
            return
        }

        if(isRegistering) {
            image.close()
            return
        }

        if (isProcessing) {
            image.close()
            return
        }
        else {
            isProcessing = true

            // Rotated bitmap for the FaceNet model
            val frameBitmap = BitmapUtils.imageToBitmap( image.image!! , image.imageInfo.rotationDegrees )

            // Configure frameHeight and frameWidth for output2overlay transformation matrix.
            if ( !boundingBoxOverlay.areDimsInit ) {
                boundingBoxOverlay.frameHeight = frameBitmap.height
                boundingBoxOverlay.frameWidth = frameBitmap.width
            }

            var faceResult: List<FaceBox>? = FaceSDK.getInstance().detectFace(frameBitmap)
            if(!faceResult.isNullOrEmpty()) {
                if(faceResult!!.size == 1) {
                    hideMessage()
                    boundingBoxOverlay.livenessResult = 1

                    val faceRect = Rect(
                        faceResult!!.get(0).left,
                        faceResult!!.get(0).top,
                        faceResult!!.get(0).right,
                        faceResult!!.get(0).bottom
                    )
                    val cropRect = Utils.getBestRect(
                        frameBitmap.width,
                        frameBitmap.height,
                        faceRect
                    )
                    faceImage = Utils.crop(
                        frameBitmap,
                        cropRect.left,
                        cropRect.top,
                        cropRect.width(),
                        cropRect.height(),
                        120,
                        120
                    )

                    featData = FaceSDK.getInstance()
                        .extractFeature(frameBitmap, faceResult!!.get(0))

                    if (mode == PROC_MODE.REGISTER) {
                        isRegistering = true
                        faceResult = null
                        if (receivedImageID == 1) {
                            MainActivity.firstImage.faceImage = faceImage
                            MainActivity.firstImage.featData = featData
                        } else if (receivedImageID == 2) {
                            MainActivity.secondImage.faceImage = faceImage
                            MainActivity.secondImage.featData = featData
                        }
                        CoroutineScope(Dispatchers.Default).launch {
                            withContext(Dispatchers.Main) {
                                // Clear the BoundingBoxOverlay and set the new results ( boxes ) to be displayed.
                                frameInterface?.onRegister()
                            }
                        }
                    }
                } else {
                    boundingBoxOverlay.livenessResult = 2
                    showMessage(context.getString(R.string.multiple_face_detected))
                    mode = PROC_MODE.VERIFY
                }
            } else {
                if(mode == PROC_MODE.REGISTER) {
                    boundingBoxOverlay.livenessResult = 1
                    showMessage(context.getString(R.string.no_face_detected))
                    mode = PROC_MODE.VERIFY
                } else {
                    boundingBoxOverlay.livenessResult = 0
                    hideMessage()
                }
            }

            CoroutineScope( Dispatchers.Default ).launch {
                withContext( Dispatchers.Main ) {
                    // Clear the BoundingBoxOverlay and set the new results ( boxes ) to be displayed.
                    boundingBoxOverlay.faceBoundingBoxes = faceResult
                    boundingBoxOverlay.invalidate()
                }
            }

            isProcessing = false
            image.close()
        }
    }

    private fun showMessage(msg: String) {
        CoroutineScope( Dispatchers.Default ).launch {
            withContext( Dispatchers.Main ) {
                textViewMessage.text = msg
                viewBackgroundOfMessage.alpha = 1.0f
                textViewMessage.alpha = 1.0f
            }
        }
    }

    private fun hideMessage() {
        CoroutineScope( Dispatchers.Default ).launch {
            withContext( Dispatchers.Main ) {
                viewBackgroundOfMessage.alpha = 0.0f
                textViewMessage.alpha = 0.0f

            }
        }
    }
}
