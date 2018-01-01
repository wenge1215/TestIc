package example.wen.com.testic.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import example.wen.com.testic.MainActivity;

/**
 * Created by wen on 2017/12/31.
 * 开机启动
 */

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent startIintent = new Intent(context, MainActivity.class);
        startIintent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(startIintent);
    }
}
