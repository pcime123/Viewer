package com.sscctv.seeeyesmonitor;

import android.content.Context;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.os.Handler;
import android.util.Log;

import static android.media.AudioManager.FLAG_PLAY_SOUND;
import static android.media.AudioManager.FLAG_SHOW_UI;

public class SettingsContentObserver extends ContentObserver {
    Context context;
    private String TAG = "Settings: Test";

    SettingsContentObserver(Context c, Handler handler) {
        super(handler);
        context = c;

        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        assert audio != null;
        int previousVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
    }

    @Override
    public boolean deliverSelfNotifications() {
        return super.deliverSelfNotifications();
    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        assert audio != null;
        int currentVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        Log.d(TAG, "onChange: " + currentVolume);
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, FLAG_PLAY_SOUND);

        audio.setParameters("dac_volume=" + (currentVolume));

    }
}