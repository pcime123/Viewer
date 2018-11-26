package com.sscctv.seeeyesmonitor;

import android.support.v4.app.Fragment;
//import android.util.Log;
import android.view.KeyEvent;

/**
 * Created by trlim on 2016. 1. 8..
 *
 * 제어 프래그먼트를 추상화하기 위한 베이스 클래스
 */
public class PtzControlFragment extends Fragment {
    public static final String TAG = "PtzControlFragment";

    /**
     * 제어 프래그먼트에 대한 키 이벤트를 처리한다.
     * 파생 클래스에서 오버라이드 해야 한다.
     *
     * @param keyCode 키 값
     * @param event 이벤트
     * @return 이벤트를 처리했으면 true, 아니면 false
     */
    public boolean onKeyPress(int keyCode, KeyEvent event) {
        //Log.w(TAG, "onKeyPress(" + keyCode + ") : should be overrided");
        return false;
    }
}
