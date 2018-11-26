package com.sscctv.seeeyesmonitor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceGroupAdapter;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.PreferenceViewHolder;
import android.support.v7.widget.RecyclerView;
//import android.util.Log;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Objects;

import static android.support.constraint.Constraints.TAG;

/**
 * A simple {@link PreferenceFragmentCompat} subclass.
 * Activities that contain this fragment must implement the
 * {@link SettingsFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link SettingsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
    //private static final String TAG = "SettingsFragment";
    private static final String ARG_RESOURCE = "resource";

    private int clickKeyFlag = 0;
    private View mFirstItemView;

    private OnFragmentInteractionListener mListener;

    public SettingsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param resource 설정을 정의하는 XML resource.
     * @return A new instance of fragment SettingsFragment.
     */
    public static SettingsFragment newInstance(int resource) {
        SettingsFragment fragment = new SettingsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_RESOURCE, resource);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String s) {
        //Log.d(TAG, "onCreatePreferences");
        Bundle args = getArguments();

        if (args != null) {
            int resource = getArguments().getInt(ARG_RESOURCE);

            // Load the preferences from an XML resource
            addPreferencesFromResource(resource);
        }
    }

    @Override
    public RecyclerView onCreateRecyclerView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        //Log.d(TAG, "onCreateRecyclerView");
        RecyclerView recyclerView = (RecyclerView) inflater.inflate(R.layout.fragment_settings, parent, false);
        recyclerView.setLayoutManager(this.onCreateLayoutManager());
        return recyclerView;
    }

    @Override
    protected RecyclerView.Adapter onCreateAdapter(PreferenceScreen preferenceScreen) {
        return new PreferenceGroupAdapter(preferenceScreen) {

            @SuppressLint("RestrictedApi")
            @Override
            public void onBindViewHolder(PreferenceViewHolder holder, int position) {
                super.onBindViewHolder(holder, position);
                //Log.d(TAG, "onBindViewHolder="+holder+","+position);

                // 첫번째 메뉴를 선택되게 한다.
                if ((mFirstItemView == null) || (clickKeyFlag == 0)) {
                    if (holder.getAdapterPosition() == 1) {
                        mFirstItemView = holder.itemView;
                        mFirstItemView.requestFocus();
                    }
                }

                final Preference preference = getItem(position);

                holder.itemView.setOnKeyListener(new View.OnKeyListener() {
                    @Override
                    public boolean onKey(View v, int keyCode, KeyEvent event) {
                        clickKeyFlag = 1;
                        if (event.getAction() == KeyEvent.ACTION_DOWN) {
                            //Log.d(TAG, "ACTION_DOWN="+keyCode);
/*
                            if (preference instanceof ListPreference) {
                                // ListPreference의 경우 목록의 이전 또는 다음 값으로 바꾸어 준다.
                                ListPreference listPreference = (ListPreference) preference;
                                int index = listPreference.findIndexOfValue(listPreference.getValue());
                                switch (keyCode) {
                                    case KeyEvent.KEYCODE_DPAD_LEFT:
                                        if (index == 0) {
                                            index = listPreference.getEntryValues().length;
                                        }
                                        index -= 1;
                                        listPreference.setValueIndex(index);
                                        return true;

                                    case KeyEvent.KEYCODE_DPAD_RIGHT:
                                        index += 1;
                                        if (index == listPreference.getEntryValues().length) {
                                            index = 0;
                                        }
                                        listPreference.setValueIndex(index);
                                        return true;
                                }
                            } else
*/
                            if (preference instanceof SwitchPreference) {
                                // SwitchPreference는 오른쪽 키를 누르면 켜고, 왼쪽 키를 누르면 꺼준다.
                                SwitchPreference switchPreference = (SwitchPreference) preference;
                                switch (keyCode) {
                                    case KeyEvent.KEYCODE_DPAD_LEFT:
                                        switchPreference.setChecked(false);
                                        return true;

                                    case KeyEvent.KEYCODE_DPAD_RIGHT:
                                        switchPreference.setChecked(true);
                                        return true;
                                }
                            }
/*
                            else if (preference instanceof NumberPickerPreference) {
                                NumberPickerPreference numberPickerPreference = (NumberPickerPreference) preference;
                                int value = numberPickerPreference.getValue();
                                switch (keyCode) {
                                    case KeyEvent.KEYCODE_DPAD_LEFT:
                                        if (value == numberPickerPreference.getMin()) {
                                            value = numberPickerPreference.getMax();
                                        } else {
                                            value -= 1;
                                        }
                                        numberPickerPreference.setValue(value);
                                        return true;

                                    case KeyEvent.KEYCODE_DPAD_RIGHT:
                                        if (value == numberPickerPreference.getMax()) {
                                            value = numberPickerPreference.getMin();
                                        } else {
                                            value += 1;
                                        }
                                        numberPickerPreference.setValue(value);
                                        return true;
                                }
                            }

                            else if (preference instanceof SeekBarPreference) {
                                SeekBarPreference seekBarPreference = (SeekBarPreference) preference;
                                int value = seekBarPreference.getValue();
                                switch (keyCode) {
                                    case KeyEvent.KEYCODE_DPAD_LEFT:
                                        if (value > 0) {
                                            value -= 1;
                                        }
                                        seekBarPreference.setValue(value);
                                        return true;

                                    case KeyEvent.KEYCODE_DPAD_RIGHT:
                                        if (value < seekBarPreference.getMax()) {
                                            value += 1;
                                        }
                                        seekBarPreference.setValue(value);
                                        return true;
                                }
                            }
*/
                        } else if (event.getAction() == KeyEvent.ACTION_UP) {
                            //Log.d(TAG, "ACTION_UP="+keyCode);
                        }


                        return false;
                    }
                });
            }
        };
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onResume() {
        super.onResume();
//        mediaFolderSet();

        //Log.d(TAG, "onResume");
/*
        // 모든 설정의 초기값을 표시
        SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
        for (String key : sharedPreferences.getAll().keySet()) {
            onSharedPreferenceChanged(sharedPreferences, key);
        }

        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
*/
        mFirstItemView = null;
        clickKeyFlag = 0;

        // Activity에서 preference의 상태를 업데이트 할 수 있는 기회를 준다.
        if (mListener != null) {
            mListener.onResumeSettings();
        }
    }

    @Override
    public void onPause() {
        //Log.d(TAG, "onPause");
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        //Log.d(TAG, "onSharedPreferenceChanged="+key);
        Preference pref = findPreference(key);

        if (pref instanceof ListPreference) {
            ListPreference listPreference = (ListPreference) pref;
            pref.setSummary(listPreference.getEntry());
            //Log.d(TAG, "ListPreference");
        } else if (pref instanceof NumberPickerPreference) {
            NumberPickerPreference numberPickerPreference = (NumberPickerPreference) pref;
            String format = numberPickerPreference.getFormat();
            String value;
            if (format != null) {
                value = String.format(format, numberPickerPreference.getValue());
            } else {
                value = Integer.toString(numberPickerPreference.getValue());
            }
            pref.setSummary(value);
            //Log.d(TAG, "NumberPickerPreference");
        }
    }

    public interface OnFragmentInteractionListener {
        void onResumeSettings();
    }

    public void mediaFolderSet() {
        String str;
        str = ((MainActivity)MainActivity.mContext).mediaStorageLocation();
        SwitchPreference pref = (SwitchPreference) findPreference("pref_media_folder");
        if (str.equals("extsd")) {
            pref.setSwitchTextOn(str);
            Log.d(TAG, "Set Text: " + str);
        } else if (str.equals("usb")) {
            pref.setSwitchTextOn(str);
            Log.d(TAG, "Set Text: " + str);
        }
    }


}
