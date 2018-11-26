package com.sscctv.seeeyesmonitor;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link PocOverlayFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link PocOverlayFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PocOverlayFragment extends Fragment {
    private static final String TAG = "PocOverlayFragment";
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

    public static final int MODE_CHECK = 0;
    public static final int MODE_POWER = 1;
    public static final int MODE_PSE = 2;

    private int _mode;
    private View _yesButton;

    private OnFragmentInteractionListener mListener;

    public PocOverlayFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment PocOverlayFragment.
     */
    public static PocOverlayFragment newInstance() {
        PocOverlayFragment fragment = new PocOverlayFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_poc_overlay, container, false);

        View yesButton = v.findViewById(R.id.yes);
        yesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
        		Log.d(TAG, "onClick = yes");
                if (mListener != null) {
                    switch (_mode) {
                        case MODE_CHECK:
                            mListener.onStartPocCheck();
                            break;

                        case MODE_POWER:
                            mListener.onApplyPocPower();
                            break;

                        case MODE_PSE:
                            break;
                    }
                }
            }
        });
        _yesButton = yesButton;

        View noButton = v.findViewById(R.id.no);
        noButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
        		Log.d(TAG, "onClick = no");
                if (mListener != null) {
                    switch (_mode) {
                        case MODE_CHECK:
                            mListener.onCancelPocCheck();
                            break;

                        case MODE_POWER:
                            mListener.onRemovePocPower();
                            break;

                        case MODE_PSE:
                            break;
                    }
                }
            }
        });

        return v;
    }

    public void setPocMode(int mode) {
        View v = getView();
        if (v != null) {
            TextView message = (TextView) v.findViewById(R.id.poc_message);
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
        }

        _mode = mode;

        _yesButton.requestFocus();
    }

    public void setPocState(int state) {
        View v = getView();
        if (v != null) {
            TextView pocState = (TextView) v.findViewById(R.id.poc_state);
            final String[] pocStates = getResources().getStringArray(R.array.poc_states);
            if (state < pocStates.length) {
                pocState.setText(pocStates[state]);
            }
        }
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

        setPocMode(MODE_CHECK);
        setPocState(STATE_POWER_OFF);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        void onStartPocCheck();
        void onCancelPocCheck();
        void onApplyPocPower();
        void onRemovePocPower();
    }
}
