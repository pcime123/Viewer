package com.sscctv.seeeyesmonitor;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.preference.DialogPreference;
//import android.util.Log;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;


/**
 * A simple {@link DialogFragment} subclass.
 * Use the {@link PanelFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 * PreferencesFragment를 일반적인 뷰에 표시하는 것은 간단하다.
 * 메인 메뉴를 이렇게 구현하면 간단하겠지만 문제는 메인 아이콘 뷰와 PreferencesFragment를 표시하는 하위메뉴 뷰가
 * 같은 레벨에 있기 때문에 키를 눌러 선택을 옮기면 상위메뉴와 하위메뉴의 이동이 자유로운 문제가 있다.
 * 이를 피하기 위한 편법으로 하위메뉴 뷰에 fragment를 표시하는 대신 다이얼로그 박스에 PreferencesFragment를 넣고
 * 하위메뉴 위치에 커스텀 다이얼로그를 표시하는 방식을 써 본다. 전통적인 방식으로는 별도의 Activity로 다이얼로그를 띄우기도 하지만
 * 시험결과 DialogFragment를 쓰는 것이 좀 더 빠른 반응을 보이며 코드량도 크게 차이가 나지 않는다.
 *
 * 즉 이 클래스의 용도는,
 * - 다이얼로그 박스 대신 쓴다
 * - 다이얼로그처럼 띄우며 그 안에 다른 fragment를 표시한다
 * - 부가적으로 스타일링을 위한 하드코딩...
 */
public abstract class PanelFragment extends DialogFragment implements DialogInterface.OnClickListener {
    //private static final String TAG = "PanelFragment";

    private static final String ARG_KEY = "key";

    private DialogPreference mPreference;
    private Dialog dialog;
    private PtzOverlayFragment ptzOverlayFragment;

    /** Which button was clicked. */
    private int mWhichButtonClicked;

    public PanelFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param key 표시할 설정의 키 이름
     * @return A new instance of fragment PanelFragment.
     */
    protected static PanelFragment newInstance(PanelFragment fragment, String key) {
        Bundle args = new Bundle();
        args.putString(ARG_KEY, key);
        fragment.setArguments(args);
        return fragment;
    }

    public DialogPreference getPreference() {
        return mPreference;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Fragment rawFragment = getTargetFragment();
        if (!(rawFragment instanceof DialogPreference.TargetFragment)) {
            throw new IllegalStateException("Target fragment must implement TargetFragment" +
                    " interface");
        }

        final DialogPreference.TargetFragment fragment =
                (DialogPreference.TargetFragment) rawFragment;

        final String key = getArguments().getString(ARG_KEY);
        mPreference = (DialogPreference) fragment.findPreference(key);
    }

    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mWhichButtonClicked = DialogInterface.BUTTON_NEGATIVE;

        /**
         * 참고 문서에는 onCreateDialog를 구현하면 onCreateView가 필요없는 것으로 착각할 수 있는 설명이 있지만
         * 실제로는 그렇지 않다. 별도의 custom layout을 구현할 필요가 없이 AlertDialog만을 쓴다면 onCreateView를
         * 구현할 필요가 없지만 여기서는 custom layout을 구현한 DialogFragment를 구현하는게 목적이므로
         * Dialog를 만든 다음 일반 DialogFragment를 만들 때와 마찬가지로 onCreateView에서 custom layout을
         * 만들어 준다.
         *
         * 참조: 대화를 전체 화면으로 또는 포함된 프래그먼트로 표시
         * http://developer.android.com/intl/ko/guide/topics/ui/dialogs.html
         */

        // 커스텀 스타일로 지정해 둔다.
        // 프레임을 제거하려면 STYLE_NO_FRAME을 지정해야 한다.
        setStyle(STYLE_NORMAL, R.style.AppTheme_Panel);

        dialog = super.onCreateDialog(savedInstanceState);

        dialog.setTitle(getPreference().getDialogTitle());
//
        return dialog;
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Window window = getDialog().getWindow();

        WindowManager.LayoutParams wmlp = new WindowManager.LayoutParams();
        wmlp.copyFrom(dialog.getWindow().getAttributes());
        wmlp.width = 350;
        wmlp.height = 1500;
        wmlp.gravity = Gravity.END;

        wmlp.x = getResources().getDimensionPixelOffset(R.dimen.main_menu_width);

        window.setAttributes(wmlp);
//        WindowManager.LayoutParams wmlp = window.getAttributes();

        // 1. 우선 다이얼로그가 가운데 표시되는 것을 막는다. 오른쪽으로 붙게 한다.
        //    wmlp.gravity = Gravity.END와 같은 효과
//        window.setGravity(Gravity.END);

        // 2. 오른쪽으로 붙이되 메인메뉴가 보일 수 있게 오른쪽에 간격을 띄운다.
        //    wmlp.x는 예상과 달리 화면 오른쪽에서 왼쪽으로 띄우는 오프셋으로 동작한다.
        wmlp.x = getResources().getDimensionPixelOffset(R.dimen.main_menu_width);

        // 3. 다이얼로그의 폭을 설정한다.
        //    특이하게도 위의 wmlp.width를 설정해도 효과가 없으며 titleDivider의 폭을 변경해 주어야 한다.
        //    @id/titleDivider는 android.R.id를 통해 접근할 수 없으므로 직접 알아낸다.

        int titleDiviederId = getResources().getIdentifier("titleDivider", "id", "android");
        View titleDivider = window.getDecorView().findViewById(titleDiviederId);
        ViewGroup.LayoutParams lp = titleDivider.getLayoutParams();
        lp.width = getResources().getDimensionPixelOffset(R.dimen.panel_width);
        lp.height = getResources().getDimensionPixelOffset(R.dimen.panel_height);
        titleDivider.setBackgroundColor(Color.TRANSPARENT);
        // 4. 다이얼로그가 표시될 때 배경이 어두워지는 것을 막는다.
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        // 배경 스타일을 지정한다.
        window.setBackgroundDrawableResource(R.drawable.rounded_panel);

        // Inflate the layout for this fragment
        View v = inflater.inflate(getPreference().getDialogLayoutResource(), container, false);

        onCreatePanelContent(v);
        return v;
    }

    protected void onCreatePanelContent(View view) {}

    @Override
    public void onClick(DialogInterface dialog, int which) {
//        Log.d("Panel", "onClick(" + which + ")");
        mWhichButtonClicked = which;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        onDialogClosed(mWhichButtonClicked == DialogInterface.BUTTON_POSITIVE);
    }

    public abstract void onDialogClosed(boolean positiveResult);
}
