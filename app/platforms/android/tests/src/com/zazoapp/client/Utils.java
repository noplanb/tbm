package com.zazoapp.client;

import android.content.res.AssetManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by skamenkovych@codeminders.com on 3/18/2015.
 */
public final class Utils {
    private Utils() {}

    public static File createFileFromAssets(AssetManager am, String dstPath, String assetFile) throws Exception {
        InputStream inputStream = null;
        OutputStream outputStream = null;
        File f = new File(dstPath);
        if (f.exists()) {
            return f;
        }
        try{
            inputStream = am.open(assetFile);
            outputStream = new FileOutputStream(f);
            byte buffer[] = new byte[1024];
            int length = 0;

            while((length=inputStream.read(buffer)) > 0) {
                outputStream.write(buffer,0,length);
            }
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {}
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {}
            }
        }

        return f;
    }
}
