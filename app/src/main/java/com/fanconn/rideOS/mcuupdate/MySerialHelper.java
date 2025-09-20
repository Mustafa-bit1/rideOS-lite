package com.fanconn.rideOS.mcuupdate;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;

import android_serialport_api.SerialPort;
import tp.xmaihh.serialport.SerialHelper;

public abstract class MySerialHelper extends SerialHelper {
    private SerialPort mSerialPort;
    private OutputStream mOutputStream;

    public MySerialHelper(String sPort, int iBaudRate) {
        super(sPort, iBaudRate);
    }
    public void setOutputStream(OutputStream outputStream) {
        this.mOutputStream = outputStream;
    }

    public FileDescriptor getFileDescriptor() throws IOException {
        if (mOutputStream instanceof FileOutputStream) {
            return ((FileOutputStream) mOutputStream).getFD();
        }
        return null;
    }
    public OutputStream getOutputStream() {
        try {
            Field outputStreamField = SerialHelper.class.getDeclaredField("mOutputStream");
            outputStreamField.setAccessible(true);
            return (OutputStream) outputStreamField.get(this);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
