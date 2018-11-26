package com.sscctv.seeeyes.ptz;

/**
 * Created by trlim on 2016. 3. 2..
 *
 * 신호 레벨 관련 정보를 처리한다.
 */
public abstract class LevelMeterListener extends McuCommandListener {
    public static final int LEVEL_BURST = 0;
    public static final int LEVEL_FOCUS = 1;
    public static final int LEVEL_SYNC = 2;
    public static final int LEVEL_SDI = 3;
    public static final int LEVEL_LINK = 4;

    @Override
    public void onMcuCommand(char cmd, char data) {
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
//                Log.d("LevelMeterListener","SDI Level Change" );
                onLevelChanged(LEVEL_SDI, data);
                break;

            //case 'L':
            //    onLevelChanged(LEVEL_LINK, data);
            //    break;
        }

//                        Log.d("LevelMeterListener","cmd: " + cmd + " data: " + data);

    }

    public abstract void onLevelChanged(int level, int value);
}
