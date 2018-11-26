package com.sscctv.seeeyesmonitor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
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
public class OsdControlFragment extends PtzControlFragment {
    //private static final String TAG = "OsdControlFragment";
    private OnFragmentInteractionListener mListener;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_osd_control, container, false);


        TextView upButton = view.findViewById(R.id.key_up);
        upButton.setOnTouchListener((v, event) -> {
            int action = event.getAction();

            if (MotionEvent.ACTION_DOWN == action) {
                if (mListener != null) {
                    upButton.setPressed(true);
                    mListener.onPtzMenuUp();
                }
            } else if (MotionEvent.ACTION_UP == action) {
                upButton.setPressed(false);
                mListener.onPtzStop();
            }
            return true;
        });

        TextView downButton = view.findViewById(R.id.key_down);
        downButton.setOnTouchListener((v, event) -> {
            int action = event.getAction();

            if (MotionEvent.ACTION_DOWN == action) {
                if (mListener != null) {
                    downButton.setPressed(true);
                    mListener.onPtzMenuDown();
                }
            } else if (MotionEvent.ACTION_UP == action) {
                downButton.setPressed(false);
                mListener.onPtzStop();
            }
            return true;
        });

        TextView leftButton = view.findViewById(R.id.key_left);
        leftButton.setOnTouchListener((v, event) -> {
            int action = event.getAction();

            if (MotionEvent.ACTION_DOWN == action) {
                leftButton.setPressed(true);

                if (mListener != null) {
                    mListener.onPtzMenuLeft();
                }
            } else if (MotionEvent.ACTION_UP == action) {
                leftButton.setPressed(false);
                mListener.onPtzStop();
            }
            return true;
        });

        TextView rightButton = view.findViewById(R.id.key_right);
        rightButton.setOnTouchListener((v, event) -> {
            int action = event.getAction();

            if (MotionEvent.ACTION_DOWN == action) {
                rightButton.setPressed(true);

                if (mListener != null) {
                    mListener.onPtzMenuRight();
                }
            } else if (MotionEvent.ACTION_UP == action) {
                rightButton.setPressed(false);
                mListener.onPtzStop();
            }
            return true;
        });


        View enterButton = view.findViewById(R.id.key_enter);
        enterButton.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onPtzMenuEnter();
            }
        });

        View.OnClickListener switchToPtz = v -> {
            if (mListener != null) {
                mListener.onSwitchToPanTilt();
            }
        };
        View ptzButton = view.findViewById(R.id.key_ptz);
        ptzButton.setOnClickListener(switchToPtz);
        View osdButton = view.findViewById(R.id.key_osd);
        osdButton.setOnClickListener(switchToPtz);

        View exitButton = view.findViewById(R.id.key_exit);
        exitButton.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onPtzMenuEsc();
            }
        });

        View closeButton = view.findViewById(R.id.key_close);
        closeButton.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onExitPtzMode();
            }
        });

        View menuButton = view.findViewById(R.id.key_menu);
        menuButton.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onPtzMenuOn();
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
                    mListener.onSwitchToPanTilt();
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
                    return true;

                case KeyEvent.KEYCODE_BUTTON_MODE:
                    return true;

                case KeyEvent.KEYCODE_HOME:
                    mListener.onExitPtzMode();
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
    }
}
