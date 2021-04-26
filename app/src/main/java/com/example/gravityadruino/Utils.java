package com.example.gravityadruino;

import android.os.Environment;

import java.io.File;

public class Utils {
    public  static String getExternalStorage(String aPath){
        String filePath = Environment.getExternalStorageDirectory()
                /*.getAbsolutePath()*/ + File.separator + aPath;
        File f = new File(Environment.getExternalStorageDirectory()
                .getAbsolutePath() + File.separator + aPath);
//        File file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
//        File filepath = new File(file,"DE Disimpan");
        return filePath;
    }


}
