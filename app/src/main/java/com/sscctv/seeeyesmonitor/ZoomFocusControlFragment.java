package com.sscctv.seeeyesmonitor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
//import android.util.Log;
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
 * {@link ZoomFocusControlFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ZoomFocusControlFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ZoomFocusControlFragment extends PtzControlFragment  implements View.OnTouchListener {
    //private static final String TAG = "ZoomFocusControlFragment";
    private OnFragmentInteractionListener mListener;
    private TextView inButton, outButton, nearButton, farButton, closeButton;

    public ZoomFocusControlFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment ZoomFocusControlFragment.
     */
    public static ZoomFocusControlFragment newInstance() {
        ZoomFocusControlFragment fragment = new ZoomFocusControlFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_zoom_focus_control, container, false);

        inButton = view.findViewById(R.id.zf_key_up);
        inButton.setOnTouchListener(this);
//        upButton.setOnTouchListener((v, event) -> {
//            int action = event.getAction();
//
//            if (MotionEvent.ACTION_DOWN == action) {
//                if (mListener != null) {
//                    upButton.setPressed(true);
//                    mListener.onZoomIn();
//                }
//            } else if (MotionEvent.ACTION_UP == action) {
//                upButton.setPressed(false);
//                mListener.onPtzStop();
//            }
//            return true;
//        });

        outButton = view.findViewById(R.id.zf_key_down);
        outButton.setOnTouchListener(this);
//        downButton.setOnTouchListener((v, event) -> {
//            int action = event.getAction();
//
//            if (MotionEvent.ACTION_DOWN == action) {
//                if (mListener != null) {
//                    downButton.setPressed(true);
//                    mListener.onZoomOut();
//                }
//            } else if (MotionEvent.ACTION_UP == action) {
//                downButton.setPressed(false);
//                mListener.onPtzStop();
//            }
//            return true;
//        });

        nearButton = view.findViewById(R.id.zf_key_left);
        nearButton.setOnTouchListener(this);
//        leftButton.setOnTouchListener((v, event) -> {
//            int action = event.getAction();
//
//            if (MotionEvent.ACTION_DOWN == action) {
//                leftButton.setPressed(true);
//
//                if (mListener != null) {
//                    mListener.onFocusNear();
//                }
//            } else if (MotionEvent.ACTION_UP == action) {
//                leftButton.setPressed(false);
//                mListener.onPtzStop();
//            }
//            return true;
//        });

        farButton = view.findViewById(R.id.zf_key_right);
        farButton.setOnTouchListener(this);
//        rightButton.setOnTouchListener((v, event) -> {
//            int action = event.getAction();
//
//            if (MotionEvent.ACTION_DOWN == action) {
//                rightButton.setPressed(true);
//
//                if (mListener != null) {
//                    mListener.onFocusFar();
//                }
//            } else if (MotionEvent.ACTION_UP == action) {
//                rightButton.setPressed(false);
//                mListener.onPtzStop();
//            }
//            return true;
//        });

        closeButton = view.findViewById(R.id.zf_key_close);
        closeButton.setOnTouchListener(this);

        View.OnClickListener switchToPanTilt = v -> {
            if (mListener != null) {
                mListener.onSwitchToPanTilt();
            }
        };
        View zoomFocusButton = view.findViewById(R.id.key_zoomfocus);
        zoomFocusButton.setOnClickListener(switchToPanTilt);
        View panTiltButton = view.findViewById(R.id.key_pantilt);
        panTiltButton.setOnClickListener(switchToPanTilt);

        View.OnClickListener switchToOsd = v -> {
            if (mListener != null) {
                mListener.onSwitchToOsd();
            }
        };
        View ptzButton = view.findViewById(R.id.key_ptz);
        ptzButton.setOnClickListener(switchToOsd);
        View osdButton = view.findViewById(R.id.key_osd);
        osdButton.setOnClickListener(switchToOsd);

//        exitButton = view.findViewById(R.id.key_close);
//        exitButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (mListener != null) {
//                    mListener.onExitPtzMode();
//                }
//            }
//        });

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
            case R.id.zf_key_up:
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    inButton.setBackground(getResources().getDrawable(R.drawable.btn_rounded_active));
                    mListener.onZoomIn();
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    inButton.setBackground(getResources().getDrawable(R.drawable.btn_rounded_setting));
                    mListener.onPtzStop();
                    return true;
                }
                break;
            case R.id.zf_key_down:
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    outButton.setBackground(getResources().getDrawable(R.drawable.btn_rounded_active));
                    mListener.onZoomOut();
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    outButton.setBackground(getResources().getDrawable(R.drawable.btn_rounded_setting));
                    mListener.onPtzStop();
                    return true;
                }
                break;
            case R.id.zf_key_left:
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    nearButton.setBackground(getResources().getDrawable(R.drawable.btn_rounded_active));
                    mListener.onFocusNear();
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    nearButton.setBackground(getResources().getDrawable(R.drawable.btn_rounded_setting));
                    mListener.onPtzStop();
                    return true;
                }
                break;
            case R.id.zf_key_right:
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    farButton.setBackground(getResources().getDrawable(R.drawable.btn_rounded_active));
                    mListener.onFocusFar();
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    farButton.setBackground(getResources().getDrawable(R.drawable.btn_rounded_setting));
                    mListener.onPtzStop();
                    return true;
                }
                break;
            case R.id.zf_key_close:
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
		//Log.d(TAG, "onKey="+keyCode);

        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_UP:
                    mListener.onZoomIn();
                    return true;

                case KeyEvent.KEYCODE_DPAD_DOWN:
                    mListener.onZoomOut();
                    return true;

                case KeyEvent.KEYCODE_DPAD_LEFT:
                    mListener.onFocusNear();
                    return true;

                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    mListener.onFocusFar();
                    return true;

                case KeyEvent.KEYCODE_BUTTON_MODE:
//                    mListener.onSwitchToOsd();
                    return true;

                case KeyEvent.KEYCODE_BACK:
                case KeyEvent.KEYCODE_HOME:
                    mListener.onExitPtzMode();
                    mListener.onExitMenu();

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
                    mListener.onSwitchToPanTilt();
                    return true;

//                case KeyEvent.KEYCODE_BACK:
//                case KeyEvent.KEYCODE_HOME:
//                    mListener.onExitPtzMode();
//                    mListener.onExitMenu();
//
//                    return true;

                case KeyEvent.KEYCODE_BUTTON_MODE:
                    mListener.onSwitchToOsd();
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
        void onZoomIn();
        void onZoomOut();
        void onFocusFar();
        void onFocusNear();
        void onSwitchToPanTilt();
        void onSwitchToOsd();
        void onExitPtzMode();
        void onExitMenu();

    }
}
