package com.sscctv.seeeyesmonitor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
//import android.util.Log;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link PanTiltControlFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link PanTiltControlFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PanTiltControlFragment extends PtzControlFragment implements View.OnTouchListener {
    //private static final String TAG = "PanTiltControlFragment";
    private OnFragmentInteractionListener mListener;
    private TextView upButton, downButton, leftButton, rightButton, closeButton;

    public PanTiltControlFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment PanTiltControlFragment.
     */
    public static PanTiltControlFragment newInstance() {
        PanTiltControlFragment fragment = new PanTiltControlFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_pan_tilt_control, container, false);

        upButton = view.findViewById(R.id.ptz_key_up);
        upButton.setOnTouchListener(this);
        downButton = view.findViewById(R.id.ptz_key_down);
        downButton.setOnTouchListener(this);
        leftButton = view.findViewById(R.id.ptz_key_left);
        leftButton.setOnTouchListener(this);
        rightButton = view.findViewById(R.id.ptz_key_right);
        rightButton.setOnTouchListener(this);
        closeButton = view.findViewById(R.id.ptz_key_close);
        closeButton.setOnTouchListener(this);


        View.OnClickListener switchToZoomFocus = v -> {
            if (mListener != null) {
                mListener.onSwitchToZoomFocus();
            }
        };
        View zoomFocusButton = view.findViewById(R.id.key_zoomfocus);
        zoomFocusButton.setOnClickListener(switchToZoomFocus);
        View panTiltButton = view.findViewById(R.id.key_pantilt);
        panTiltButton.setOnClickListener(switchToZoomFocus);

        View.OnClickListener switchToOsd = v -> {
            if (mListener != null) {
                mListener.onSwitchToOsd();
            }
        };
        View ptzButton = view.findViewById(R.id.key_ptz);
        ptzButton.setOnClickListener(switchToOsd);
        View osdButton = view.findViewById(R.id.key_osd);
        osdButton.setOnClickListener(switchToOsd);

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

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (mListener == null)
            return false;

        switch (v.getId()) {
            case R.id.ptz_key_up:
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    upButton.setBackground(getResources().getDrawable(R.drawable.btn_rounded_active));
                    mListener.onTiltUp();
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    upButton.setBackground(getResources().getDrawable(R.drawable.btn_rounded_setting));
                    mListener.onPtzStop();
                    return true;
                }
                break;
            case R.id.ptz_key_down:
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    downButton.setBackground(getResources().getDrawable(R.drawable.btn_rounded_active));
                    mListener.onTiltDown();
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    downButton.setBackground(getResources().getDrawable(R.drawable.btn_rounded_setting));
                    mListener.onPtzStop();
                    return true;
                }
                break;
            case R.id.ptz_key_left:
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    leftButton.setBackground(getResources().getDrawable(R.drawable.btn_rounded_active));
                    mListener.onPanLeft();
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    leftButton.setBackground(getResources().getDrawable(R.drawable.btn_rounded_setting));
                    mListener.onPtzStop();
                    return true;
                }
                break;
            case R.id.ptz_key_right:
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    rightButton.setBackground(getResources().getDrawable(R.drawable.btn_rounded_active));
                    mListener.onPanRight();
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    rightButton.setBackground(getResources().getDrawable(R.drawable.btn_rounded_setting));
                    mListener.onPtzStop();
                    return true;
                }
                break;
            case R.id.ptz_key_close:
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    closeButton.setBackground(getResources().getDrawable(R.drawable.btn_rounded_active));
                    mListener.onExitPtzMode();
                    mListener.onExitMenu();
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    closeButton.setBackground(getResources().getDrawable(R.drawable.btn_rounded_setting));
                    return true;
                }
                break;

        }
        return false;
    }

    @Override
    public boolean onKeyPress(int keyCode, KeyEvent event) {
        if (mListener == null)
            return false;
//        Log.d(TAG, "onKey=" + keyCode + " ," + event);

        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_UP:
                    mListener.onTiltUp();
                    return true;

                case KeyEvent.KEYCODE_DPAD_DOWN:
                    mListener.onTiltDown();
                    return true;

                case KeyEvent.KEYCODE_DPAD_LEFT:
                    mListener.onPanLeft();
                    return true;

                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    mListener.onPanRight();
                    return true;

                case KeyEvent.KEYCODE_BUTTON_MODE:
//                    mListener.onSwitchToOsd();
                    return true;

                case KeyEvent.KEYCODE_ENTER:
                case KeyEvent.KEYCODE_HOME:
                case KeyEvent.KEYCODE_MENU:
                case KeyEvent.KEYCODE_BACK:
                    return true;

            }
        } else if (event.getAction() == KeyEvent.ACTION_UP) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_UP:
                case KeyEvent.KEYCODE_DPAD_DOWN:
                case KeyEvent.KEYCODE_DPAD_LEFT:
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    mListener.onPtzStop();
                    return true;

                case KeyEvent.KEYCODE_ENTER:
                    mListener.onSwitchToZoomFocus();
                    return true;

                case KeyEvent.KEYCODE_HOME:
                case KeyEvent.KEYCODE_BACK:
                    mListener.onExitPtzMode();
                    mListener.onExitMenu();
                    return true;

                case KeyEvent.KEYCODE_BUTTON_MODE:
                    mListener.onSwitchToOsd();
                    return true;

                case KeyEvent.KEYCODE_MENU:
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
        void onPtzStop();

        void onTiltUp();

        void onTiltDown();

        void onPanLeft();

        void onPanRight();

        void onSwitchToZoomFocus();

        void onSwitchToOsd();

        void onExitPtzMode();

        void onExitMenu();

    }
}
