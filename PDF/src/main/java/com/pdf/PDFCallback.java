package com.pdf;

import java.io.File;

/**
 * PDF回调
 * Created by lishilin on 2020/06/09
 */
public abstract class PDFCallback {

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
     * 保存文件
     */
    public void onSaveFile() {

    }

    /**
     * 成功
     *
     * @param file 保存的文件
     */
    public void onSuccess(File file) {

    }

    /**
     * 失败
     *
     * @param e Exception
     */
    public void onFail(Exception e) {

    }

}
