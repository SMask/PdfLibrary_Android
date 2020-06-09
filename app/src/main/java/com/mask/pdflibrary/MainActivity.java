package com.mask.pdflibrary;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.huantansheng.easyphotos.EasyPhotos;
import com.huantansheng.easyphotos.callback.SelectCallback;
import com.huantansheng.easyphotos.models.album.entity.Photo;
import com.pdf.PDFCallback;
import com.pdf.PDFHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private View btn_choose;
    private View btn_start;
    private TextView tv_input;
    private TextView tv_output;

    private List<Uri> photoList;

    private boolean isLoading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        setListener();
        initData();
    }

    private void initView() {
        btn_choose = findViewById(R.id.btn_choose);
        btn_start = findViewById(R.id.btn_start);
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
    }

    private void initData() {
        photoList = new ArrayList<>();

        isLoading = false;

        refreshView();
    }

    /**
     * 刷新View
     */
    private void refreshView() {
        btn_start.setEnabled(!isLoading && !photoList.isEmpty());
    }

    /**
     * 选择图片
     */
    private void choose() {
        EasyPhotos.createAlbum(this, true, GlideEngine.getInstance())
                .setFileProviderAuthority(getPackageName() + ".FileProvider")
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
     * 生成PDF
     */
    private void start() {
        final File outputFile = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), System.currentTimeMillis() + ".pdf");
        new Thread(new Runnable() {
            @Override
            public void run() {
                PDFHelper.getInstance().photoToPDF(MainActivity.this, photoList, outputFile, new PDFCallback() {
                    @Override
                    public void onStart() {
                        super.onStart();

                        LogUtil.i("onStart");

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplication(), "生成PDF开始", Toast.LENGTH_SHORT).show();

                                isLoading = true;

                                refreshView();
                            }
                        });
                    }

                    @Override
                    public void onProgress(final int index, final int total) {
                        super.onProgress(index, total);

                        LogUtil.i("onProgress: " + index + "/" + total);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplication(), "生成PDF进度 " + index + "/" + total, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onSaveFile() {
                        super.onSaveFile();

                        LogUtil.i("onSaveFile");

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplication(), "生成PDF写入文件", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onSuccess(final File file) {
                        super.onSuccess(file);

                        LogUtil.i("onSuccess: " + file.getAbsolutePath());

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplication(), "生成PDF成功", Toast.LENGTH_LONG).show();

                                tv_output.setText("Output\n:" + file.getAbsolutePath());

                                isLoading = false;

                                refreshView();
                            }
                        });
                    }

                    @Override
                    public void onFail(Exception e) {
                        super.onFail(e);

                        e.printStackTrace();

                        LogUtil.e("onFail");

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplication(), "生成PDF失败", Toast.LENGTH_LONG).show();

                                tv_output.setText("Output\n:");

                                isLoading = false;

                                refreshView();
                            }
                        });
                    }
                });
            }
        }).start();
    }
}