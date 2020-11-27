package com.bkrc.car2019.car_tesseracttest_custom_demo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.provider.Settings;
//import android.support.v4.app.ActivityCompat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

//import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class    MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final int PHOTO_PICK = 11;// 选择照片
    private static final int PHOTO_RESULT = 12;// 拍照结果

    private static String LANGUAGE = "eng";
    private static final String ZIKU_PATH = getSDPath() + java.io.File.separator
            + "tessdata";

    private  TextView tvResult;
    private  TextView tvInfo;
    private  ImageView ivSelected;
    private  ImageView ivTreated;
    private  CheckBox chPreTreat;
    private static String textResult=" ";
    private static Bitmap bitmapSelected;
    private static Bitmap bitmapTreated;
    private static final int SHOWRESULT = 0x101;
    private static final int SHOWTREATEDIMG = 0x102;
    // 要申请的权限
    private final String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};

    // 该handler用于处理修改结果的任务
    @SuppressLint("HandlerLeak")
    public Handler myHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case SHOWRESULT:
                    if (textResult.equals(""))
                        tvResult.setText("识别失败");
                    else
                        tvResult.setText(textResult);
                    break;
                case SHOWTREATEDIMG:
                    tvResult.setText("识别中......");
                    showPicture(ivTreated, bitmapTreated);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Button btnCamera;
        Button btnSelect;
        Button btnImport;
        RadioGroup radioGroup;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvResult   = findViewById(R.id.tv_result);
        ivSelected = findViewById(R.id.iv_selected);
        ivTreated  = findViewById(R.id.iv_treated);
        btnCamera  = findViewById(R.id.btn_camera);
        btnSelect  = findViewById(R.id.btn_select);
        btnImport  = findViewById(R.id.btn_import);
        chPreTreat = findViewById(R.id.ch_pretreat);
        radioGroup = findViewById(R.id.radiogroup);
        tvInfo     = findViewById(R.id.TextView_info);

        btnCamera.setOnClickListener(new cameraButtonListener());
        btnSelect.setOnClickListener(new selectButtonListener());
        btnImport.setOnClickListener(new importButtonListener());

        // 用于设置解析语言
        radioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if(checkedId == R.id.rb_en) LANGUAGE = "eng"; else if(checkedId == R.id.rb_ch) LANGUAGE = "chi_sim";
                //在Gradle插件5.0以后，资源ID不再是final类型，应该避免在switch case语句中使用。
            }
        });

        checkReadSd();
    }

    /**
     * 判断是否需要获取sd卡写权限
     */
    private void checkReadSd() {
        // 版本判断。当手机系统大于 23 时，才有必要去判断权限是否获取
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 检查该权限是否已经获取
            int i = ContextCompat.checkSelfPermission(this, permissions[0]);
            // 权限是否已经 授权 GRANTED---授权  DINIED---拒绝
            if (i != PackageManager.PERMISSION_GRANTED) {
                // 如果没有授予该权限，就去提示用户请求
                ActivityCompat.requestPermissions(this, permissions, 1);
                // 直接跳转到权限设置界面
                Toast.makeText(this, "打开存储权限后才能进行识别", Toast.LENGTH_LONG).show();
                goToAppSetting();
            }else{
                tvInfo.setText(R.string.SD_Permission_Granted);
            }
        }
    }

    // 跳转到当前应用的设置界面
    private void goToAppSetting() {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!new File(ZIKU_PATH).exists())
            Toast.makeText(this, R.string.PLS_import_words_lib, Toast.LENGTH_SHORT).show();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == Activity.RESULT_CANCELED)
            return;

        // 处理结果
        if (requestCode == PHOTO_PICK || requestCode == PHOTO_RESULT) {
            if (requestCode == PHOTO_PICK) {
                bitmapSelected = BitmapUtils.getSmallBitmap(decodeUriAsBitmap(data.getData()));
            } else {
                bitmapSelected = BitmapUtils.getSmallBitmap(new File(ZIKU_PATH, "temp_cropped.jpg"));
            }
            if (chPreTreat.isChecked())
                tvResult.setText("预处理中......");
            else
                tvResult.setText("识别中......");
            // 显示选择的图片
            showPicture(ivSelected, bitmapSelected);

            // 新线程来处理识别
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (chPreTreat.isChecked()) {
                        bitmapTreated = ImgPretreatment
                                .doPretreatment(bitmapSelected);
                        // 释放多余资源
                        bitmapSelected.recycle();
                        bitmapSelected = null;

                        Message msg = new Message();
                        msg.what = SHOWTREATEDIMG;
                        myHandler.sendMessage(msg);
                    } else {
                        bitmapTreated = ImgPretreatment
                                .converyToGrayImg(bitmapSelected);

                        bitmapSelected.recycle();
                        bitmapSelected = null;

                        Message msg = new Message();
                        msg.what = SHOWTREATEDIMG;
                        myHandler.sendMessage(msg);
                    }
                    textResult = doOcr(bitmapTreated, LANGUAGE);
                    Message msg2 = new Message();
                    msg2.what = SHOWRESULT;
                    myHandler.sendMessage(msg2);
                }

            }).start();
        }
        try {
            this.getResources().getAssets().list("");
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    //一键导入字库
    class importButtonListener implements OnClickListener{

        @Override
        public void onClick(View v) {
            //CopyAssets();
            //tvInfo = findViewById(R.id.TextView_info);
            //tvInfo.setText("Try to import ZIKU.");

            final String[] files;
            try {
                // 获得Assets一共有多少文件,无二级目录即填写""
                files = getApplicationContext().getResources().getAssets().list("");
                Log.e("IMPORT OnClick:::",Build.VERSION.SDK_INT + "版内核；" + MainActivity.ZIKU_PATH);
            } catch (IOException e1) {
                return;
            }

            final File mWorkingPath = new File(MainActivity.ZIKU_PATH);
            Log.e("IMPORT OnClick:::",Build.VERSION.SDK_INT + "版内核；" + mWorkingPath.toString());
            // 如果文件路径不存在
            if (!mWorkingPath.exists()) {
                // 创建文件夹
                if (!mWorkingPath.mkdirs()) {
                    // 文件夹创建不成功时调用
                    Toast.makeText(getApplicationContext(), R.string.Fail_To_Create_ZIKU_check_it, Toast.LENGTH_SHORT).show();
                }
            }

            new Thread(new Runnable() {
                @Override
                public void run() {
                    for (String file : files) {
                        try {
                            // 获得每个文件的名字
                            if (file.contains(".traineddata")) {
                                File outFile = new File(mWorkingPath, file);
                                if(!outFile.exists())
                                    Log.e("IMPORT OnClick:::",Build.VERSION.SDK_INT + "版内核；*" + mWorkingPath.toString());
                                if (outFile.exists())
                                    outFile.delete();
                                InputStream in;
                                in = getAssets().open(file);// 读取字库

                                OutputStream out = new FileOutputStream(outFile);
                                // Transfer bytes from in to out
                                byte[] buf = new byte[1024];
                                int len;
                                while ((len = in.read(buf)) > 0) {
                                    out.write(buf, 0, len);     // 开始写入
                                }
                                out.flush();
                                in.close();
                                out.close();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();

        }


    }

    // 拍照识别
    class cameraButtonListener implements OnClickListener {

        @Override
        public void onClick(View arg0) {
            StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
            StrictMode.setVmPolicy(builder.build());
            //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                builder.detectFileUriExposure();
            //}
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT,
                    Uri.fromFile(new File(ZIKU_PATH, "temp_cropped.jpg")));
            intent.putExtra("outputFormat",
                    Bitmap.CompressFormat.JPEG.toString());
            startActivityForResult(intent, PHOTO_RESULT);
        }
    }

    // 从相册选取照片并裁剪
    class selectButtonListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");                                        //删掉了intent.putExtra所有语句是否实现的是图片切割
            startActivityForResult(intent, PHOTO_PICK);
        }

    }

    // 将图片显示在view中
    public static void showPicture(ImageView iv, Bitmap bmp) {
        iv.setImageBitmap(bmp);
    }

    /**
     * 进行图片识别
     *
     * @param bitmap   待识别图片
     * @param language 识别语言
     * @return 识别结果字符串
     */
    public String doOcr(Bitmap bitmap, String language) {
        TessBaseAPI baseApi = new TessBaseAPI();
        // 必须加此行，tess-two要求BMP必须为此配置
        baseApi.init(getSDPath(), language);
        System.gc();
        bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        baseApi.setImage(bitmap);

        String text = baseApi.getUTF8Text();

        baseApi.clear();

        baseApi.end();

        return text;
    }


    /**
     * 获取sd卡的路径
     *
     * @return 路径的字符串
     */
    public static String getSDPath() {

        File sdDir = null;
        boolean sdCardExist = Environment.getExternalStorageState().equals(
                android.os.Environment.MEDIA_MOUNTED); // 判断sd卡是否存在
        if (sdCardExist) {
            sdDir = Environment.getExternalStorageDirectory();// 获取外存目录
            Log.e(TAG, "getSDPath: " + sdDir);
        }
        return sdDir.toString();
    }

    /**
     * 根据URI获取位图
     *
     * @param uri uri统一资源标志符(Uniform Resource Identifier)
     * @return 对应的位图
     */
    private Bitmap decodeUriAsBitmap(Uri uri) {
        Bitmap bitmap;
        try {
            bitmap = BitmapFactory.decodeStream(getContentResolver()
                    .openInputStream(uri));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        return bitmap;
    }


    //对获取权限处理的结果

    /**
     *
     * @param requestCode   索取码
     * @param permissions   允许权限
     * @param grantResults  给予结果
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

          if(requestCode == 1)
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //检验是否获取权限，如果获取权限，外部存储会处于开放状态，会弹出一个toast提示获得授权
                    String sdCard = Environment.getExternalStorageState();
                    if (sdCard.equals(Environment.MEDIA_MOUNTED)) {
                        Toast.makeText(this, "获得授权", Toast.LENGTH_LONG).show();
                    }
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "授权失败,无法使用车牌识别", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
