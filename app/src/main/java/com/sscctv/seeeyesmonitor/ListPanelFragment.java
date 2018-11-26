package com.sscctv.seeeyesmonitor;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.support.v7.preference.ListPreference;
//import android.util.Log;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/**
 * Created by trlim on 2015. 12. 16..
 * 설정에 쓰는 목록 대화상자를 대체한다
 */
public class ListPanelFragment extends PanelFragment {
    //private static final String TAG = "ListPanelFragment";

    private int mClickedDialogEntryIndex;
    PtzOverlayFragment ptzOverlayFragment;
    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param key 표시할 설정의 키 이름
     * @return A new instance of fragment PanelFragment.
     */
    public static ListPanelFragment newInstance(String key) {
        return (ListPanelFragment) PanelFragment.newInstance(new ListPanelFragment(), key);
    }

    private ListPreference getListPreference() {
        return (ListPreference)this.getPreference();
    }

    @Override
    public void onCreatePanelContent(View view) {
        super.onCreatePanelContent(view);

        final ListView listView = (ListView)view.findViewById(R.id.list);

        final ListPreference preference = getListPreference();
        if (preference.getEntries() == null || preference.getEntryValues() == null) {
            throw new IllegalStateException(
                    "ListPreference requires an entries array and an entryValues array.");
        }

        mClickedDialogEntryIndex = preference.findIndexOfValue(preference.getValue());
        //Log.d(TAG, "Prev selection = " + mClickedDialogEntryIndex);

        listView.setAdapter(new ArrayAdapter<CharSequence>(getContext(), R.layout.panel_list_item, preference.getEntries()) {

        });
        listView.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, View view, int position, long id) {
//                Log.d("TEST", "onItemClick(" + position + ") " + listView.getChoiceMode());
                mClickedDialogEntryIndex = position;

                DialogInterface dialog = getDialog();

                ListPanelFragment.this.onClick(dialog, DialogInterface.BUTTON_POSITIVE);
                dialog.dismiss();
            }
        });

        // 기본값은 첫번째 항목을 선택하므로 이전에 설정한 항목을 선택한다.
        // 단 안드로이드의 터치모드 동작 방식에 따라 버튼으로 메뉴를 조작할 때만 표시가 되고 터치모드에서는 보이지 않는다.
        listView.setSelection(mClickedDialogEntryIndex);
    }
    private void putIntegerPreference(SharedPreferences sharedPreferences, String key, int value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, Integer.toString(value));
        editor.apply();
    }
    private void changePtzProtocol(int item) {
        final String key = getString(R.string.pref_ptz_protocol);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        putIntegerPreference(sharedPreferences, key, item);
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        //Log.d(TAG, "onDialogClosed(" + positiveResult + ")");

        final ListPreference preference = getListPreference();
        if (positiveResult && mClickedDialogEntryIndex >= 0 && preference.getEntryValues() != null) {
            String value = preference.getEntryValues()[mClickedDialogEntryIndex].toString();
            if (preference.callChangeListener(value)) {
                preference.setValue(value);
            }
        }
    }
}
