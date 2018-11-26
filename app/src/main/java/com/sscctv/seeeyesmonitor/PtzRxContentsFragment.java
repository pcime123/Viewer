package com.sscctv.seeeyesmonitor;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PtzRxContentsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PtzRxContentsFragment extends PtzContentsFragment {
    private TextView mTextView;
    private int mTextViewLines;

    public PtzRxContentsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment PtzRxContentsFragment.
     */
    public static PtzRxContentsFragment newInstance() {
        PtzRxContentsFragment fragment = new PtzRxContentsFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_ptz_rx_contents, container, false);

        mTextView = view.findViewById(R.id.ptz_rx_contents);

        mTextViewLines = view.getResources().getInteger(R.integer.ptz_rx_lines);

        return view;
    }

    public void addBytes(String hexBytes) {
        mTextView.append(hexBytes);

        int excessLines = mTextView.getLineCount() - mTextViewLines;
        while (excessLines > 0) {
            // 첫번째 줄을 제거하여 스크롤 효과를 줌. 원래 제품은 상단부터 다시 표시하는 방식.
            int lineEnd = mTextView.getLayout().getLineEnd(0);

            Editable editable = mTextView.getEditableText();
            editable.delete(0, lineEnd);

            excessLines = mTextView.getLineCount() - mTextViewLines;
        }
    }

    @Override
    public void clear() {
        mTextView.setText(null);
    }
}
