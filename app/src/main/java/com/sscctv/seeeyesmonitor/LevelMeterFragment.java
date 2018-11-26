package com.sscctv.seeeyesmonitor;


import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import static android.content.ContentValues.TAG;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link LevelMeterFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LevelMeterFragment extends Fragment {
    private static final String ARG_LAYOUT_RESOURCE = "layout_resource";

    private int mLayoutResource;

    private int mFocusLevelMax;
    private TextView mFocusLevelText;

    private TextView mSignalLevelText;
    private TextView mSignalQualityText;

    private TextView mBurstLevelText;
    private TextView mSyncLevelText;

    public LevelMeterFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param layoutResource Resource ID of layout to use.
     * @return A new instance of fragment LevelMeterFragment.
     */
    public static LevelMeterFragment newInstance(int layoutResource) {
        LevelMeterFragment fragment = new LevelMeterFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_LAYOUT_RESOURCE, layoutResource);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mLayoutResource = getArguments().getInt(ARG_LAYOUT_RESOURCE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(mLayoutResource, container, false);

        mFocusLevelText = (TextView) view.findViewById(R.id.focus_level);

        mSignalLevelText = (TextView) view.findViewById(R.id.signal_level);
        mSignalQualityText = (TextView) view.findViewById(R.id.signal_quality);

        mBurstLevelText = (TextView) view.findViewById(R.id.f_level);
        mSyncLevelText = (TextView) view.findViewById(R.id.a_level);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        mFocusLevelMax = 0;
    }

    public void updateFocusLevel(int focusLevel) {
        if (mFocusLevelMax < focusLevel) {
            mFocusLevelMax = focusLevel;
        }
        mFocusLevelText.setText(String.format("FOCUS : %03d/%03d", focusLevel, mFocusLevelMax));
    }

    public void updateFocusNa() {
            mFocusLevelText.setText(String.format("FOCUS : N/A"));
    }

    public void resetFocusLevel(int focusLevel) {
        mFocusLevelMax = focusLevel;
        mFocusLevelText.setText(String.format("FOCUS : %03d/%03d", focusLevel, mFocusLevelMax));
    }

    @SuppressLint("DefaultLocale")
    public void updateSignalLevel(boolean hasSignal, int signalLevel) {
        if (mSignalLevelText != null) {
            mSignalLevelText.setText(String.format("S.LVL : %03d%%", signalLevel));
        }
        if (mSignalQualityText != null) {
            if (hasSignal) {
                int quality;
                int color;

                if (signalLevel >= 60) {
                    quality = R.string.level_good;
                    color = R.color.colorLevelGood;
                } else if (signalLevel >= 30) {
                    quality = R.string.level_normal;
                    color = R.color.colorLevelNormal;
                } else {
                    quality = R.string.level_ng;
                    color = R.color.colorLevelNG;
                }

                mSignalQualityText.setTextColor(getResources().getColor(color));
                mSignalQualityText.setText(quality);

                mSignalQualityText.setVisibility(View.VISIBLE);
            } else {
                mSignalQualityText.setVisibility(View.INVISIBLE);
            }
        }
    }

    public void updateBurstLevel(int burstLevel) {
        if (mBurstLevelText != null) {
            mBurstLevelText.setText(String.format("F.LVL : %03d%%", burstLevel));
        }
    }
    public void setFocusLevelNa() {
        mFocusLevelText.setText(String.format("FOCUS : N/A      "));
    }

    public void updateSyncLevel(int syncLevel) {
        if (mSyncLevelText != null) {
            mSyncLevelText.setText(String.format("A.LVL : %03d%%", syncLevel));
        }
    }
}
