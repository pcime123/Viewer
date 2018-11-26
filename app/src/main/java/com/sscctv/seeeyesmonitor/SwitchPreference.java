package com.sscctv.seeeyesmonitor;

import android.content.Context;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Checkable;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
//import android.util.Log;

/**
 * Created by trlim on 2016. 1. 14..
 *
 * android.support.v7.preference.SwitchPreferenceCompat이 기존 Holo 테마의 스위치를 표시하지 못하는 문제를
 * 해결하기 위한 클래스.
 * 하지만 switch text 대신 summary text로 간단하게 같은 효과를 얻을 수도 있을 것 같다.
 */
public class SwitchPreference extends android.support.v7.preference.SwitchPreferenceCompat {
    private static final String TAG = "SwitchPreference";
    private final Listener mListener = new Listener();
    private TextView txt;
    private class Listener implements CompoundButton.OnCheckedChangeListener {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//	        Log.d(TAG, "onCheckedChanged="+buttonView+","+isChecked);
            if (!callChangeListener(isChecked)) {
                // Listener didn't like it, change it back.
                // CompoundButton will make sure we don't recurse.
                buttonView.setChecked(!isChecked);
                return;
            }
            setChecked(isChecked);
        }
    }

    public SwitchPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public SwitchPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public SwitchPreference(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.switchPreferenceCompatStyle);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        txt = (TextView) holder.findViewById(R.id.storage_text);
//        Log.d(TAG, "onBindViewHolder="+holder);
        View checkableView = holder.findViewById(R.id.switchWidget);
        if (checkableView != null && checkableView instanceof Checkable) {
            if (checkableView instanceof Switch) {
                final Switch switchView = (Switch) checkableView;
                switchView.setOnCheckedChangeListener(null);
            }
            ((Checkable) checkableView).setChecked(mChecked);
            if (checkableView instanceof Switch) {
                final Switch switchView = (Switch) checkableView;
                switchView.setTextOn(getSwitchTextOn());
                switchView.setTextOff(getSwitchTextOff());
                switchView.setOnCheckedChangeListener(mListener);
            }
        }
        syncSummaryView(holder);
    }

}
