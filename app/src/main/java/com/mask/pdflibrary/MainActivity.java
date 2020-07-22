package com.mask.pdflibrary;

import android.app.Activity;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.huantansheng.easyphotos.EasyPhotos;
import com.huantansheng.easyphotos.callback.SelectCallback;
import com.huantansheng.easyphotos.models.album.entity.Photo;
import com.mask.photo.interfaces.SaveBitmapCallback;
import com.mask.photo.utils.BitmapUtils;
import com.pdf.PdfHelper;
import com.pdf.PdfReadCallback;
import com.pdf.PdfWriteCallback;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private Activity activity;

    private View btn_choose;
    private View btn_start;
    private View btn_display;
    private ViewGroup layout_content;
    private TextView tv_input;
    private TextView tv_output;

    private File dirFile;

    private List<Uri> photoList;
    private File saveFile;

    private boolean isLoading;

    private ExecutorService threadPool;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        activity = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        setListener();
        initData();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            LogUtil.i("Environment.isExternalStorageLegacy: " + Environment.isExternalStorageLegacy());
        }
    }

    private void initView() {
        btn_choose = findViewById(R.id.btn_choose);
        btn_start = findViewById(R.id.btn_start);
        btn_display = findViewById(R.id.btn_display);
        layout_content = findViewById(R.id.layout_content);
        tv_input = findViewById(R.id.tv_input);
        tv_output = findViewById(R.id.tv_output);
    }

    private void setListener() {
        btn_choose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                choose();
            }
        });
        btn_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                start();
            }
        });
        btn_display.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                display();
            }
        });
    }

    private void initData() {
        dirFile = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);

        photoList = new ArrayList<>();
        saveFile = new File(dirFile, "1595401981634.pdf");

        isLoading = false;

        threadPool = Executors.newCachedThreadPool();

        refreshView();
    }

    /**
     * 刷新View
     */
    private void refreshView() {
        boolean isExists = saveFile != null && saveFile.exists();

        btn_start.setEnabled(!isLoading && !photoList.isEmpty());
        btn_display.setEnabled(isExists);

        tv_output.setText("Output:\n");
        if (isExists) {
            tv_output.append(saveFile.getAbsolutePath());
        }
    }

    /**
     * 选择图片
     */
    private void choose() {
        EasyPhotos.createAlbum(activity, true, GlideEngine.getInstance())
                .setFileProviderAuthority(FileUtils.getAuthority(activity))
                .setCount(10)
                .start(new SelectCallback() {
                    @Override
                    public void onResult(ArrayList<Photo> photos, boolean isOriginal) {
                        photoList.clear();
                        tv_input.setText("Input:");
                        for (Photo photo : photos) {
                            photoList.add(photo.uri);

                            tv_input.append("\n");
                            tv_input.append(photo.path);
                        }

                        refreshView();
                    }
                });
    }

    /**
     * 生成Pdf
     */
    private void start() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                final File outputFile = new File(dirFile, System.currentTimeMillis() + ".pdf");
                PdfHelper.getInstance().photoToPdf(activity, photoList, outputFile, new PdfWriteCallback() {
                    @Override
                    public void onStart() {
                        super.onStart();

                        LogUtil.i("start onStart");

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplication(), "生成Pdf开始", Toast.LENGTH_SHORT).show();

                                isLoading = true;

                                refreshView();
                            }
                        });
                    }

                    @Override
                    public void onProgress(final int index, final int total) {
                        super.onProgress(index, total);

                        LogUtil.i("start onProgress: " + index + "/" + total);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplication(), "生成Pdf进度 " + index + "/" + total, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onSaveFile() {
                        super.onSaveFile();

                        LogUtil.i("start onSaveFile");

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplication(), "生成Pdf写入文件", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onSuccess(final File file) {
                        super.onSuccess(file);

                        LogUtil.i("start onSuccess: " + file.getAbsolutePath());

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                saveFile = file;

                                Toast.makeText(getApplication(), "生成Pdf成功", Toast.LENGTH_LONG).show();

                                isLoading = false;

                                refreshView();
                            }
                        });
                    }

                    @Override
                    public void onFail(Exception e) {
                        super.onFail(e);

                        e.printStackTrace();

                        LogUtil.e("start onFail");

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                saveFile = null;

                                Toast.makeText(getApplication(), "生成Pdf失败", Toast.LENGTH_LONG).show();

                                isLoading = false;

                                refreshView();
                            }
                        });
                    }
                });
            }
        };
        threadPool.execute(runnable);
    }

    /**
     * 显示Pdf
     */
    private void display() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                PdfHelper.getInstance().readPdf(activity, saveFile, new PdfReadCallback() {
                    @Override
                    public void onStart() {
                        super.onStart();

                        LogUtil.i("display onStart");
                    }

                    @Override
                    public void onProgress(int index, int total) {
                        super.onProgress(index, total);

                        LogUtil.i("display onProgress: " + index + "/" + total);
                    }

                    @Override
                    public void onSuccess(final List<Bitmap> bitmapList) {
                        super.onSuccess(bitmapList);

                        LogUtil.i("display onSuccess: " + bitmapList.size());

                        final int size = bitmapList.size();
                        final int count = layout_content.getChildCount();

                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // 显示图片
                                for (int i = 0; i < size; i++) {
                                    Bitmap bitmap = bitmapList.get(i);
                                    ImageView img_pdf;
                                    if (i < count) {
                                        img_pdf = (ImageView) layout_content.getChildAt(i);
                                    } else {
                                        img_pdf = new ImageView(activity);
                                        layout_content.addView(img_pdf, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                                    }
                                    img_pdf.setImageBitmap(bitmap);
                                }
                                // 移除多余的ImageView
                                if (count > size) {
                                    int num = count - size;
                                    layout_content.removeViews(size, num);
                                }
                            }
                        });

                        Runnable runnable = new Runnable() {
                            @Override
                            public void run() {
                                // 保存图片
                                for (int i = 0; i < size; i++) {
                                    Bitmap bitmap = bitmapList.get(i);
                                    final String fileName = saveFile.getName() + "_" + (i + 1) + ".png";
                                    File file = new File(dirFile, fileName);
                                    if (!file.exists()) {
                                        BitmapUtils.saveBitmapToFile(bitmap, file, new SaveBitmapCallback() {
                                            @Override
                                            public void onSuccess(File file) {
                                                super.onSuccess(file);

                                                LogUtil.i("save onSuccess: " + file.getAbsolutePath());
                                            }

                                            @Override
                                            public void onFail(Exception e) {
                                                super.onFail(e);

                                                LogUtil.e("save onFail");
                                            }
                                        });
                                    }
                                }
                            }
                        };
                        threadPool.execute(runnable);
                    }

                    @Override
                    public void onFail(Exception e) {
                        super.onFail(e);

                        LogUtil.e("display onFail");
                    }
                });
            }
        };
        threadPool.execute(runnable);
    }

}