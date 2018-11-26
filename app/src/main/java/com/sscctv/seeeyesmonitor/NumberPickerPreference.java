package com.sscctv.seeeyesmonitor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.content.res.TypedArrayUtils;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.NumberPicker;

/**
 * Created by trlim on 2015. 12. 16..
 *
 * EditTextPreference가 처리하지 못하는 부가적인 xml 속성을 지원하기 위한 클래스.
 * XML에 설정한 속성을 가지고 있다가 클라이언트(NumberPickerPanelFragment 등)에서 요청하면 그 값을 알려주는 일만 한다.
 */
public class NumberPickerPreference extends Preference {
    private final Listener mListener;

    private int mMin;
    private int mMax;
    private String mFormat;

    private int mValue;

    @SuppressLint("RestrictedApi")
    public NumberPickerPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        mListener = new Listener();

        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.NumberPickerPreference, defStyleAttr, defStyleRes);

        mMin = TypedArrayUtils.getInt(a, R.styleable.NumberPickerPreference_min, R.styleable.NumberPickerPreference_min, 0);
        mMax = TypedArrayUtils.getInt(a, R.styleable.NumberPickerPreference_max, R.styleable.NumberPickerPreference_max, 0);
        mFormat = TypedArrayUtils.getString(a, R.styleable.NumberPickerPreference_format, R.styleable.NumberPickerPreference_format);

        a.recycle();
    }

    public NumberPickerPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public NumberPickerPreference(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.numberPickerPreferenceStyle);
    }

    public NumberPickerPreference(Context context) {
        this(context, null);
    }

    public int getMin() {
        return mMin;
    }

    public int getMax() {
        return mMax;
    }

    public String getFormat() {
        return mFormat;
    }

    public void setValue(int value) {
        boolean changed = this.mValue != value;
        if (changed) {
            mValue = value;
            persistString(Integer.toString(value));
            notifyDependencyChange(shouldDisableDependents());
            notifyChanged();
        }
//        if(changed || !this.mCheckedSet) {
//            this.mChecked = checked;
//            this.mCheckedSet = true;
//            this.persistBoolean(checked);
//            if(changed) {
//                this.notifyDependencyChange(this.shouldDisableDependents());
//                this.notifyChanged();
//            }
//        }
    }

    public int getValue() {
        return mValue;
    }

    protected Object onGetDefaultValue(TypedArray a, int index) {
        // 실제 설정은 String으로 저장하지만 XML에서 설정하는 default 값은 Integer type이다.
        return a.getInt(index, 0);
    }

    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        this.setValue(restoreValue ? Integer.parseInt(getPersistedString(Integer.toString(mValue))) : (Integer) defaultValue);
    }

    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        if(this.isPersistent()) {
            return superState;
        } else {
            NumberPickerPreference.SavedState myState = new NumberPickerPreference.SavedState(superState);
            myState.value = this.getValue();
            return myState;
        }
    }

    protected void onRestoreInstanceState(Parcelable state) {
        if(state != null && state.getClass().equals(NumberPickerPreference.SavedState.class)) {
            NumberPickerPreference.SavedState myState = (NumberPickerPreference.SavedState)state;
            super.onRestoreInstanceState(myState.getSuperState());
            setValue(myState.value);
        } else {
            super.onRestoreInstanceState(state);
        }
    }

    private static class SavedState extends BaseSavedState {
        int value;

        public static final Creator CREATOR = new Creator() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };

        public SavedState(Parcel source) {
            super(source);
            value = source.readInt();
        }

        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(value);
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        View view = holder.findViewById(R.id.numberPickerWidget);
        if (view instanceof NumberPicker) {
            final NumberPicker numberPicker = (NumberPicker) view;

            numberPicker.setMinValue(getMin());
            numberPicker.setMaxValue(getMax());

            final String format = getFormat();
            if (format != null) {
                numberPicker.setFormatter(new NumberPicker.Formatter() {
                    @Override
                    public String format(int value) {
                        return String.format(format, value);
                    }
                });
            }

            numberPicker.setOnValueChangedListener(null);
            numberPicker.setValue(mValue);
            numberPicker.setOnValueChangedListener(mListener);
        }
//        this.syncSummaryView(holder);
    }

    private class Listener implements NumberPicker.OnValueChangeListener {
        @Override
        public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
            if (!callChangeListener(newVal)) {
                // TODO
//                Log.d("NumPick", "change not apply " + newVal);
            } else {
                setValue(newVal);
            }
        }
    }
}
