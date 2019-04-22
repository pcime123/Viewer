package com.sscctv.seeeyesmonitor;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.StatFs;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.sscctv.seeeyes.VideoSource;
import com.sscctv.seeeyes.ptz.McuControl;
import com.sscctv.seeeyes.ptz.McuDataListener;
import com.sscctv.seeeyes.ptz.PtzAnalyzer;
import com.sscctv.seeeyes.ptz.PtzControl;
import com.sscctv.seeeyes.ptz.PtzMode;
import com.sscctv.seeeyes.ptz.PtzReader;
import com.sscctv.seeeyes.ptz.PtzWriter;
import com.sscctv.seeeyes.ptz.UtcProtocol;
import com.sscctv.seeeyes.ptz.UtcWriter;
import com.sscctv.seeeyes.video.AnalogInput;
import com.sscctv.seeeyes.video.HdmiInput;
import com.sscctv.seeeyes.video.SdiInput;
import com.sscctv.seeeyes.video.VideoInput;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import static android.media.AudioManager.FLAG_PLAY_SOUND;
import static com.sscctv.seeeyes.video.VideoInput.Listener;
import static com.sscctv.seeeyes.video.VideoInput.SignalInfo;
import static com.sscctv.seeeyes.video.VideoInput.SnapshotCallback;
import static com.sscctv.seeeyes.video.VideoInput.getSystem;

//import android.media.AudioRecord;
//import android.widget.FrameLayout;
//import static com.sscctv.seeeyes.video.CameraInput.getMode;


/**
 * View 계층 구조
 * <p>
 * activity_main
 * app_bar_main
 * content_main
 * surface(mSurfaceView)   : 카메라 비디오 입력 화면 표시
 * content_overlay             : 비디오 화면 위에 오버레이로 표시되는 레이어
 * fragment_ptz_overlay(PtzOverlayFragment)
 * LinearLayout        : PTZ 화면 상단에 표시하는 현재 설정값
 * label_mode
 * value_mode
 * label_protocol
 * value_protocol
 * label_address
 * value_address
 * label_baudrate
 * value_baudrate
 * ptz_contents        : PTZ 수신 데이터 표시를 위한 영역
 * fragment_ptz_rx_contents
 * ptz_rx_contents
 * fragment_ptz_analyzer_contents
 * ptz_analyzer_header
 * ptz_analyzer_contents
 * ptz_controls        : PTZ 화면 하단에 표시하는 도움말 및 버튼
 * fragment_pan_tilt_control
 * fragment_zoom_focus_control
 * fragment_osd_control
 * fragment_ptz_rx_control
 * fragment_ptz_analyzer_control
 * fragment_record_overlay : 녹화/스냅샷 모드에서 표시하는 오버레이 레이어
 * fragment_poc_overlay    : PoC 모드에서 표시하는 오버레이 레이어
 * drawer_button           : main_menu 토글 버튼
 * main_menu
 */
public class MainActivity extends AppCompatActivity
        implements SettingsFragment.OnFragmentInteractionListener,
        PanTiltControlFragment.OnFragmentInteractionListener,
        ZoomFocusControlFragment.OnFragmentInteractionListener,
        OsdControlFragment.OnFragmentInteractionListener,
        PtzRxControlFragment.OnFragmentInteractionListener,
        PtzAnalyzerControlFragment.OnFragmentInteractionListener,
        RecordOverlayFragment.OnFragmentInteractionListener,
        SharedPreferences.OnSharedPreferenceChangeListener,
        PreferenceFragmentCompat.OnPreferenceDisplayDialogCallback, SurfaceHolder.Callback, View.OnTouchListener {
    private static final String TAG = "Viewer_1904181300";

    private static final String EXTRA_SOURCE = "com.sscctv.seeeyesmonitor.source";

    private static final int MODE_INVALID = -1;
    private static final int MODE_VIEW = 0;
    private static final int MODE_PTZ = 1;
    private static final int MODE_RECORD = 2;
    private static final int MODE_SNAPSHOT = 3;
    private static final int MODE_POC = 4;

    private static final int AUDIO_MODE_IN_NONE = 0;
    private static final int AUDIO_MODE_IN_HDMI = 1;
    private static final int AUDIO_MODE_IN_SDI = 2;

    private static final int STAT_READY = 0;
    private static final int STAT_SNAPSHOT = 1;
    private static final int STAT_RECORD = 2;
//    private static final int STAT_FULL = 3;

    private static final String LEVEL_METER_FRAGMENT_TAG = "level_meter_fragment";
    private static final String MENU_FRAGMENT_TAG = "menu_fragment";
    private static final String OVERLAY_FRAGMENT_TAG = "overlay_fragment";
    private static final String PANEL_FRAGMENT_TAG = "panel_fragment";

//    private static final String GPIO_PIN_48V_CTRL = "PE11";

    private VideoSource mSource;

//    private boolean mLockTouchScreen;

    private VideoInput mVideoInput;
    private SurfaceView mSurfaceView;

    private Listener mVideoInputListener;

    private VideoInput.SignalInfo mSignalInfo;

    private TextView mSignalInfoView;
    private TextView mPocState;
    private LinearLayout mPocOverlay;
    private RelativeLayout mPocDialog;

    private Runnable mHideSignalInfoTask;

    private ViewGroup mNoSignalView;

    private ViewGroup mLevelMeterView;

    private ViewGroup mCrcStatsView;
    private TextView mCrcCountsText;
    private TextView mCrcTimeText;
    private long mCrcStartTime;
    private Runnable mUpdateCrcTimeTask;

    private Button tdm_set;
    private ViewGroup mTdmSetupView;
    private Button tdm_ch1, tdm_ch2, tdm_ch3, tdm_ch4, tdm_ch5, tdm_ch6, tdm_ch7, tdm_ch8;

    private DrawerLayout mDrawer;
    private View mMainMenu;
    private View mSubMenu;
    private View mDrawerButton;
    private ListView mDrawerList;
    private ArrayAdapter<CharSequence> mMenuAdaptor;

    private PtzMenuFragment mPtzSubmenuFragment;
    private SettingsFragment mUserSubmenuFragment;
    private SettingsFragment mUserSubmenuFragmentHDMI;

    private Fragment mCurrentSubmenuFragment;

    private View mOverlayView;
    private PtzOverlayFragment mPtzOverlay;

    private McuControl mMcuControl;

    private int mMode;
    private int mPendingMode;

    private PtzControl mPtzControl;
    private PtzSettings mPtzSettings;

    private int isCaptureReady;
    private TextView mRecordingTimeText;
    private long mRecordingStartTime;
    private Runnable mUpdateRecordingTimeTask;

    private HandlerThread mVideoThread;
    private Handler mVideoHandler;

    private Toast mActiveToast;

    private String initSource;
    private String sourceId;
    private DataOutputStream opt;

    private boolean btnChk = true;
    private TimerTask tdmTask;
    private Timer tdmTimer;
    private SdiInput _sdiInput;
    private int cvbs_Input;
    private int utc_value;

    private TextView zoomView;
    private Timer zoomTimer;
    private TimerTask zoomTask;

    private AudioManager audioManager;

    private boolean pocMode;

    public static final int MODE_CHECK = 0;
    public static final int MODE_POWER = 1;
    public static final int MODE_PSE = 2;

    public static final int STATE_POWER_OFF = 0;
    //    public static final int STATE_STB_CHK = 1;
//    public static final int STATE_LINK_CHK = 2;
    public static final int STATE_LINK_OK = 3;
    //    public static final int STATE_POWER_ON = 4;
//    public static final int STATE_POWER_BRK = 5;
//    public static final int STATE_POWER_CHK = 6;
    public static final int STATE_C_OPEN = 7;
//    public static final int STATE_C_SHORT = 8;
//    public static final int STATE_C_OVER = 9;
//    public static final int STATE_C_NONE = 10;

    private int _mode;
    private Button yesButton;

    private SettingsContentObserver mSettingsContentObserver;

    private Thread mPlayThread;
    private TextView test;

    private boolean onPocDialog;
    private boolean onPtzMenu;

    private int index;
    private String mediaPath;
    private boolean setLevelMeter;

    private File mediaFile;
    private boolean isPocMode = false;
    private String getLocale;


    @SuppressLint("StaticFieldLeak")
    public static Context mContext;

    @SuppressLint("ClickableViewAccessibility")

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        Log.i(TAG, "Viewer App Data: 201903261134   Version: 2.0.0");

        mContext = this;
        // 설정값을 초기화한다.
        SharedPreferences defaultValueSp = this.getSharedPreferences("_has_set_default_values", 0);
        if (!defaultValueSp.getBoolean("_has_set_default_values", false)) {
//            Log.i(TAG, "_has_set_default_values = " + defaultValueSp.getBoolean("_has_set_default_values", false));
            PreferenceManager.setDefaultValues(this, R.xml.prefs_ptz, true);
            PreferenceManager.setDefaultValues(this, R.xml.prefs_settings, true);
        }

        SharedPreferences pref = getSharedPreferences("poc_gallery", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean("poc_gallery", true);
        editor.apply();

//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
//                WindowManager.LayoutParams.FLAG_FULLSCREEN);
//
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
//                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (getIntent().hasExtra(EXTRA_SOURCE)) {
            initSource = (getIntent().getStringExtra(EXTRA_SOURCE)).toUpperCase();
        } else {
            initSource = VideoSource.AUTO;
        }

        setContentView(R.layout.activity_main);

        Locale systemLocale = getApplicationContext().getResources().getConfiguration().locale;
        getLocale = systemLocale.getLanguage();
//        Log.d(TAG, "getLocale: " + getLocale);

        mSurfaceView = findViewById(R.id.surface);
        mSurfaceView.getHolder().addCallback(this);
//        mSurfaceView.setZOrderOnTop(true);
//        mSurfaceView.setBackgroundColor(ContextCompat.getColor(getApplication(), R.color.colorLevelGood));

        mSignalInfoView = findViewById(R.id.video_info);
        mPocOverlay = findViewById(R.id.poc_overlay);
        mPocState = findViewById(R.id.poc_state);
        mPocDialog = findViewById(R.id.poc_dialog);


        mNoSignalView = findViewById(R.id.no_signal);
        mMcuControl = new McuControl();

        tdm_set = findViewById(R.id.tdm_set_button);
        mTdmSetupView = findViewById(R.id.tdm_slot);
        mTdmSetupView.setOnTouchListener(this);

        tdm_ch1 = findViewById(R.id.tdm_ch1);
        tdm_ch2 = findViewById(R.id.tdm_ch2);
        tdm_ch3 = findViewById(R.id.tdm_ch3);
        tdm_ch4 = findViewById(R.id.tdm_ch4);
        tdm_ch5 = findViewById(R.id.tdm_ch5);
        tdm_ch6 = findViewById(R.id.tdm_ch6);
        tdm_ch7 = findViewById(R.id.tdm_ch7);
        tdm_ch8 = findViewById(R.id.tdm_ch8);


        zoomView = findViewById(R.id.test_view);

        gpioPortSet();
        startMenuOverlayHandler();
        startInputInfoCheckHandler();
        startMcuHandler();
        tdm_set.setFocusable(false);

        //        getLayoutValue();
//        mSurfaceView.setLayoutParams(new FrameLayout.LayoutParams(1920 , 1200));
        yesButton = findViewById(R.id.yes);
        yesButton.setOnClickListener(v -> {
            switch (_mode) {
                case MODE_CHECK:
                    onStartPocCheck();
                    break;

                case MODE_POWER:
                    onApplyPocPower();
                    break;
                case MODE_PSE:
                    break;
            }

        });

        Button noButton = findViewById(R.id.no);
        noButton.setOnClickListener(v -> {
            pocMode = false;
            switch (_mode) {
                case MODE_CHECK:
                    onCancelPocCheck();
                    break;

                case MODE_POWER:
                    onRemovePocPower();
                    break;

                case MODE_PSE:
                    break;
            }

            hidePocDialog();
            hidePocOverlay();

            if (hasNoSignal(mSignalInfo)) {
                mNoSignalView.setVisibility(View.VISIBLE);
            }

        });


//        test = findViewById(R.id.test);
        mSettingsContentObserver = new SettingsContentObserver(this, new Handler());
        getApplicationContext().getContentResolver().registerContentObserver(android.provider.Settings.System.CONTENT_URI, true, mSettingsContentObserver);



    }


//    private void getLayoutValue() {
//        int width, height;
//
//        SharedPreferences pref = this.getSharedPreferences("layout", MODE_PRIVATE);
//        width = pref.getInt("width", 0);
//        height = pref.getInt("height", 0);
//        Log.d("LayoutSettings", "Width = " + width + " " + "Height = " + height);
//
//        mSurfaceView.setLayoutParams(new FrameLayout.LayoutParams(width, height));
//
//    }


    public void delay_ms(long ms_cnt, int nano_cnt) {
        try {
            Thread.sleep(ms_cnt, nano_cnt);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();
        if (mMcuControl != null) {
            mMcuControl.start(initSource);
        }

        closePoeView();
        Intent intent = new Intent();
        intent.setAction("com.sscctv.seeeyesmonitor");
        intent.putExtra("state", "resume");
        sendBroadcast(intent);

        IntentFilter screen_filter = new IntentFilter();
        screen_filter.addAction(Intent.ACTION_SCREEN_OFF);
        screen_filter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(mScreen, screen_filter);

        zoomView.setVisibility(View.INVISIBLE);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);


        SharedPreferences pref = getSharedPreferences("poc_gallery", MODE_PRIVATE);
        boolean poc_gallery = pref.getBoolean("poc_gallery", true);

//        Log.d(TAG, "PoC Gallery: " + poc_gallery);
//        if(poc_gallery) {

//        }

        if (mMode == MODE_PTZ) {
            enterPtzMode();
        } else {
            enterVideoMode();
        }
        createVideoInput();

        mVideoThread = new HandlerThread("VideoThread");
        mVideoThread.start();
        mVideoHandler = new Handler(mVideoThread.getLooper());

        mVideoHandler.post(() -> {
            // Thread를 yield하는 것만으로 onResume시 부하가 분산되어 실행 속도가 빨라진다.
            delay_ms(0, 0);

            // 카메라 장치를 열고 동작을 시작한다
            startVideoInput();
        });


        if (mMode == MODE_RECORD) updateRecordingState(false);
        if (mMode == MODE_POC) enterVideoMode();

        isCaptureReady = STAT_READY;
//        mLockTouchScreen = isTouchScreenLocked(sharedPreferences);
        startWatchingExternalStorage();

//        setVolumeControlStream(AudioManager.STREAM_MUSIC);      // 미디어 볼륨 컨트롤 고정

        zoomTask = zoomTimeTask();

        if (zoomTimer == null) {
            zoomTimer = new Timer();
            zoomTimer.schedule(zoomTask, 100, 500);
        }

//        index = 0;

    }


    @Override
    protected void onPause() {
        Log.i(TAG, "onPause");
        if (isRecording()) {
            stopRecord();
        }
        new Thread(() -> {
            stopHdmiAudioPlay(true);
        }).start();

        zoomView.setVisibility(View.INVISIBLE);

        if (tdmTask != null) {
            tdmTimer.cancel();
            tdmTask.cancel();
            tdmTimer = null;
//            Log.d(TAG, "Timer Task Pause: " + timerTask);
        }
        // Toast를 숨김
        if (mActiveToast != null) {
            mActiveToast.cancel();
            mActiveToast = null;
        }

        if (zoomTimer != null) {
            zoomTimer.cancel();
            zoomTask.cancel();
            zoomTimer = null;
        }

        if (mMode == MODE_POC || pocMode) {
            exitPocMode();
            pocMode = false;
        }
        closeDrawer();
        hideSignalInfo();

//        SharedPreferences pref = getSharedPreferences("poc_gallery", MODE_PRIVATE);
//        boolean poc_gallery = pref.getBoolean("poc_gallery", true);
//
//        Log.d(TAG, "PoC Gallery: " + poc_gallery);
//        if(poc_gallery) {
//        }


        mVideoHandler.post(() -> {
            delay_ms(0, 0);
            // 카메라 장치를 중지시킨다
            stopVideoInput();
            Looper looper = Looper.myLooper();
            if (looper != null) {
                looper.quit();
            }
        });

        try {
            mVideoThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        mVideoThread = null;
        mSignalInfo = null;                // 다음 번 실행할 때 신호 정보를 갱신하게 함


        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
        stopWatchingExternalStorage();

        Intent intent = new Intent();
        intent.setAction("com.sscctv.seeeyesmonitor");
        intent.putExtra("state", "pause");
        sendBroadcast(intent);

        if (mMcuControl != null) {
//            Log.d(TAG, "SourceId: " + sourceId);
            mMcuControl.stop();
//            Log.d(TAG, "mMcuControl.stop");

//            mMcuControl = null;
        }
        openPoeView();

        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (mActiveToast != null) {
            mActiveToast.cancel();
            mActiveToast = null;
        }
        stopBroadCastScreen();
        getApplicationContext().getContentResolver().unregisterContentObserver(mSettingsContentObserver);
        super.onDestroy();
    }

//--------------------------------------------------------------------------------------------------


    BroadcastReceiver mScreen = new BroadcastReceiver() {
        @Override

        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            assert action != null;
//            if (action.equals(Intent.ACTION_SCREEN_ON)) {
//                Log.d(TAG, "Screen ON");
                //TODO
//            }
            if (action.equals(Intent.ACTION_SCREEN_OFF)) {
//                Log.d(TAG, "Screen OFF");
                if (isRecording()) {
                    stopRecord();
                }

                if (setLevelMeter) {
                    try {
                        mMcuControl.stopLevelMeter();
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                if (mMode == MODE_POC || pocMode) {
                    exitPocMode();
//                    Log.d(TAG, "Sleep off: PoC");
                    pocMode = false;
                }

            }
        }
    };

    public void stopBroadCastScreen() {
        if (mScreen != null) {
            unregisterReceiver(mScreen);
        }
    }

    private void gpioPortSet() {
        try {
            Runtime command = Runtime.getRuntime();
            Process proc;

            proc = command.exec("su");
            opt = new DataOutputStream(proc.getOutputStream());
        } catch (Exception e) {
            e.printStackTrace();
            throw new SecurityException();
        }
    }

//    private void gpioPinSetValue(String Pin, int value) {
//        String command = String.format("echo %d > /sys/class/gpio_sw/%s/data", value, Pin);
// 		try {
// 			String[] test = new String[] {"su", "-c", command};
// 			Runtime.getRuntime().exec(test);
// 		} catch (IOException e) {
//            e.printStackTrace();
// 		}
//
//        try {
//            opt.writeBytes(command);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    /**
     * PTZ 기능의 지원여부를 반환한다
     *
     * @return PTZ를 지원하면 true, 아니면 false
     */
    private boolean supportsPTZ() {
        return !initSource.equals(VideoSource.HDMI);
    }

//    private boolean supportsPoC() {
//
//    }

    private int supportsUTC() {

        switch (initSource) {
            case VideoSource.SDI:
                utc_value = UtcProtocol.TYPE_NONE;
                break;

            case VideoSource.AUTO:
                utc_value = UtcProtocol.TYPE_TVI;
                break;
        }
        return utc_value;
    }

    //
    private int utcType() {
        int utcType;

        switch (sourceId) {
            case VideoSource.CVBS:
                utcType = UtcProtocol.TYPE_CVBS;
                break;
            case VideoSource.TVI:
                utcType = UtcProtocol.TYPE_TVI;
                break;
            case VideoSource.AHD:
                utcType = UtcProtocol.TYPE_AHD;
                break;
            case VideoSource.CVI:
                utcType = UtcProtocol.TYPE_CVI;
                break;
            default:
                utcType = UtcProtocol.TYPE_NONE;
                break;
        }
        return utcType;
    }

    private int utcTypeNum() {
        String value;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        switch (sourceId) {
            case VideoSource.CVBS:
                value = sharedPreferences.getString(getString(R.string.pref_utc_protocol_cvbs), "0");
                break;
            case VideoSource.TVI:
                value = sharedPreferences.getString(getString(R.string.pref_utc_protocol_tvi), "0");
                break;
            case VideoSource.AHD:
                value = sharedPreferences.getString(getString(R.string.pref_utc_protocol_ahd), "0");
                break;
            case VideoSource.CVI:
                value = sharedPreferences.getString(getString(R.string.pref_utc_protocol_cvi), "0");
                break;
            default:
                value = "0";
                break;
        }

        return Integer.valueOf(value);

    }

    /**
     * PoC 기능의 지원여부를 반환한다
     *
     * @return PoC 지원하면 true, 아니면 false
     */
    private boolean supportsPoC() {
        return initSource.equals(VideoSource.SDI);
    }

    //--------------------------------------------------------------------------------------------------
    private void createVideoInput() {
        VideoInput videoInput;

        switch (initSource) {
            case VideoSource.AUTO:
                videoInput = new AnalogInput(mSurfaceView, mVideoInputListener);
                sourceId = VideoSource.AUTO;
                break;

            case VideoSource.SDI:
                videoInput = new SdiInput(mSurfaceView, mVideoInputListener);
                sourceId = VideoSource.SDI;

                break;

            case VideoSource.HDMI:
                videoInput = new HdmiInput(mSurfaceView, mVideoInputListener);
                sourceId = VideoSource.HDMI;

                break;

            default:
                throw new IllegalArgumentException("입력 소스를 지정하지 않았습니다");
        }
        mSource = new VideoSource(sourceId);
        mVideoInput = videoInput;
    }

    private synchronized void startVideoInput() {
        Bundle startArgs = new Bundle();
        mVideoInput.start(startArgs);
    }

    private synchronized void stopVideoInput() {
//        Log.d(TAG, "stopVideoInput()");
        if (mVideoInput != null) {
            mVideoInput.stop();
        }
        mMcuControl.stop();
    }

    private void startInputInfoCheckHandler() {
        mVideoInputListener = signalInfo -> runOnUiThread(() -> {
//                        Log.d(TAG, "onSignalChange " + signalInfo.signal);
            if (mSignalInfo != null) {
                if (mSignalInfo.signal == signalInfo.signal &&
                        mSignalInfo.width == signalInfo.width &&
                        mSignalInfo.height == signalInfo.height &&
                        mSignalInfo.scan == signalInfo.scan &&
                        mSignalInfo.rate == signalInfo.rate &&
                        mSignalInfo.std == signalInfo.std) {
                    return;
                }
            }

            mSignalInfo = signalInfo;
            updateSurfaceView(mSignalInfo);

            if (mSignalInfo.signal) {
                int type;
                type = mSignalInfo.std;
                cvbs_Input = getSystem(mSignalInfo);

                switch (type) {
                    case 0:
                        if (cvbs_Input == -1) {
                            sourceId = VideoSource.TVI;
                        } else {
                            sourceId = VideoSource.CVBS;
                        }
                        break;
                    case 1:
                        sourceId = VideoSource.AHD;
                        break;
                    case 2:
                        sourceId = VideoSource.CVI;
                        break;
                    case 253:
                        sourceId = VideoSource.HDMI;
                        break;
                    case 254:
                        sourceId = VideoSource.SDI;
                        break;
                    case 255:
                        sourceId = VideoSource.AUTO;
                        break;
                }
//                Log.d(TAG, "SourceId: " + sourceId);
                mPtzSubmenuFragment = PtzMenuFragment.newInstance(utcType());
                mPtzOverlay = PtzOverlayFragment.newInstance(utcType());
                changeUtcMode(utcType());

                switch (mMode) {
                    case MODE_PTZ:
                        enterPtzMode();
                        break;
                    case MODE_VIEW:
                    case MODE_RECORD:
                    case MODE_SNAPSHOT:
                        mNoSignalView.setVisibility(View.INVISIBLE);
                        break;
                }

                startHdmiAudioPlay();


            } else if (hasNoSignal(mSignalInfo)) {
//                Log.d(TAG, "startInputInfoCheckHandler mMode: " + mMode + " PoC: " + pocMode);
                switch (mMode) {
                    case MODE_POC:
                        mNoSignalView.setVisibility(View.INVISIBLE);
//                        mPocDialog.setVisibility(View.VISIBLE);
                        break;
                    case MODE_PTZ:
                        if (pocMode) {
                            mNoSignalView.setVisibility(View.INVISIBLE);
                        } else {
                            hidePocDialog();
                            hidePocOverlay();
                            hideLevelMeter();
                            mNoSignalView.setVisibility(View.INVISIBLE);
                        }
                        break;
                    case MODE_VIEW:
                    case MODE_RECORD:
                    case MODE_SNAPSHOT:
                        if (pocMode) {
                            mNoSignalView.setVisibility(View.INVISIBLE);
                        } else {
                            hidePocDialog();
                            hidePocOverlay();
                            mNoSignalView.setVisibility(View.VISIBLE);
                        }
                        break;
                }
                zoomView.setVisibility(View.INVISIBLE);
                tdm_set.setVisibility(View.INVISIBLE);
                mTdmSetupView.setVisibility(View.INVISIBLE);
                if (!mSignalInfo.signal && isRecording()) {
                    stopRecording();
                }
//                if(setLevelMeter) {
//                    try {
//                        mMcuControl.stopLevelMeter();
//                    } catch (IOException | InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
                stopHdmiAudioPlay(false);
            }

            notifySignalChangeToMcu(sourceId, mSignalInfo);
            if ((mMode == MODE_VIEW || mMode == MODE_POC) && mPendingMode == MODE_INVALID) {
                showSignalInfo(mSignalInfo);
            } else if (isDrawerOpen()) {
                if (hasNoSignal(mSignalInfo)) {
//                    mNoSignalView.setVisibility(View.INVISIBLE);
                    enterVideoMode();
                    closeDrawer();
                }
            }


        });
    }

    private static boolean hasNoSignal(VideoInput.SignalInfo signalInfo) {
        return ((!signalInfo.signal || signalInfo.width == 0 || signalInfo.height == 0) || (signalInfo.width > 5000 || signalInfo.height > 5000));
    }

    private void updateSurfaceView(VideoInput.SignalInfo signalInfo) {
//        Log.d(TAG, "updateSurfaceView: " + hasNoSignal(signalInfo));
        AudioManager audio = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        assert audio != null;
        int currentVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);

        if (hasNoSignal(signalInfo)) {
            // 이전에 입력된 영상이 남아있을 수 있으므로 검은 배경을 씌워 화면을 지우는 효과를 낸다.
//            Log.d(TAG, "updateSurfaceView - NoSignal - SignalInfo: " + signalInfo.signal);
            mSurfaceView.setBackgroundColor(ContextCompat.getColor(getApplication(), R.color.noSignalBackground));

            new Handler().postDelayed(() -> mSurfaceView.setBackgroundColor(ContextCompat.getColor(getApplication(), R.color.noSignalBackground)), 1000);

        } else {
            // 이전 메모리 잔상이 보이는 것을 가리기 위한 딜레이
//            audio.setParameters("dac_volume=" + (0));
//            delay_ms(0, 500);
            new Handler().postDelayed(() -> {
//                Log.d(TAG, "UpdateSurfaceView setBackgroundColor: TRANSPARENT");
                mSurfaceView.setBackgroundColor(Color.TRANSPARENT);
                audio.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, FLAG_PLAY_SOUND);
                audio.setParameters("dac_volume=" + (currentVolume));

            }, 1000);
            // 배경을 제거하여 입력 영상이 보이게 한다.
        }
//        Log.d(TAG, "currentVolume: " + currentVolume);

    }

    private void showSignalInfo(SignalInfo signalInfo) {
//        Log.w(TAG, "showSignalInfo: " + signalInfo.width + "x" + signalInfo.height + signalInfo.scan + "@" + signalInfo.rate);

        if (mHideSignalInfoTask != null) {
            mSignalInfoView.removeCallbacks(mHideSignalInfoTask);
            mHideSignalInfoTask = null;
        }

        StringBuilder builder = new StringBuilder();


        if (mSource.is(VideoSource.SDI)) {
            switch (signalInfo.mode) {
                case SdiInput.MODE_STOP:
                case SdiInput.MODE_HD:
                    builder.append("SDI");
                    break;

                case SdiInput.MODE_3G:
                    builder.append("3G-SDI");
                    break;

                case SdiInput.MODE_EX1:
                    builder.append("EX-SDI [1.0]");
                    break;

                case SdiInput.MODE_EX_3G:
                    if (mSignalInfo.height == 1080) {
                        builder.append("EX-SDI [3G]");
                    } else if (mSignalInfo.height == 1440) {
                        builder.append("EX-SDI [4M]");
                    } else {
                        builder.append("EX-SDI [3G]");
                    }
                    break;

                case SdiInput.MODE_EX2:
                    builder.append("EX-SDI [2.0]");
                    break;

                case SdiInput.MODE_EX_4K:
                    builder.append("EX-SDI [4K]");
                    break;

                case SdiInput.MODE_EX_TDM:
                    builder.append("EX-SDI [TDM]");
                    restartTdrCheck();
                    break;
            }
            if (SdiInput.MODE_EX_TDM == signalInfo.mode) {
                tdm_set.setVisibility(View.VISIBLE);
            } else {
                tdm_set.setVisibility(View.INVISIBLE);
            }


        } else if (mSource.is(VideoSource.AUTO)) {

            switch (signalInfo.std) {
                case 0:
//                    Log.d(TAG, "CVBS INPUT: " + cvbs_Input);
                    if (cvbs_Input == -1) {
                        builder.append("TVI");
                    } else {
                        builder.append("CVBS");
                    }
                    break;
                case 1:
                    builder.append("AHD");
                    break;
                case 2:
                    builder.append("CVI");
                    break;
                case 255:
                    builder.append("HD Analog");
                    break;
            }

        } else if (mSource.is(VideoSource.HDMI)) {
            builder.append("HDMI");
        }
        builder.append('\n');

        int noSignal;

        if (hasNoSignal(signalInfo)) {
            noSignal = View.VISIBLE;
            mSignalInfoView.setVisibility(View.VISIBLE);

            Intent intent = new Intent();
            intent.setAction("com.sscctv.seeeyesmonitor");
            intent.putExtra("state", "nosignal");
            sendBroadcast(intent);

            builder.append("-----\n");
            hideCrcStats();
            hideTdmSetup();

        } else {
            noSignal = View.INVISIBLE;

            Intent intent = new Intent();
            intent.setAction("com.sscctv.seeeyesmonitor");
            intent.putExtra("state", "signal");
            sendBroadcast(intent);

            if (sourceId.equals(VideoSource.CVBS)) {
                switch (cvbs_Input) {
                    case 0:
                        builder.append("NTSC");
                        break;

                    case 1:
                        builder.append("PAL");
                        break;

                    default:
                        builder.append("----");
                        break;
                }
            } else {
                if (signalInfo.height == 1440) {
                    builder.append("2560");
                } else if (signalInfo.height == 1520) {
                    builder.append("2688");
                } else if (signalInfo.height == 1536) {
                    builder.append("2048");
                } else if (signalInfo.height == 1080) {
                    builder.append("1920");
                } else if (signalInfo.height == 720) {
                    builder.append("1280");
                } else {
                    builder.append(signalInfo.width);
                }
                builder.append('x');
                if (signalInfo.height == 1936) {
                    builder.append("1944");
                } else {
                    builder.append(signalInfo.height);
                }

                builder.append(signalInfo.scan);
                //builder.append('@');
                if (signalInfo.rate == Math.ceil(signalInfo.rate)) {
                    builder.append((int) signalInfo.rate);
                } else {
                    builder.append(signalInfo.rate);
                }

            }

            // 5초 있다가 숨긴다.
            mHideSignalInfoTask = () -> {
                mSignalInfoView.setVisibility(View.INVISIBLE);
                mHideSignalInfoTask = null;
            };
            mSignalInfoView.postDelayed(mHideSignalInfoTask, 10 * 500);
        }

        mSignalInfoView.setText(builder.toString());
        mSignalInfoView.setVisibility(View.VISIBLE);
        mMenuAdaptor.notifyDataSetChanged();
//        Log.d(TAG, "showSignalInfo mMode: " + mMode + " PoCMode: " + pocMode);
        switch (mMode) {
            case MODE_POC:
                if (pocMode) {
                    mNoSignalView.setVisibility(View.INVISIBLE);
                } else {
                    hidePocDialog();
                    hidePocOverlay();
                    mNoSignalView.setVisibility(noSignal);
                }
                break;
            case MODE_VIEW:
            case MODE_PTZ:

                if (pocMode) {
                    mNoSignalView.setVisibility(View.INVISIBLE);
                } else {
                    hidePocDialog();
                    hidePocOverlay();
                    hideLevelMeter();
                    mNoSignalView.setVisibility(noSignal);
                }
                break;
            case MODE_SNAPSHOT:
            case MODE_RECORD:
                if (pocMode) {
                    mNoSignalView.setVisibility(View.INVISIBLE);
                } else {
                    hidePocDialog();
                    hidePocOverlay();
                    mNoSignalView.setVisibility(noSignal);
                }
                break;
        }
        if (!mSource.is(VideoSource.HDMI)) {

            if (isLevelMeterEnabled(PreferenceManager.getDefaultSharedPreferences(this))) {

                showLevelMeter();
                clearLevelMeter();


            } else {
                hideLevelMeter();
            }
        }

    }

    private void hideSignalInfo() {
//        Log.d(TAG, "HideSignalInfo");
        mSignalInfoView.removeCallbacks(mHideSignalInfoTask);
        mHideSignalInfoTask = null;
        mSignalInfoView.setVisibility(View.INVISIBLE);
        mNoSignalView.setVisibility(View.INVISIBLE);

        hideCrcStats();
        hideLevelMeter();
        hideTdmSetup();
    }

    /**
     * 입력 신호의 상태가 바뀌었음을 MCU에 알린다.
     *
     * @param sourceId   현재의 입력모드
     * @param signalInfo 입력 신호 정보
     */
    private void notifySignalChangeToMcu(String sourceId, SignalInfo signalInfo) {
        if (signalInfo.mode == 0xff) return;

        switch (sourceId) {
            case VideoSource.SDI:
                try {
                    mMcuControl.notifySdiLockState(signalInfo.mode);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
                break;

            case VideoSource.AUTO:
            case VideoSource.AHD:
            case VideoSource.CVI:
            case VideoSource.TVI:
            case VideoSource.CVBS:
                try {
                    mMcuControl.sendInputSourceMode(sourceId);
                    mMcuControl.setFormat(signalInfo.mode);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
                break;
        }
    }

//--------------------------------------------------------------------------------------------------

    public void startMcuHandler() {

        final FragmentManager fragmentManager = getSupportFragmentManager();
        LevelMeterFragment levelMeterFragment = null;

        mLevelMeterView = findViewById(R.id.level_meter);

        switch (initSource) {
            case VideoSource.SDI:
                levelMeterFragment = LevelMeterFragment.newInstance(R.layout.fragment_sdi_level_meter);

                break;

            case VideoSource.AUTO:
            case VideoSource.AHD:
            case VideoSource.TVI:
            case VideoSource.CVI:
                levelMeterFragment = LevelMeterFragment.newInstance(R.layout.fragment_analog_level_meter);
                break;
        }
        if (levelMeterFragment != null) {
            fragmentManager.beginTransaction()
                    .replace(R.id.level_meter, levelMeterFragment, LEVEL_METER_FRAGMENT_TAG)
                    .commit();


            McuDataListener mMcuDataListener = new McuDataListener() {
                @Override
                public void onPocStarted() {
//                Log.d(TAG, "onPocStarted()");
                    try {
                        opt.writeBytes("echo 0 > /sys/class/gpio_sw/PE11/data\n");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    runOnUiThread(() -> {
                        showPocDialog();
                        showPocOverlay();
                        inputPocState(0);
                    });
                    // 전원 공급 모드로 바꿈

                }

                @Override
                public void onPocStateChange(final int state) {
//                Log.d(TAG, "PoC state = " + state);

                    runOnUiThread(() -> {
//                    setPocState(state);
                        inputPocState(state);
                        if (state == STATE_LINK_OK) {
                            setPocMode(MODE_POWER);
                            showPocDialog();
                        } else if (state >= STATE_C_OPEN) {
                            setPocMode(MODE_CHECK);
                            showPocDialog();
                        }
                    });
                }

                @Override
                public void onPocStopped() {
//                Log.d(TAG, "onPocStopped()");
                }

                @Override
                public void onPocNotSupported() {
                    //Log.d(TAG, "onPocNotSupported()");
                    runOnUiThread(() -> {
                        //showToast(R.string.poc_not_support);
                        hidePocDialog();
                        hidePocOverlay();
                        showPocNotSupportDialog();
                    });
                }

                @Override
                public void onLevelChanged(final int level, final int value) {
                    runOnUiThread(() -> {

                        LevelMeterFragment levelMeterFragment1 = (LevelMeterFragment) fragmentManager.findFragmentByTag(LEVEL_METER_FRAGMENT_TAG);
                        if ((levelMeterFragment1 != null) && (mSignalInfo != null)) {
                            switch (level) {
                                case LEVEL_FOCUS:
                                    if (!hasNoSignal(mSignalInfo)) {
                                        if ((sourceId.equals(VideoSource.TVI)) && ((mSignalInfo.mode == 0x04) || (mSignalInfo.mode == 0x05) || (mSignalInfo.mode == 0x00) || (mSignalInfo.mode == 0x01))) {
                                            break;
                                        }
                                        levelMeterFragment1.updateFocusLevel(value);
//                                        Log.w(TAG, "onLevelChanged(" + level + ", " + value + ")");
                                    }
                                    break;

                                case LEVEL_BURST:
                                    levelMeterFragment1.updateBurstLevel(value);
                                    break;

                                case LEVEL_SYNC:
                                    levelMeterFragment1.updateSyncLevel(value);
                                    break;

                                case LEVEL_SDI:
                                    levelMeterFragment1.updateSignalLevel(mSignalInfo.signal, value);
                                    break;

                                default:
//                                    Log.w(TAG, "onLevelChanged(" + level + ", " + value + ")");
                                    break;
                            }
                        }
                    });
                }
            };
            mMcuControl.addReceiveBufferListener(mMcuDataListener);


        }
    }

    /**
     * 현재 비디오 입력에 따라 레벨 미터를 표시한다.
     */
    private void showLevelMeter() {
        boolean input;
        input = sourceId != null;

        if (input && mSignalInfo.signal) {
            switch (sourceId) {
                case VideoSource.SDI:
                case VideoSource.AUTO:
                case VideoSource.AHD:
                case VideoSource.TVI:
                case VideoSource.CVI:
                case VideoSource.CVBS:
                    startLevelMeter();
                    break;
                default:
                    hideLevelMeter();
                    break;
            }
        } else {
            hideLevelMeter();
        }

    }


    /**
     * 레벨 미터 시작 명령을 MCU에 보내고 레벨미터 창을 보이게 한다.
     */
    private void startLevelMeter() {
        if (!hasNoSignal(mSignalInfo)) {
            try {
                setLevelMeter = true;
//                opt.writeBytes("echo 1 > /sys/class/gpio_sw/PE13/data\n");
                mMcuControl.startLevelMeter();

            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }

        mLevelMeterView.setVisibility(View.VISIBLE);

    }

    /**
     * 레벨 미터 중지 명령을 MCU에 보내고 레벨미터 창을 숨긴다.
     */
    private void hideLevelMeter() {
//        if (!initSource.equals(VideoSource.HDMI)) {
        try {
//                opt.writeBytes("echo 0 > /sys/class/gpio_sw/PE13/data\n");
            setLevelMeter = false;

            mMcuControl.stopLevelMeter();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        mLevelMeterView.setVisibility(View.INVISIBLE);

//        }
    }

    private void clearLevelMeter() {

        Fragment fragment = getSupportFragmentManager().findFragmentByTag(LEVEL_METER_FRAGMENT_TAG);
        if (fragment instanceof LevelMeterFragment) {
            LevelMeterFragment levelMeterFragment = (LevelMeterFragment) fragment;
//            Log.d(TAG, "sourceId: " + sourceId + " mSignalInfo.mode: " + mSignalInfo.mode);

            if (sourceId.equals(VideoSource.SDI)) {
                levelMeterFragment.updateSignalLevel(mSignalInfo != null && mSignalInfo.signal, 0);
            } else {
                levelMeterFragment.updateBurstLevel(0);
                levelMeterFragment.updateSyncLevel(0);
            }

            if ((sourceId.equals(VideoSource.TVI)) && ((mSignalInfo.mode == 0x04) || (mSignalInfo.mode == 0x05) || (mSignalInfo.mode == 0x00) || (mSignalInfo.mode == 0x01))) {
                levelMeterFragment.setFocusLevelNa();
            } else {
                levelMeterFragment.resetFocusLevel(0);
            }
        }
    }

    private void resetFocusLevel() {
        if ((sourceId.equals(VideoSource.TVI)) && ((mSignalInfo.mode == 0x04) || (mSignalInfo.mode == 0x05) || (mSignalInfo.mode == 0x00) || (mSignalInfo.mode == 0x01)))
            return;

        Fragment fragment = getSupportFragmentManager().findFragmentByTag(LEVEL_METER_FRAGMENT_TAG);
        if (fragment instanceof LevelMeterFragment) {
            LevelMeterFragment levelMeterFragment = (LevelMeterFragment) fragment;
            levelMeterFragment.resetFocusLevel(0);
        }
    }

    private void showTdmSetup() {
        mTdmSetupView.setVisibility(View.VISIBLE);

//        tdm_ch2.setFocusableInTouchMode(true);
//        tdm_ch3.setFocusableInTouchMode(true);
//        tdm_ch4.setFocusableInTouchMode(true);
//        tdm_ch5.setFocusableInTouchMode(true);


//        tdm_ch1.setEnabled(false);
//        tdm_ch2.setEnabled(false);
//        tdm_ch3.setEnabled(false);
//        tdm_ch4.setEnabled(false);
//        tdm_ch5.setEnabled(false);
//        tdm_ch6.setEnabled(false);
//        tdm_ch7.setEnabled(false);
//        tdm_ch8.setEnabled(false);
    }

    @SuppressLint("SetTextI18n")
    private void updateTdmSetup(boolean live) {
//        Log.d(TAG, "updateTdmSetup: " + live);
        SdiInput sdiInput = (SdiInput) mVideoInput;
        SdiInput.TDMValue tdmValue = sdiInput.getTdm();
        int stat;
        int resId;
        int select;
        if (!live) {
            for (int i = 1; i < 9; i++) {
                resId = getResources().getIdentifier("tdm_ch" + i, "id", "com.sscctv.seeeyesmonitor");
                ((Button) findViewById(resId)).setTextColor(Color.GRAY);
                switch (i) {
                    case 1:
                        tdm_ch1.setText("CH 1:   -   ");
                        tdm_ch1.setEnabled(false);
                        break;
                    case 2:
                        tdm_ch2.setText("CH 2:   -   ");
                        tdm_ch2.setEnabled(false);
                        break;
                    case 3:
                        tdm_ch3.setText("CH 3:   -   ");
                        tdm_ch3.setEnabled(false);
                        break;
                    case 4:
                        tdm_ch4.setText("CH 4:   -   ");
                        tdm_ch4.setEnabled(false);
                        break;
                    case 5:
                        tdm_ch5.setText("CH 5:   -   ");
                        tdm_ch5.setEnabled(false);
                        break;
                    case 6:
                        tdm_ch6.setText("CH 6:   -   ");
                        tdm_ch6.setEnabled(false);
                        break;
                    case 7:
                        tdm_ch7.setText("CH 7:   -   ");
                        tdm_ch7.setEnabled(false);
                        break;
                    case 8:
                        tdm_ch8.setText("CH 8:   -   ");
                        tdm_ch8.setEnabled(false);
                        break;

                }
            }
        } else {
            for (int i = 0; i < 8; i++) {
                stat = getBit(tdmValue.state, i);
                select = tdmValue.select;
//                Log.d(TAG, "TDM Channel: " + i + " Stat: " + stat + " Select: " + select);
                switch (i + 1) {
                    case 1:
                        if (stat == 0) {
//                            Log.w(TAG, "TDM CH1 = OFF");
                            tdm_ch1.setText("CH 1:   -   ");
                            tdm_ch1.setTextColor(Color.GRAY);
                            tdm_ch1.setEnabled(false);
                            if (select == 1) {
//                                Log.w(TAG, "TDM CH1 = ON & Select  " + mNoSignalView.getVisibility());
                                if(mNoSignalView.getVisibility() == View.INVISIBLE){
//                                    Log.w(TAG, "mNoSignalView.setVisibility(View.VISIBLE)  " + mNoSignalView.getVisibility());

                                    mNoSignalView.setVisibility(View.VISIBLE);
                                }
                            }
                        } else {
//                            Log.w(TAG, "TDM CH1 = ON");

                            int value = tdmValue.ch1;
                            tdm_ch1.setText("CH 1:   " + value);
                            tdm_ch1.setEnabled(true);


                            if (select == 1) {
                                tdm_ch1.setTextColor(Color.CYAN);
                                if (mNoSignalView.getVisibility() == View.VISIBLE) {
                                    mNoSignalView.setVisibility(View.INVISIBLE);
                                showSignalInfo(mSignalInfo);
                                }
                            } else {
                                tdm_ch1.setTextColor(Color.GREEN);
                            }
                        }
                        break;
                    case 2:
                        if (stat == 0) {
                            tdm_ch2.setText("CH 2:   -   ");
                            tdm_ch2.setTextColor(Color.GRAY);
                            tdm_ch2.setEnabled(false);
                            if (select == 2) {
                                if(mNoSignalView.getVisibility() != View.VISIBLE){
                                    mNoSignalView.setVisibility(View.VISIBLE);
                                }
                            }
                        } else {
                            int value = tdmValue.ch2;
                            tdm_ch2.setText("CH 2:   " + value);
                            tdm_ch2.setEnabled(true);

                            if (select == 2) {
                                tdm_ch2.setTextColor(Color.CYAN);
                                if (mNoSignalView.getVisibility() == View.VISIBLE) {
                                    mNoSignalView.setVisibility(View.INVISIBLE);
                                    showSignalInfo(mSignalInfo);
                                }
                            } else {
                                tdm_ch2.setTextColor(Color.GREEN);
                            }
                        }
                        break;
                    case 3:
                        if (stat == 0) {
                            tdm_ch3.setText("CH 3:   -   ");
                            tdm_ch3.setTextColor(Color.GRAY);
                            tdm_ch3.setEnabled(false);
                             if (select == 3) {
                                if(mNoSignalView.getVisibility() != View.VISIBLE){
                                    mNoSignalView.setVisibility(View.VISIBLE);
                                }
                            }
                        } else {
                            int value = tdmValue.ch3;
                            tdm_ch3.setText("CH 3:   " + value);
                            tdm_ch3.setEnabled(true);

                            if (select == 3) {
                                tdm_ch3.setTextColor(Color.CYAN);
                                if (mNoSignalView.getVisibility() == View.VISIBLE) {
                                    mNoSignalView.setVisibility(View.INVISIBLE);
                                    showSignalInfo(mSignalInfo);
                                }
                            } else {
                                tdm_ch3.setTextColor(Color.GREEN);
                            }
                        }
                        break;
                    case 4:
                        if (stat == 0) {
                            tdm_ch4.setText("CH 4:   -   ");
                            tdm_ch4.setTextColor(Color.GRAY);
                            tdm_ch4.setEnabled(false);
                            if (select == 4) {
                                if(mNoSignalView.getVisibility() != View.VISIBLE){
                                    mNoSignalView.setVisibility(View.VISIBLE);
                                }
                            }
                        } else {
                            int value = tdmValue.ch4;
                            tdm_ch4.setText("CH 4:   " + value);
                            tdm_ch4.setEnabled(true);

                            if (select == 4) {
                                tdm_ch4.setTextColor(Color.CYAN);
                                if (mNoSignalView.getVisibility() == View.VISIBLE) {
                                    mNoSignalView.setVisibility(View.INVISIBLE);
                                    showSignalInfo(mSignalInfo);
                                }
                            } else {
                                tdm_ch4.setTextColor(Color.GREEN);
                            }
                        }
                        break;
                    case 5:
                        if (stat == 0) {
                            tdm_ch5.setText("CH 5:   -   ");
                            tdm_ch5.setTextColor(Color.GRAY);
                            tdm_ch5.setEnabled(false);
                            if (select == 5) {
                                if(mNoSignalView.getVisibility() != View.VISIBLE){
                                    mNoSignalView.setVisibility(View.VISIBLE);
                                }
                            }
                        } else {
                            int value = tdmValue.ch5;
                            tdm_ch5.setText("CH 5:   " + value);
                            tdm_ch5.setEnabled(true);

                            if (select == 5) {
                                tdm_ch5.setTextColor(Color.CYAN);
                                if (mNoSignalView.getVisibility() == View.VISIBLE) {
                                    mNoSignalView.setVisibility(View.INVISIBLE);
                                    showSignalInfo(mSignalInfo);
                                }
                            } else {
                                tdm_ch5.setTextColor(Color.GREEN);
                            }
                        }
                        break;
                    case 6:
                        if (stat == 0) {
                            tdm_ch6.setText("CH 6:   -   ");
                            tdm_ch6.setTextColor(Color.GRAY);
                            tdm_ch6.setEnabled(false);
                            if (select == 6) {
                                if(mNoSignalView.getVisibility() != View.VISIBLE){
                                    mNoSignalView.setVisibility(View.VISIBLE);
                                }
                            }
                        } else {
                            int value = tdmValue.ch6;
                            tdm_ch6.setText("CH 6:   " + value);
                            tdm_ch6.setEnabled(true);

                            if (select == 6) {
                                tdm_ch6.setTextColor(Color.CYAN);
                                if (mNoSignalView.getVisibility() == View.VISIBLE) {
                                    mNoSignalView.setVisibility(View.INVISIBLE);
                                    showSignalInfo(mSignalInfo);
                                }
                            } else {
                                tdm_ch6.setTextColor(Color.GREEN);
                            }
                        }
                        break;
                    case 7:
                        if (stat == 0) {
                            tdm_ch7.setText("CH 7:   -   ");
                            tdm_ch7.setTextColor(Color.GRAY);
                            tdm_ch7.setEnabled(false);
                            if (select == 7) {
                                if(mNoSignalView.getVisibility() != View.VISIBLE){
                                    mNoSignalView.setVisibility(View.VISIBLE);
                                }
                            }
                        } else {
                            int value = tdmValue.ch7;
                            tdm_ch7.setText("CH 7:   " + value);
                            tdm_ch7.setEnabled(true);

                            if (select == 7) {
                                tdm_ch7.setTextColor(Color.CYAN);
                                if (mNoSignalView.getVisibility() == View.VISIBLE) {
                                    mNoSignalView.setVisibility(View.INVISIBLE);
                                    showSignalInfo(mSignalInfo);
                                }
                            } else {
                                tdm_ch7.setTextColor(Color.GREEN);
                            }
                        }
                        break;
                    case 8:
                        if (stat == 0) {
                            tdm_ch8.setText("CH 8:   -   ");
                            tdm_ch8.setTextColor(Color.GRAY);
                            tdm_ch8.setEnabled(false);
                            if (select == 8) {
                                if(mNoSignalView.getVisibility() != View.VISIBLE){
                                    mNoSignalView.setVisibility(View.VISIBLE);
                                }
                            }
                        } else {
                            int value = tdmValue.ch8;
                            tdm_ch8.setText("CH 8:   " + value);
                            tdm_ch8.setEnabled(true);

                            if (select == 8) {
                                tdm_ch8.setTextColor(Color.CYAN);
                                if (mNoSignalView.getVisibility() == View.VISIBLE) {
                                    mNoSignalView.setVisibility(View.INVISIBLE);
                                    showSignalInfo(mSignalInfo);
                                }
                            } else {
                                tdm_ch8.setTextColor(Color.GREEN);
                            }
                        }
                        break;

                }
            }
        }
    }


    private void restartTdrCheck() {
        updateTdmSetup(false);

        @SuppressLint("HandlerLeak") final Handler handler = new Handler() {
            public void handleMessage(Message msg) {
                updateTdmSetup(true);

            }
        };
        TimerTask tdmTask = new TimerTask() {
            @Override
            public void run() {
//                Log.v(TAG, "timer run");
                Message msg = handler.obtainMessage();
                handler.sendMessage(msg);
//                Log.d(TAG, "CurrentFocus: " + getCurrentFocus());
            }
        };

        if (tdmTimer == null) {
            tdmTimer = new Timer();
            tdmTimer.schedule(tdmTask, 0, 500);
        }


    }

    private boolean hideTdmSetup() {
//        Log.d(TAG, "hideTdmSetup");

        btnChk = true;
        if (tdmTimer != null) {
            tdmTimer.cancel();
            tdmTimer = null;
            mTdmSetupView.setVisibility(View.INVISIBLE);
//            Log.d(TAG, "hideTdmSetup Timer: " + tdmTimer);

//            longKeyChkCnt = 0;
//            mTdmSetupView = null;
            return true;

        }
        return false;

    }


    //--------------------------------------------------------------------------------------------------
    private void showCrcStats() {
        mCrcStatsView = findViewById(R.id.crc_stats);
        mCrcCountsText = findViewById(R.id.crc_counts);
        mCrcTimeText = findViewById(R.id.crc_time);
        mCrcStatsView.setVisibility(View.VISIBLE);
    }

    @SuppressLint("DefaultLocale")
    private void updateCrcStats(boolean live) {
        SdiInput sdiInput = (SdiInput) mVideoInput;

        StringBuilder builder = new StringBuilder();

        if (!live) {
            sdiInput.resetCrcCounts();

            switch (mSignalInfo.mode) {
                case SdiInput.MODE_HD:
                case SdiInput.MODE_3G:
                    builder.append("CCRC : 000");
                    builder.append(System.getProperty("line.separator"));
                    builder.append("YCRC : 000");
                    break;
                default:
                    builder.append("XCRC : 000");
                    break;
            }
        } else {
            SdiInput.CrcCounts crcCounts = sdiInput.getCrcCounts();

            switch (mSignalInfo.mode) {
                case SdiInput.MODE_HD:
                case SdiInput.MODE_3G:
                    builder.append(String.format("CCRC : %03d", crcCounts.ccrc));
                    builder.append(System.getProperty("line.separator"));
                    builder.append(String.format("YCRC : %03d", crcCounts.ycrc));
                    break;

                case SdiInput.MODE_EX1:
                case SdiInput.MODE_EX2:
                case SdiInput.MODE_EX_3G:
                case SdiInput.MODE_EX_4K:
                case SdiInput.MODE_EX_TDM:
                    builder.append(String.format("XCRC : %03d", crcCounts.xcrc));
                    break;

                default:
                    builder.append("000");
                    break;
            }
        }

        mCrcCountsText.setText(builder.toString());
    }

    private void restartCrcStats() {
        updateCrcStats(false);

        mCrcTimeText.setText(getString(R.string.crc_time));

        mCrcStartTime = System.currentTimeMillis();
        mUpdateCrcTimeTask = () -> {
            updateCrcStats(true);

            long duration = System.currentTimeMillis() - mCrcStartTime;

            DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss", Locale.KOREAN);
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            String durationText = "TIME : " + dateFormat.format(new Date(duration));
            mCrcTimeText.setText(durationText);

            mCrcTimeText.postDelayed(mUpdateCrcTimeTask, 1000);
        };
        mCrcTimeText.postDelayed(mUpdateCrcTimeTask, 1000);
    }

    /**
     * CRC 뷰를 숨긴다.
     *
     * @return CRC 뷰가 보이는 상태였으면 true, 이미 숨겨진 상태면 false
     */
    private boolean hideCrcStats() {
        if (mUpdateCrcTimeTask != null) {
            mCrcTimeText.removeCallbacks(mUpdateCrcTimeTask);
            mUpdateCrcTimeTask = null;

            mCrcStatsView.setVisibility(View.INVISIBLE);

            mCrcStatsView = null;
            mCrcCountsText = null;
            mCrcTimeText = null;
            return true;
        }

        return false;
    }

    //--------------------------------------------------------------------------------------------------
    public void startMenuOverlayHandler() {
        mMainMenu = findViewById(R.id.main_menu);
        mSubMenu = findViewById(R.id.sub_menu);

        // Enter 키에 의해 메뉴가 열리는 문제를 피하기 위해 focusable=false로 설정한다.
        // XML 에서 속성을 설정해도 풀리는 문제가 있어 여기서 강제로 설정한다.
        mDrawerButton = findViewById(R.id.drawer_button);
        mDrawerButton.setFocusable(false);

        mDrawer = findViewById(R.id.drawer_layout);
        mDrawer.setFocusable(true);
        mDrawer.setFocusableInTouchMode(false);
        mDrawer.setScrimColor(Color.TRANSPARENT);
        mDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        mDrawer.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
//                unlockMenu();

                // 오버레이 레이아웃의 모든 내용을 서랍이 열리면 숨게 함
                mOverlayView.setAlpha(1 - slideOffset);
                mDrawerButton.setAlpha(1 - slideOffset);
            }

            @Override
            public void onDrawerOpened(@NonNull View drawerView) {
//                Log.d(TAG, "onDrawerOpened");
                if (mMode == MODE_PTZ) {
                    mDrawerButton.requestFocus();
                }
                hideSignalInfo();
                onPtzMenu = true;

            }

            @Override
            public void onDrawerClosed(@NonNull View drawerView) {
                // 열려 있는 서브메뉴를 없애고 모드 전환을 마친다.
//                Log.d(TAG, "onDrawerClosed");
//                mPtzOverlay.protocolSet();
                setMenuFragment(null);
                unlockMenu();
//                lockMenu();
                switchToPendingMode();
                new Handler().postDelayed(() -> {
                    onPtzMenu = false;
                }, 100);
            }

            @Override
            public void onDrawerStateChanged(int newState) {
//                Log.d(TAG, "onDrawerStateChanged = " + newState);

            }
        });

        mDrawerList = findViewById(R.id.menu_items);

        CharSequence[] menuTitles;
        menuTitles = getResources().getStringArray(R.array.menu_titles);
        mMenuAdaptor = new ArrayAdapter<CharSequence>(this, R.layout.drawer_list_item, menuTitles) {
            // CVBS와 SDI 모드에서만 PTZ/PoC를 지원한다
            @Override
            public boolean areAllItemsEnabled() {
                return false;
            }

            @Override
            public boolean isEnabled(int position) {
//                Log.d(TAG, "position: " + position + " Mode: " + mMode);

                if (position == 0) {
                    return supportsPTZ();
                }
                if(getLocale.equals("ja")) {
                    if (position == 4) {
                        if (!supportsPoC()) {
                            return false;
                        }
                        return (mMode == MODE_VIEW) || (mMode == MODE_POC);
                    }
                } else {
                    return (position != 4);
                }

//                if (position == 4) {
//                    if (supportsPoC()) {
//
//                        return mMode == MODE_VIEW;
//                    } else {
//                        Log.d(TAG, "supportsPoC1: " + position + " Mode: " + mMode);
//
//                        return false;
//                    }
//                }
                return true;
                //return (position != 4);                       // POC 기능 사용하지 않는 경우
//                return true;
            }

            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                // ImageView 를 따로 만들지 않고 compound view 를 써서 간단하게 아이콘을 표시한다
                // 아이콘은 제목 위에 위치
                final TypedArray menuIcons = getResources().obtainTypedArray(R.array.menu_icons);
                view.setCompoundDrawablesRelativeWithIntrinsicBounds(null, menuIcons.getDrawable(position), null, null);
                menuIcons.recycle();
                // TO DO
                // isEnabled() == false 라도 뷰가 자동으로 disable 되지 않는다.
                // 원래 그런지 모르겠지만 여기서 수동으로 enable 상태를 업데이트 해 주어야 한다.
                view.setEnabled(mDrawerList.isEnabled() && isEnabled(position));
                return view;
            }
        };
        mDrawerList.setAdapter(mMenuAdaptor);
        // 메뉴 선택 이벤트 핸들러를 등록한다
        mDrawerList.setOnItemSelectedListener(new ListView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                index = position;
//                Log.d(TAG, "onItemSelected = " + position);

                //selectMenu(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        mDrawerList.setOnItemClickListener((parent, view, position, id) -> {
            index = position;
//            Log.d(TAG, "setOnItemClickListener = " + position);

            clickMenu(position);
        });
        mDrawerList.setFastScrollEnabled(true);


        mPtzSubmenuFragment = PtzMenuFragment.newInstance(supportsUTC());
        mUserSubmenuFragment = SettingsFragment.newInstance(R.xml.prefs_settings);
        mUserSubmenuFragmentHDMI = SettingsFragment.newInstance(R.xml.prefs_settings_hdmi);

        mOverlayView = findViewById(R.id.content_overlay);

        // 프래그먼트를 미리 만들어 둔다
        mPtzOverlay = PtzOverlayFragment.newInstance(supportsUTC());

    }


    private boolean isDrawerOpen() {
        return mDrawer.isDrawerOpen(mMainMenu);
    }

    /**
     * /**
     * 서랍을 연다.
     *
     * @return 서랍을 열었으면 true, 아니면 false
     */
    private boolean openDrawer() {


        if (isRecording()) {
            stopRecording();
        }

        if (!isDrawerOpen()) {

            mDrawer.openDrawer(mMainMenu);

            mDrawerList.setFocusable(true);
            mDrawerList.setFocusableInTouchMode(true);


//            Log.d(TAG, "onClick: Drawer1 = " + mDrawerList.getFirstVisiblePosition());
//            Log.d(TAG, "openDrawer Focus: " + getCurrentFocus());

            mDrawerList.requestFocus();
            mDrawerList.requestFocusFromTouch();  //This is critical

//            Log.d(TAG, "openDrawer Focus1: " + index);

            mDrawerList.setSelectionFromTop(index, 0);
            mDrawerList.setSelection(index);
//            Log.d(TAG, "onClick: Drawer2 = " + mDrawerList.getFirstVisiblePosition());
//            Log.d(TAG, "Test: " + mDrawerList.getSelectedItemPosition());
//            Log.d(TAG, "openDrawer Focus2 = " + getCurrentFocus());

            if(mMode != MODE_PTZ) {
                stopPtzMode();
            }


            return true;
        } else {
            return false;
        }
    }

    /**
     * 서랍을 닫는다.
     *
     * @return 서랍을 닫았으면 true, 아니면 false
     */
    private boolean closeDrawer() {
        if (isDrawerOpen()) {

            mDrawer.closeDrawer(mMainMenu);

//            mDrawerList.clearFocus();
//            mDrawerList.clearChoices();

//            mDrawer.setFocusable(false);
//            mDrawer.setFocusableInTouchMode(false);
//            Log.d(TAG, "Focus: " + getCurrentFocus());

            return true;
        } else {
            return false;
        }
    }

//--------------------------------------------------------------------------------------------------

    /**
     * BACK 키가 눌렸을 때 메인 메뉴가 열려 있으면 닫고 아니면 실행을 종료한다.
     */
    @Override
    public void onBackPressed() {
//        Log.d(TAG, "onBackPressed: " + mMode);

        if (!closeDrawer()) {

            switch (mMode) {
                case MODE_VIEW:
                    if (mSource.is(VideoSource.SDI)) {
                        if (hideCrcStats()) {
                            return;
                        }
                        if (hideTdmSetup()) {
                            btnChk = true;
//                            longKeyChkCnt = 0;
                            return;
                        }
                    }

                    if (pocMode) {
                        exitPocMode();
                        enterVideoMode();
                        return;
                    }

                    break;

                case MODE_RECORD:
                    if (isRecording()) {
                        stopRecording();
                        return;
                    }

                case MODE_SNAPSHOT:
                    enterVideoMode();
                    isCaptureReady = STAT_READY;
                    return;

                case MODE_POC:
                    if (hideCrcStats()) {
                        return;
                    }
                    exitPocMode();
                    enterVideoMode();

                    return;
            }
//            Log.d(TAG, "onBackPressed : " + mMode);
//            Log.d(TAG, "onBackPressed Focus: " + getCurrentFocus());
            super.onBackPressed();
        }
    }

    /**
     * 제어 프래그먼트에 키 이벤트를 전달한다.
     * 각 제어 프래그먼트는 이를 분석하여 자신의 제어 모드에 맞는 키면 callback interface를 통해 해당 이벤트를 발생시킨다.
     *
     * @param keyCode 키 값
     * @param event   이벤트
     * @return 이벤트를 처리했으면 true, 아니면 false
     */
    private boolean forwardKeyToControlFragment(int keyCode, KeyEvent event) {
        if (!isDrawerOpen()) {
            PtzControlFragment controlFragment = (PtzControlFragment) getSupportFragmentManager().findFragmentByTag(PtzControlFragment.TAG);
            if (controlFragment != null) {
                return controlFragment.onKeyPress(keyCode, event);
            }
        }
        return false;
    }

    int longKeyChkCnt = 0;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
//        Log.d(TAG, "onKeyDown=" + keyCode);
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:

                break;
            case KeyEvent.KEYCODE_VOLUME_DOWN:

                break;
            case KeyEvent.KEYCODE_ENTER:
                switch (mMode) {
                    case MODE_VIEW:
                    case MODE_POC:
                        if (mSource.is(VideoSource.SDI)) {
                            longKeyChkCnt++;
                            if (longKeyChkCnt == 5) {
                                if (mSignalInfo != null && !hasNoSignal(mSignalInfo)) {
                                    showCrcStats();
                                    restartCrcStats();
                                }
                            }
                        }
                        break;
                }
                break;

            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
                switch (mMode) {
                    case MODE_VIEW:
                        if (SdiInput.MODE_EX_TDM == mSignalInfo.mode) {
//                            longKeyChkCnt++;
//                            if (longKeyChkCnt == 3) {
                            if (!hasNoSignal(mSignalInfo)) {
                                if (btnChk) {
                                    btnChk = false;
                                    showTdmSetup();
                                    restartTdrCheck();
                                    tdm_ch1.requestFocus();
                                    return true;
                                }
                            }
//                            }
                        }
                }
                if (forwardKeyToControlFragment(keyCode, event)) {
                    return true;
                }

                break;
            case KeyEvent.KEYCODE_MENU:
            case KeyEvent.KEYCODE_BUTTON_MODE:
                if (forwardKeyToControlFragment(keyCode, event)) {
                    return true;
                }
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:

                if (forwardKeyToControlFragment(keyCode, event)) {
                    return true;
                }

            default:
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
//        Log.d(TAG, "onKeyUp=" + keyCode);
        Log.d(TAG, "Focus: " + getCurrentFocus() + " btnchk: " + btnChk);
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_UP:
//                if (isPlaying)
//                    audioManager.setParameters("dac_volume=" + audioManager.getStreamVolume(STREAM_MUSIC));
                break;
            case KeyEvent.KEYCODE_MENU:
                if (forwardKeyToControlFragment(keyCode, event)) {
                    return true;
                }

                // MENU 키를 눌렀을 때 메인 메뉴가 닫혀 있으면 메뉴를 연다.
                if (!onPocDialog) {
//                    Log.d(TAG, "onPocDialog : " + onPocDialog);
                    if (openDrawer()) {
//                        Log.d(TAG, "KEYCODE_DPAD Focus: " + mDrawerList.getSelectedItemPosition());

                        return true;
                    }
                }

                // TO DO
                return true;

            case KeyEvent.KEYCODE_ENTER:
                if (!isDrawerOpen()) {
                    switch (mMode) {
                        case MODE_VIEW:
                        case MODE_POC:
                            if (isLevelMeterEnabled(PreferenceManager.getDefaultSharedPreferences(this))) {
                                if (!mSource.is(VideoSource.HDMI)) {
                                    if (mSource.is(VideoSource.SDI)) {
                                        if (longKeyChkCnt < 5) {
                                            resetFocusLevel();
                                        }
                                    } else resetFocusLevel();
                                }
                            }
                            longKeyChkCnt = 0;
                            return true;
                        case MODE_RECORD:
                            toggleRecording();
                            return true;

                        case MODE_SNAPSHOT:
                            takeSnapshot();
                            return true;
                    }
                }
            case KeyEvent.KEYCODE_BUTTON_MODE:

            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
//                Log.d(TAG, "KEYCODE_DPAD Focus: " + mDrawerList.getSelectedItemPosition());

            default:
                if (forwardKeyToControlFragment(keyCode, event)) {
                    return true;
                }
                break;
        }
        //Log.d(TAG, "onKeyUp="+keyCode+", mMode="+mMode);
        return super.onKeyUp(keyCode, event);
    }

    public void onClick(View view) {
//        Log.d(TAG, "onClick()" + view.getId());

        if (sourceId.equals(VideoSource.SDI)) {
            _sdiInput = (SdiInput) mVideoInput;

        }
        switch (view.getId()) {
            case R.id.drawer_button:
//                Log.d(TAG, "onClick: Drawer = " + getCurrentFocus());
                openDrawer();
//                mDrawerList.requestFocus();
                break;
            case R.id.surface:
                break;
            case R.id.tdm_set_button:
                if (btnChk) {
                    btnChk = false;
                    showTdmSetup();
                    restartTdrCheck();
                    tdm_ch1.setFocusableInTouchMode(true);
                    tdm_ch1.requestFocus();
                    if (tdm_ch1.isFocusableInTouchMode()) {
                        tdm_ch1.setFocusableInTouchMode(false);
                    }
                } else {
                    mTdmSetupView.setVisibility(View.INVISIBLE);
                    hideTdmSetup();
                }

                break;
            case R.id.tdm_ch1:
                _sdiInput.setTdm('1');
                tdm_ch1.setFocusableInTouchMode(true);
                tdm_ch1.requestFocus();
//                if(tdm_ch1.isFocusableInTouchMode()) {
//                    tdm_ch1.setFocusableInTouchMode(false);
//                }
                break;
            case R.id.tdm_ch2:
                _sdiInput.setTdm('2');
                tdm_ch2.setFocusableInTouchMode(true);
                tdm_ch2.requestFocus();
                if (tdm_ch2.isFocusableInTouchMode()) {
                    tdm_ch2.setFocusableInTouchMode(false);
                }
                break;
            case R.id.tdm_ch3:
                _sdiInput.setTdm('3');
                tdm_ch3.setFocusableInTouchMode(true);
                tdm_ch3.requestFocus();
                if (tdm_ch3.isFocusableInTouchMode()) {
                    tdm_ch3.setFocusableInTouchMode(false);
                }
                break;
            case R.id.tdm_ch4:
                _sdiInput.setTdm('4');
                tdm_ch4.setFocusableInTouchMode(true);
                tdm_ch4.requestFocus();
                if (tdm_ch4.isFocusableInTouchMode()) {
                    tdm_ch4.setFocusableInTouchMode(false);
                }
                break;
            case R.id.tdm_ch5:
                _sdiInput.setTdm('5');
                tdm_ch5.setFocusableInTouchMode(true);
                tdm_ch5.requestFocus();
                if (tdm_ch5.isFocusableInTouchMode()) {
                    tdm_ch5.setFocusableInTouchMode(false);
                }
                break;
            case R.id.tdm_ch6:
                _sdiInput.setTdm('6');
                tdm_ch6.setFocusableInTouchMode(true);
                tdm_ch6.requestFocus();
                if (tdm_ch6.isFocusableInTouchMode()) {
                    tdm_ch6.setFocusableInTouchMode(false);
                }
                break;
            case R.id.tdm_ch7:
                _sdiInput.setTdm('7');
                tdm_ch7.setFocusableInTouchMode(true);
                tdm_ch7.requestFocus();
                if (tdm_ch7.isFocusableInTouchMode()) {
                    tdm_ch7.setFocusableInTouchMode(false);
                }
                break;
            case R.id.tdm_ch8:
                _sdiInput.setTdm('8');
                tdm_ch8.setFocusableInTouchMode(true);
                tdm_ch8.requestFocus();
                if (tdm_ch8.isFocusableInTouchMode()) {
                    tdm_ch8.setFocusableInTouchMode(false);
                }
                break;

        }
    }


    int getBit(int x, int n) {
        return (x & (1 << n)) >> n;
    }

    float oldXvalue;
    float oldYvalue;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {

        int width = ((ViewGroup) v.getParent()).getWidth() - v.getWidth();
        int height = ((ViewGroup) v.getParent()).getHeight() - v.getHeight();

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            oldXvalue = event.getX();
            oldYvalue = event.getY();
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            v.setX(event.getRawX() - oldXvalue);
            v.setY(event.getRawY() - (oldYvalue + v.getHeight()));
            //  Log.i("Tag2", "Action Down " + me.getRawX() + "," + me.getRawY());
        } else if (event.getAction() == MotionEvent.ACTION_UP) {

            if (v.getX() > width && v.getY() > height) {
                v.setX(width);
                v.setY(height);
            } else if (v.getX() < 0 && v.getY() > height) {
                v.setX(0);
                v.setY(height);
            } else if (v.getX() > width && v.getY() < 0) {
                v.setX(width);
                v.setY(0);
            } else if (v.getX() < 0 && v.getY() < 0) {
                v.setX(0);
                v.setY(0);
            } else if (v.getX() < 0 || v.getX() > width) {
                if (v.getX() < 0) {
                    v.setX(0);
                    v.setY(event.getRawY() - oldYvalue - v.getHeight());
                } else {
                    v.setX(width);
                    v.setY(event.getRawY() - oldYvalue - v.getHeight());
                }
            } else if (v.getY() < 0 || v.getY() > height) {
                if (v.getY() < 0) {
                    v.setX(event.getRawX() - oldXvalue);
                    v.setY(0);
                } else {
                    v.setX(event.getRawX() - oldXvalue);
                    v.setY(height);
                }
            }
        }
        return true;
    }

    //    @Override
//    public boolean dispatchTouchEvent(MotionEvent ev) {
//        return mLockTouchScreen || super.dispatchTouchEvent(ev);
//    }

//--------------------------------------------------------------------------------------------------
    /*
    private void selectMenu(int position) {
        switch (position) {
            case 0:
                break;

            case 1:
                break;

            case 2:
                break;

            case 3:
                break;

            case 4:
                break;

            case 5:
                break;

            default:
                break;
        }

        setMenuFragment(null);

        unlockMenu();
    }
*/

    /**
     * 키나 터치로 메뉴를 실행했을 때의 동작을 구현한다.
     *
     * @param position 실행한 메뉴의 인덱스
     */
    private void clickMenu(int position) {
        int mode;

        switch (position) {
            case 0:
                mode = MODE_PTZ;
                break;

            case 1:
                mode = MODE_RECORD;
                break;

            case 2:
                mode = MODE_SNAPSHOT;
                break;

            case 3:
                mode = MODE_VIEW;
                break;

            case 4:
//                Log.d(TAG, "clickMenu : " + position + " , " + mMode);
                if (position == mMode) return;
                mode = MODE_POC;
                break;

            case 5:
                mode = MODE_VIEW;
                break;

            default:
                return;
        }
//        Log.d(TAG, "clickMenu after: " + mode + " , " + mMode);

        if (mode != mMode) {
            switch (mMode) {
                case MODE_VIEW:
//                    Log.d(TAG, "Mode _ View ");
                    hideSignalInfo();
                    break;


                case MODE_POC:
                    if (mode == MODE_RECORD || mode == MODE_PTZ || mode == MODE_SNAPSHOT) {
                        hideSignalInfo();
                    }
                    break;
                case MODE_PTZ:
                    break;
            }
        }

        Fragment menuFragment = null;
        boolean delayModeSwitch = false;

        switch (position) {
            case 0:
                menuFragment = mPtzSubmenuFragment;
                delayModeSwitch = true;
                break;

            case 1:
                break;

            case 2:
                break;

            case 3:
                // 갤러리 앱을 실행한다
                launchGalleryApp();
                onBackPressed();
                //break;
                return;

            case 4:
                break;

            case 5:
                if (mSource.is(VideoSource.HDMI)) {
                    menuFragment = mUserSubmenuFragmentHDMI;
                } else {
                    menuFragment = mUserSubmenuFragment;
                }
                delayModeSwitch = true;
                break;
        }

        setMenuFragment(menuFragment);

        // 오버레이를 미리 표시해 둔다. 메뉴서랍이 닫혀야 표시가 된다.
        setOverlayFragment(mode);

        if (menuFragment != null) {
            lockMenu();
        }

        if (delayModeSwitch) {
            // 하위 메뉴가 있는 경우에는 사용자가 설정을 마칠때까지 모드 전환을 연기한다.
            mPendingMode = mode;
        } else {
            enterMode(mode);
            closeDrawer();
        }
    }

    /**
     * 메인 메뉴를 선택할 수 없게 한다.
     * 하위 메뉴를 표시할 때 메인 메뉴를 선택할 수 없게 하여 키 조작시 UX를 향상시키기 위함.
     */
    private void lockMenu() {
//        Log.d(TAG, "lockMenu()");
        mDrawerList.setEnabled(false);
        mDrawerList.setFocusable(false);
        mMenuAdaptor.notifyDataSetChanged();
    }

    /**
     * 메인 메뉴를 선택할 수 있게 한다.
     */
    private void unlockMenu() {
//        Log.d(TAG, "unlockMenu()");
        mDrawerList.setFocusable(true);
        mDrawerList.setEnabled(true);
        mMenuAdaptor.notifyDataSetChanged();
    }

    /**
     * 하위 메뉴 프래그먼트를 표시하거나 제거한다.
     *
     * @param menuFragment null이면 제거, 아니면 표시한다.
     */
    private void setMenuFragment(Fragment menuFragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();

        if (menuFragment != null) {
            fragmentManager.beginTransaction()
                    .replace(R.id.sub_menu, menuFragment, MENU_FRAGMENT_TAG)
                    .commit();

            mSubMenu.setVisibility(View.VISIBLE);
            //Log.d(TAG, "mSubMenu.setVisibility(View.VISIBLE);");
        } else {
            Fragment prevFragment = fragmentManager.findFragmentByTag(MENU_FRAGMENT_TAG);
            if (prevFragment != null) {
                fragmentManager.beginTransaction().remove(prevFragment).commit();
            }

            mSubMenu.setVisibility(View.GONE);
            //Log.d(TAG, "mSubMenu.setVisibility(View.GONE);");
        }

        mCurrentSubmenuFragment = menuFragment;
    }

    /**
     * 전환할 모드에 따른 오버레이 프래그먼트를 표시한다
     *
     * @param mode 전환할 모드
     */
    private void setOverlayFragment(int mode) {
        Fragment overlayFragment = null;

        switch (mode) {
            case MODE_VIEW:
                if (pocMode) {
                    showPocOverlay();
                } else {
                    hidePocOverlay();
                }
                break;

            case MODE_PTZ:
                if (supportsPTZ()) {
                    overlayFragment = mPtzOverlay;
                    if (mSignalInfo != null) {
                        if (SdiInput.MODE_EX_TDM == mSignalInfo.mode) {
                            tdm_set.setVisibility(View.INVISIBLE);

                        }
                    }
                }
                mNoSignalView.setVisibility(View.INVISIBLE);
                mSignalInfoView.setVisibility(View.INVISIBLE);

                if (pocMode) {
                    hidePocOverlay();
                    mLevelMeterView.setVisibility(View.INVISIBLE);
                }
                break;

            case MODE_RECORD:
//                if(mSignalInfo != null) mNoSignalView.setVisibility(View.VISIBLE);
                overlayFragment = RecordOverlayFragment.newInstance(RecordOverlayFragment.CAPTURE_MOVIE);

                break;

            case MODE_SNAPSHOT:
//                if(mSignalInfo != null) mNoSignalView.setVisibility(View.VISIBLE);
                overlayFragment = RecordOverlayFragment.newInstance(RecordOverlayFragment.CAPTURE_PHOTO);

                break;

            case MODE_POC:
//                overlayFragment = PocOverlayFragment.newInstance();
//                mPocOverlay.setVisibility(View.VISIBLE);
                break;


            default:
                break;

        }

        FragmentManager fragmentManager = getSupportFragmentManager();

        if (overlayFragment != null) {
            fragmentManager.beginTransaction()
                    .replace(R.id.content_overlay, overlayFragment, OVERLAY_FRAGMENT_TAG)
                    .commit();
        } else {
            overlayFragment = fragmentManager.findFragmentByTag(OVERLAY_FRAGMENT_TAG);
            if (overlayFragment != null) {
                fragmentManager.beginTransaction().remove(overlayFragment).commit();
            }
        }
    }

    /**
     * 계류 중인 모드 전환이 있으면 이를 실행한다.
     */
    private void switchToPendingMode() {
        if (mPendingMode != MODE_INVALID) {
            enterMode(mPendingMode);
        } else if (mMode == MODE_VIEW || mMode == MODE_POC) {
            if (mSignalInfo != null) showSignalInfo(mSignalInfo);
        }
    }

    /**
     * 모드를 바꾸고 서랍을 닫는다.
     *
     * @param mode 전환할 모드
     */
    private void enterMode(int mode) {
//        Log.d(TAG, "Enter Mode: " + mode);
        switch (mode) {
            case MODE_PTZ:
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
                PtzSettings ptzSettings = getPtzSettings(sharedPreferences);
                changePtzMode(ptzSettings, true);
                break;

            case MODE_POC:
//                stopPtzMode();
                if (!pocMode) startPocMode();


                break;

            case MODE_VIEW:
//                stopPtzMode();
                if (mSignalInfo != null) showSignalInfo(mSignalInfo);
                if (pocMode) mode = MODE_POC;
                break;
            default:
//                stopPtzMode();
                break;
        }

        mPendingMode = MODE_INVALID;
        mMode = mode;

        // TO DO: 이게 필요한가? 없애거나 더 적당한 위치로 옮기는 것이 바람직하다.
        //closeDrawer();

        mDrawer.requestFocus();
    }

    private void enterVideoMode() {
        setOverlayFragment(MODE_VIEW);
        enterMode(MODE_VIEW);
        stopPtzMode();
    }

    private void enterPtzMode() {
//        Log.d(TAG, "EnterPtzMode");
        setOverlayFragment(MODE_PTZ);
        enterMode(MODE_PTZ);
    }

    //--------------------------------------------------------------------------------------------------
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * 사용자 설정 메뉴를 표시하기 전에 SD카드가 있는지 확인하여 없으면 설정을 disable한다.
     */
    @Override
    public void onResumeSettings() {
//        Log.d(TAG, "onResumeSettings()");
        updateMediaFolderPreferenceEnabled();
    }

    /**
     * 개별 설정을 선택하면 뜨는 목록의 표시를 처리한다
     *
     * @param preferenceFragmentCompat 메소드를 호출한 fragment
     * @param preference               대화상자를 표시한 preference
     * @return 대화상자를 표시했으면 true, 아니면 false
     */
    @Override
    public boolean onPreferenceDisplayDialog(@NonNull PreferenceFragmentCompat
                                                     preferenceFragmentCompat, Preference preference) {
        if (mCurrentSubmenuFragment == null) {
//            Log.w(TAG, "mCurrentSubmenuFragment is null. Using default DialogPreference behaviour!");
            return false;
        }
        //Log.i(TAG, "Opening dialog for " + preference.getKey());

        final DialogFragment fragment;
        if (preference instanceof ListPreference) {
            fragment = ListPanelFragment.newInstance(preference.getKey());
        } else {
            return false;
        }
        fragment.setTargetFragment(mCurrentSubmenuFragment, 0);
        fragment.show(getSupportFragmentManager(), PANEL_FRAGMENT_TAG);

        return true;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.pref_ptz_mode_utc)) ||
                key.equals(getString(R.string.pref_ptz_mode_sdi))) {
            Log.d(TAG, key + "=" + sharedPreferences.getString(key, null));
        } else if (key.equals(getString(R.string.pref_ptz_protocol))) {
            if (!isDrawerOpen() && mPtzControl != null) {
                if (mPtzControl instanceof PtzAnalyzer) {
                    PtzAnalyzer analyzer = (PtzAnalyzer) mPtzControl;

                    analyzer.setProtocol(getPtzProtocol(sharedPreferences));
                }
            }
        } else //noinspection StatementWithEmptyBody
            if (key.equals(getString(R.string.pref_ptz_address))) {

            } else if (key.equals(getString(R.string.pref_ptz_baudrate))) {
                if (!isDrawerOpen() && mPtzControl instanceof PtzMode) {
                    PtzMode ptzMode = (PtzMode) mPtzControl;
                    ptzMode.setBaudRate(getPtzBaudRate(sharedPreferences));
                }
            } else if (key.equals(getString(R.string.pref_level_meter))) {
                if (sharedPreferences.getBoolean(key, true)) {
                    showLevelMeter();
                    mLevelMeterView.setVisibility(View.VISIBLE);
                } else {
                    hideLevelMeter();
                    mLevelMeterView.setVisibility(View.INVISIBLE);
                }
            }
//            else if (key.equals(getString(R.string.pref_recording_resolution))) {
//
//                boolean recordMode;
//                if (sharedPreferences.getBoolean(key, true)) {
//                    recordMode = false;
////                    showToast(R.string.recording_2);
//                } else {
//                    recordMode = true;
////                    showToast(R.string.recording_1);
//                }
//            }
//            else if (key.equals(getString(R.string.pref_view_change))) {
//                runDialog(1);
//                if (sharedPreferences.getBoolean(key, true)) {
//
//                    mSurfaceView.setLayoutParams(new FrameLayout.LayoutParams(1280, 736));
//                    SharedPreferences layout_pref = getSharedPreferences("layout", MODE_PRIVATE);
//                    SharedPreferences.Editor editor = layout_pref.edit();
//                    editor.putInt("width", 1280);
//                    editor.putInt("height", 736);
//                    editor.apply();
//
//                } else {
//
//                    mSurfaceView.setLayoutParams(new RelativeLayout.LayoutParams(980, 736));
//                    SharedPreferences layout_pref = getSharedPreferences("layout", MODE_PRIVATE);
//                    SharedPreferences.Editor editor = layout_pref.edit();
//                    editor.putInt("width", 980);
//                    editor.putInt("height", 736);
//                    editor.apply();
//
//                }
//            }
/*
        } else if (key.equals(getString(R.string.pref_ptz_termination))) {
            Log.d(TAG, key + "=" + sharedPreferences.getBoolean(key, false));
        } else if (key.equals(getString(R.string.pref_media_folder))) {
        } else if (key.equals(getString(R.string.pref_menu_transparency))) {
            int transparency = Integer.parseInt(sharedPreferences.getString(key, "5"));
            Log.d(TAG, key + "=" + transparency);
        } else if (key.equals(getString(R.string.pref_touch_lock))) {
            mLockTouchScreen = isTouchScreenLocked(sharedPreferences);
        else {
            Log.w(TAG, "onSharedPreferenceChanged " + key + "=" + sharedPreferences.getString(key, null));
        }
*/
    }


    private String getPtzModeKey() {
        String key;
        switch (sourceId) {
            case VideoSource.SDI:
                key = getString(R.string.pref_ptz_mode_sdi);
                break;

            default:
                key = getString(R.string.pref_ptz_mode_utc);
                break;
        }
        return key;
    }

    /**
     * 사용자가 설정한 PTZ 모드를 반환한다.
     *
     * @param sharedPreferences 설정을 읽을 SharedPreferences 인스턴스
     * @return 정수의 모드값. 디폴트는 0.
     */
    private int getPtzMode(SharedPreferences sharedPreferences) {
        return Integer.parseInt(sharedPreferences.getString(getPtzModeKey(), "0"));
    }

    /*
      현재 PTZ 모드가 UTC 모드인지를 반환한다.

      @param sharedPreferences 설정을 읽을 SharedPreferences 인스턴스
     * @return UTC 모드면 true, 아니면 false
     */
//    private boolean isUtcMode(SharedPreferences sharedPreferences) {
//        return getPtzMode(sharedPreferences) == PtzMode.UTC;
//    }

    /**
     * 사용자가 설정한 PTZ 프로토콜을 반환한다.
     *
     * @param sharedPreferences 설정을 읽을 SharedPreferences 인스턴스
     * @return 정수의 프로토콜 인덱스. 디폴트는 0.
     */
    private int getPtzProtocol(SharedPreferences sharedPreferences) {
        return Integer.parseInt(sharedPreferences.getString(getString(R.string.pref_ptz_protocol), "0"));
    }

    /**
     * 사용자가 설정한 UTC 프로토콜을 반환한다.
     *
     * @param sharedPreferences 설정을 읽을 SharedPreferences 인스턴스
     * @return 정수의 프로토콜 인덱스. 디폴트는 0.
     */
    private int getUtcProtocol(SharedPreferences sharedPreferences) {
        int key;
//        Log.d(TAG, "SourceID : " + sourceId);
        if (sourceId == null) {
            sourceId = VideoSource.TVI;
        }
        switch (sourceId) {
            case VideoSource.CVBS:
                key = R.string.pref_utc_protocol_cvbs;
                break;

            case VideoSource.TVI:
                key = R.string.pref_utc_protocol_tvi;
                break;

            case VideoSource.AHD:
                key = R.string.pref_utc_protocol_ahd;
                break;

            case VideoSource.CVI:
                key = R.string.pref_utc_protocol_cvi;
                break;


            default:
                return 0;
        }
//
        return Integer.parseInt(sharedPreferences.getString(getString(key), "0"));
    }

    /**
     * 사용자가 설정한 PTZ 주소를 반환한다.
     *
     * @param sharedPreferences 설정을 읽을 SharedPreferences 인스턴스
     * @return 정수의 주소값. 디폴트는 1.
     */
    private char getPtzAddress(SharedPreferences sharedPreferences) {
        return (char) Integer.parseInt(sharedPreferences.getString(getString(R.string.pref_ptz_address), "1"));
    }

    /**
     * 사용자가 설정한 PTZ 속도를 반환한다.
     *
     * @param sharedPreferences 설정을 읽을 SharedPreferences 인스턴스
     * @return 정수의 속도값. 디폴트는 2400.
     */
    private int getPtzBaudRate(SharedPreferences sharedPreferences) {
        final String[] ptzBaudRates = getResources().getStringArray(R.array.ptz_baudrates);
        return Integer.parseInt(ptzBaudRates[Integer.parseInt(sharedPreferences.getString(getString(R.string.pref_ptz_baudrate), "0"))]);
    }

    /**
     * 사용자가 설정한 PTZ 터미네이션 설정을 반환한다.
     *
     * @param sharedPreferences 설정을 읽을 SharedPreferences 인스턴스
     * @return 터미네이션 설정값. true면 enable, false면 disable.
     */
    private boolean getPtzTermination(SharedPreferences sharedPreferences) {
        return sharedPreferences.getBoolean(getString(R.string.pref_ptz_termination), false);
    }

    private boolean useExternalSd(SharedPreferences sharedPreferences) {
//        Log.d(TAG, "pref_media_folder = " + sharedPreferences.getBoolean(getString(R.string.pref_media_folder), false));
        return !sharedPreferences.getBoolean(getString(R.string.pref_media_folder), false);
    }

//    private boolean isTouchScreenLocked(SharedPreferences sharedPreferences) {
//        return sharedPreferences.getBoolean(getString(R.string.pref_touch_lock), false);
//    }

    private boolean isLevelMeterEnabled(SharedPreferences sharedPreferences) {
        //Log.d(TAG, "pref_level_meter = " + sharedPreferences.getBoolean(getString(R.string.pref_level_meter), false));
        return sharedPreferences.getBoolean(getString(R.string.pref_level_meter), false);
    }


    class PtzSettings {
        int mode;
        final int protocol;
        final char address;
        final int baudRate;
        final boolean termination;

        PtzSettings(int mode, int protocol, char address, int baudRate, boolean termination) {
            this.mode = mode;
            this.protocol = protocol;
            this.address = address;
            this.baudRate = baudRate;
            this.termination = termination;
        }
    }

    /**
     * 모든 PTZ 설정을 반환한다.
     * UTC 모드인 경우에는 PTZ 프로토콜이 아닌 UTC 프로토콜을 반환한다.
     *
     * @param sharedPreferences 설정을 불러올 공유 사용자 설정 객체
     * @return PtzSettings
     */
    public PtzSettings getPtzSettings(SharedPreferences sharedPreferences) {
        int protocol = 0;
        int mode = getPtzMode(sharedPreferences);
        if (initSource.equals(VideoSource.SDI)) {
            if (mode != PtzMode.UCC) {
                protocol = getPtzProtocol(sharedPreferences);
            } else {
                protocol = getUtcProtocol(sharedPreferences);
            }
        } else if (initSource.equals(VideoSource.AUTO)) {
            if (mode != PtzMode.UTC) {
                protocol = getPtzProtocol(sharedPreferences);
            } else {
                protocol = getUtcProtocol(sharedPreferences);
            }
        }

        return new PtzSettings(mode, protocol, getPtzAddress(sharedPreferences), getPtzBaudRate(sharedPreferences), getPtzTermination(sharedPreferences))
                ;


    }


    /**
     * PTZ 모드를 시작한다.
     *
     * @param ptzSettings 새롭게 시작할 PTZ 모드의 설정을 나타내는 PtzSettings 객체
     */

    public void changePtzMode(PtzSettings ptzSettings, boolean i) {
        int v = 0;

        if (ptzSettings.mode == PtzMode.UTC && !sourceId.equals(VideoSource.SDI)) {
            int type = utcTypeNum();
            UtcWriter utcWriter = new UtcWriter(ptzSettings.address, type, utcType());
            utcWriter.start();
            mPtzControl = utcWriter;
        }

        if (mPtzSettings != null &&
                mPtzSettings.mode == ptzSettings.mode &&
                mPtzSettings.protocol == ptzSettings.protocol &&
                mPtzSettings.address == ptzSettings.address &&
                mPtzSettings.baudRate == ptzSettings.baudRate &&
                mPtzSettings.termination == ptzSettings.termination) {
            return;
        }


//        if (i) {
//            stopPtzMode();
//        } else {
//            stopPtzOverlayMode();
//
//        }

        // 시작하기 전에 터미네이션 설정을 먼저 적용
        mMcuControl.setTermination(ptzSettings.termination);
//        Log.d(TAG, "Termination: " + ptzSettings.termination);
//        Log.d(TAG, "Protocol: " + ptzSettings.protocol);
//        Log.d(TAG, "Address: " + Character.toString(ptzSettings.address));
//        Log.d(TAG, "Baudrate: " + ptzSettings.baudRate);
//        Log.d(TAG, "PtzSettings = " + ptzSettings.mode);

        switch (ptzSettings.mode) {
            case PtzMode.TX:
                PtzWriter writer = new PtzWriter(mMcuControl, ptzSettings.protocol, ptzSettings.address, ptzSettings.baudRate);
                setModeUcc(0);
                writer.startTx();
                writer.setMode(PtzWriter.MODE_PT);
                mPtzControl = writer;
                break;

            case PtzMode.UTC:
                if (sourceId.equals(VideoSource.SDI)) {
                    PtzWriter writerUcc = new PtzWriter(mMcuControl, ptzSettings.protocol, ptzSettings.address, ptzSettings.baudRate);
                    setModeUcc(1);
                    writerUcc.startUcc();
                    writerUcc.setMode(PtzWriter.MODE_PT);
                    mPtzControl = writerUcc;
                    ptzSettings.mode = PtzMode.UCC;
                } else {
                    int type = utcTypeNum();
                    UtcWriter utcWriter = new UtcWriter(ptzSettings.address, type, utcType());
                    utcWriter.start();
                    mPtzControl = utcWriter;
                }

                break;

            case PtzMode.RX:
                PtzReader reader = new PtzReader(mMcuControl, ptzSettings.baudRate);

                reader.start((reader1, bytes) -> {
                    // RS485 통신은 다른 thread에서 실행하므로 UI thread로 실행을 옮겨주어야 한다.
                    runOnUiThread(() -> {
                        if (!onPtzMenu) {
                            PtzRxContentsFragment fragment = (PtzRxContentsFragment) getSupportFragmentManager().findFragmentByTag(PtzOverlayFragment.CONTENTS_FRAGMENT_TAG);
                            if (fragment != null) {
                                fragment.addBytes(PtzReader.buildHexString(bytes));
                            }
                        }

                    });
                });
                mPtzControl = reader;

                break;

            case PtzMode.ANALYZER:
                PtzAnalyzer analyzer = new PtzAnalyzer(mMcuControl, ptzSettings.protocol, ptzSettings.baudRate);

                analyzer.start((analyzer1, address, command, packet) -> {
                    final String commandString = PtzAnalyzer.buildCommandString(command);
                    final String packetString = analyzer1.buildPacketString(packet);

                    // RS485 통신은 다른 thread에서 실행하므로 UI thread로 실행을 옮겨주어야 한다.
                    runOnUiThread(() -> {
                        if (!onPtzMenu) {
                            PtzAnalyzerContentsFragment fragment = (PtzAnalyzerContentsFragment)
                                    getSupportFragmentManager().findFragmentByTag(PtzOverlayFragment.CONTENTS_FRAGMENT_TAG);
                            if (fragment != null) {
                                fragment.addPacket(address, commandString, packetString);
                            }
                        }

                    });
                });
                mPtzControl = analyzer;

                break;

        }


        mPtzSettings = ptzSettings;
    }

    private void stopPtzMode() {
//        Log.d(TAG, "stopPtzMode----");
        if (null != mPtzControl) {

            mPtzControl.stop();

            mPtzControl = null;
        }

        // PTZ 모드에서 빠져나가면 같은 설정이라도 다시 실행할 수 있게 해야 한다.
        mPtzSettings = null;
    }

    private void stopPtzOverlayMode() {
//        Log.d(TAG, "stopPtzOverlayMode----");
        if (null != mPtzControl) {
            mPtzControl.stop();

            mPtzControl = null;
        }
        // PTZ 모드에서 빠져나가면 같은 설정이라도 다시 실행할 수 있게 해야 한다.
        mPtzSettings = null;
    }

//--------------------------------------------------------------------------------------------------

    /**
     * PTZ 명령을 카메라로 보낸다.
     *
     * @param command 보낼 명령 코드. PtzControl에 정의되어 있음.
     */
    private void sendPtzCommand(char command) {
        if (mPtzControl != null) {
            mPtzControl.sendCommand(command);
//            Log.d(TAG, "Send Command: " + command);
        }
    }

    @Override
    public void onPtzStop() {
        sendPtzCommand(PtzControl.STOP);
    }

    @Override
    public void onTiltUp() {
        sendPtzCommand(PtzControl.TILT_UP);
    }

    @Override
    public void onTiltDown() {
        sendPtzCommand(PtzControl.TILT_DOWN);
    }

    @Override
    public void onPanLeft() {
        sendPtzCommand(PtzControl.PAN_LEFT);
    }

    @Override
    public void onPanRight() {
        sendPtzCommand(PtzControl.PAN_RIGHT);
    }

    @Override
    public void onZoomIn() {
        sendPtzCommand(PtzControl.ZOOM_IN);
    }

    @Override
    public void onZoomOut() {
        sendPtzCommand(PtzControl.ZOOM_OUT);
    }

    @Override
    public void onFocusFar() {
        sendPtzCommand(PtzControl.FOCUS_FAR);
    }

    @Override
    public void onFocusNear() {
        sendPtzCommand(PtzControl.FOCUS_NEAR);
    }

    @Override
    public void onPtzMenuUp() {
        sendPtzCommand(PtzControl.MENU_UP);
    }

    @Override
    public void onPtzMenuDown() {
        sendPtzCommand(PtzControl.MENU_DOWN);
    }

    @Override
    public void onPtzMenuLeft() {
        sendPtzCommand(PtzControl.MENU_LEFT);
    }

    @Override
    public void onPtzMenuRight() {
        sendPtzCommand(PtzControl.MENU_RIGHT);
    }

    @Override
    public void onPtzMenuOn() {
        sendPtzCommand(PtzControl.MENU_ON);
    }

    @Override
    public void onPtzMenuEnter() {
        sendPtzCommand(PtzControl.MENU_ENTER);
    }

    @Override
    public void onPtzMenuEsc() {
        sendPtzCommand(PtzControl.MENU_ESC);
    }

    /**
     * //     * 오버레이 fragment의 제어 모드를 바꾼다.
     * //     *
     * //     * @param controlMode PtzOverlayFragment의 제어 모드
     * //
     */
    public void setControlMode(int controlMode) {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(OVERLAY_FRAGMENT_TAG);
        if (fragment instanceof PtzOverlayFragment) {
            PtzOverlayFragment ptzOverlayFragment = (PtzOverlayFragment) fragment;
            ptzOverlayFragment.setControlMode(controlMode);
        }
    }

    @Override
    public void onSwitchToZoomFocus() {
        if (mPtzControl instanceof PtzWriter) {
            ((PtzWriter) mPtzControl).setMode(PtzWriter.MODE_ZF);
        }
        setControlMode(PtzOverlayFragment.CONTROL_ZOOM_FOCUS);
    }

    @Override
    public void onSwitchToPanTilt() {
        if (mPtzControl instanceof PtzWriter) {
            ((PtzWriter) mPtzControl).setMode(PtzWriter.MODE_PT);
        }
        setControlMode(PtzOverlayFragment.CONTROL_PAN_TILT);
    }

    @Override
    public void onSwitchToOsd() {
        if (mPtzControl instanceof PtzWriter) {
            ((PtzWriter) mPtzControl).setMode(PtzWriter.MODE_MENU);
        }
        setControlMode(PtzOverlayFragment.CONTROL_OSD);
    }

    private void putIntegerPreference(SharedPreferences sharedPreferences, String key, int value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, Integer.toString(value));
        editor.apply();
    }

    private void changeUtcMode(int value) {
        final String key = getString(R.string.pref_utc_mode);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        putIntegerPreference(sharedPreferences, key, value);
    }

    private void changePtzProtocol(int direction) {
        final String key = getString(R.string.pref_ptz_protocol);
        final String[] ptzProtocols = getResources().getStringArray(R.array.ptz_protocol_values);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        int value = Integer.parseInt(sharedPreferences.getString(key, "0")) + direction;
        if (value == ptzProtocols.length) {
            value = 0;
        } else if (value < 0) {
            value = ptzProtocols.length - 1;
        }
        putIntegerPreference(sharedPreferences, key, value);
    }

    private void changePtzBaudRate(int direction) {
        final String key = getString(R.string.pref_ptz_baudrate);
        final String[] ptzBaudRates = getResources().getStringArray(R.array.ptz_baudrate_values);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        int value = Integer.parseInt(sharedPreferences.getString(key, "0")) + direction;
        if (value == ptzBaudRates.length) {
            value = 0;
        } else if (value < 0) {
            value = ptzBaudRates.length - 1;
        }
        putIntegerPreference(sharedPreferences, key, value);
    }

//    /*
//    private void togglePtzTermination() {
//        final String key = getString(R.string.pref_ptz_termination);
//        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
//        boolean value = sharedPreferences.getBoolean(key, false);
//
//        SharedPreferences.Editor editor = sharedPreferences.edit();
//        editor.putBoolean(key, !value);
//        editor.apply();
//    }
//    */

    @Override
    public void onPrevPtzProtocol() {
        changePtzProtocol(-1);
    }

    @Override
    public void onNextPtzProtocol() {
        changePtzProtocol(1);
    }

    @Override
    public void onIncPtzBaudRate() {
        changePtzBaudRate(1);
    }

    @Override
    public void onDecPtzBaudRate() {
        changePtzBaudRate(-1);
    }

    @Override
    public void onClearPtzScreen() {
        PtzContentsFragment fragment = (PtzContentsFragment)
                getSupportFragmentManager().findFragmentByTag(PtzOverlayFragment.CONTENTS_FRAGMENT_TAG);
        if (fragment != null) {
            fragment.clear();
        }
    }

    @Override
    public void onExitPtzMode() {
        // 비디오 입력 모드로 복귀
        enterVideoMode();
    }

    @Override
    public void onExitMenu() {
        // 비디오 입력 모드로 복귀
        try {
            mMcuControl.setPtzMode(-1);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        enterVideoMode();
        mMenuAdaptor.notifyDataSetChanged();
    }


    private static final int MEDIA_TYPE_IMAGE = 1;
    private static final int MEDIA_TYPE_VIDEO = 2;

    /**
     * Create a File for saving an image or video
     */
    @SuppressLint("DefaultLocale")
    private File getOutputMediaFile(int type) {
        String directory;
        File mediaStorageDir;
//        Log.d(TAG, "getOutputMediaFile: " + mExternalStorageAvailable);

        if (useExternalSd(PreferenceManager.getDefaultSharedPreferences(this)) || !mExternalStorageAvailable) {
            if (type == MEDIA_TYPE_IMAGE) {
                directory = Environment.DIRECTORY_PICTURES;
            } else if (type == MEDIA_TYPE_VIDEO) {
                directory = Environment.DIRECTORY_MOVIES;
            } else {
                return null;
            }

            mediaStorageDir = Environment.getExternalStoragePublicDirectory(directory);
//            Log.d(TAG, "getOutputMediaFile 1: " + mediaStorageDir);

        } else {
            // 저장할 경로를 만든다.
            // 예) /<SD카드 마운트 위치>/SD Card Movies
            directory = mExternalStoragePath +
                    //File.separatorChar + getString(R.string.storage_folder) +
                    File.separatorChar + getString(R.string.storage_external) + ' ';

            if (type == MEDIA_TYPE_IMAGE) {
                directory += Environment.DIRECTORY_PICTURES;
            } else if (type == MEDIA_TYPE_VIDEO) {
                directory += Environment.DIRECTORY_MOVIES;
            } else {
                return null;
            }

            mediaStorageDir = new File(directory);
//            Log.d(TAG, "getOutputMediaFile 2: " + mediaStorageDir);

        }

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.e(TAG, "failed to create directory : " + mediaStorageDir.getPath());
                return null;
            }
        }
//        Log.d(TAG, "getOutputMediaFile 3" );

        // Create a media file name
        String fileName = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.KOREAN).format(new Date());
        fileName += String.format("_%s_%d%c%.2f", sourceId, mSignalInfo.height, mSignalInfo.scan, mSignalInfo.rate);
//        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_" + fileName + ".jpg");
        } else {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_" + fileName + ".mp4");
        }
//        Log.d(TAG, "getOutputMediaFile 4: " + mediaStorageDir);

        return mediaFile;
    }

    /**
     * 미디어 파일을 미디어 라이브러리에 등록하여 갤러리에서 볼 수 있게 한다.
     *
     * @param file_path 등록할 미디어 파일 객체(경로/파일명)
     */
    private void addMediaToLibrary(String file_path) {
        Log.d(TAG, "Start addMediaToLibrary");
        if (file_path != null) {

            MediaScanning scanning = new MediaScanning(mContext);
            scanning.startScan(file_path);

            Log.d(TAG, "File: " + file_path);

            if (isCaptureReady == STAT_SNAPSHOT) {
                Log.i(TAG, "onSnapshot end.");
            } else if (isCaptureReady == STAT_RECORD) {
                Log.i(TAG, "Recording end.");
            }

            isCaptureReady = STAT_READY;

//            MediaScannerConnection.scanFile(
//                    getApplicationContext(),
//                    new String[]{file_path},
//                    null,
//                    (path, uri) -> {
//                        Log.v(TAG, "file " + path + " was scanned Completed: " + uri);
//
//                        if (isCaptureReady == STAT_SNAPSHOT) {   // snapshot
//                            // 1초 이내에 다시 캡쳐하는 경우 파일이 겹쳐지는 경우를 피하기 위한 딜레이
//                            long duration = System.currentTimeMillis() - mRecordingStartTime;
//                            //Log.d(TAG, "duration = " + duration);
//                            if (1000 > duration) {
//                                delay_ms(1500 - duration, 0);       //20181210 ms_cnt 1000 -> 1500 수정 : 셔터 사운드 두번 동작함.
//                            }
////                            new Handler().postDelayed(() -> {
////                                Log.i(TAG, "onSnapshot end.");
////
////                            }, 1000);
//                        } else if (isCaptureReady == STAT_RECORD) {
//                            Log.i(TAG, "Recording end.");
//                        }
//
//                        isCaptureReady = STAT_READY;
//                    });
        }
    }

    public void deleteMediaToLibrary(String file_path) {
        if (file_path != null) {
            File file = new File(file_path);
            file.delete();


            MediaScannerConnection.scanFile(
                    getApplicationContext(), new String[]{file_path}, null, (file_path1, uri) -> {
//                            Log.i(TAG, "Scanned " + file_path + ":");
//                            Log.i(TAG, "uri = " + uri);
                        if (uri != null)
                            mContext.getContentResolver().delete(uri, null, null);
                    }
            );
        }
    }

    public void showToast(int message) {
        if (mActiveToast != null) {
            mActiveToast.cancel();
        }

        mActiveToast = Toast.makeText(MainActivity.this, getString(message), Toast.LENGTH_SHORT);

        mActiveToast.show();
    }

    @Override
    public void onCaptureClick(int captureType) {
//        Log.d(TAG, "onCaptureClick " + captureType);
//        getMode(recordMode);
//        Log.d(TAG, "onCaptureClick0");


        if (!isDrawerOpen()) {

            if (captureType == RecordOverlayFragment.CAPTURE_MOVIE) {
//                Log.d(TAG, "onCaptureClick2");

                toggleRecording();
            } else if (captureType == RecordOverlayFragment.CAPTURE_PHOTO) {

                takeSnapshot();

            }
        }
    }

    private boolean isRecording() {
        return (mVideoInput != null) && mVideoInput.isRecording();
    }

    private void toggleRecording() {

        if (!isRecording()) {
            if (startVideoRecording()) {
                onRecordingStart();
//                Log.d(TAG, "Recording....");
            }
        } else {
            stopRecording();
//            Log.d(TAG, "Stop ! Recording....");

        }
//        Log.d(TAG, "toggle.." + isRecording());
    }

    /**
     * 녹화를 중지하고 후반 처리를 수행한다.
     */
    public void stopRecording() {
        stopVideoRecording();

        onRecordingStop(true, true);
    }


    private void stopRecord() {
        stopVideoRecording();

        onRecordingStop(false, false);
    }

    private void onRecordingStart() {

        updateRecordingState(true);
//        Log.d(TAG, "onRecordingStart");

        mRecordingTimeText = findViewById(R.id.recording_time);
        mRecordingTimeText.setText(getString(R.string.time_start));

        mRecordingStartTime = System.currentTimeMillis();
        mUpdateRecordingTimeTask = () -> {
            long duration = System.currentTimeMillis() - mRecordingStartTime;

            DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss", Locale.KOREAN);
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            String durationText = dateFormat.format(new Date(duration));
            mRecordingTimeText.setText(durationText);
//            Log.d(TAG, "mRecordingTimeText = " + duration);

            mRecordingTimeText.postDelayed(mUpdateRecordingTimeTask, 1000);


            if (chkAvailableStorageSize(false)) {
                stopVideoRecording();
                onRecordingStop(true, false);
                showToast(R.string.msg_storage_space);
                Log.w(TAG, "Stop recording. not enough storage space(<50M)!");
            } else if (duration > 300000) {
                stopVideoRecording();
                onRecordingStop(true, true);
                Log.w(TAG, "Stop recording. Recording duration > 300000");

            }
        };
//        Log.d(TAG, "onRecordingStart1");

        mRecordingTimeText.postDelayed(mUpdateRecordingTimeTask, 1000);
        showToast(R.string.msg_recording_started);


    }

    private void onRecordingStop(boolean readyToRec, boolean stopToast) {
        mRecordingTimeText.removeCallbacks(mUpdateRecordingTimeTask);
        mUpdateRecordingTimeTask = null;

        if (readyToRec) {
            updateRecordingState(false);
        }
        if (stopToast) {
            showToast(R.string.msg_recording_stopped);
        }
    }

    private void updateRecordingState(boolean recording) {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(OVERLAY_FRAGMENT_TAG);
        if (fragment instanceof RecordOverlayFragment) {
            RecordOverlayFragment recordOverlayFragment = (RecordOverlayFragment) fragment;
            recordOverlayFragment.setRecordingState(recording);
        }
    }

    private boolean startVideoRecording() {
        if (isCaptureReady == STAT_READY) {
//            Log.d(TAG, "startVideoRecording0");

            if (hasNoSignal(mSignalInfo)) {
                showToast(R.string.msg_no_signal_to_capture);
                return false;
            }
//
//            if (mSignalInfo.height > 1100) {
//                showToast(R.string.msg_not_support_recording);
//                return false;
//            }

            if (chkAvailableStorageSize(true)) {
                showToast(R.string.msg_storage_space);
                Log.w(TAG, "not enough storage space(<50M)!");
                return false;
            }

            if (mVideoInput != null) {
                isCaptureReady = STAT_RECORD;
//                Log.i(TAG, "Recording start!");

//                stopHdmiAudioPlay(false);
//                Log.d(TAG, "startVideoRecording");

                File file = getOutputMediaFile(MEDIA_TYPE_VIDEO);
                if (file != null) {
//                    Log.d(TAG, "startVideoRecording1");
                    return mVideoInput.startRecording(file.toString());
                }
            }
        }
        return false;
    }


    private void stopVideoRecording() {
        if (mVideoInput != null) {
            Log.d(TAG, "stopVideoRecording");

            // stop 명령이 1초 미만이면 파일이 정상적으로 저장되지 않는 문제를 피하기 위해 1초가 될 때까지 stop을 기다림
            long duration = System.currentTimeMillis() - mRecordingStartTime;

            if (1500 > duration) {
                delay_ms(1500 - duration, 0);
            }
            mediaPath = mVideoInput.stopRecording(hasNoSignal(mSignalInfo));

//            new Handler().postDelayed(() -> {
            if (mediaPath != null) {
                addMediaToLibrary(mediaPath);
            }
//            }, 1000);
//            Log.d(TAG, "stopVideoRecording1: " + mediaPath);

//            if (mSignalInfo.signal)
//                startHdmiAudioPlay(true);
        }
    }

    //--------------------------------------------------------------------------------------------------
    private void takeSnapshot() {
//        getMode(recordMode);

        if (isCaptureReady == STAT_READY) {
            if (hasNoSignal(mSignalInfo)) {
                showToast(R.string.msg_no_signal_to_capture);
                return;
            }

            if (chkAvailableStorageSize(true)) {
                showToast(R.string.msg_storage_space);
                Log.w(TAG, "not enough storage space(<50M)!");
                return;
            }

            if (mVideoInput != null) {
                isCaptureReady = STAT_SNAPSHOT;
//                Log.i(TAG, "Snapshot start!");
                mRecordingStartTime = System.currentTimeMillis();

                mVideoInput.takeSnapshot(new SnapshotCallback() {

                    @Override
                    public void onShutter() {
//                        Log.i(TAG, "onShutter");a
//                        sp.play(soundID, 1, 1, 0, 0, 1);
//                        MediaActionSound sound = new MediaActionSound();
//                        sound.play(MediaActionSound.SHUTTER_CLICK);
                        //shutterSound();
                    }

                    @Override
                    public void onSnapshotTaken(int type, byte[] data, VideoInput videoInput) {
//                        Log.i(TAG, "onSnapshotTaken(" + type + ", " + (data != null ? data.length : 0) + ")");

                        switch (type) {
                            case SNAPSHOT_RAW:
                                break;

                            case SNAPSHOT_POSTVIEW:
                                break;

                            case SNAPSHOT_JPEG:
                                addMediaToLibrary(storeSnapshotImage(data));
                                onSnapshotComplete();
                                break;

                            default:
                                break;
                        }
                    }
                });
            }
        }
    }

    /*
    private MediaPlayer mShutterSound;

    public void shutterSound()
    {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int volume = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION);

        if (volume != 0) {
            if (mShutterSound == null)
                mShutterSound = MediaPlayer.create(getApplication(), Uri.parse("file:///system/media/audio/ui/camera_click.ogg"));
            if (mShutterSound != null)
                mShutterSound.start();
        }
    }
    */
    private String storeSnapshotImage(byte[] data) {
        File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
        if (pictureFile == null) {
            Log.e(TAG, "Error creating media file, check storage permissions");
            return null;
        }

        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            fos.write(data);
            fos.close();
        } catch (FileNotFoundException e) {
            Log.e(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, "Error accessing file: " + e.getMessage());
        }

        return pictureFile.getAbsolutePath();
    }

    private void onSnapshotComplete() {
        showToast(R.string.msg_snapshot_complete);
    }

    //--------------------------------------------------------------------------------------------------
    private void launchGalleryApp() {
        SharedPreferences pref = getSharedPreferences("poc_gallery", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean("poc_gallery", false);
        editor.apply();

        PackageManager pm = getPackageManager();
        Intent intent = pm.getLaunchIntentForPackage("com.android.gallery3d");
//        Intent intent = new Intent(Intent.ACTION_GET_CONTENT, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivity(intent);
    }

    ////--------------------------------------------------------------------------------------------------
//    private void setPocMode(int mode) {
//        Fragment fragment = getSupportFragmentManager().findFragmentByTag(OVERLAY_FRAGMENT_TAG);
//        if (fragment instanceof PocOverlayFragment) {
//            PocOverlayFragment pocOverlayFragment = (PocOverlayFragment) fragment;
//            pocOverlayFragment.setPocMode(mode);
//        }
//    }

//    private void setPocState(int state) {
//        Fragment fragment = getSupportFragmentManager().findFragmentByTag(OVERLAY_FRAGMENT_TAG);
//        if (fragment instanceof PocOverlayFragment) {
//            PocOverlayFragment pocOverlayFragment = (PocOverlayFragment) fragment;
//            pocOverlayFragment.setPocState(state);
//        }
//    }

    private void inputPocState(int state) {
        final String[] pocStates = getResources().getStringArray(R.array.poc_states);
        if (state < pocStates.length) {
            mPocState.setText(pocStates[state]);
        }
    }

//    private void setPocDialogVisible(int visibility) {
//        Fragment fragment = getSupportFragmentManager().findFragmentByTag(OVERLAY_FRAGMENT_TAG);
//        if (fragment instanceof PocOverlayFragment) {
//            PocOverlayFragment pocOverlayFragment = (PocOverlayFragment) fragment;
//            if (pocOverlayFragment.getView() != null) {
//                View view = pocOverlayFragment.getView().findViewById(R.id.poc_dialog);
//                view.setVisibility(visibility);
//            }
//        }
//    }

    private void setPocDialogVisible(int visibility) {
        RelativeLayout view = findViewById(R.id.poc_dialog);
        view.setVisibility(visibility);
    }

    private void showPocNotSupportDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        //alert.setTitle("Title");
        alert.setMessage(R.string.poc_not_support);

        // Set an EditText view to get user input
        //final EditText input = new EditText(this);
        //alert.setView(input);

        //alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
        //    public void onClick(DialogInterface dialog, int whichButton) {
        //        String value = input.getText().toString();
        //        value.toString();
        // Do something with value!
        //    }
        //});

        alert.setNegativeButton(R.string.close,
                (dialog, whichButton) -> {
                    // Canceled.
                    onCancelPocCheck();
                });

        alert.show();
    }

    public void showRecordErrorDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        //alert.setTitle("Title");
        alert.setMessage(R.string.stop_error);

        // Set an EditText view to get user input
        //final EditText input = new EditText(this);
        //alert.setView(input);

        //alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
        //    public void onClick(DialogInterface dialog, int whichButton) {
        //        String value = input.getText().toString();
        //        value.toString();
        // Do something with value!
        //    }
        //});

        alert.setNegativeButton(R.string.close,
                (dialog, whichButton) -> {
                    // Canceled.
                    ActivityCompat.finishAffinity(this);
//                    System.runFinalizersOnExit(true);
                    System.exit(0);
                });

        alert.show();
    }


    private void showPocPseCheckDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        //alert.setTitle("Title");
        alert.setMessage(R.string.poc_pse_support);

        // Set an EditText view to get user input
        //final EditText input = new EditText(this);
        //alert.setView(input);

        //alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
        //    public void onClick(DialogInterface dialog, int whichButton) {
        //        String value = input.getText().toString();
        //        value.toString();
        // Do something with value!
        //    }
        //});


        alert.setNegativeButton(R.string.close,
                (dialog, whichButton) -> {
                    // Canceled.
                    onCancelPocCheck();
                });

        alert.show();
    }

    private void showPocDialog() {
        onPocDialog = true;
        if (mPocDialog != null) {
            mPocDialog.setVisibility(View.VISIBLE);
        } else {
            mPocDialog = findViewById(R.id.poc_dialog);
            mPocDialog.setVisibility(View.VISIBLE);
        }
        mDrawerButton.setEnabled(false);
        mDrawerButton.setSelected(false);
    }

    private void showPocOverlay() {
        if (mPocOverlay != null) {
            mPocOverlay.setVisibility(View.VISIBLE);
        } else {
            mPocOverlay = findViewById(R.id.poc_overlay);

            mPocOverlay.setVisibility(View.VISIBLE);
        }
    }

    private void hidePocDialog() {
        onPocDialog = false;

        if (mPocDialog != null) {
            mPocDialog.setVisibility(View.INVISIBLE);
            mPocDialog = null;
        }
        mDrawerButton.setEnabled(true);
        mDrawerButton.setSelected(true);

    }

    private void hidePocOverlay() {
        if (mPocOverlay != null) {
            mPocOverlay.setVisibility(View.INVISIBLE);
            mPocOverlay = null;
        }
    }

    public String getPseState() {
        String sValue = "";

        try {
            Process p = Runtime.getRuntime().exec("cat /sys/class/gpio_sw/PE17/data");
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            sValue = input.readLine();
            input.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return sValue;
    }

    public String getPocState() {
        String sValue = "";

        try {
            Process p = Runtime.getRuntime().exec("cat /sys/class/gpio_sw/PE11/data");
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            sValue = input.readLine();
            input.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return sValue;
    }

    private void startPocMode() {
//        Log.d(TAG, "startPocMode");

        pocMode = true;

        setPocMode(MODE_CHECK);

        if (getPseState().equals("1")) {
            onCancelPocCheck();
            runOnUiThread(() -> {
                hidePocDialog();
                hidePocOverlay();
                showPocPseCheckDialog();
            });
            return;
        }

//        mPocStateListener = new PocStateListener() {
//
//            @Override
//            public void onPocStarted() {
////                Log.d(TAG, "onPocStarted()");
//                try {
//                    opt.writeBytes("echo 0 > /sys/class/gpio_sw/PE11/data\n");
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                runOnUiThread(() -> {
//                    showPocDialog();
//                    showPocOverlay();
//                    inputPocState(0);
//                });
//                // 전원 공급 모드로 바꿈
//
//            }
//
//            @Override
//            public void onPocStateChange(final int state) {
////                Log.d(TAG, "PoC state = " + state);
//
//                runOnUiThread(() -> {
////                    setPocState(state);
//                    inputPocState(state);
//                    if (state == STATE_LINK_OK) {
//                        setPocMode(MODE_POWER);
//                        showPocDialog();
//                    } else if (state >= STATE_C_OPEN) {
//                        setPocMode(MODE_CHECK);
//                        showPocDialog();
//                    }
//                });
//            }
//
//            @Override
//            public void onPocStopped() {
////                Log.d(TAG, "onPocStopped()");
//            }
//
//            @Override
//            public void onPocNotSupported() {
//                //Log.d(TAG, "onPocNotSupported()");
//                runOnUiThread(() -> {
//                    //showToast(R.string.poc_not_support);
//                    hidePocDialog();
//                    hidePocOverlay();
//                    showPocNotSupportDialog();
//                });
//            }
//        };
//        mMcuControl.addReceiveBufferListener(mPocStateListener);

        setVpMode();
    }

    private void setVpMode() {
//        Log.d(TAG, "setVpMode...");

        try {
            mMcuControl.setVpMode(true);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }


    }


    public void onStartPocCheck() {
//        Log.d(TAG, "Checking PoC link...");

        try {
            mMcuControl.attemptVpTest();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        hidePocDialog();
    }


    public void onApplyPocPower() {
        try {
            mMcuControl.attemptVpTest();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        hidePocDialog();
    }

    private void exitPocMode() {
        Log.d(TAG, "exitPocMode");

        pocMode = false;
        inputPocState(STATE_POWER_OFF);
        hidePocOverlay();
        hidePocDialog();

//        try {
//            opt.writeBytes("echo 1 > /sys/class/gpio_sw/PE11/data\n");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        try {
            mMcuControl.setVpMode(false);
//                Log.d(TAG, "Poc EXit");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
//        mMcuControl.removeReceiveBufferListener(mPocStateListener);
//        mPocStateListener = null;

    }

    public void setPocMode(int mode) {
        TextView message = findViewById(R.id.poc_message);
        switch (mode) {
            case MODE_CHECK:
                message.setText(getString(R.string.msg_starting_poc_check));
                break;

            case MODE_POWER:
                message.setText(getString(R.string.msg_enable_poc_power));
                break;

            case MODE_PSE:
                message.setText(getString(R.string.usb));
                break;

            default:
                return;
        }

        _mode = mode;
//        yesButton.requestFocus();
    }


    public void onCancelPocCheck() {
        exitPocMode();

        // 비디오 입력 모드로 복귀
        enterVideoMode();
    }


    public void onRemovePocPower() {
        exitPocMode();

        // 비디오 입력 모드로 복귀
        enterVideoMode();
    }

    ////--------------------------------------------------------------------------------------------------
    private BroadcastReceiver mExternalStorageReceiver;
    private boolean mExternalStorageAvailable;
    private boolean mExternalRemove;
    private boolean mExternalState;
    private String mExternalStoragePath;

    public static HashSet<String> getExternalMounts() {
        final HashSet<String> out = new HashSet<>();
        String reg = "(?i).*vold.*(vfat|ntfs|exfat|fat32|ext3|ext4).*rw.*";
        StringBuilder s = new StringBuilder();
        try {
            final Process process = new ProcessBuilder().command("mount")
                    .redirectErrorStream(true).start();
            process.waitFor();
            final InputStream is = process.getInputStream();
            final byte[] buffer = new byte[1024];
            while (is.read(buffer) != -1) {
                s.append(new String(buffer));
            }
            is.close();
        } catch (final Exception e) {
            e.printStackTrace();
        }

        // parse output
        final String[] lines = s.toString().split("\n");
        for (String line : lines) {
            if (!line.toLowerCase(Locale.US).contains("asec")) {
                if (line.matches(reg)) {
                    String[] parts = line.split(" ");
                    for (String part : parts) {
                        if (part.startsWith("/"))
                            if (!part.toLowerCase(Locale.US).contains("vold"))
                                out.add(part);
                    }
                }
            }
        }
        return out;
    }

    private void updateMediaFolderPreferenceEnabled() {
        Fragment menuFragment = getSupportFragmentManager().findFragmentByTag(MENU_FRAGMENT_TAG);

        if (menuFragment instanceof SettingsFragment) {
            Preference preference = ((SettingsFragment) menuFragment).findPreference(getString(R.string.pref_media_folder));
            preference.setEnabled(mExternalStorageAvailable);
            Log.d(TAG, "Updating MediaFolderPreferenceEnabled = " + mExternalStorageAvailable);
        }
    }

    private void updateMediaLocation(boolean update) {
        Fragment menuFragment = getSupportFragmentManager().findFragmentByTag(MENU_FRAGMENT_TAG);
        if (menuFragment instanceof SettingsFragment) {
            Preference preference = ((SettingsFragment) menuFragment).findPreference(getString(R.string.pref_media_folder));
            preference.setEnabled(update);

            Log.d(TAG, "MediaLocation = " + update);
        }
    }

    public void setTextExternalStorage() {
        String mode;
        SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
        mode = pref.getString("mnt", "");
        TextView storageView = findViewById(R.id.recording_storage);
//        Log.d(TAG, "Mode: " + mode);
        if (storageView != null) {
            if (!storageBoolean()) {
                storageView.setText(R.string.storage_internal);
            } else {
                switch (mode) {
                    case "internal":
                        storageView.setText(R.string.storage_internal);
                        break;
                    case "usb":
                        storageView.setText(R.string.usb);
                        break;
                    case "extsd":
                        storageView.setText(R.string.extsd);
                        break;
                }
            }
        }
    }

    public String storageStat() {
        String mode;
        SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
        mode = pref.getString("mnt", "");
        return mode;
    }

    public boolean storageBoolean() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        return pref.getBoolean(getString(R.string.pref_media_folder), false);
    }

    private void updateExternalStorageState() {
        HashSet<String> mounts = getExternalMounts();
        mounts.remove(Environment.getExternalStorageDirectory().getPath());
        mExternalStorageAvailable = mounts.size() > 0;
//        Log.d(TAG, "mExternalStorageAvailable: " + mExternalStorageAvailable + " Mount.size: " + mounts.size());
//
//        String primary_sd = System.getenv("EXTERNAL_STORAGE");
//        if(primary_sd != null)  Log.i("EXTERNAL_STORAGE", primary_sd);
//        String secondary_sd = System.getenv("SECONDARY_STORAGE");
//        if(secondary_sd != null)    Log.i("SECONDARY_STORAGE", secondary_sd);

        // 외부 SD카드 마운트 경로를 기억해 둔다.
        mExternalStoragePath = null;
        if (!mExternalRemove) {
            for (String path : mounts) {
                mExternalStoragePath = path;
            }
        }
        SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        if (mExternalStoragePath != null) {
            if (mExternalStoragePath.equals("/mnt/media_rw/usbhost")) {
                editor.putString("mnt", "usb");
                updateMediaLocation(true);
                mExternalStoragePath = "/storage/usbhost";

            } else if (mExternalStoragePath.equals("/mnt/media_rw/extsd")) {
                editor.putString("mnt", "extsd");
                updateMediaLocation(true);
                mExternalStoragePath = "/storage/extsd";

            }
        } else {
            editor.putString("mnt", "internal");
            updateMediaLocation(false);
            mExternalStoragePath = "/storage/emulated/0/";
            mExternalStorageAvailable = false;

        }
        editor.apply();

        if (mMode == MODE_RECORD || mMode == MODE_SNAPSHOT) {
            setTextExternalStorage();
        }

        updateMediaFolderPreferenceEnabled();
//        Log.d(TAG, "mExternalStoragePath = " + mExternalStoragePath);
    }

    public String mediaStorageLocation() {
        String str;

        SharedPreferences pref1 = getSharedPreferences("pref", MODE_PRIVATE);
        str = pref1.getString("mnt", "");

        return str;
    }

    private void startWatchingExternalStorage() {
        mExternalStorageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                updateMediaFolderPreferenceEnabled();

                if (Intent.ACTION_MEDIA_MOUNTED.equals(action)) {
//                    Log.d(TAG, "isRecording: " + isRecording() + " mediaStorageLocation: " + mediaStorageLocation());
                    if (isRecording() && !mediaStorageLocation().equals("internal")) {
                        if (mVideoInput != null) {
                            stopRecording();
                        }
                    }
                    mExternalRemove = false;
                    updateExternalStorageState();

                } else if ((Intent.ACTION_MEDIA_REMOVED.equals(action)) || (Intent.ACTION_MEDIA_UNMOUNTED.equals(action))) {
                    Log.d(TAG, "isRecording: " + isRecording() + " mediaStorageLocation: " + mediaStorageLocation());
                } else if (Intent.ACTION_MEDIA_EJECT.equals(action)) {
//                    Log.e(TAG, "ACTION_MEDIA_EJECT");
                    mExternalStoragePath = null;
                    mExternalStorageAvailable = false;
                    mExternalRemove = true;

                    String value = mediaStorageLocation();
                    updateExternalStorageState();
                    String value1 = mediaStorageLocation();

//                    Log.d(TAG, "Value: " +value + " Value1: " + value1 + " State: " + storageBoolean());
                    if (isRecording()) {
                        if (!value.equals(value1) && storageBoolean()) {
                            if (mVideoInput != null) {
                                stopRecording();
                                deleteMediaToLibrary(mediaPath);
                            }
                        }
                    }
//                    Log.i(TAG, "mediaStorageLocation1: " + mediaStorageLocation());

                    if (mMode == MODE_RECORD || mMode == MODE_SNAPSHOT) {
                        setTextExternalStorage();
                    }
                }
            }
        };


        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_REMOVED);
        filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
        //filter.addAction(Intent.ACTION_MEDIA_SHARED);
        filter.addAction(Intent.ACTION_MEDIA_EJECT);
        // Android 예제 문서에는 없지만 다음의 설정이 있어야 제대로 통지를 받을 수 있다
        filter.addDataScheme("file");
        registerReceiver(mExternalStorageReceiver, filter);
//        if (mMode == MODE_RECORD || mMode == MODE_SNAPSHOT) {
//            setTextExternalStorage();
//        }
        updateExternalStorageState();
    }

    private void stopWatchingExternalStorage() {
        unregisterReceiver(mExternalStorageReceiver);
        mExternalStorageReceiver = null;
    }


    public boolean chkAvailableStorageSize(boolean print_log) {
        StatFs statFs;
        double gigaSize, sdSize;
        long free;
//        Log.d(TAG, "chkAvailableStorageSize");
        if (useExternalSd(PreferenceManager.getDefaultSharedPreferences(this)) || !mExternalStorageAvailable) {
            statFs = new StatFs("/mnt/sdcard");
//            Log.d(TAG, "chkAvailableStorageSize 1: " + statFs);

        } else {
            statFs = new StatFs(mExternalStoragePath);
//            Log.d(TAG, "chkAvailableStorageSize 1: " + statFs);
        }

        sdSize = (double) statFs.getAvailableBlocks() * (double) statFs.getBlockSize();
        //One binary gigabyte equals 1,073,741,824 bytes.
        gigaSize = sdSize / 1073741824;
        free = (long) (gigaSize * 1000);
        if (print_log) Log.d(TAG, "Available storage size = " + free + " MB");

        return (free <= 50);
    }

    //
//    /*
//    private void toggleMediaFolder() {
//        final String key = getString(R.string.pref_media_folder);
//        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
//        boolean value = sharedPreferences.getBoolean(key, false);
//
//        SharedPreferences.Editor editor = sharedPreferences.edit();
//        editor.putBoolean(key, !value);
//        editor.apply();
//    }
//    */
    ////--------------------------------------------------------------------------------------------------
    private boolean isMode = false;
    private boolean isPlaying = false;
    private boolean isRecord = false;
    private AudioRecord audioRecorder = null;
    private AudioTrack audioPlayer = null;
    private Thread playThread = null;
    private boolean stopFlag = false;
    private int bufferSize;

    private Thread mRecordThread = null;

    private int mAudioSource = MediaRecorder.AudioSource.CAMCORDER;
    private int mSamleRate = 48000;
    private int mChannelCount = AudioFormat.CHANNEL_IN_STEREO;
    private int mAudioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private int mBufferSize = AudioTrack.getMinBufferSize(mSamleRate, mChannelCount, mAudioFormat);

    private ToneGenerator toneGenerator;
    private final int TONE_TYPE = ToneGenerator.TONE_DTMF_5;
    private final int STREAM = AudioManager.STREAM_MUSIC;
    private final int DOT_TIME = 3;

    public void setAudioMode(int mode) {
        audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            int audio_volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            audioManager.setParameters("playback=" + mode + ";dac_volume=" + audio_volume);
//            audioManager.requestAudioFocus(audioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, audio_volume, FLAG_PLAY_SOUND);
//            toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC,audio_volume*14);

        }
    }

    private AudioManager.OnAudioFocusChangeListener audioFocusChangeListener = focusChange -> {

        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
//                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, audioManager.getStreamMaxVolume(AudioManager.STREAM_SYSTEM), AudioManager.FLAG_PLAY_SOUND);
                Log.d(TAG, "AudioFocus Gain");
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
//                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0,AudioManager.FLAG_PLAY_SOUND);
                Log.d(TAG, "AudioFocus Loss");
                break;


        }
    };

    public void onMode(View view) {
        if (isMode) {
            isMode = false;
            Log.d(TAG, "Audio Mode None");
        } else {
            isMode = true;
            Log.d(TAG, "Audio Mode HDMI");

        }
    }

    public void onRecord(View view) {
        setModeUcc(0);
    }


    public void onPlay(View view) {
        setModeUcc(1);
    }

    private void startHdmiAudioPlay() {
        if (mSource.is(VideoSource.HDMI) || mSource.is(VideoSource.SDI)) {
//            Log.d(TAG, "startHdmiAudioPlay()");

            if (mSource.is(VideoSource.HDMI)) setAudioMode(AUDIO_MODE_IN_HDMI);
            if (mSource.is(VideoSource.SDI)) setAudioMode(AUDIO_MODE_IN_SDI);


//            if (!isPlaying) {
////                int samplingRate = 48000; // in Hz
////                int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
//
////                bufferSize = AudioRecord.getMinBufferSize(samplingRate, AudioFormat.CHANNEL_IN_STEREO, audioFormat);
//                //Log.d(TAG, "AudioRecord buffer size = " + bufferSize);       // min = 8960 // the minimum buffer size expressed in bytes
////                audioRecorder = new AudioRecord(MediaRecorder.AudioSource.CAMCORDER, samplingRate, AudioFormat.CHANNEL_IN_STEREO, audioFormat, bufferSize * 2);
//                audioRecorder = new AudioRecord(mAudioSource, mSamleRate, mChannelCount, mAudioFormat, mBufferSize);
//                audioRecorder.startRecording();
//
//                //samplingRate = 44100;
//                bufferSize = AudioTrack.getMinBufferSize(mSamleRate, AudioFormat.CHANNEL_OUT_STEREO, mAudioFormat);
//                //Log.d(TAG, "AudioTrack  buffer size = " + bufferSize);       // min = 18432
//                audioPlayer = new AudioTrack(AudioManager.STREAM_MUSIC, mSamleRate, AudioFormat.CHANNEL_OUT_STEREO, mAudioFormat, bufferSize, AudioTrack.MODE_STREAM);
//
//                audioPlayer.flush();
//                audioPlayer.play();
//
//
//                isPlaying = true;
//                bufferSize /= 2;
//
//                playThread = new Thread(new Runnable() {
//                    public void run() {
//                        if (stopFlag) {
//                            isPlaying = false;
//                        }
//                        if (isPlaying) {
//                            routingAudio();
//                        }
//                    }
//                }, "AudioPlay Thread");
//
//                playThread.start();
//                //Log.d(TAG, "playback mode = " + audioManager.getParameters("playback"));
//            }

        }
    }

    //
//    private void routingAudio() {
//        int readShorts;
//        short[] sData1 = new short[bufferSize];
//        short[] sData2 = new short[bufferSize];
//
//        readShorts = audioRecorder.read(sData1, 0, bufferSize);
//
//        if ((readShorts != AudioRecord.ERROR_INVALID_OPERATION) && (readShorts != AudioRecord.ERROR_BAD_VALUE)) {
//            audioPlayer.write(sData2, 0, readShorts);
//            audioPlayer.flush();
//        }
//    }
//
    private void stopHdmiAudioPlay(boolean play_mode) {
        if (play_mode) {
            if (audioManager != null) {
                setAudioMode(AUDIO_MODE_IN_NONE);
//                audioManager.abandonAudioFocus(audioFocusChangeListener);
//                audioManager = null;
            }

        }
//        isPlaying = false;
////        if (isPlaying) {
//        stopFlag = true;
//////            while(isPlaying);
////        }
//        if (audioPlayer != null) {
//            audioPlayer.release();
//            audioPlayer = null;
//        }
//        if (audioRecorder != null) {
//            audioRecorder.release();
//            audioRecorder = null;
//        }
//        if (null != playThread) playThread = null;

    }

    @SuppressLint("HandlerLeak")
    final Handler handler = new Handler() {
        public void handleMessage(Message msg) {

            if (mSignalInfo != null) {
                zoomStateSet();
            }
        }
    };


    public TimerTask zoomTimeTask() {
        return new TimerTask() {
            @Override
            public void run() {
                Message msg = handler.obtainMessage();
                handler.sendMessage(msg);

            }

        };
    }

    private void zoomStateSet() {
        SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
        float scale = pref.getFloat("scale", 0);
        @SuppressLint("DefaultLocale") String strScale = String.format("%.2f X", scale);
        if (scale == 1.0 || !mSignalInfo.signal) {
            zoomView.setVisibility(View.INVISIBLE);
        } else {
            zoomView.setVisibility(View.VISIBLE);
            zoomView.setText(strScale);
        }
//        Log.d(TAG, "Scale: " + pref.getFloat("scale", 0));
    }

    private void openPoeView() {
        Intent sendIntent = new Intent("com.sscctv.poeView");
        sendIntent.putExtra("location", "open");
        sendBroadcast(sendIntent);
    }

    private void closePoeView() {
        Intent sendIntent = new Intent("com.sscctv.poeView");
        sendIntent.putExtra("location", "close");
        sendBroadcast(sendIntent);
    }

    public void setModeUcc(int input) {
        String value = String.valueOf(input);
        try {
            FileOutputStream file = new FileOutputStream("/sys/class/sdi/en332/ucc");
            file.write(value.getBytes());
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

//--------------------------------------------------------------------------------------------------
/*
    public void toggleWiFi(boolean status) {
        WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        int wifistat = wifiManager.getWifiState();
        //if (status == true && !wifiManager.isWifiEnabled())
        if (status == true && (wifistat <= 1)) {            // WIFI_STATE_DISABLING or WIFI_STATE_DISABLED
            Log.d(TAG, "WiFi On!");
            wifiManager.setWifiEnabled(true);
        //} else if ((status == false) && wifiManager.isWifiEnabled())
        } else if ((status == false) && ((wifistat == 2) || (wifistat == 3))) {  // WIFI_STATE_ENABLING or WIFI_STATE_ENABLED
            Log.d(TAG, "WiFi Off!");
            wifiManager.setWifiEnabled(false);
            wifiStat = true;
        }
    }
*/

//--------------------------------------------------------------------------------------------------
/*
    public static class RbLog {

        //public static boolean D = false;

        public static final boolean D = true;

        public static void d (String tag, String msg) {
            if(D && tag !=null && msg !=null)
                Log.d(tag, msg);
        }
        public static void d (Activity activity, String msg) {
            if (D && activity != null && msg != null)
                Log.d(activity.getLocalClassName(),msg);
        }
    }
*/
}


