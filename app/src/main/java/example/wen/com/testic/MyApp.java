package example.wen.com.testic;

import android.app.Application;

import example.wen.com.testic.utils.Utils;

/**
 * Created by wen on 2017/12/31.
 */

public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Utils.init(this);
    }
}
