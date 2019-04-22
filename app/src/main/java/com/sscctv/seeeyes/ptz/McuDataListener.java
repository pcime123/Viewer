package com.sscctv.seeeyes.ptz;

import android.util.Log;

/**
 * Created by trlim on 2016. 3. 2..
 *
 * 신호 레벨 관련 정보를 처리한다.
 */
public abstract class McuDataListener extends McuCommandListener {
    protected static final int LEVEL_BURST = 0;
    protected static final int LEVEL_FOCUS = 1;
    protected static final int LEVEL_SYNC = 2;
    protected static final int LEVEL_SDI = 3;
    public static final int LEVEL_LINK = 4;

    @Override
    public void onMcuCommand(char cmd, char data) {
//        Log.d("McuDataListener", " Cmd: " + cmd + " data: " + data );

        switch (cmd) {
            case 'B':
                onLevelChanged(LEVEL_BURST, data);
                break;

            case 'F':
                onLevelChanged(LEVEL_FOCUS, data);
                break;

            case 'S':
                onLevelChanged(LEVEL_SYNC, data);
                break;

            case 's':
                onLevelChanged(LEVEL_SDI, data);
                break;
        }

        if (cmd == 'V') {
            switch (data) {
                case 'O':
                    onPocStarted();
                    break;

                default:
                    onPocStateChange(data);
                    break;

                case 'X':
                    onPocStopped();
                    break;

                case 'N':
                    onPocNotSupported();
                    break;
            }
        }

    }
    public abstract void onPocStarted();
    public abstract void onPocStateChange(int state);
    public abstract void onPocStopped();
    public abstract void onPocNotSupported();
    public abstract void onLevelChanged(int level, int value);
}
