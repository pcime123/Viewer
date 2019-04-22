package com.sscctv.seeeyesmonitor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
 * {@link OsdControlFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link OsdControlFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class OsdControlFragment extends PtzControlFragment implements View.OnTouchListener {
    //private static final String TAG = "OsdControlFragment";
    private OnFragmentInteractionListener mListener;
    private Button upButton, downButton, leftButton, rightButton, menuButton, closeButton, exitButton, enterButton;

    public OsdControlFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment OsdControlFragment.
     */
    public static OsdControlFragment newInstance() {
        OsdControlFragment fragment = new OsdControlFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_osd_control, container, false);
        upButton = view.findViewById(R.id.osd_key_up);
        upButton.setOnTouchListener(this);
        downButton = view.findViewById(R.id.osd_key_down);
        downButton.setOnTouchListener(this);
        leftButton = view.findViewById(R.id.osd_key_left);
        leftButton.setOnTouchListener(this);
        rightButton = view.findViewById(R.id.osd_key_right);
        rightButton.setOnTouchListener(this);
        enterButton = view.findViewById(R.id.osd_key_enter);
        enterButton.setOnTouchListener(this);
        exitButton = view.findViewById(R.id.osd_key_exit);
        exitButton.setOnTouchListener(this);
        closeButton = view.findViewById(R.id.osd_key_close);
        closeButton.setOnTouchListener(this);
        menuButton = view.findViewById(R.id.osd_key_menu);
        menuButton.setOnTouchListener(this);

        View.OnClickListener switchToPtz = v -> {
            if (mListener != null) {
                mListener.onSwitchToPanTilt();
            }
        };
        View ptzButton = view.findViewById(R.id.key_ptz);
        ptzButton.setOnClickListener(switchToPtz);
        View osdButton = view.findViewById(R.id.key_osd);
        osdButton.setOnClickListener(switchToPtz);

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
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);
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
            case R.id.osd_key_up:
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    upButton.setBackground(getResources().getDrawable(R.drawable.btn_rounded_active));
                    mListener.onPtzMenuUp();
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    upButton.setBackground(getResources().getDrawable(R.drawable.btn_rounded_setting));
                    mListener.onPtzStop();
                    return true;
                }
                break;
            case R.id.osd_key_down:
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    downButton.setBackground(getResources().getDrawable(R.drawable.btn_rounded_active));
                    mListener.onPtzMenuDown();
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    downButton.setBackground(getResources().getDrawable(R.drawable.btn_rounded_setting));
                    mListener.onPtzStop();
                    return true;
                }
                break;
            case R.id.osd_key_left:
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    leftButton.setBackground(getResources().getDrawable(R.drawable.btn_rounded_active));
                    mListener.onPtzMenuLeft();
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    leftButton.setBackground(getResources().getDrawable(R.drawable.btn_rounded_setting));
                    mListener.onPtzStop();
                    return true;
                }
                break;
            case R.id.osd_key_right:
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    rightButton.setBackground(getResources().getDrawable(R.drawable.btn_rounded_active));
                    mListener.onPtzMenuRight();
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    rightButton.setBackground(getResources().getDrawable(R.drawable.btn_rounded_setting));
                    mListener.onPtzStop();
                    return true;
                }
                break;
            case R.id.osd_key_menu:
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    menuButton.setBackground(getResources().getDrawable(R.drawable.btn_rounded_active));
                    mListener.onPtzMenuOn();
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    menuButton.setBackground(getResources().getDrawable(R.drawable.btn_rounded_setting));
                    return true;
                }
                break;
            case R.id.osd_key_enter:
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    enterButton.setBackground(getResources().getDrawable(R.drawable.btn_rounded_active));
                    mListener.onPtzMenuEnter();
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    enterButton.setBackground(getResources().getDrawable(R.drawable.btn_rounded_setting));
                    return true;
                }
                break;
            case R.id.osd_key_exit:
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    exitButton.setBackground(getResources().getDrawable(R.drawable.btn_rounded_active));
                    mListener.onPtzMenuEsc();
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    exitButton.setBackground(getResources().getDrawable(R.drawable.btn_rounded_setting));
                    return true;
                }
                break;
            case R.id.osd_key_close:
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    closeButton.setBackground(getResources().getDrawable(R.drawable.btn_rounded_active));
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
//        Log.d(TAG, "Osd: " + keyCode + " ," + event);
        if (mListener == null)
            return false;

        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_UP:
                    mListener.onPtzMenuUp();
                    return true;

                case KeyEvent.KEYCODE_DPAD_DOWN:
                    mListener.onPtzMenuDown();
                    return true;

                case KeyEvent.KEYCODE_DPAD_LEFT:
                    mListener.onPtzMenuLeft();
                    return true;

                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    mListener.onPtzMenuRight();
                    return true;

                case KeyEvent.KEYCODE_BUTTON_MODE:
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
                    mListener.onPtzMenuEnter();
//                    mListener.onPtzStop();
                    return true;

                case KeyEvent.KEYCODE_BUTTON_MODE:
                    mListener.onSwitchToPanTilt();
                    return true;

                case KeyEvent.KEYCODE_HOME:
                    mListener.onExitPtzMode();
                    mListener.onExitMenu();
                    return true;

                case KeyEvent.KEYCODE_MENU:
                    mListener.onPtzMenuOn();
                    return true;

                case KeyEvent.KEYCODE_BACK:
                    mListener.onPtzMenuEsc();
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

        void onPtzMenuUp();

        void onPtzMenuDown();

        void onPtzMenuLeft();

        void onPtzMenuRight();

        void onPtzMenuOn();

        void onPtzMenuEnter();

        void onPtzMenuEsc();

        void onSwitchToPanTilt();

        void onExitPtzMode();

        void onExitMenu();
    }
}
