package com.sscctv.seeeyesmonitor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.content.res.TypedArrayUtils;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.support.v7.widget.AppCompatSeekBar;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;

/**
 * Created by trlim on 2016. 3. 3..
 *
 * Custom SeekBar preference를 구현하는 클래스
 */
public class SeekBarPreference extends Preference {
    private final Listener mListener;

    private int mMax;

    private int mValue;

    @SuppressLint("RestrictedApi")
    public SeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        mListener = new Listener();

        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.SeekBarPreference, defStyleAttr, defStyleRes);

        mMax = TypedArrayUtils.getInt(a, R.styleable.SeekBarPreference_max, R.styleable.SeekBarPreference_max, 0);

        a.recycle();
    }

    public SeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public SeekBarPreference(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.numberPickerPreferenceStyle);
    }

    public SeekBarPreference(Context context) {
        this(context, null);
    }

    public int getMax() {
        return mMax;
    }

    public void setValue(int value) {
        boolean changed = this.mValue != value;
        if (changed) {
            mValue = value;
            persistString(Integer.toString(value));
            notifyDependencyChange(shouldDisableDependents());
            notifyChanged();
        }
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
            SeekBarPreference.SavedState myState = new SeekBarPreference.SavedState(superState);
            myState.value = this.getValue();
            return myState;
        }
    }

    protected void onRestoreInstanceState(Parcelable state) {
        if(state != null && state.getClass().equals(SeekBarPreference.SavedState.class)) {
            SeekBarPreference.SavedState myState = (SeekBarPreference.SavedState)state;
            super.onRestoreInstanceState(myState.getSuperState());
            setValue(myState.value);
        } else {
            super.onRestoreInstanceState(state);
        }
    }

    private static class SavedState extends Preference.BaseSavedState {
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
        View view = holder.findViewById(R.id.seekBarWidget);
        if (view instanceof AppCompatSeekBar) {
            final AppCompatSeekBar seekBar = (AppCompatSeekBar) view;

            seekBar.setMax(getMax());

            seekBar.setOnSeekBarChangeListener(null);
            seekBar.setProgress(mValue);
            seekBar.setOnSeekBarChangeListener(mListener);
        }
//        this.syncSummaryView(holder);
    }

    private class Listener implements AppCompatSeekBar.OnSeekBarChangeListener {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (!callChangeListener(progress)) {
                // TODO
//                Log.d("SeekBar", "change not apply " + progress);
            } else {
                setValue(progress);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    }
}
