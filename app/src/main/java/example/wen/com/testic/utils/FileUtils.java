package example.wen.com.testic.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by WENGE on 2018/1/2.
 * 备注：
 */


public class FileUtils {
    /**
     * 读取asses目录下的文件
     *
     * @param fileName
     * @return
     */
    public static String getAssetsCacheFile(String fileName) {
        File cacheFile = new File(Utils.getApp().getCacheDir(), fileName);
        try {
            InputStream inputStream = Utils.getApp().getAssets().open(fileName);
            try {
                FileOutputStream outputStream = new FileOutputStream(cacheFile);
                try {
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = inputStream.read(buf)) > 0) {
                        outputStream.write(buf, 0, len);
                    }
                } finally {
                    outputStream.close();
                }
            } finally {
                inputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return cacheFile.getAbsolutePath();
    }

}
