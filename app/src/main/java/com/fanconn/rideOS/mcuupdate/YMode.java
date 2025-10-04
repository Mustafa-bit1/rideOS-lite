package com.fanconn.rideOS.mcuupdate;


import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import com.fanconn.rideOS.R;
import com.fanconn.rideOS.SerialPortManager;
import com.fanconn.rideOS.menu.menu;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class YMode {

    private SerialPortManager serialPortManager;
    private static final int TIMEOUT = 30;

    /* Protocol characters used */
    protected static final byte SOH = 0x01; /* Start Of Header */
    protected static final byte STX = 0x02; /* Start Of Text (used like SOH but means 1024 block size) */
    protected static final byte EOT = 0x04; /* End Of Transmission */
    protected static final byte ACK = 0x06; /* ACKnowlege */
    protected static final byte NAK = 0x15; /* Negative AcKnowlege */
    protected static final byte CAN = 0x18; /* CANcel character */
    protected static final byte LUCKLY = 0x08; /* CANcel character */
    protected static final byte CPMEOF = 0x1A;
    protected static final byte ST_C = 'C';

    protected static final int MAXERRORS = 100;
    private Context context;
    private byte mcuAnswerData;
    private menu activity = new menu();

    public YMode(Context context) {
        this.context = context;
        this.serialPortManager = SerialPortManager.getInstance(null, 0, 0, null);
    }

    public byte getMcuOtaAnswer() {
        return  serialPortManager.mad.getMcuOtaAnswer();
    }

    public void setMcuOtaAnswer() {
        byte mcudata = (byte) 0;
        serialPortManager.mad.setMcuOtaAnswer(mcudata);
    }

    public interface ProgressListener {
        void onProgressUpdate(int progress);
    }

    public void send(File file, ProgressListener progressListener) throws InterruptedException, IOException {
        Log.d("SendMethod", "Starting to send file: " + file.getName());
        //serialPortManager.clearSendBuffer();
        writeData();
        CRC crc = new CRC16();
        doMcuUpgrade(file,crc,progressListener);
    }

    /**
     * 发送开始升级指令,与单片机约定
     * AT+firmware=upgrade
     */
    private void writeData() {
        byte[] data = "AT+firmware=upgrade".getBytes();
        serialPortManager.sendByteSerialPort(data);
    }

    public void doMcuUpgrade(File file, CRC crc,ProgressListener progressListener) throws IOException, InterruptedException {
        byte[] mcuFileBytes = readAllBytes(file.getPath());
        List<byte[]> mcuFileBytesList = splitByteArray(mcuFileBytes, 1024);
        int totalPackages = mcuFileBytesList.size();
        String fileNameString = file.getName() + (char) 0 + file.length() + ' ';
        byte[] fileNameBytes = Arrays.copyOf(fileNameString.getBytes(), 128);
        int packCount = (int) Math.ceil((double) file.length() / 1024);
        Log.i("doMcuUpgrade", "fileNameString:" + fileNameString + " packCount:" + packCount);

        sendHandshakeFrame(fileNameBytes, crc);

        Log.d("doMcuUpgrade", "mcuFileBytesList size:" + mcuFileBytesList.size());

        for (int pkgNum = 0; pkgNum < mcuFileBytesList.size(); pkgNum++) {
            int ackReceived = 0;
            while (ackReceived == 0){
                byte[] dataBlock = mcuFileBytesList.get(pkgNum);
                if (dataBlock.length < 1024) {
                    Log.d("gh0st","dataBlock.length:" + dataBlock.length);
                    byte[] paddedDataBlock = new byte[1024];
                    System.arraycopy(dataBlock, 0, paddedDataBlock, 0, dataBlock.length);
                    Arrays.fill(paddedDataBlock, dataBlock.length, 1024, (byte) CPMEOF);
                    dataBlock = paddedDataBlock;
                }
                serialPortManager.clearSendBuffer();
                shortSleepTime(20);
                sendPackage(dataBlock, pkgNum, crc);
                ackReceived = waitForAcknowledgment(pkgNum);
                if(ackReceived == -1){
                    return;
                }
            }
            int progress = (int) (((pkgNum + 1) / (float) totalPackages) * 100);
            if (progressListener != null) {
                progressListener.onProgressUpdate(progress);
            }
        }
        sendEOT();
    }


    private void sendHandshakeFrame(byte[] fileInfo, CRC crc) throws IOException, InterruptedException {
        long startTime = System.currentTimeMillis(); // 记录开始时间
        long elapsedTime = 0; // 初始化已经经过的时间
        serialPortManager.clearSendBuffer();
        // 等待 ST_C
        while (elapsedTime < TIMEOUT * 1000) {
            mcuAnswerData = getMcuOtaAnswer();
            //Log.d("sendHandshakeFrame", "ST_C response1111: " + mcuAnswerData);
            elapsedTime = System.currentTimeMillis() - startTime;
            if (mcuAnswerData == ST_C) { // 如果收到 ST_C，继续执行
                mcuAnswerData = 0;
                setMcuOtaAnswer();
                Log.d("sendHandshakeFrame", "ST_C response: " + mcuAnswerData);
                break;
            } else {
                shortSleepTime(20);
                Log.d("sendHandshakeFrame", "ST_CIgnoring unexpected response: " + mcuAnswerData);
            }

        }

        if (elapsedTime >= TIMEOUT * 1000) {
            ((FragmentActivity) context).runOnUiThread(() -> Toast.makeText(context, R.string.tips_mcuota_update_fail, Toast.LENGTH_SHORT).show());
//            activity.runOnUiThread(() -> Toast.makeText(context, R.string.tips_mcuota_update_fail, Toast.LENGTH_SHORT).show());
            shortSleepTime(1000);
            throw new IOException("Timeout waiting for ST_C");
        }
        byte[] packet = new byte[133]; // 1 byte STX + 128 bytes data + 2 bytes CRC
        int pkgNum= 0;
        packet[0] = SOH;
        packet[1] = (byte) pkgNum;
        packet[2] = (byte) ~pkgNum;
        System.arraycopy(fileInfo, 0, packet, 3, 128);
        int crcValue = getCRC(fileInfo, crc);
        packet[131] = (byte) (crcValue >> 8);
        packet[132] = (byte) (crcValue & 0xff);
        write(packet);
        //serialPortManager.clearSendBuffer();
        shortSleepTime(20);
        // 等待 ACK
        startTime = System.currentTimeMillis(); // 重置开始时间
        elapsedTime = 0; // 重置已经经过的时间
        while (elapsedTime < TIMEOUT * 1000) {
            elapsedTime = System.currentTimeMillis() - startTime;
            mcuAnswerData = getMcuOtaAnswer();
            //Log.d("sendHandshakeFrame","ACKwaitmcuAnswerData :"+mcuAnswerData);
            switch (mcuAnswerData){
                case ACK:
                    mcuAnswerData = 0;
                    setMcuOtaAnswer();
                    return;
                case NAK:
                    mcuAnswerData = 0;
                    setMcuOtaAnswer();
                    write(packet);
                    // serialPortManager.clearSendBuffer();
                    Log.d("sendHandshakeFrame", "NAK response: " + mcuAnswerData);
                    break;
                default:
                    shortSleepTime(20);
                    Log.d("sendHandshakeFrame", "Ignoring unexpected response: " + mcuAnswerData);
            }
        }
        ((FragmentActivity) context).runOnUiThread(() -> Toast.makeText(context, R.string.tips_mcuota_update_fail, Toast.LENGTH_SHORT).show());

//        activity.runOnUiThread(() -> Toast.makeText(context, R.string.tips_mcuota_update_fail, Toast.LENGTH_SHORT).show());
        shortSleepTime(1000);
        throw new IOException("Timeout waiting for ACK");
    }

    private void sendPackage(byte[] dataBlock, int pkgNum, CRC crc) throws IOException {
        int Ymode_crc = 0;
        byte[] onePkgString = new byte[1029];
        onePkgString[0] = STX;
        onePkgString[1] = (byte) (pkgNum+1);
        onePkgString[2] = (byte) ~(pkgNum+1);
        System.arraycopy(dataBlock, 0, onePkgString, 3, dataBlock.length);
        Ymode_crc=getCRC(dataBlock, crc);
        onePkgString[dataBlock.length+3] = (byte) (Ymode_crc>>8);
        onePkgString[dataBlock.length+4]= (byte) (Ymode_crc & 0xff);
        write(onePkgString);
        // serialPortManager.clearSendBuffer();
        Log.d("doMcuUpgrade", "Package sent, pkgNum: " + pkgNum);
    }
    private int  waitForAcknowledgment(int pkgNum) throws IOException, InterruptedException {
        long startTime = System.currentTimeMillis();
        long elapsedTime = 0;
        while (elapsedTime < TIMEOUT * 1000) {
            mcuAnswerData = getMcuOtaAnswer();
            //Log.d("waitForAcknowledgment","mcuAnswerData :"+mcuAnswerData);
            elapsedTime = System.currentTimeMillis() - startTime;
            switch (mcuAnswerData) {
                case ACK:
                case EOT:
                    mcuAnswerData=0;
                    setMcuOtaAnswer();
                    Log.d("waitForAcknowledgment", "ACK received, block number: " + pkgNum);
                    return 1;
                case NAK:
                    mcuAnswerData=0;
                    setMcuOtaAnswer();
                    Log.d("waitForAcknowledgment", "NAK received, retrying block number: " + pkgNum);
                    return 0;
                case CAN:
                    mcuAnswerData=0;
                    setMcuOtaAnswer();
                    ((FragmentActivity) context).runOnUiThread(() -> Toast.makeText(context, R.string.tips_mcuota_transmission_terminated, Toast.LENGTH_SHORT).show());

//                    activity.runOnUiThread(() -> Toast.makeText(context, R.string.tips_mcuota_transmission_terminated, Toast.LENGTH_SHORT).show());
                    return -1;
                default:
                    shortSleepTime(20);
                    break;
            }
        }
        ((FragmentActivity) context).runOnUiThread(() -> Toast.makeText(context, R.string.tips_mcuota_update_fail, Toast.LENGTH_SHORT).show());

//        activity.runOnUiThread(() -> Toast.makeText(context, R.string.tips_mcuota_update_fail, Toast.LENGTH_SHORT).show());
        shortSleepTime(1000);
        throw new IOException("Timeout waiting for response");
    }

    protected void sendEOT() throws IOException, InterruptedException {
        long startTime = System.currentTimeMillis();
        long elapsedTime = 0;
        while (elapsedTime < TIMEOUT * 1000) {
            mcuAnswerData = getMcuOtaAnswer();
            elapsedTime = System.currentTimeMillis() - startTime;
            if (mcuAnswerData == LUCKLY) {
                mcuAnswerData = 0;
                setMcuOtaAnswer();
                ((FragmentActivity) context).runOnUiThread(() -> Toast.makeText(context, R.string.tips_mcuota_update_successfully, Toast.LENGTH_SHORT).show());
//                activity.runOnUiThread(() -> Toast.makeText(context, R.string.tips_mcuota_update_successfully, Toast.LENGTH_SHORT).show());
                return;
            } else if (mcuAnswerData == CAN) {
                mcuAnswerData = 0;
                setMcuOtaAnswer();
                ((FragmentActivity) context).runOnUiThread(() -> Toast.makeText(context, R.string.tips_mcuota_transmission_terminated, Toast.LENGTH_SHORT).show());

//                activity.runOnUiThread(() -> Toast.makeText(context, R.string.tips_mcuota_transmission_terminated, Toast.LENGTH_SHORT).show());
                return;
            }
        }
        ((FragmentActivity) context).runOnUiThread(() -> Toast.makeText(context, R.string.tips_mcuota_update_fail, Toast.LENGTH_SHORT).show());

//        activity.runOnUiThread(() -> Toast.makeText(context, R.string.tips_mcuota_update_fail, Toast.LENGTH_SHORT).show());
        shortSleepTime(1000);
        throw new IOException("Timeout waiting for response");
    }


    private int getCRC(byte[] block, CRC crc) throws IOException {
        long crcValue = crc.calcCRC(block);
        return (int) crcValue;
    }

    private void shortSleepTime(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            try {
                interruptTransmission();
            } catch (IOException ignore) {
            }
            throw new RuntimeException("Transmission was interrupted", e);
        }
    }

    protected void interruptTransmission() throws IOException {
        sendByte(CAN);
        sendByte(CAN);
    }

    protected void sendByte(byte b) throws IOException {
        write(new byte[]{b});
    }

    public void write(byte[] b) {
        serialPortManager.sendByteSerialPort(b);
    }

    public byte[] readAllBytes(String path) {
        byte[] fileBytes = null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                fileBytes = Files.readAllBytes(Paths.get(path));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return fileBytes;
    }


    public List<byte[]> splitByteArray(byte[] source, int subArraySize) {
        List<byte[]> result = new ArrayList<>();
        int index = 0;

        while (index < source.length) {
            if (source.length - index < subArraySize) {
                // create a subArray which size less than subArraySize
                byte[] subArray = new byte[source.length - index];
                System.arraycopy(source, index, subArray, 0, subArray.length);
                result.add(subArray);
                break;
            } else {
                byte[] subArray = new byte[subArraySize];
                System.arraycopy(source, index, subArray, 0, subArraySize);
                result.add(subArray);
                index += subArraySize;
            }
        }

        return result;
    }





}
