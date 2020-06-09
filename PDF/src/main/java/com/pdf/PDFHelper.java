package com.pdf;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.util.DisplayMetrics;

import androidx.exifinterface.media.ExifInterface;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * PDF帮助类
 * Created by lishilin on 2020/06/09
 */
public class PDFHelper {

    private static class InstanceHolder {
        private static final PDFHelper instance = new PDFHelper();
    }

    public static PDFHelper getInstance() {
        return InstanceHolder.instance;
    }

    private PDFHelper() {
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        paint.setFilterBitmap(true);// 防止图片模糊

        matrix = new Matrix();
    }

    private Paint paint;
    private Matrix matrix;

    /**
     * 图片生成PDF
     *
     * @param context    context
     * @param uriList    uriList
     * @param outputFile outputFile
     * @param callback   callback
     */
    public void photoToPDF(Context context, List<Uri> uriList, File outputFile, PDFCallback callback) {
        if (outputFile == null) {
            callback.onFail(new FileNotFoundException("outputFile 非法！"));
            return;
        }
        File parentFile = outputFile.getParentFile();
        if (parentFile == null) {
            callback.onFail(new FileNotFoundException("outputFile 非法！"));
            return;
        }
        boolean mkdirs = parentFile.mkdirs();

        if (uriList == null || uriList.isEmpty()) {
            callback.onFail(new IllegalArgumentException("uriList 不能为空！"));
            return;
        }

        callback.onStart();

        int size = uriList.size();

        // 获取屏幕宽高
        DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();
        int width = displayMetrics.widthPixels;
        int height = displayMetrics.heightPixels;

        // 创建PDF
        PdfDocument pdfDocument = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(width, height, size).create();

        // 创建页
        for (int i = 0; i < size; i++) {
            callback.onProgress(i + 1, size);

            Uri uri = uriList.get(i);
            Bitmap bitmap = getBitmap(context, uri);
            if (bitmap == null) {
                continue;
            }

            PdfDocument.Page page = pdfDocument.startPage(pageInfo);
            Canvas canvas = page.getCanvas();

            int bitmapWidth = bitmap.getWidth();
            int bitmapHeight = bitmap.getHeight();

            // 图片大小变换
            matrix.reset();
            // 缩放
            if (bitmapWidth > width || bitmapHeight > height) {
                float ratioWidth = width * 1.0f / bitmapWidth;
                float ratioHeight = height * 1.0f / bitmapHeight;
                float ratio = Math.min(ratioWidth, ratioHeight);
                matrix.setScale(ratio, ratio);

                bitmapWidth *= ratio;
                bitmapHeight *= ratio;
            }
            // 位移
            int left = (width - bitmapWidth) / 2;
            int top = (height - bitmapHeight) / 2;
            matrix.postTranslate(left, top);

            canvas.drawBitmap(bitmap, matrix, paint);

            recycle(bitmap);

            pdfDocument.finishPage(page);
        }

        // 写入文件
        callback.onSaveFile();
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(outputFile);
            pdfDocument.writeTo(fileOutputStream);
            callback.onSuccess(outputFile);
        } catch (Exception e) {
            e.printStackTrace();
            callback.onFail(e);
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            pdfDocument.close();
        }
    }

    /**
     * 获取Bitmap
     *
     * @param context context
     * @param uri     uri
     * @return Bitmap
     */
    private Bitmap getBitmap(Context context, Uri uri) {
        // 获取Bitmap
        Bitmap bitmap = null;
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
            bitmap = BitmapFactory.decodeStream(inputStream);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (bitmap == null) {
            return null;
        }

        // 获取ExifInterface
        ExifInterface exif = null;
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
            if (inputStream != null) {
                exif = new ExifInterface(inputStream);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (exif == null) {
            return bitmap;
        }

        return adjustOrientation(bitmap, exif);
    }

    /**
     * 校准Bitmap方向
     *
     * @param bitmap bitmap
     * @param exif   ExifInterface
     * @return Bitmap
     */
    public static Bitmap adjustOrientation(Bitmap bitmap, ExifInterface exif) {
        if (bitmap == null || exif == null) {
            return bitmap;
        }

        // 计算旋转角度
        int angle;
        int ori = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
        switch (ori) {
            default:
                angle = 0;
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
            case ExifInterface.ORIENTATION_ROTATE_90:
                angle = 90;
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
            case ExifInterface.ORIENTATION_ROTATE_180:
                angle = 180;
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
            case ExifInterface.ORIENTATION_ROTATE_270:
                angle = 270;
                break;
        }
        if (angle == 0) {
            return bitmap;
        }

        // 旋转图片
        Matrix matrix = new Matrix();
        matrix.setRotate(angle);
        Bitmap result = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        recycle(bitmap);
        return result;
    }

    /**
     * 回收Bitmap
     *
     * @param bitmap Bitmap
     */
    public static void recycle(Bitmap bitmap) {
        if (bitmap != null) {
            if (!bitmap.isRecycled()) {
                bitmap.recycle();
            }
            bitmap = null;
        }
    }

}
