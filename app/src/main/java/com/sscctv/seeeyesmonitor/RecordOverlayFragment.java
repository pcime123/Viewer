package com.sscctv.seeeyesmonitor;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link RecordOverlayFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link RecordOverlayFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class RecordOverlayFragment extends Fragment {
    public static final int CAPTURE_MOVIE = 1;
    public static final int CAPTURE_PHOTO = 2;

    private static final String ARG_CAPTURE_TYPE = "captureType";

    private int _captureType;
    private long mLastClickTime;

    private static final long MIN_CLICK_INTERVAL = 600;

    private OnFragmentInteractionListener mListener;
    private String TAG = "RecordOverlayFragment";

    public RecordOverlayFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param captureType Capture type.
     * @return A new instance of fragment RecordOverlayFragment.
     */
    public static RecordOverlayFragment newInstance(int captureType) {
        if (captureType != CAPTURE_MOVIE && captureType != CAPTURE_PHOTO) {
            return null;
        }

        RecordOverlayFragment fragment = new RecordOverlayFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_CAPTURE_TYPE, captureType);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            _captureType = getArguments().getInt(ARG_CAPTURE_TYPE);
            mLastClickTime = SystemClock.elapsedRealtime();

        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_record_overlay, container, false);
        TextView stat = v.findViewById(R.id.recording_storage);
        String mode;
        boolean value;
        value = ((MainActivity) MainActivity.mContext).storageBoolean();
        mode = ((MainActivity) MainActivity.mContext).storageStat();
        if(!value) {
            stat.setText(R.string.storage_internal);
        } else {
            switch (mode) {
                case "internal":
                    stat.setText(R.string.storage_internal);
                    break;
                case "usb":
                    stat.setText(R.string.usb);
                    break;
                case "extsd":
                    stat.setText(R.string.extsd);
                    break;
            }
        }

        // UGLY: XML에서 선언적으로 설정할 수가 없다.
        // Fragment XML에 onClick을 설정해도 무조건 Activity로 이벤트를 전달한다.
        final ImageButton button = v.findViewById(R.id.button_capture);
        if (_captureType == CAPTURE_MOVIE) {
            button.setImageResource(R.drawable.ic_media_record);
        } else {
            button.setImageResource(R.drawable.ic_media_snapshot);
        }
        button.setOnClickListener(v1 -> {
            if (SystemClock.elapsedRealtime() - mLastClickTime < 500) {
                return;
            }
            mLastClickTime = SystemClock.elapsedRealtime();
            onCaptureClick();
        });

        final View recordInfo = v.findViewById(R.id.record_info);
        recordInfo.setVisibility(_captureType == CAPTURE_MOVIE ? View.VISIBLE : View.GONE);

        return v;
    }

    public void onCaptureClick() {
        if (mListener != null) {
            mListener.onCaptureClick(_captureType);
        }
    }

    public void setRecordingState(boolean recording) {
        if (_captureType == CAPTURE_MOVIE) {
            View v = getView();
            if (v != null) {
                final ImageButton button = v.findViewById(R.id.button_capture);
                if (button != null) {
                    button.setImageResource(recording ? R.drawable.ic_media_stop : R.drawable.ic_media_record);
                }
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

    public interface OnFragmentInteractionListener {
        void onCaptureClick(int captureType);
    }
}
