package com.pdf;

import android.graphics.Bitmap;

import java.util.List;

/**
 * Pdf回调(读)
 * Created by lishilin on 2020/06/09
 */
public abstract class PdfReadCallback {

    /**
     * 开始
     */
    public void onStart() {

    }

    /**
     * 进度
     *
     * @param index 当前第几个
     * @param total 总共多少个
     */
    public void onProgress(int index, int total) {

    }

    /**
     * 成功
     *
     * @param bitmapList 读取的Bitmap
     */
    public void onSuccess(List<Bitmap> bitmapList) {

    }

    /**
     * 失败
     *
     * @param e Exception
     */
    public void onFail(Exception e) {

    }

}
