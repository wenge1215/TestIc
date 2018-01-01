package example.wen.com.testic.utils;

import android.app.PendingIntent;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;

import java.util.HashMap;

import cc.lotuscard.LotusCardDriver;

import static android.content.Context.USB_SERVICE;

/**
 * Created by wen on 2017/12/31.
 */

public class UsbTypeCallBack {
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private static final int m_nVID = 1306;
    private static final int m_nPID = 20763;
    /**
     * usb接口是否可用
     *
     * @return
     */
    public static Boolean SetUsbCallBack() {
        String m_strDeviceNode;
        UsbDevice m_LotusCardDevice = null;
        UsbInterface m_LotusCardInterface = null;
        UsbDeviceConnection m_LotusCardDeviceConnection = null;
        Boolean bResult = false;
        PendingIntent pendingIntent;
        pendingIntent = PendingIntent.getBroadcast(Utils.getApp(), 0, new Intent(
                ACTION_USB_PERMISSION), 0);
        UsbManager  m_UsbManager = (UsbManager) Utils.getApp().getSystemService(USB_SERVICE);
        if (null == m_UsbManager)
            return bResult;


        HashMap<String, UsbDevice> deviceList = m_UsbManager.getDeviceList();
        if (!deviceList.isEmpty()) {
            for (UsbDevice device : deviceList.values()) {
                if ((m_nVID == device.getVendorId())
                        && (m_nPID == device.getProductId())) {
                    m_LotusCardDevice = device;
                    m_strDeviceNode = m_LotusCardDevice.getDeviceName();
                    break;
                }
            }
        }
        if (null == m_LotusCardDevice)
            return bResult;
        m_LotusCardInterface = m_LotusCardDevice.getInterface(0);
        if (null == m_LotusCardInterface)
            return bResult;
        if (false == m_UsbManager.hasPermission(m_LotusCardDevice)) {
            m_UsbManager.requestPermission(m_LotusCardDevice, pendingIntent);
        }
        UsbDeviceConnection conn = null;
        if (m_UsbManager.hasPermission(m_LotusCardDevice)) {
            conn = m_UsbManager.openDevice(m_LotusCardDevice);
        }

        if (null == conn)
            return bResult;

        if (conn.claimInterface(m_LotusCardInterface, true)) {
            m_LotusCardDeviceConnection = conn;
        } else {
            conn.close();
        }
        if (null == m_LotusCardDeviceConnection)
            return bResult;
        // ?????????????????????????????????
        LotusCardDriver.m_UsbDeviceConnection = m_LotusCardDeviceConnection;
        if (m_LotusCardInterface.getEndpoint(1) != null) {
            LotusCardDriver.m_OutEndpoint = m_LotusCardInterface.getEndpoint(1);
        }
        if (m_LotusCardInterface.getEndpoint(0) != null) {
            LotusCardDriver.m_InEndpoint = m_LotusCardInterface.getEndpoint(0);
        }
        bResult = true;
        return bResult;
    }
}
