package com.sscctv.seeeyesmonitor;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.util.Log;

import java.io.File;

public class MediaScanning  implements MediaScannerConnection.MediaScannerConnectionClient {
    private MediaScannerConnection mScanner;
    private String mFilepath = null;

    MediaScanning(Context context) {
        mScanner = new MediaScannerConnection(context, this);
    }

    void startScan(String filepath) {
        mFilepath = filepath;
        mScanner.connect(); // 이 함수 호출 후 onMediaScannerConnected가 호출 됨.
    }

    @Override
    public void onMediaScannerConnected() {
        if(mFilepath != null) {
            String filepath = mFilepath;
            mScanner.scanFile(filepath, null); // MediaStore의 정보를 업데이트
        }

        mFilepath = null;
    }

    @Override
    public void onScanCompleted(String path, Uri uri) {
        Log.d("MediaScanning", "ScanCompleted: " + path + " Uri: " +uri );
        mScanner.disconnect(); // onMediaScannerConnected 수행이 끝난 후 연결 해제
    }
}

