package example.wen.com.testic;

import android.app.PendingIntent;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import cc.lotuscard.LotusCardDriver;
import cc.lotuscard.LotusCardParam;

import static example.wen.com.testic.utils.ConvertUtils.add;
import static example.wen.com.testic.utils.ConvertUtils.bytes2Double;
import static example.wen.com.testic.utils.ConvertUtils.double2Bytes;
import static example.wen.com.testic.utils.ConvertUtils.sub;
import static example.wen.com.testic.utils.UsbTypeCallBack.SetUsbCallBack;

public class MainActivity extends AppCompatActivity {
    private LotusCardDriver mLotusCardDriver;

    private Boolean m_bUseUsbHostApi = true;
    /**
     * chockbox 是否勾选
     */
    private Boolean m_bCanUseUsbHostApi = true;
    private String m_strDeviceNode;
    /**
     * 设备句柄
     */
    private int m_nDeviceHandle = -1;
    private Handler m_Handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
            Date curDate = new Date(System.currentTimeMillis());// ?????????
            String strDate = formatter.format(curDate);
            if (null == m_edtLog)
                return;
            String strLogs = m_edtLog.getText().toString().trim();
            if (strLogs.equals("")) {
                strLogs = strDate + " " + msg.obj.toString();
            } else {
                strLogs += "\r\n" + strDate + " " + msg.obj.toString();
            }
            m_edtLog.setText(strLogs);
            super.handleMessage(msg);
        }
    };
    private MainActivity.CardOperateThread m_CardOperateThread;
    private Boolean m_bCardOperateThreadRunning = false;
    /*********************************** UI *********************************/
    private Button m_btnTest;
    private Button m_btnAutoTest;
    private CheckBox m_chkUseUsbHostApi;
    private EditText m_edtLog;
    private TextView m_tvDeviceNode;
    private TextView m_tvMessage;
    private VideoView mVideoView;
    private String mString;
    private String mS;
    private String TAG = "MainActivity";

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        if ((m_nDeviceHandle != -1) && (null != m_CardOperateThread)) {
            if (true == m_bCardOperateThreadRunning) {
                m_CardOperateThread.cancel();
                m_CardOperateThread = null;
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            m_bCardOperateThreadRunning = !m_bCardOperateThreadRunning;

        }
        if (-1 != m_nDeviceHandle)
            mLotusCardDriver.CloseDevice(m_nDeviceHandle);
        super.onDestroy();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        m_btnTest = (Button) findViewById(R.id.btnTest);
        m_btnAutoTest = (Button) findViewById(R.id.btnAutoTest);
        m_edtLog = (EditText) findViewById(R.id.edtLog);
        m_tvDeviceNode = (TextView) findViewById(R.id.tvDeviceNode);
        m_tvMessage = (TextView) findViewById(R.id.tvMessage);
        mVideoView = (VideoView) findViewById(R.id.video_view);

        m_edtLog.setText("");

        m_chkUseUsbHostApi = (CheckBox) findViewById(R.id.chkUseUsbHostApi);

        // 设置USB读写回调 串口可以不用此操作
        m_bCanUseUsbHostApi = SetUsbCallBack();
        if (m_bCanUseUsbHostApi) {
            AddLog("Find LotusSmart IC Reader!");
            m_tvDeviceNode.setText("Device Node:" + m_strDeviceNode);
        } else {
            AddLog("Not Find LotusSmart IC Reader!");
        }
        m_chkUseUsbHostApi.setChecked(m_bCanUseUsbHostApi);

        mLotusCardDriver = new LotusCardDriver();

//        startVideo();

    }

    private void startVideo() {
        String name = "card.mp4";
        String filePath = getAssetsCacheFile(name);
        //用来设置要播放的mp4文件
        mVideoView.setVideoPath(filePath);
        //用来设置控制台样式
//        mVideoView.setMediaController(new MediaController(this));
        //用来设置起始播放位置，为0表示从开始播放
        mVideoView.seekTo(0);
        //用来设置mp4播放器是否可以聚焦
        mVideoView.requestFocus();
        //开始播放
        mVideoView.start();
        //videoView.pause();暂停播放
        //监听视频播放完的代码
        mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mPlayer) {
                // TODO Auto-generated method stub
                mPlayer.start();
                mPlayer.setLooping(true);
            }
        });
    }

    /**
     * 读取asses目录下的文件
     *
     * @param fileName
     * @return
     */
    public String getAssetsCacheFile(String fileName) {
        File cacheFile = new File(getCacheDir(), fileName);
        try {
            InputStream inputStream = getAssets().open(fileName);
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

    /**
     * 单次读取测试
     *
     * @param arg0
     */
    public void OnTestListener(View arg0) {
        if (null == mLotusCardDriver)
            return;
        if ((m_nDeviceHandle != -1) && (null != m_CardOperateThread)) {
            if (true == m_bCardOperateThreadRunning) {
                m_CardOperateThread.cancel();
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            m_bCardOperateThreadRunning = !m_bCardOperateThreadRunning;
            m_tvMessage.setText("Message:");
            m_btnAutoTest.setText("AutoTest");
        }
        /**
         * 判断USB设备是否打开
         */
        if (m_nDeviceHandle == -1)   /*??*/ {
            m_nDeviceHandle = mLotusCardDriver.OpenDevice("", 0, 0,
                    m_chkUseUsbHostApi.isChecked());
        }
        if (m_nDeviceHandle != -1) {  /*???????????*/
            AddLog("Open Device Success!");
            testIcCardReader(m_nDeviceHandle, 1, 2.5);
//			mLotusCardDriver.CloseDevice(m_nDeviceHandle);
        } else {
            AddLog("Open Device False!");
        }

    }

    /**
     * 清空日志
     *
     * @param arg0
     */
    public void OnClearLogListener(View arg0) {
        if (null == m_edtLog)
            return;
        m_edtLog.setText("");

    }

    /**
     * 连续刷卡
     *
     * @param arg0
     */
    public void OnAutoTestListener(View arg0) {
        if (-1 == m_nDeviceHandle) {
            m_nDeviceHandle = mLotusCardDriver.OpenDevice("", 0, 0,
                    m_chkUseUsbHostApi.isChecked());
        }
        if (null == m_CardOperateThread) {
            m_CardOperateThread = new MainActivity.CardOperateThread();
        }
        if ((m_nDeviceHandle != -1) && (null != m_CardOperateThread)) {
            if (false == m_bCardOperateThreadRunning) {
                m_CardOperateThread.start();
                m_btnAutoTest.setText("AutoTesting");
                m_tvMessage.setText("Message:Please Put the IC Card to Reader");
            } else {
                m_CardOperateThread.cancel();
                m_CardOperateThread = null;
                m_btnAutoTest.setText("AutoTest");
                m_tvMessage.setText("Message:");
            }
            m_bCardOperateThreadRunning = !m_bCardOperateThreadRunning;
        }
    }

    /**
     * 测试单次 读 写 卡操作
     *
     * @param nDeviceHandle 设备句柄
     * @param type          操作类型  1.读卡          2. 消费  3. 充值
     * @param moey          金额
     */
    private void testIcCardReader(int nDeviceHandle, int type, double moey) {
        boolean bResult = false;
            /*寻卡请求类型*/
        int nRequestType;
        LotusCardParam tLotusCardParam1 = new LotusCardParam();
        bResult = mLotusCardDriver.Beep(nDeviceHandle, 10);

        if (!bResult) {
            AddLog("Call Beep Error!");
            return;
        }
        AddLog("Call Beep Ok!");
        /**
         * 1.设置寻卡请求类型
         */
        nRequestType = LotusCardDriver.RT_NOT_HALT;


        /**
         * 读卡
         */
        readCard(nDeviceHandle, nRequestType, tLotusCardParam1);

        /*            解析读取到的数据               */

        double v = bytes2Double(tLotusCardParam1.arrBuffer);

        if (type == 1) {
            //显示数据
            Toast.makeText(this, String.valueOf(v), Toast.LENGTH_LONG).show();
            return;
        } else if (type == 2) {
            //消费
            double sub = sub(v, moey);
            byte[] bytes = double2Bytes(sub);
            boolean b = writeCard(nDeviceHandle, tLotusCardParam1, bytes);
            if (b) {
                testIcCardReader(m_nDeviceHandle, 1, 0);
            }
        } else if (type == 3) {
            //充值
            double sub = add(v, moey);
            byte[] bytes = double2Bytes(sub);
            boolean b = writeCard(nDeviceHandle, tLotusCardParam1, bytes);
            if (b) {
                testIcCardReader(m_nDeviceHandle, 1, 0);
            }
        }
    }

    private boolean readCard(int nDeviceHandle, int nRequestType, LotusCardParam tLotusCardParam1) {
        boolean bResult = false;
        long lCardNo = 0;
        bResult = mLotusCardDriver.GetCardNo(nDeviceHandle, nRequestType,
                tLotusCardParam1);
        if (!bResult) {
            AddLog("Call GetCardNo Error!");
            return false;
        }

        /**
         * 拼接秘钥
         */
        tLotusCardParam1.arrKeys[0] = (byte) 0xff;
        tLotusCardParam1.arrKeys[1] = (byte) 0xff;
        tLotusCardParam1.arrKeys[2] = (byte) 0xff;
        tLotusCardParam1.arrKeys[3] = (byte) 0xff;
        tLotusCardParam1.arrKeys[4] = (byte) 0xff;
        tLotusCardParam1.arrKeys[5] = (byte) 0xff;
        tLotusCardParam1.nKeysSize = 6;

        /**
         * 3.装载秘钥
         */
        bResult = mLotusCardDriver.LoadKey(nDeviceHandle, LotusCardDriver.AM_A,
                0, tLotusCardParam1);
        if (!bResult) {
            AddLog("Call LoadKey Error!");
            return false;
        }

        AddLog("Call LoadKey Ok!");
        /**
         * 装载秘钥成功后
         *
         * 4.密钥验证
         */
        bResult = mLotusCardDriver.Authentication(nDeviceHandle,
                LotusCardDriver.AM_A, 0, tLotusCardParam1);
        if (!bResult) {
            AddLog("Call Authentication(A) Error!");
            return false;
        }

        /**
         * 5. 验证通过，读取卡片内容
         */
        AddLog("Call Authentication(A) Ok!");
        bResult = mLotusCardDriver.Read(nDeviceHandle, 1, tLotusCardParam1);
        if (!bResult) {
            AddLog("Call Read Error!");
            return false;
        }

        AddLog("Call Read Ok!");
        return true;
    }

    /**
     * 写卡
     *
     * @param nDeviceHandle
     * @param tLotusCardParam1
     * @param bytes
     */
    private boolean writeCard(int nDeviceHandle, LotusCardParam tLotusCardParam1, byte[] bytes) {
        boolean bResult;
        tLotusCardParam1.arrBuffer = bytes;

         /*            缓存大小              */
//        tLotusCardParam1.nBufferSize = 16;

        /**
         * 写数据
         */

        bResult = mLotusCardDriver.Write(nDeviceHandle, 1, tLotusCardParam1);

        if (!bResult) {
            AddLog("Call Write Error!");
            //TODO  播放充值失败
            return false;
        }
        //TODO  播放充值成功
        AddLog("Call Write Ok!");
        return true;
    }


    /**
     * 添加日志
     *
     * @param strLog
     */
    private void AddLog(String strLog) {
        Log.e(TAG, strLog);
    }

    public long bytes2long(byte[] byteNum) {
        long num = 0;
        for (int ix = 3; ix >= 0; --ix) {
            num <<= 8;
            if (byteNum[ix] < 0) {
                num |= (256 + (byteNum[ix]) & 0xff);
            } else {
                num |= (byteNum[ix] & 0xff);
            }
        }
        return num;
    }


    /**
     * 充值，写入数据
     *
     * @param view
     */
    public void pay(View view) {
        testIcCardReader(m_nDeviceHandle, 3, 50);
    }

    /**
     * 卡片操作线程控制类
     * 连续消费
     */
    public class CardOperateThread extends Thread {
        volatile boolean m_bStop = false;


        public void cancel() {
            Thread.currentThread().interrupt();
            m_bStop = true;
        }

        public void run() {
            boolean bResult = false;
            int nRequestType;
            int nCount = 0;
            long lCardNo = 0;
            LotusCardParam tLotusCardParam1 = new LotusCardParam();
            /**
             * Thread.currentThread()返回当前线程
             * .isInterrupted()测试是否当前线程已被中断 中断返回true，否则返回false
             * 总的说，这句就是无限判断当前线程状态，如果没有中断，就一直执行while内容。
             */
            while (!Thread.currentThread().isInterrupted()) {        //µ±Ç°Ïß³ÌÃ»ÓÐ±»ÖÐ¶Ï£¬Ö´ÐÐËÀÑ­»·
                if (m_bStop) break;
                try {
                    nRequestType = LotusCardDriver.RT_NOT_HALT;
                    bResult = readCard(m_nDeviceHandle, nRequestType, tLotusCardParam1);

                    if (!bResult) {
                        AddLog("Call Read Card Error!");
                        Thread.sleep(200);
                        continue;
                    }


                    double v = bytes2Double(tLotusCardParam1.arrBuffer);
                    //消费
                    double sub = sub(v, 2.5);
                    byte[] bytes = double2Bytes(sub);
                    bResult = writeCard(m_nDeviceHandle, tLotusCardParam1, bytes);

                    if (!bResult) {
                        AddLog("Call Read Card Error!");
                        Thread.sleep(200);
                        continue;
                    }
                    Message msg = new Message();
                    lCardNo = bytes2long(tLotusCardParam1.arrCardNo);
                    msg.obj = "CardNo(DEC):" + lCardNo;
                    m_Handler.sendMessage(msg);
                    Message msg1 = new Message();
                    msg1.obj = "当前余额:" + sub;
                    m_Handler.sendMessage(msg1);
                    mLotusCardDriver.Beep(m_nDeviceHandle, 10);
                    mLotusCardDriver.Halt(m_nDeviceHandle);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
