package com.vrooml.mypainting


import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.*
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.webkit.MimeTypeMap
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.SeekBar.*
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.FloatRange
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.net.toFile
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.vrooml.mypainting.databinding.ActivityEditBinding
import okhttp3.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.*
import java.lang.Math.abs
import java.nio.IntBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class EditActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityEditBinding
    private lateinit var styleList: ArrayList<PaintingStyle>
    private lateinit var image:Bitmap
    private lateinit var processedImage:Bitmap


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityEditBinding.inflate(layoutInflater)
        getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS)
//        val explode: Transition = TransitionInflater.from(this).inflateTransition(android.R.transition.explode).setDuration(400L)
//        getWindow().setEnterTransition(explode)
        setContentView(viewBinding.root)


        viewBinding.saveButton.setOnClickListener {
            finish()
        }

        val imageUri = intent.getParcelableExtra<Uri>("imageUri")
        val options = BitmapFactory.Options()
        options.inSampleSize = 3
        image = BitmapFactory.decodeFile(getRealPathFromURI(applicationContext, imageUri!!),options)

        image = rotateBitmapByDegree(
            image,
            getBitmapDegree(getRealPathFromURI(applicationContext, imageUri!!))
        )!!

        Glide
            .with(applicationContext)
            .load(image)
            .into(viewBinding.previewImage);
        val blurImage=BitmapUtil.blurBitmap(applicationContext,image,25f)
        Glide
            .with(applicationContext)
            .load(BitmapUtil.brightnessBitmap(blurImage,0.8f))
            .centerCrop()
            .into(viewBinding.previewImageBlur)



        styleList = ArrayList()
        styleList.add(PaintingStyle("Mosaic","mosaic"))
        styleList.add(PaintingStyle("CafeNight","cafe_night"))
        styleList.add(PaintingStyle("Mondrian","mondrian"))
        styleList.add(PaintingStyle("Udnie","udnie"))
        styleList.add(PaintingStyle("Ukiyo","ukiyo"))
        styleList.add(PaintingStyle("Sketch","sketch"))
        styleList.add(PaintingStyle("The Starry Night","the_starry_night"))
        styleList.add(PaintingStyle("Turner","turner"))
        styleList.add(PaintingStyle("Rain Princess","rain_princess"))
        styleList.add(PaintingStyle("The Scream","the_scream"))
        styleList.add(PaintingStyle("Monet","monet"))
        styleList.add(PaintingStyle("Candy","candy"))
        styleList.add(PaintingStyle("Edtaonisl","edtaonisl"))
        styleList.add(PaintingStyle("Georges Seurat","georges_seurat"))

        val viewPagerAdapter=ViewPagerAdapter()
        viewBinding.filterViewPager.setPageTransformer(MarginPageTransformer(30))
        viewPagerAdapter.setData(styleList,viewBinding.previewImage.context,imageUri,this)
        viewBinding.filterViewPager.adapter=viewPagerAdapter
        viewBinding.filterViewPager.offscreenPageLimit = 10

        viewBinding.seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                viewBinding.tenseText.setText("$progress")
                viewBinding.previewImageProcessed.alpha = progress.toFloat()/100f
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }
            override fun onStopTrackingTouch(seekBar: SeekBar) {
            }
        })
        viewBinding.seekBar.progress=100
        setSeekBarVisibility(false)

        viewBinding.saveButton.setOnClickListener {
//            val bitmap = (viewBinding.previewImageProcessed.background as BitmapDrawable).bitmap

            var saveBitmap = combineBitmap(processedImage,image,viewBinding.seekBar.progress.toFloat()/100)!!
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.R){
                saveImageToGallery2(applicationContext, saveBitmap);
            }else{
                saveBitmap(saveBitmap);
            }
        }


        val compositePageTransformer = CompositePageTransformer()
        compositePageTransformer.addTransformer(ScaleInTransformer())
        compositePageTransformer.addTransformer(MarginPageTransformer(30))
        viewBinding.filterViewPager.setPageTransformer(compositePageTransformer)

        viewPagerAdapter.notifyDataSetChanged()



    }

    private fun setSeekBarVisibility(visibility: Boolean) {
        if(visibility){
            viewBinding.seekBar.visibility = VISIBLE
            viewBinding.tenseText.visibility = VISIBLE
        }else{
            viewBinding.seekBar.visibility = GONE
            viewBinding.tenseText.visibility = GONE
        }
    }


    fun getRealPathFromURI(context: Context, contentURI: Uri): String? {
        val result: String?
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(contentURI, null, null, null, null)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        if (cursor == null) {
            result = contentURI.path
        } else {
            cursor.moveToFirst()
            val idx: Int = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
            result = cursor.getString(idx)
            cursor.close()
        }
        return result
    }

    fun getBitmapDegree(path: String?): Int {
        var degree = 0
        try {
            // 从指定路径下读取图片，并获取其EXIF信息
            val exifInterface = ExifInterface(path!!)
            // 获取图片的旋转信息
            val orientation = exifInterface.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> degree = 90
                ExifInterface.ORIENTATION_ROTATE_180 -> degree = 180
                ExifInterface.ORIENTATION_ROTATE_270 -> degree = 270
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return degree
    }

    fun rotateBitmapByDegree(bm: Bitmap, degree: Int): Bitmap? {
        var returnBm: Bitmap? = null

        // 根据旋转角度，生成旋转矩阵
        val matrix = Matrix()
        matrix.postRotate(degree.toFloat())
        try {
            // 将原始图片按照旋转矩阵进行旋转，并得到新的图片
            returnBm = Bitmap.createBitmap(bm, 0, 0, bm.width, bm.height, matrix, true)
        } catch (e: OutOfMemoryError) {
        }
        if (returnBm == null) {
            returnBm = bm
        }
        if (bm != returnBm) {
            bm.recycle()
        }
        return returnBm
    }

    class ViewPagerAdapter : RecyclerView.Adapter<ViewPagerAdapter.CardViewHolder>() {
        lateinit var editActivity: EditActivity
        var styleList: ArrayList<PaintingStyle> = ArrayList()
        lateinit var context: Context
        lateinit var imageUrl:Uri

        fun setData(styleList: ArrayList<PaintingStyle>,context: Context,imageUrl:Uri,editActivity: EditActivity) {
            this.context = context
            this.styleList.clear()
            this.styleList = styleList
            this.imageUrl = imageUrl
            this.editActivity = editActivity
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
            return CardViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.vp_view_item, parent, false)
            )
        }

        override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
            holder.textView.setText(styleList.get(position).name)

            val drawable=context.resources.getDrawable(context.resources.getIdentifier(styleList[position].imageUrl, "drawable", "com.vrooml.mypainting"))
            val dialog: LoadingDialog = LoadingDialog.Builder(context)
                .setMessage("加载中...")
                .setCancelable(false)
                .create()

            Glide
                .with(context)
                .load(drawable)
                .centerCrop()
                .placeholder(R.drawable.focus_ring)
                .into(holder.imageView);

            holder.cardView.setOnClickListener{
                val okHttpClient = OkHttpClient.Builder()
                    .retryOnConnectionFailure(false)
                    .connectTimeout(1, TimeUnit.MINUTES)
                    .writeTimeout(1, TimeUnit.MINUTES) // write timeout
                    .readTimeout(1, TimeUnit.MINUTES) // read timeout
                    .build()

                val retrofit = Retrofit.Builder()
                    .baseUrl("http://b23374a582.51vip.biz")
//                    .baseUrl("http://127.0.0.1:5000")
                    .addConverterFactory(GsonConverterFactory.create())//设置使用Gson解析
                    .client(okHttpClient)
                    .build()
                val retrofitInterface = retrofit.create(RetrofitInterface::class.java)

                val file = saveBitmapFile(editActivity.image, uriToFileName(imageUrl,context))

                //将路径file转化为RequestBody
                val requestBody = RequestBody.create(MediaType.parse("multipart/form-data"), file)
                //将RequestBody转化为MultipartBody.Part
                val finalRequest = MultipartBody.Part.createFormData("image", file.name, requestBody)

                val call: Call<ResponseBody> = retrofitInterface.processPicture(styleList[position].imageUrl,finalRequest)
                dialog.show()
                call.enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                        if (response.raw().code() == 200) {
//                                val processedImage:File = response.raw().body()
//                                val string = response.body()!!.bytes()
//                            val imageByteArray: ByteArray = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                                Base64.getDecoder().decode(string)
//                            } else {
//                                TODO("VERSION.SDK_INT < O")
//                            }
                            val bytes = response.body()!!.bytes()
                            Glide
                                    .with(context)
                                    .load(bytes)
                                    .into(editActivity.viewBinding.previewImageProcessed)
                            editActivity.processedImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                            editActivity.viewBinding.previewImageProcessed.visibility = VISIBLE
                            editActivity.setSeekBarVisibility(true)
                        } else {
                        Toast.makeText(context, "图片请求失败了:"+response.message(), Toast.LENGTH_SHORT).show()
                        }
                        dialog.dismiss()
                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        Toast.makeText(context, "未连接服务器，请检查网络连接", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                })
            }
        }

        override fun getItemCount(): Int {
            return styleList.size
        }

        class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var cardView: CardView
            var textView: TextView
            var imageView: ImageView
            init {
                cardView = itemView.findViewById(R.id.card_view)
                textView = itemView.findViewById(R.id.style_tv)
                imageView = itemView.findViewById(R.id.style_iv)
            }
        }

        fun saveBitmapFile(bitmap: Bitmap,filename: String): File {
            val file = File(context.cacheDir.absolutePath+filename) //将要保存图片的路径
            try {

                val bos = BufferedOutputStream(FileOutputStream(file))
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos)
                bos.flush()
                bos.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return file
        }
        @SuppressLint("Range")
        fun uriToFileName(uri:Uri, context: Context):String{
            return when(uri.scheme){
                ContentResolver.SCHEME_FILE -> uri.toFile().name
                ContentResolver.SCHEME_CONTENT->{
                    val cursor = context.contentResolver.query(uri, null, null, null, null, null)
                    cursor?.let {
                        it.moveToFirst()
                        val displayName = it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                        cursor.close()
                        displayName
                    }?:"${System.currentTimeMillis()}.${MimeTypeMap.getSingleton().getExtensionFromMimeType(context.contentResolver.getType(uri))}}"

                }
                else -> "${System.currentTimeMillis()}.${MimeTypeMap.getSingleton().getExtensionFromMimeType(context.contentResolver.getType(uri))}}"
            }
        }

    }

//    fun writeResponseBodyToDisk(imageName: String?, body: ResponseBody?) {
//        if (body == null) {
//            return
//        }
//        try {
//            val `is`: InputStream = body.byteStream()
//            val fileDr: File = File(APP_IMAGE_DIR)
//            if (!fileDr.exists()) {
//                fileDr.mkdir()
//            }
//            var file: File = File(APP_IMAGE_DIR, imageName)
//            if (file.exists()) {
//                file.delete()
//                file = File(APP_IMAGE_DIR, imageName)
//            }
//            val fos = FileOutputStream(file)
//            val bis = BufferedInputStream(`is`)
//            val buffer = ByteArray(1024)
//            var len: Int
//            while (bis.read(buffer).also { len = it } != -1) {
//                fos.write(buffer, 0, len)
//            }
//            fos.flush()
//            fos.close()
//            bis.close()
//            `is`.close()
//        } catch (e: IOException) {
//            e.printStackTrace()
//        }
//    }


    internal class ScaleInTransformer : ViewPager2.PageTransformer {
        override fun transformPage(view: View, position: Float) {
            view.elevation = -abs(position)
            val pageWidth = view.width
            val pageHeight = view.height
            view.pivotY = (pageHeight / 2).toFloat()
            view.pivotX = (pageWidth / 2).toFloat()
            if (position < -1) {
                view.scaleX = DEFAULT_MIN_SCALE
                view.scaleY = DEFAULT_MIN_SCALE
                view.pivotX = pageWidth.toFloat()
            } else if (position <= 1) {
                if (position < 0) {
                    val scaleFactor = (1 + position) * (1 - DEFAULT_MIN_SCALE) + DEFAULT_MIN_SCALE
                    view.scaleX = scaleFactor
                    view.scaleY = scaleFactor
                    view.pivotX = pageWidth * (DEFAULT_CENTER + DEFAULT_CENTER * -position)
                } else {
                    val scaleFactor = (1 - position) * (1 - DEFAULT_MIN_SCALE) + DEFAULT_MIN_SCALE
                    view.scaleX = scaleFactor
                    view.scaleY = scaleFactor
                    view.pivotX = pageWidth * ((1 - position) * DEFAULT_CENTER)
                }
            } else {
                view.pivotX = 0f
                view.scaleX = DEFAULT_MIN_SCALE
                view.scaleY = DEFAULT_MIN_SCALE
            }
        }

        companion object {
            const val DEFAULT_MIN_SCALE = 0.85f
            const val DEFAULT_CENTER = 0.5f
        }
    }

    private fun saveBitmap(bitmap: Bitmap) {
        val extStorageDirectory = Environment.getExternalStorageDirectory().toString()
        var outStream: OutputStream? = null
        val filename: String //声明文件名
        //以保存时间为文件名
        val date = Date(System.currentTimeMillis())
        val sdf = SimpleDateFormat("yyyyMMdd-HHmmss")
        filename = sdf.format(date)
        val file = File(extStorageDirectory, "$filename.JPEG") //创建文件，第一个参数为路径，第二个参数为文件名
        try {
            outStream = FileOutputStream(file) //创建输入流
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream)
            outStream.close()
            //       这三行可以实现相册更新
            val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            val uri = Uri.fromFile(file)
            intent.data = uri
            sendBroadcast(intent)
            //这个广播的目的就是更新图库，发了这个广播进入相册就可以找到你保存的图片了！*/
            Toast.makeText(
                this, "已保存到相册",
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            Toast.makeText(
                this, "保存失败:$e",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.R)
    fun saveImageToGallery2(context: Context, image: Bitmap) {
        val mImageTime = System.currentTimeMillis()
        val imageDate: String = SimpleDateFormat("yyyyMMdd-HHmmss").format(Date(mImageTime))
        val SCREENSHOT_FILE_NAME_TEMPLATE = "mypainting_%s.png"
        val mImageFileName = String.format(SCREENSHOT_FILE_NAME_TEMPLATE, imageDate)
        val values = ContentValues()
        values.put(
            MediaStore.MediaColumns.RELATIVE_PATH, (Environment.DIRECTORY_PICTURES
                    + File.separator) + "mypainting"
        ) //Environment.DIRECTORY_SCREENSHOTS:截图,图库中显示的文件夹名。"dh"
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, mImageFileName)
        values.put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
        values.put(MediaStore.MediaColumns.DATE_ADDED, mImageTime / 1000)
        values.put(MediaStore.MediaColumns.DATE_MODIFIED, mImageTime / 1000)
        values.put(
            MediaStore.MediaColumns.DATE_EXPIRES,
            (mImageTime + DateUtils.DAY_IN_MILLIS) / 1000
        )
        values.put(MediaStore.MediaColumns.IS_PENDING, 1)
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        try {
            // First, write the actual data for our screenshot
            resolver.openOutputStream(uri!!).use { out ->
                if (!image.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                    throw IOException("Failed to compress")
                }
            }
            // Everything went well above, publish it!
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            values.putNull(MediaStore.MediaColumns.DATE_EXPIRES)
            resolver.update(uri, values, null, null)
            Toast.makeText(
                context, "已保存到相册",
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: IOException) {
            resolver.delete(uri!!, null)
            Toast.makeText(
                context, "保存失败",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun combineBitmap(first: Bitmap, secend: Bitmap, @FloatRange(from = 0.0, to = 1.0) aPerc: Float): Bitmap? {
        var aPerc = aPerc
        if (aPerc < 0) aPerc = 0f
        if (aPerc > 1) aPerc = 1f
        val start = Date().time
        val firstWidth = first.width
        val firstHeight = first.height
        val secendWidth = secend.width
        val secendHeight = secend.height
        if (firstWidth != secendWidth || firstHeight != secendHeight) {
            return null
        }
        val bufferSize = firstWidth * firstHeight
        val firstCopy = IntArray(bufferSize)
        val secendCopy = IntArray(bufferSize)
        first.copyPixelsToBuffer(IntBuffer.wrap(firstCopy))
        secend.copyPixelsToBuffer(IntBuffer.wrap(secendCopy))
        val colorBuffer = IntArray(bufferSize)
        for (i in 0 until bufferSize) {
            val A =
                ((firstCopy[i] shr 24 and 0xFF) * aPerc + (secendCopy[i] shr 24 and 0xFF) * (1 - aPerc)).toInt()
            val R =
                ((firstCopy[i] and 0xFF) * aPerc + (secendCopy[i] and 0xFF) * (1 - aPerc)).toInt()
            val G =
                ((firstCopy[i] shr 8 and 0xFF) * aPerc + (secendCopy[i] shr 8 and 0xFF) * (1 - aPerc)).toInt()
            val B =
                ((firstCopy[i] shr 16 and 0xFF) * aPerc + (secendCopy[i] shr 16 and 0xFF) * (1 - aPerc)).toInt()
            colorBuffer[i] = Color.argb(A, R, G, B)
        }
        val end = Date().time
        return Bitmap.createBitmap(colorBuffer, firstWidth, firstHeight, Bitmap.Config.ARGB_8888)
    }

}

