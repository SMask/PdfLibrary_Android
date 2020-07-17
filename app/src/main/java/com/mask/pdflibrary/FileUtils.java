package com.mask.pdflibrary;

import android.content.Context;

/**
 * FileUtils
 * Created by lishilin on 2020/07/14
 */
public class FileUtils {

    /**
     * 获取 Authority(7.0Uri适配)
     *
     * @param context context
     * @return String Authority
     */
    public static String getAuthority(Context context) {
        return context.getPackageName() + ".FileProvider";
    }

}
