package example.wen.com.testic;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.kongqw.serialportlibrary.SerialPortManager;
import com.kongqw.serialportlibrary.listener.OnOpenSerialPortListener;
import com.kongqw.serialportlibrary.listener.OnSerialPortDataListener;

import java.io.File;
import java.util.Arrays;

import cc.lotuscard.LotusCardDriver;
import cc.lotuscard.LotusCardParam;
import example.wen.com.testic.utils.MediaUtils;
import example.wen.com.testic.utils.SPUtils;

import static example.wen.com.testic.utils.ConvertUtils.bytes2Double;
import static example.wen.com.testic.utils.ConvertUtils.double2Bytes;
import static example.wen.com.testic.utils.ConvertUtils.sub;
import static example.wen.com.testic.utils.MediaUtils.playMp3;
import static example.wen.com.testic.utils.UsbTypeCallBack.SetUsbCallBack;

/**
 * 自动扣费界面
 */
public class TestActivity extends AppCompatActivity implements OnOpenSerialPortListener {

    private TextView mTv_balance;
    private TextView mTv_curr_moey;
    private String TAG = "TestActivity";
    private SerialPortManager mSerialPortManager;

    private double balance = 2.5;
    private Double mADalance;

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
    private TestActivity.CardOperateThread m_CardOperateThread;
    private Boolean m_bCardOperateThreadRunning = false;


    private Handler m_Handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            MediaUtils.playMp3(MediaUtils.SUCCEED);
            scanSucceed();
            super.handleMessage(msg);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        initView();
        initBalance();
        initIcCard();
        initScanCode();
        initUsbHostApi();
    }



    private void initBalance() {
        String balance = SPUtils.getInstance().getString("balance");
        mADalance = Double.valueOf(balance);

    }

    private void initView() {
        mTv_balance = (TextView) findViewById(R.id.tv_balance);
        mTv_curr_moey = (TextView) findViewById(R.id.tv_curr_moey);
    }

    private void initScanCode() {
        String div = "/dev/ttySAC2";
        File file = new File(div);

        mSerialPortManager = new SerialPortManager();

        // 打开串口
        boolean openSerialPort = mSerialPortManager.setOnOpenSerialPortListener(this)
                .setOnSerialPortDataListener(new OnSerialPortDataListener() {
                    @Override
                    public void onDataReceived(byte[] bytes) {
                        Log.i(TAG, "onDataReceived [ byte[] ]: " + Arrays.toString(bytes));
                        Log.i(TAG, "onDataReceived [ String ]: " + new String(bytes));
                        final byte[] finalBytes = bytes;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //  TODO  扫码成功
                                if (null != finalBytes && finalBytes.length > 0) {
                                    scanSucceed();
                                    playMp3(MediaUtils.SCAN_SUCCEED);
                                } else {
                                    playMp3(MediaUtils.FAILURE);
                                }
                            }
                        });
                    }

                    @Override
                    public void onDataSent(byte[] bytes) {
                        Log.i(TAG, "onDataSent [ byte[] ]: " + Arrays.toString(bytes));
                        Log.i(TAG, "onDataSent [ String ]: " + new String(bytes));
                        final byte[] finalBytes = bytes;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                            }
                        });
                    }
                })
                .openSerialPort(file, 115200);


    }

    /**
     * 扫码成功
     * 处理余额
     */
    private void scanSucceed() {
        double v = mADalance - balance;
        mTv_curr_moey.setText("当前金额为： " + mADalance);
        mTv_balance.setText("消费后余额： " + v);
        String s = String.valueOf(v);
        SPUtils.getInstance().put("balance", s);
        initBalance();
    }

    private void initIcCard() {

    }

    /*********************************** 开启串口监听 *********************************/
    @Override
    public void onSuccess(File device) {
        Toast.makeText(this, "开启串口成功：" + device.getName(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onFail(File device, Status status) {
        Toast.makeText(this, "开启串口失败", Toast.LENGTH_SHORT).show();
    }


    /**
     * 设置USB读写回调 串口可以不用此操作
     */
    private void initUsbHostApi() {
        m_bCanUseUsbHostApi = SetUsbCallBack();
        if (m_bUseUsbHostApi) {
            mLotusCardDriver = new LotusCardDriver();
            onAutoTestListener();
        }

    }

    /**
     * 连续刷卡
     */
    public void onAutoTestListener() {
        if (-1 == m_nDeviceHandle) {
            m_nDeviceHandle = mLotusCardDriver.OpenDevice("", 0, 0,
                    true);
        }
        if (null == m_CardOperateThread) {
            m_CardOperateThread = new TestActivity.CardOperateThread();
        }
        if ((m_nDeviceHandle != -1) && (null != m_CardOperateThread)) {
            if (false == m_bCardOperateThreadRunning) {
                m_CardOperateThread.start();
            } else {
                m_CardOperateThread.cancel();
                m_CardOperateThread = null;
            }
            m_bCardOperateThreadRunning = !m_bCardOperateThreadRunning;
        }
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

        @Override
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
                if (m_bStop) {
                    break;
                }
                try {
                    nRequestType = LotusCardDriver.RT_NOT_HALT;
                    bResult = readCard(m_nDeviceHandle, nRequestType, tLotusCardParam1);

                    if (!bResult) {
//                        AddLog("Call Read Card Error!");
                        Thread.sleep(200);
                        continue;
                    }

                    double v = bytes2Double(tLotusCardParam1.arrBuffer);
                    //消费
                    double sub = sub(v, 2.5);
                    byte[] bytes = double2Bytes(sub);
                    bResult = writeCard(m_nDeviceHandle, tLotusCardParam1, bytes);

                    if (!bResult) {
//                        AddLog("Call Read Card Error!");
                        Thread.sleep(200);
                        continue;
                    }
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

    private boolean readCard(int nDeviceHandle, int nRequestType, LotusCardParam tLotusCardParam1) {
        boolean bResult = false;
        long lCardNo = 0;
        bResult = mLotusCardDriver.GetCardNo(nDeviceHandle, nRequestType,
                tLotusCardParam1);
        if (!bResult) {
            Log.e(TAG, "Call GetCardNo Error!");
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
            Log.e(TAG, "Call LoadKey Error!");
            return false;
        }

        Log.e(TAG, "Call LoadKey Ok!");
        /**
         * 装载秘钥成功后
         *
         * 4.密钥验证
         */
        bResult = mLotusCardDriver.Authentication(nDeviceHandle,
                LotusCardDriver.AM_A, 0, tLotusCardParam1);
        if (!bResult) {
            Log.e(TAG, "Call Authentication(A) Error!");
            return false;
        }

        /**
         * 5. 验证通过，读取卡片内容
         */
        Log.e(TAG, "Call Authentication(A) Ok!");
        bResult = mLotusCardDriver.Read(nDeviceHandle, 1, tLotusCardParam1);
        if (!bResult) {
            Log.e(TAG, "Call Read Error!");
            return false;
        }

        Log.e(TAG, "Call Read Ok!");
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
            Log.e(TAG, "Call Write Error!");
            //TODO  播放充值失败
            return false;
        }
        //TODO  播放充值成功
        Log.e(TAG, "Call Write Ok!");
        return true;
    }


}
