package com.sscctv.seeeyesmonitor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.preference.DialogPreference;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.EditTextPreferenceDialogFragmentCompat;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.ListPreferenceDialogFragmentCompat;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import com.sscctv.seeeyes.ptz.PtzMode;
import com.sscctv.seeeyes.ptz.UtcProtocol;

import java.util.Arrays;
import java.util.Objects;

//import android.util.Log;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PtzMenuFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PtzMenuFragment extends Fragment implements ListView.OnItemClickListener,
        PreferenceManager.OnDisplayPreferenceDialogListener,
        DialogPreference.TargetFragment,
        SharedPreferences.OnSharedPreferenceChangeListener,
        View.OnKeyListener {
    private static final String TAG = "PtzMenuFragment";
    private static final String ARG_UTC_TYPE = "utcType";

    private int mUtcType;
    private String mPtzModeKey;

    private PreferenceManager mPreferenceManager;
    private Preference mMode;
    private Preference mPtzProtocol;
    private Preference mUtcProtocol;
    private NumberPickerPreference mAddress;
    private Preference mBaudRate;
    private SwitchPreference mTermination;

    private SharedPreferences mSharedPrefs;

    private ListView mListView;
    private PtzMenuAdapter mAdapter;
    private boolean stat;

    public PtzMenuFragment() {
        // Required empty public constructor
    }

    private boolean isUtcSupported() {
        return mUtcType != UtcProtocol.TYPE_NONE;
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param utcType Type of UTC mode to support.
     * @return A new instance of fragment PtzMenuFragment.
     */
    public static PtzMenuFragment newInstance(int utcType) {
        PtzMenuFragment fragment = new PtzMenuFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_UTC_TYPE, utcType);
        fragment.setArguments(args);
        return fragment;
    }


    @SuppressLint("RestrictedApi")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mUtcType = getArguments().getInt(ARG_UTC_TYPE, UtcProtocol.TYPE_NONE);
            mPtzModeKey = getString(isUtcSupported() ? R.string.pref_ptz_mode_utc : R.string.pref_ptz_mode_sdi);

            mPreferenceManager = new PreferenceManager(Objects.requireNonNull(getContext()));

            PreferenceScreen preferenceScreen = mPreferenceManager.inflateFromResource(getContext(), R.xml.prefs_ptz, null);

            mPreferenceManager.setPreferences(preferenceScreen);

            mMode = preferenceScreen.findPreference(mPtzModeKey);
            mPtzProtocol = preferenceScreen.findPreference(getString(R.string.pref_ptz_protocol));

            switch (mUtcType) {
                case UtcProtocol.TYPE_CVBS:
                    mUtcProtocol = preferenceScreen.findPreference(getString(R.string.pref_utc_protocol_cvbs));
                    break;

                case UtcProtocol.TYPE_TVI:
                    mUtcProtocol = preferenceScreen.findPreference(getString(R.string.pref_utc_protocol_tvi));
                    break;

                case UtcProtocol.TYPE_AHD:
                    mUtcProtocol = preferenceScreen.findPreference(getString(R.string.pref_utc_protocol_ahd));
                    break;

                case UtcProtocol.TYPE_CVI:
                    mUtcProtocol = preferenceScreen.findPreference(getString(R.string.pref_utc_protocol_cvi));
                    break;
            }

            mAddress = (NumberPickerPreference) preferenceScreen.findPreference(getString(R.string.pref_ptz_address));
            mBaudRate = preferenceScreen.findPreference(getString(R.string.pref_ptz_baudrate));
            mTermination = (SwitchPreference) preferenceScreen.findPreference(getString(R.string.pref_ptz_termination));

            mSharedPrefs = mPreferenceManager.getSharedPreferences();
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_ptz_menu, container, false);

        mListView = view.findViewById(R.id.list);

        TextView header = (TextView) inflater.inflate(R.layout.preference_header, mListView, false);

        header.setText(isUtcSupported() ? R.string.category_ptz_utc : R.string.category_ptz);

        mListView.addHeaderView(header, null, false);

        mListView.setOnItemClickListener(this);

        // 아이템을 추가해야만 제목이 보인다.
        mAdapter = new PtzMenuAdapter(inflater);
        mListView.setAdapter(mAdapter);

        // 키 이벤트를 처리한다. RecyclerView와는 다르게 키 이벤트를 아이템이 아닌 리스트뷰에서 처리해야 한다.
        mListView.setOnKeyListener(this);

        return view;
    }

    private int getPtzMode() {
        return Integer.parseInt(mSharedPrefs.getString(mPtzModeKey, "0"));
    }

    private boolean isUtcMode() {
        return getPtzMode() == PtzMode.UTC;
    }

    private class PtzMenuAdapter extends BaseAdapter {
        private static final int TYPE_INVALID = -1;
        private static final int TYPE_LIST = 0;
        private static final int TYPE_NUMBER = 1;
        private static final int TYPE_SWITCH = 2;

        private LayoutInflater mInflater;

        PtzMenuAdapter(LayoutInflater inflater) {
            mInflater = inflater;
        }

        @Override
        public int getCount() {
            return 5;
        }

        @Override
        public Preference getItem(int position) {
            switch (position) {
                case 0:
                    return mMode;

                case 1:
                    return isUtcMode() ? mUtcProtocol : mPtzProtocol;

                case 2:
                    return mAddress;

                case 3:
                    return mBaudRate;

                case 4:
                    return mTermination;
            }
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        private class ViewHolder {
            TextView title;
            TextView summary;
            View widget;
        }

        @Override
        public int getItemViewType(int position) {
            switch (position) {
                case 0:
                    return TYPE_LIST;

                case 1:
                    return TYPE_LIST;

                case 2:
                    return TYPE_NUMBER;

                case 3:
                    return TYPE_LIST;

                case 4:
                    return TYPE_SWITCH;
            }
            return TYPE_INVALID;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            switch (position) {
                case 0:
                    return true;

                case 1:
                    switch (getPtzMode()) {
                        case PtzMode.TX:
                            return true;
                        case PtzMode.RX:
                            return false;
                        case PtzMode.ANALYZER:
                            return true;
                        case PtzMode.UTC:
                            return true;
                    }
                    break;

                case 2:
                    switch (getPtzMode()) {
                        case PtzMode.TX:
                            return true;
                        case PtzMode.RX:
                            return false;
                        case PtzMode.ANALYZER:
                            return false;
                        case PtzMode.UTC:
                            return false;
                    }
                    break;

                case 3:
                    switch (getPtzMode()) {
                        case PtzMode.TX:
                            return true;
                        case PtzMode.RX:
                            return true;
                        case PtzMode.ANALYZER:
                            return true;
                        case PtzMode.UTC:
                            return false;
                    }
                    break;

                case 4:
                    return true;
            }
            return false;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;

            if (convertView == null) {
                viewHolder = new ViewHolder();

                int type = getItemViewType(position);

                switch (type) {
                    case TYPE_LIST:
                        convertView = mInflater.inflate(R.layout.preference_submenu, parent, false);
                        break;

                    case TYPE_NUMBER:
                        convertView = mInflater.inflate(R.layout.preference_no_summary, parent, false);
                        break;

                    case TYPE_SWITCH:
                        convertView = mInflater.inflate(R.layout.preference_no_summary, parent, false);
                        break;
                }

                assert convertView != null;

                viewHolder.title = convertView.findViewById(android.R.id.title);
                viewHolder.summary = convertView.findViewById(android.R.id.summary);

                ViewGroup widgetLayout;
                widgetLayout = convertView.findViewById(android.R.id.widget_frame);
                switch (type) {
                    case TYPE_LIST:
                        break;

                    case TYPE_NUMBER:
                        viewHolder.widget = mInflater.inflate(R.layout.preference_widget_updown, widgetLayout, false);
                        break;

                    case TYPE_SWITCH:
                        viewHolder.widget = mInflater.inflate(R.layout.preference_widget_switch, widgetLayout, false);
                        break;
                }
                if (viewHolder.widget != null) {
                    widgetLayout.addView(viewHolder.widget);

                    // Widget에 summary가 포함된 경우를 처리
                    if (viewHolder.summary == null) {
                        viewHolder.summary = viewHolder.widget.findViewById(android.R.id.summary);
                    }
                }

                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            boolean enabled = isEnabled(position);
            int title = 0;
            String value = "";

            switch (position) {
                case 0:
                    title = R.string.label_mode;
                    final String[] ptzModes = getResources().getStringArray(isUtcSupported() ? R.array.ptz_modes_utc : R.array.ptz_modes_sdi);
                    value = ptzModes[getPtzMode()];
                    stat = value.equals("Analyze");
                    break;

                case 1:
                    title = R.string.label_protocol;
                    if (isUtcSupported() && isUtcMode()) {
                        switch (mUtcType) {
                            case UtcProtocol.TYPE_AHD:
                                final String[] ahdProtocols = getResources().getStringArray(R.array.utc_protocols_ahd);
                                value = ahdProtocols[Integer.parseInt(mSharedPrefs.getString(getString(R.string.pref_utc_protocol_ahd), "0"))];
                                break;

                            case UtcProtocol.TYPE_TVI:
                                final String[] tviProtocols = getResources().getStringArray(R.array.utc_protocols_tvi);
                                value = tviProtocols[Integer.parseInt(mSharedPrefs.getString(getString(R.string.pref_utc_protocol_tvi), "0"))];
                                break;

                            case UtcProtocol.TYPE_CVI:
                                final String[] cviProtocols = getResources().getStringArray(R.array.utc_protocols_cvi);
                                value = cviProtocols[Integer.parseInt(mSharedPrefs.getString(getString(R.string.pref_utc_protocol_cvi), "0"))];

                                Context context = getActivity();
                                SharedPreferences pref = context.getSharedPreferences("pref", Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = pref.edit();
                                editor.putString("cvi", value);
                                editor.apply();
                                break;

                            case UtcProtocol.TYPE_CVBS:
                                final String[] cvbsProtocols = getResources().getStringArray(R.array.utc_protocols_cvbs);
                                value = cvbsProtocols[Integer.parseInt(mSharedPrefs.getString(getString(R.string.pref_utc_protocol_cvbs), "0"))];
                                break;

                        }
                    } else {
                        final String[] ptzProtocols = getResources().getStringArray(R.array.ptz_protocols);
                        value = ptzProtocols[Integer.parseInt(mSharedPrefs.getString(getString(R.string.pref_ptz_protocol), "0"))];
//                        Log.d(TAG, "ptzProtocol: " + value);

                    }
                    break;

                case 2:
                    title = R.string.label_address;
                    value = String.format(mAddress.getFormat(), mAddress.getValue());
                    if (viewHolder.widget != null) {
                        View leftButton = viewHolder.widget.findViewById(R.id.down);
                        leftButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                int value = mAddress.getValue();
                                if (value == mAddress.getMin()) {
                                    value = mAddress.getMax();
                                } else {
                                    value -= 1;
                                }
                                mAddress.setValue(value);
                            }
                        });
                        View rightButton = viewHolder.widget.findViewById(R.id.up);
                        rightButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                int value = mAddress.getValue();
                                if (value == mAddress.getMax()) {
                                    value = mAddress.getMin();
                                } else {
                                    value += 1;
                                }
                                mAddress.setValue(value);
                            }
                        });
                    }
                    break;

                case 3:
                    title = R.string.label_baudrate;
                    final String[] ptzBaudRates = getResources().getStringArray(R.array.ptz_baudrates);
                    value = ptzBaudRates[Integer.parseInt(mSharedPrefs.getString(getString(R.string.pref_ptz_baudrate), "0"))];
                    break;

                case 4:
                    title = R.string.label_termination;
                    if (viewHolder.widget != null) {
                        // 참고: Switch widget은 clickable=true여야 선택했을 때 전환이 된다.
                        Switch widget = (Switch) viewHolder.widget;

                        widget.setOnCheckedChangeListener(null);
                        widget.setChecked(mSharedPrefs.getBoolean(getString(R.string.pref_ptz_termination), false));
                        widget.setTextOn(mTermination.getSwitchTextOn());
                        widget.setTextOff(mTermination.getSwitchTextOff());

                        widget.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                //Log.d(TAG, "onCheckedChanged=" + buttonView + "," + isChecked);
                                mTermination.setChecked(isChecked);
                            }
                        });
                    }
                    break;
            }

            viewHolder.title.setText(getString(title));
            viewHolder.title.setTextSize(20);

            if (viewHolder.summary != null) {
                viewHolder.summary.setText(enabled ? value : "---");
            }

            // isEnabled()는 해당 항목을 선택할 수 있는지에만 영향을 준다.
            // 화면에 표시되는 건 여기서 수동으로 반영해 주어야 한다.
            // setEnabled()는 자식에는 효과가 없으므로 모두 찾아서 일일이 설정을 바꾸어 주어야 한다.
            setEnabledAll(convertView, enabled);

            return convertView;
        }
    }

    public static void setEnabledAll(View v, boolean enabled) {
        v.setEnabled(enabled);

        if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            for (int i = 0; i < vg.getChildCount(); i++)
                setEnabledAll(vg.getChildAt(i), enabled);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        mSharedPrefs.registerOnSharedPreferenceChangeListener(this);
        mPreferenceManager.setOnDisplayPreferenceDialogListener(this);

        if (getView() != null) {
            getView().requestFocus();
        }
    }

    @Override
    public void onPause() {
        mPreferenceManager.setOnDisplayPreferenceDialogListener(null);
        mSharedPrefs.unregisterOnSharedPreferenceChangeListener(this);

        super.onPause();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Preference preference;

        //Log.d(TAG, "onItemClick="+position+","+id);
        switch (position - 1) {
            case 0:
                preference = mMode;
//                Log.d(TAG, "Mode " + mMode);
                break;

            case 1:

                String protocol = "";
                final String[] ptzModes = getResources().getStringArray(isUtcSupported() ? R.array.ptz_modes_utc : R.array.ptz_modes_sdi);
                protocol = ptzModes[getPtzMode()];
//                Log.d(TAG, "Protocol: " + protocol);
                if (protocol.equals("UCC") || protocol.equals("RS-485 TX") || protocol.equals("Analyze")) {
                    preference = mPtzProtocol;
                } else if (protocol.equals("UTC") && stat) {
                    preference = mUtcProtocol;
                } else if (protocol.equals("UTC") && !stat) {
                    preference = mUtcProtocol;
                } else {
                    preference = mUtcProtocol;
                }
//                preference = isUtcMode() ? mUtcProtocol : mPtzProtocol;
//                Log.d(TAG, "pre: "  + preference + " isUtc: " + isUtcMode());
                break;

            case 3:
                preference = mBaudRate;
                break;

            case 4:
                mTermination.setChecked(!mTermination.isChecked());
                return;

            default:
                return;
        }

        mPreferenceManager.showDialog(preference);
    }

    @Override
    public Preference findPreference(CharSequence key) {
        return mPreferenceManager == null ? null : mPreferenceManager.findPreference(key);
    }

    public void onDisplayPreferenceDialog(Preference preference) {
        // PreferenceFragmentCompat에서 가져옴
        boolean handled = false;

        if (this.getCallbackFragment() instanceof PreferenceFragmentCompat.OnPreferenceDisplayDialogCallback) {
            handled = ((PreferenceFragmentCompat.OnPreferenceDisplayDialogCallback) this.getCallbackFragment()).onPreferenceDisplayDialog(null, preference);
        }

        if (!handled && this.getActivity() instanceof PreferenceFragmentCompat.OnPreferenceDisplayDialogCallback) {
            handled = ((PreferenceFragmentCompat.OnPreferenceDisplayDialogCallback) this.getActivity()).onPreferenceDisplayDialog(null, preference);
        }

        if (!handled) {
            assert this.getFragmentManager() != null;
            if (this.getFragmentManager().findFragmentByTag("android.support.v7.preference.PreferenceFragment.DIALOG") == null) {
                Object f;
                if (preference instanceof EditTextPreference) {
                    f = EditTextPreferenceDialogFragmentCompat.newInstance(preference.getKey());
                } else {
                    if (!(preference instanceof ListPreference)) {
                        throw new IllegalArgumentException("Tried to display dialog for unknown preference type. Did you forget to override onDisplayPreferenceDialog()?");
                    }

                    f = ListPreferenceDialogFragmentCompat.newInstance(preference.getKey());
                }

                ((DialogFragment) f).setTargetFragment(this, 0);
                ((DialogFragment) f).show(this.getFragmentManager(), "android.support.v7.preference.PreferenceFragment.DIALOG");
            }
        }
    }

    public Fragment getCallbackFragment() {
        return null;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.pref_ptz_mode_utc)) ||
                key.equals(getString(R.string.pref_ptz_mode_sdi)) ||
                key.equals(getString(R.string.pref_ptz_protocol)) ||
                key.equals(getString(R.string.pref_utc_protocol_cvbs)) ||
                key.equals(getString(R.string.pref_utc_protocol_tvi)) ||
                key.equals(getString(R.string.pref_utc_protocol_ahd)) ||
                key.equals(getString(R.string.pref_utc_protocol_cvi)) ||
                key.equals(getString(R.string.pref_ptz_address)) ||
                key.equals(getString(R.string.pref_ptz_baudrate)) ||
                key.equals(getString(R.string.pref_ptz_termination))) {
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        // getSelectedItemPosition()은 헤더를 포함한 인덱스를 반환하므로 -1을 해야 한다.
        final Preference preference = mAdapter.getItem(mListView.getSelectedItemPosition() - 1);
        //Log.d(TAG, "onKey="+keyCode);

        if (event.getAction() == KeyEvent.ACTION_DOWN) {
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
            } else if (preference instanceof SwitchPreference) {
                // SwitchPreference는 오른쪽 키를 누르면 켜고, 왼쪽 키를 누르면 꺼준다.
                SwitchPreference switchPreference = (SwitchPreference) preference;
                switch (keyCode) {
                    case KeyEvent.KEYCODE_DPAD_LEFT:
                        switchPreference.setChecked(false);
                        return true;

                    case KeyEvent.KEYCODE_DPAD_RIGHT:
                        switchPreference.setChecked(true);
                        return true;

                    case KeyEvent.KEYCODE_ENTER:
                        if (switchPreference.isChecked()) {
                            switchPreference.setChecked(false);
                        } else if (!switchPreference.isChecked()) {
                            switchPreference.setChecked(true);
                        }
                        return true;
                }
            } else if (preference instanceof NumberPickerPreference) {
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
        }
        return false;
    }
}
