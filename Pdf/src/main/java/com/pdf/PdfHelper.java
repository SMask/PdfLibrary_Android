package com.pdf;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.DisplayMetrics;

import androidx.exifinterface.media.ExifInterface;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Pdf帮助类
 * Created by lishilin on 2020/06/09
 */
public class PdfHelper {

    private static class InstanceHolder {
        private static final PdfHelper instance = new PdfHelper();
    }

    public static PdfHelper getInstance() {
        return InstanceHolder.instance;
    }

    private PdfHelper() {
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        paint.setFilterBitmap(true);// 防止图片模糊

        matrix = new Matrix();
    }

    private Paint paint;
    private Matrix matrix;

    /**
     * 图片生成Pdf
     *
     * @param activity   activity
     * @param uriList    uriList
     * @param outputFile outputFile
     * @param callback   callback
     */
    public void photoToPdf(Activity activity, List<Uri> uriList, File outputFile, PdfWriteCallback callback) {
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

        final int size = uriList.size();

        // 获取屏幕宽高
        DisplayMetrics displayMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);
        int width = displayMetrics.widthPixels;
        int height = displayMetrics.heightPixels;

        // 创建Pdf
        PdfDocument pdfDocument = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(width, height, size).create();

        // 创建页
        for (int i = 0; i < size; i++) {
            callback.onProgress(i + 1, size);

            Uri uri = uriList.get(i);
            Bitmap bitmap = getBitmap(activity, uri);
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

        try (FileOutputStream fileOutputStream = new FileOutputStream(outputFile)) {
            pdfDocument.writeTo(fileOutputStream);
            fileOutputStream.flush();
            callback.onSuccess(outputFile);
        } catch (Exception e) {
            e.printStackTrace();
            callback.onFail(e);
        } finally {
            pdfDocument.close();
        }

    }

    /**
     * 读取Pdf
     *
     * @param context  context
     * @param file     file
     * @param callback callback
     */
    public void readPdf(Context context, File file, PdfReadCallback callback) {
        if (file == null || !file.exists()) {
            callback.onFail(new FileNotFoundException("file 不存在！"));
            return;
        }

        callback.onStart();

        try (ParcelFileDescriptor fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
             PdfRenderer pdfRenderer = new PdfRenderer(fileDescriptor)) {

            final int size = pdfRenderer.getPageCount();

            final List<Bitmap> bitmapList = new ArrayList<>(size);

            for (int i = 0; i < size; i++) {
                callback.onProgress(i + 1, size);

                final PdfRenderer.Page page = pdfRenderer.openPage(i);

                final Bitmap bitmap = Bitmap.createBitmap(page.getWidth(), page.getHeight(), Bitmap.Config.ARGB_8888);

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

                bitmapList.add(bitmap);

                page.close();
            }

            callback.onSuccess(bitmapList);
        } catch (Exception e) {
            e.printStackTrace();
            callback.onFail(e);
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
    private Bitmap adjustOrientation(Bitmap bitmap, ExifInterface exif) {
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
    private void recycle(Bitmap bitmap) {
        if (bitmap != null) {
            if (!bitmap.isRecycled()) {
                bitmap.recycle();
            }
            bitmap = null;
        }
    }

}
