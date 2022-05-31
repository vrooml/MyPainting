package com.vrooml.mypainting


import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.load.ImageHeaderParser.UNKNOWN_ORIENTATION
import com.sangcomz.fishbun.FishBun
import com.sangcomz.fishbun.FishBun.Companion.INTENT_PATH
import com.sangcomz.fishbun.adapter.image.impl.GlideAdapter
import com.vrooml.mypainting.databinding.ActivityMainBinding
import com.zhihu.matisse.Matisse
import com.zhihu.matisse.MimeType
import com.zhihu.matisse.engine.impl.GlideEngine
import java.io.FileDescriptor
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityMainBinding

    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var toggle: Int = 0

    private lateinit var cameraExecutor: ExecutorService

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)


        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        viewBinding.galleryButton.setOnClickListener {
//            Matisse.from(this@MainActivity)
//                .choose(MimeType.ofAll())
//                .countable(false)
//                .maxSelectable(1)
//                .thumbnailScale(0.85f)
////                .theme(R.style.Matisse_Custom)
//                .imageEngine(GlideEngine())
//                .forResult(200)
            FishBun.with(this)
                .setImageAdapter(GlideAdapter())
                .setMaxCount(1)
                .setPickerSpanCount(3)
                .setCamera(false)
                .setActionBarColor(Color.parseColor("#4097FF"), Color.parseColor("#4097FF"), false)
                .setActionBarTitleColor(Color.parseColor("#ffffff"))
                .setSelectCircleStrokeColor(Color.parseColor("#ffffff"))
                .startAlbumWithOnActivityResult(200)
        }

        viewBinding.switchCameraButton.setOnClickListener {
            if(toggle == 0){
                toggle = 1
            }else{
                toggle = 0;
            }
            startCamera()
        }

        viewBinding.imageCaptureButton.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN)
            //按下设置放大比例，比1小就是缩小
                viewBinding.imageCaptureButton.animate().scaleX(1.2f).scaleY(1.2f).setDuration(100).start()
            //抬起
            if (event.action == MotionEvent.ACTION_UP) {
                //重置原样
                viewBinding.imageCaptureButton.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                takePhoto()
            }
            true
        }



        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time stamped name and MediaStore entry.
        val name = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                .format(System.currentTimeMillis())
        } else {
            TODO("VERSION.SDK_INT < N")
        }
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
            .build()



        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "拍照失败: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults){

                    val intent=Intent(this@MainActivity, EditActivity::class.java)
                    intent.putExtra("imageUri",output.savedUri)
                    Log.e(TAG, "onImageSaved: "+output.savedUri)
//                    val compat = ActivityOptions.makeSceneTransitionAnimation(this@MainActivity)
                    startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this@MainActivity,viewBinding.imageCaptureButton, "sharedView").toBundle())
                }
            }
        )

    }



    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)


        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()



            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }



            imageCapture = ImageCapture.Builder().build()

            val orientationEventListener = object : OrientationEventListener(this) {
                override fun onOrientationChanged(orientation: Int) {
                    if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
                        return
                    }

                    val rotation = when (orientation) {
                        in 45 until 135 -> Surface.ROTATION_270
                        in 135 until 225 -> Surface.ROTATION_180
                        in 225 until 315 -> Surface.ROTATION_90
                        else -> Surface.ROTATION_0
                    }

                    imageCapture!!.targetRotation = rotation
                }
            }

            var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            // Select back camera as a default
            if(toggle == 0){
                cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            }else{
                cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            }

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                camera=cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))

        setupZoomAndTapToFocus()

        //flashlight
        viewBinding.flashButton.setOnClickListener{
            if(it.isActivated){
                it.isActivated=false
                imageCapture!!.flashMode=ImageCapture.FLASH_MODE_OFF
                camera?.cameraControl?.enableTorch(false)
            }else{
                it.isActivated=true
                imageCapture!!.flashMode=ImageCapture.FLASH_MODE_ON
                camera?.cameraControl?.enableTorch(true)
            }
        }



    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupZoomAndTapToFocus() {
        val listener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val currentZoomRatio: Float = camera!!.cameraInfo.zoomState.value?.zoomRatio ?: 1F
                val delta = detector.scaleFactor
                camera!!.cameraControl.setZoomRatio(currentZoomRatio * delta)
                return true
            }
        }

        val scaleGestureDetector = ScaleGestureDetector(viewBinding.viewFinder.context, listener)

        viewBinding.viewFinder.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_DOWN) {
                val factory = viewBinding.viewFinder.meteringPointFactory
                val point = factory.createPoint(event.x, event.y)
                val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                    .setAutoCancelDuration(5, TimeUnit.SECONDS)
                    .build()
                animateFocusRing(event.x,event.y)
                camera!!.cameraControl.startFocusAndMetering(action)
            }
            true
        }

    }

    private fun animateFocusRing(x: Float, y: Float) {
        val focusRing: ImageView = viewBinding.focusRing

        // Move the focus ring so that its center is at the tap location (x, y)
        val width: Int = focusRing.width
        val height: Int = focusRing.height
        focusRing.setX(x - width / 2)
        focusRing.setY(y - height / 2)

        // Show focus ring
        focusRing.setVisibility(View.VISIBLE)
        focusRing.setAlpha(1f)

        // Animate the focus ring to disappear
        focusRing.animate()
            .setStartDelay(500)
            .setDuration(300)
            .alpha(0f)
    }


    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 200 && resultCode == RESULT_OK) {
            val intent=Intent(this@MainActivity, EditActivity::class.java)
//            intent.putExtra("imageUri",Matisse.obtainResult(data)[0])
//            Log.e(TAG, "onImageSaved: "+Matisse.obtainResult(data)[0])
            val list = data!!.getParcelableArrayListExtra<Uri>(INTENT_PATH)
            intent.putExtra("imageUri", list!![0])
            Log.e(TAG, "onImageSaved: "+list!![0])
            startActivity(intent
//                , ActivityOptions.makeSceneTransitionAnimation(this).toBundle()
            )
        }
    }

    private fun convertImageProxyToBitmap(image: ImageProxy): Bitmap? {
        val byteBuffer: ByteBuffer = image.planes[0].buffer
        byteBuffer.rewind()
        val bytes = ByteArray(byteBuffer.capacity())
        byteBuffer.get(bytes)
        val clonedBytes = bytes.clone()
        return BitmapFactory.decodeByteArray(clonedBytes, 0, clonedBytes.size)
    }

    private fun getBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        val parcelFileDescriptor: ParcelFileDescriptor
        var mBitmap: Bitmap? = null
        try {
            parcelFileDescriptor = context.getContentResolver().openFileDescriptor(uri, "r")!!
            val fileDescriptor: FileDescriptor = parcelFileDescriptor.fileDescriptor
            mBitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor)
            parcelFileDescriptor.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return mBitmap
    }


    companion object {
        private const val TAG = "MyPainting"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf (
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    add(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }
}

