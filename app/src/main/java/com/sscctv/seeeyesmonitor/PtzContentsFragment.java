package com.sscctv.seeeyesmonitor;

import android.support.v4.app.Fragment;

/**
 * Created by trlim on 2016. 1. 9..
 *
 * PTZ 수신 데이터 표시 프래그먼트용 베이스 클래스
 */
public class PtzContentsFragment extends Fragment {
    /**
     * 표시 중인 내용을 모두 지운다.
     * 하위 클래스에서 구현해 주어야 한다.
     */
    public void clear() {
    }
}
