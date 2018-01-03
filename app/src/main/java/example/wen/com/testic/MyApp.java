package example.wen.com.testic;

import android.app.Application;

import example.wen.com.testic.utils.SPUtils;
import example.wen.com.testic.utils.Utils;

/**
 * Created by wen on 2017/12/31.
 */

public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Utils.init(this);
        initBalance();
        SPUtils.getInstance().init(this, "balance_sp");
    }

    private void initBalance() {
        SPUtils instance = SPUtils.getInstance();
        instance.init(this, "balance_sp");
        if (null == instance.getString("balance") || instance.getString("balance").length() < 0) {
            instance.put("balance", "100");
        } else if (Double.valueOf(instance.getString("balance")) < 0) {
            instance.put("balance", "100");
        }
    }
}
