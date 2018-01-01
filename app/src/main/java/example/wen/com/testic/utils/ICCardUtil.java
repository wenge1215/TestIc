package example.wen.com.testic.utils;

import android.util.Log;
import android.widget.Toast;

import java.sql.Date;
import java.text.SimpleDateFormat;

import cc.lotuscard.LotusCardDriver;
import cc.lotuscard.LotusCardParam;

import static example.wen.com.testic.utils.ConvertUtils.bytes2HexString;
import static example.wen.com.testic.utils.ConvertUtils.hexString2Bytes;
import static example.wen.com.testic.utils.ConvertUtils.sub;

/**
 * Created by wen on 2017/12/31.
 * 读写卡工具类
 */

public class ICCardUtil {
    private static LotusCardDriver mLotusCardDriver  = new LotusCardDriver();;
    private String mString;

    /**
     * 测试 读卡
     *
     * @param nDeviceHandle 设备句柄
     */
    private void testIcCardReader(int nDeviceHandle) {
        boolean bResult = false;
            /*寻卡请求类型*/
        int nRequestType;
        long lCardNo = 0;
        LotusCardParam tLotusCardParam1 = new LotusCardParam();
        bResult = mLotusCardDriver.Beep(nDeviceHandle, 10);
        // bResult = mLotusCardDriver.Beep(nDeviceHandle, 10);
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
         * 2.获取卡号
         */
        bResult = mLotusCardDriver.GetCardNo(nDeviceHandle, nRequestType,
                tLotusCardParam1);
        if (!bResult) {
            AddLog("Call GetCardNo Error!");
            return;
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
            return;
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
            return;
        }
        /**
         * 5. 验证通过，读取卡片内容
         */

        bResult = mLotusCardDriver.Read(nDeviceHandle, 1, tLotusCardParam1);
        if (!bResult) {
            AddLog("Call Read Error!");
            return;
        }

        AddLog("Call Read Ok!");
        /*            解析读取到的数据               */


            /**
             * 进行读写操作
             */
            byte[] arrBuffer = tLotusCardParam1.arrBuffer;
            mString = bytes2HexString(arrBuffer);

            double sub = sub(Double.valueOf(mString), 2.5);


            String s = String.valueOf(sub);
            byte[] bytes = hexString2Bytes(s);
            tLotusCardParam1.arrBuffer = bytes;




         /*            ???????????????               */
        tLotusCardParam1.nBufferSize = 16;
        Log.e("LotusCardParam1", tLotusCardParam1.toString());
        /**
         * 写数据
         */
        bResult = mLotusCardDriver.Write(nDeviceHandle, 1, tLotusCardParam1);
        if (!bResult) {
            return;
        }
    }


    /**
     * 添加日志
     *
     * @param strLog
     */
    private void AddLog(String strLog) {
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        Date curDate = new Date(System.currentTimeMillis());// ?????????
        String strDate = formatter.format(curDate);

    }

}
