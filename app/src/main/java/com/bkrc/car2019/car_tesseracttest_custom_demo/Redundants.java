package com.bkrc.car2019.car_tesseracttest_custom_demo;

import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Redundants {

    public Redundants(){

    }


    // 一键导入字库
  /*  private void importZiKu() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // 若文件夹不存在 首先创建文件夹 并把字库文件导入
                File path = new File(ZIKU_PATH);
                if (path.exists()) {
                    Log.e("字库文件存在", path.getPath());
                    return;
                }
                Log.e("字库文件不存在", path.getPath());
                path.mkdirs();
                OutputStream os = null;
                InputStream is = null;
                try {
                    // 创建本地的字库文件
                    os = new FileOutputStream(new File(ZIKU_PATH, "eng.traineddata"));
                    // 得到内部的字库文件
                    AssetManager manager = getAssets();
                    is = manager.open("eng.traineddata");
                    byte[] b = new byte[1024];
                    while (is.read(b) != -1) {
                        os.write(b);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    Toast.makeText(MainActivity.this, "导入成功", Toast.LENGTH_SHORT).show();
                    try {
                        if (os != null)
                            os.close();
                        if (is != null)
                            is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }*/


    /*private void CopyAssets() {
        final String[] files;
        try {
            // 获得Assets一共有多少文件,无二级目录即填写""
            files = this.getResources().getAssets().list("");
        } catch (IOException e1) {
            return;
        }
        final File mWorkingPath = new File(MainActivity.ZIKU_PATH);
        // 如果文件路径不存在
        if (!mWorkingPath.exists()) {
            // 创建文件夹
            if (!mWorkingPath.mkdirs()) {
                // 文件夹创建不成功时调用
                Toast.makeText(this, R.string.Fail_To_Create_ZIKU_check_it, Toast.LENGTH_SHORT).show();
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
    }*/

}
