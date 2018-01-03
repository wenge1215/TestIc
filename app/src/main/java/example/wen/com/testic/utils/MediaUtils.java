package example.wen.com.testic.utils;

import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;

import java.io.IOException;

/**
 * Created by WENGE on 2018/1/2.
 * 备注：
 */


public class MediaUtils {
    public static String SUCCEED = "payS.mp3";
    public static String FAILURE = "payFailed.mp3";
    public static String SCAN_SUCCEED = "scanSucc.mp3";
    private static MediaPlayer mMediaPlayer = new MediaPlayer();

    public static void playMp3(String type){


        try {
            mMediaPlayer.reset();
            AssetFileDescriptor fileDescriptor = Utils.getApp().getAssets().openFd(type);

            mMediaPlayer.setDataSource(fileDescriptor.getFileDescriptor(),fileDescriptor.getStartOffset(),
                    fileDescriptor.getStartOffset());
            mMediaPlayer.prepare();
            mMediaPlayer.start();
            mMediaPlayer.setVolume(1f,1f);
            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
//                    mMediaPlayer.reset();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
