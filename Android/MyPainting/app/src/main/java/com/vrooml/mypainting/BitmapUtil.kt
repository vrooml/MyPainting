package com.vrooml.mypainting

import android.content.Context
import android.graphics.*
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur


object BitmapUtil {
    //图片缩放比例
    private const val BITMAP_SCALE = 0.4f

    /**
     * 模糊图片的具体方法
     *
     * @param context 上下文对象
     * @param image   需要模糊的图片
     * @return 模糊处理后的图片
     */
    fun blurBitmap(context: Context?, image: Bitmap, blurRadius: Float): Bitmap {
        // 计算图片缩小后的长宽
        val width = Math.round(image.width * BITMAP_SCALE/8)
        val height = Math.round(image.height * BITMAP_SCALE/8)

        // 将缩小后的图片做为预渲染的图片
        val inputBitmap = Bitmap.createScaledBitmap(image, width, height, false)
        // 创建一张渲染后的输出图片
        val outputBitmap = Bitmap.createBitmap(inputBitmap)

        // 创建RenderScript内核对象
        val rs = RenderScript.create(context)
        // 创建一个模糊效果的RenderScript的工具对象
        val blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))

        // 由于RenderScript并没有使用VM来分配内存,所以需要使用Allocation类来创建和分配内存空间
        // 创建Allocation对象的时候其实内存是空的,需要使用copyTo()将数据填充进去
        val tmpIn = Allocation.createFromBitmap(rs, inputBitmap)
        val tmpOut = Allocation.createFromBitmap(rs, outputBitmap)

        // 设置渲染的模糊程度, 25f是最大模糊度
        blurScript.setRadius(blurRadius)
        // 设置blurScript对象的输入内存
        blurScript.setInput(tmpIn)
        // 将输出数据保存到输出内存中
        blurScript.forEach(tmpOut)



        // 将数据填充到Allocation中
        tmpOut.copyTo(outputBitmap)
        return outputBitmap
    }

    fun brightnessBitmap(bitmap: Bitmap, lum: Float): Bitmap{
        val lumMatrix = ColorMatrix()
        lumMatrix.setScale(lum, lum, lum, 1f)
        val bmp = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.setColorFilter(ColorMatrixColorFilter(lumMatrix))
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return bmp
    }
}