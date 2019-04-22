package com.sscctv.seeeyes.ptz;

import android.util.Log;

import com.sscctv.seeeyes.Rs485Port;
import com.sscctv.seeeyes.SpiPort;
import com.sscctv.seeeyes.VideoSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by trlim on 2016. 2. 24..
 * <p>
 * MCU 통신 기능을 구현
 */
public class McuControl {
    private static final String TAG = "McuControl";
//  private static final boolean DEBUG = false;

    private static final int BAUDRATE = 57600;
    private InputStream mInputStream;
    private OutputStream mOutputStream;
    private Rs485Port mPort;
    private ReadThread mReadThread;


    class ReadThread implements Runnable {

        private boolean stopFlag = false;

        @Override
        public void run() {
//            try {
//                Thread.sleep(5000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
            while (!stopFlag) {
                int length;
                try {
                    byte[] buffer = new byte[1];
                    if (mInputStream == null) return;
                    length = mInputStream.read(buffer);

                    if (length > 0) {
//                        Log.d(TAG, "ByteBuffer.wrap: " + Arrays.toString(buffer));
                        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, 0, 1);
//                        Log.d(TAG, "Input Stream: " + byteBuffer + " length: " + length);
                            onReceiveBuffer(byteBuffer, length);

//                        byteBuffer.clear();
                    }
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                    //TODO null;
                } catch (BufferUnderflowException e) {
                    e.printStackTrace();
//                    Log.e(TAG, "McuControl: " + e);
                    //TODO null;
                }
            }
        }

        public void stop() {
            this.stopFlag = true;
        }
    }


    public interface OnReceiveBufferListener {
        void onReceiveBuffer(ByteBuffer buffer, int length) throws InterruptedException;
    }

    private ArrayList<OnReceiveBufferListener> mBufferListeners;

    public McuControl() {
        mBufferListeners = new ArrayList<>();
    }

    public synchronized void start(String source) {
        if (mPort != null) {
            return;
        }
//        Log.i(TAG, "----- Mcu Start");

        mPort = new Rs485Port();
        mPort.open(BAUDRATE);
        mOutputStream = mPort.getOutputStream();
        mInputStream = mPort.getInputStream();

        mReadThread = new ReadThread();
        Thread thread = new Thread(mReadThread);
        thread.start();

        configure(source);


    }

    public synchronized void stop() {
        if (mPort != null) {
            mPort.closePort();
            mPort = null;
        }
        mReadThread.stop();

    }

    /**
     * MCU의 상태를 초기화한다.
     */
    private void configure(String source) {
        try {
            // 보드 종류 설정
            sendCommand('Z', 'I', 10);
            sendCommand('M', (char) 0, 10);
            sendInputSourceMode(source);

            setVpMode(false);
            // input source mode
//            Log.i(TAG, "----- Mcu configure");

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }


    /**
     * MCU 통신 명령을 보낸다.
     *
     * @param cmd  명령 코드
     * @param data 데이터 코드
     */
    private void sendCommand(byte cmd, byte data, long delay) throws IOException, InterruptedException {
        if (mPort == null) {
            Log.w(TAG, "Port is not ready. Ignoring command " + (char) cmd);
            return;
        }

        byte a = (byte) 0xA0;
        byte b = (byte) 0xFF;
        byte e = (byte) (0xFF + cmd + data);


        ByteBuffer buffer = ByteBuffer.allocateDirect(5);

        buffer.put(0, (byte) 0xA0);
        buffer.put(1, (byte) 0xFF);
        buffer.put(2, cmd);
        buffer.put(3, data);
        buffer.put(4, (byte) (0xFF + cmd + data));

//        byte a = buffer.get();
//        byte b = buffer.get();
//        byte c = buffer.get();
//        byte d = buffer.get();
//        byte e = buffer.get();

//        Log.d(TAG, "1: " + a + " 2: " + b + " 3: " + c + " 4: " + d + " 5: " + e);
//
        mOutputStream.write(a);
        mOutputStream.write(b);
        mOutputStream.write(cmd);
        mOutputStream.write(data);
        mOutputStream.write(e);


        if (delay > 0) {
            Thread.sleep(delay, 0);
        }
    }


    private void sendCommand(char cmd, char data, long delay) throws IOException, InterruptedException {
//        Log.d(TAG, "CMD: " + cmd + " DATA: " + data);
        sendCommand((byte) cmd, (byte) data, delay);
    }

    public void sendInputSourceMode(String source) throws IOException, InterruptedException {
        char data = '?';

        switch (source) {
            case VideoSource.HDMI:
                data = 'H';
                break;

            case VideoSource.SDI:
                data = 'S';
                break;

            case VideoSource.CVBS:
                data = 'C';
                break;

            case VideoSource.TVI:
                data = 'T';
                break;

            case VideoSource.AHD:
                data = 'A';
                break;

            case VideoSource.CVI:
                data = 'V';
                break;

            case VideoSource.AUTO:
                data = 'U';
                break;
            case VideoSource.HOME:
                data = 'M';
                break;
        }

        if (data != '?') {
            sendCommand('I', data, 10);
        }
//        Log.i(TAG, "----- Mcu sendInputSourceMode");
    }

    void setRs485BaudRate(int baudRate) throws IOException, InterruptedException {
        char data;

        switch (baudRate) {
            case 2400:
                data = 0;
                break;

            case 4800:
                data = 1;
                break;

            case 9600:
                data = 2;
                break;

            case 19200:
                data = 3;
                break;

            case 38400:
                data = 4;
                break;

            default:
                data = 2;
                break;
        }

        sendCommand('B', data, 10);
    }

    void sendRs485Packet(ByteBuffer packet, int length) throws IOException {
        byte data;

        for (int i = 0; i < length; i++) {
            data = packet.get();
            mOutputStream.write(data);
        }

    }

    public void setPtzMode(int mode) throws IOException, InterruptedException {
        char data;

        switch (mode) {
            case PtzMode.TX:
                data = 1;
                break;

            case PtzMode.RX:
                data = 2;
                break;

            case PtzMode.ANALYZER:
                data = 3;
                break;

            case PtzMode.UCC:
                data = 4;
                break;

            default:
                data = 0;
                break;
        }

        sendCommand('M', data, 10);
    }

    /**
     * SDI 모드에서 입력신호의 존재를 MCU에 알려준다.
     *
     * @param locked 신호가 있으면 EX = 1, HD = 2, 3G = 3, 아니면 false
     */
    public void notifySdiLockState(int locked) throws IOException, InterruptedException {
        switch (locked) {
            case '0':
                sendCommand('S', (char) 0, 10);
                break;
            case '1':
                sendCommand('S', (char) 1, 10);
                break;
            case '2':
                sendCommand('S', (char) 2, 10);
                break;
            case '3':
                sendCommand('S', (char) 3, 10);
                break;
            case '4':
                sendCommand('S', (char) 4, 10);
                break;
            case '5':
                sendCommand('S', (char) 5, 10);
                break;
            case '6':
                sendCommand('S', (char) 6, 10);
                break;
            case '7':
                sendCommand('S', (char) 7, 10);
                break;
            case '8':
                sendCommand('S', (char) 8, 10);
                break;
            default:
                sendCommand('S', (char) 0, 10);
                break;
        }
    }


//    public static final int MODE_STOP = '0';  //48
//    public static final int MODE_HD = '1';   //49
//    public static final int MODE_3G = '2';   //51
//    public static final int MODE_EX1 = '3';// 52
//    public static final int MODE_EX_3G = '4';// 53
//    public static final int MODE_EX2 = '5';// 54
//    public static final int MODE_EX_4K = '6';// 55
//    public static final int MODE_EX_TDM = '7';// 56

    public static final char CVBS_SYSTEM_NTSC = 'N';
    public static final char CVBS_SYSTEM_PAL = 'P';

    /*
     * CVBS 모드에서 NTSC/PAL 신호의 전환을 MCU에 통보한다.
     *
     * @param mode CVBS_SYSTEM_NTSC 또는 CVBS_SYSTEM_PAL
     * @throws IOException
     * @throws InterruptedException
     */
    //public void notifyCvbsSystem(char mode) throws IOException, InterruptedException {
    //    sendCommand('F', mode, 0);
    //}

    public void startPoeCheck() throws IOException, InterruptedException {
//        Log.d(TAG, "??");
        sendCommand('G', 'V', 10);
    }

    public void stopPoeCheck() throws IOException, InterruptedException {
        sendCommand('G', 'v', 10);
    }

    public void startLevelMeter() throws IOException, InterruptedException {
        sendCommand('G', 'F', 10);
    }

    public void stopLevelMeter() throws IOException, InterruptedException {
        sendCommand('G', 'X', 10);
    }

    public void setFormat(int format) throws IOException, InterruptedException {
        sendCommand('F', (char) format, 10);

    }


    public void setVpMode(boolean on) throws IOException, InterruptedException {
        sendCommand('V', on ? 'O' : 'X', 10);

    }

    //public void sendConfigure(String source) throws IOException, InterruptedException {
    //    configure(source);
    //}

    public void attemptVpTest() throws IOException, InterruptedException {
        sendCommand('V', 'T', 10);
    }

    //public void sendCoaxKey(char command) throws IOException, InterruptedException {
    //    sendCommand('K', command, 0);
    //}

    public void setTermination(boolean enable) {
        try {
            sendCommand('T', (char) (enable ? 1 : 0), 10);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public synchronized void addReceiveBufferListener(OnReceiveBufferListener listener) {
        mBufferListeners.add(listener);
//        Log.d(TAG, "addReceiveBufferListener: " + listener);
    }

    synchronized void removeReceiveBufferListener(OnReceiveBufferListener listener) {
        mBufferListeners.remove(listener);
//        Log.d(TAG, "removeReceiveBufferListener: " + listener);

    }


    private synchronized void onReceiveBuffer(ByteBuffer buffer, int length) throws InterruptedException {
        //if (DEBUG) {
//        Log.d(TAG, "onReceiveBuffer "  +buffer + ", " + length);
        //}
        for (OnReceiveBufferListener listener : mBufferListeners) {
            listener.onReceiveBuffer(buffer, length);
//                    Log.d(TAG, "onReceiveBuffer "  +buffer + ", " + length);

        }
    }
}
