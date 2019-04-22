package com.sscctv.seeeyesmonitor;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
//import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link PtzRxControlFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link PtzRxControlFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PtzRxControlFragment extends PtzControlFragment {
    //private static final String TAG = "PtzRxControlFragment";
    private OnFragmentInteractionListener mListener;

    public PtzRxControlFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment PtzRxControlFragment.
     */
    public static PtzRxControlFragment newInstance() {
        PtzRxControlFragment fragment = new PtzRxControlFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_ptz_rx_control, container, false);

        View clearButton = view.findViewById(R.id.key_clear);
        clearButton.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onClearPtzScreen();
            }
        });
        View exitButton = view.findViewById(R.id.key_exit);
        exitButton.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onExitPtzMode();
                mListener.onExitMenu();
            }
        });

        return view;
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
    public boolean onKeyPress(int keyCode, KeyEvent event) {
        if (mListener == null)
            return false;
		//Log.d(TAG, "onKey="+keyCode);

        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    mListener.onExitPtzMode();
                    mListener.onExitMenu();
                    return true;

            }
            return false;
        } else if (event.getAction() == KeyEvent.ACTION_UP) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_UP:
                    mListener.onIncPtzBaudRate();
                    return true;

                case KeyEvent.KEYCODE_DPAD_DOWN:
                    mListener.onDecPtzBaudRate();
                    return true;

                case KeyEvent.KEYCODE_ENTER:
                    mListener.onClearPtzScreen();
                    return true;

                case KeyEvent.KEYCODE_BACK:
                    mListener.onExitPtzMode();
                    mListener.onExitMenu();
                    return true;
            }
        }

            return false;
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
        void onIncPtzBaudRate();
        void onDecPtzBaudRate();
        void onClearPtzScreen();
        void onExitPtzMode();
        void onExitMenu();

    }
}
