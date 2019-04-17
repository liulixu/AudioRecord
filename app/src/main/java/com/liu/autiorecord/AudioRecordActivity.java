package com.liu.autiorecord;

import android.Manifest;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


import com.liu.autiorecord.utils.PcmToWavUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import static com.liu.autiorecord.utils.GlobalConfig.AUDIO_FORMAT;
import static com.liu.autiorecord.utils.GlobalConfig.CHANNEL_CONFIG;
import static com.liu.autiorecord.utils.GlobalConfig.SAMPLE_RATE_INHZ;

public class AudioRecordActivity extends BaseActivity  {

    private AudioRecord audioRecord = null;//声明AudioRecord对象
    private boolean isRecording = false;//是否录音
    private boolean isPlaying = false;//是否在播放声音
    private AudioTrack audioTrack;
    private FileInputStream fileInputStream;

    private TextView tvTime;
    private Button btnBegin;
    private Button btnConvert;
    private Button btnPlay;

    private String[] PERMS_WRITE = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.RECORD_AUDIO
    };
    private void init(){
        /**
         * 动态权限申请
         */
        checkPermissions(this, 1, PERMS_WRITE);

        tvTime = findViewById(R.id.tv_time);
        btnBegin = findViewById(R.id.btn_begin);
        btnConvert = findViewById(R.id.btn_convert);
        btnPlay = findViewById(R.id.btn_play);

        btnBegin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("isRecording:",isRecording+"");
                if (!isRecording){
                    creatAutiRecord();
                    btnBegin.setText("停止录音");
                }else {
                    closeRecord();
                    btnBegin.setText("开始录音");
                }
            }
        });

        btnConvert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PcmToWavUtil pcmToWavUtil = new PcmToWavUtil(SAMPLE_RATE_INHZ, CHANNEL_CONFIG, AUDIO_FORMAT);
                File pcmFile = new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "test.pcm");
                File wavFile = new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "video2.wav");
                if (!wavFile.mkdirs()) {
                    Log.e("--------", "wavFile Directory not created");
                }
                if (wavFile.exists()) {
                    wavFile.delete();
                }
                pcmToWavUtil.pcmToWav(Environment.getExternalStorageDirectory().getPath()+"/video2.pcm", wavFile.getAbsolutePath());
            }
        });

        btnPlay.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View v) {
                if (!isRecording&&!isPlaying){
                    play();
                    btnPlay.setText("停止");
                }else {
                    stop();
                }
            }
        });
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    /**
     * 创建------------------------------------------------
     */
    private void creatAutiRecord(){
        final  int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE_INHZ, CHANNEL_CONFIG, AUDIO_FORMAT);
        //构造函数参数：
        //1.记录源
        //2.采样率，以赫兹表示
        //3.音频声道描述，声道数
        //4.返回音频声道的描述，格式
        //5.写入音频数据的缓冲区的总大小（字节），小于最小值将创建失败
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE_INHZ,
                CHANNEL_CONFIG, AUDIO_FORMAT, minBufferSize);

        final byte[] data = new byte[minBufferSize];
        final File file = new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC),"test.pcm");
        if (!file.mkdirs()){
            Log.e("demo failed---->","Directory not created");
        }
        if (file.exists()){
            file.delete();
        }

        audioRecord.startRecording();
        isRecording = true;

        new Thread(new Runnable() {
            @Override
            public void run() {
                FileOutputStream os = null;
                try {
                    os = new FileOutputStream(file);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                if (null != os){
                    while (isRecording){
                        int read = audioRecord.read(data,0,minBufferSize);
                        if (AudioRecord.ERROR_INVALID_OPERATION!=read){
                            try {
                                os.write(data);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    try {
                        Log.e("run------>","close file output stream !");
                        os.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private void closeRecord(){
        isRecording = false;
        if (null != audioRecord){
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
    }

    /**
     * 播放----------------------------------------------------------------------
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void play(){
        /*
         * SAMPLE_RATE_INHZ 对应pcm音频的采样率
         * channelConfig 对应pcm音频的声道
         * AUDIO_FORMAT 对应pcm音频的格式
         * */
        int channelConfig = AudioFormat.CHANNEL_OUT_MONO;
        final int minBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE_INHZ, channelConfig, AUDIO_FORMAT);
        audioTrack = new AudioTrack(
                new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                new AudioFormat.Builder().setSampleRate(SAMPLE_RATE_INHZ)
                        .setEncoding(AUDIO_FORMAT)
                        .setChannelMask(channelConfig)
                        .build(),
                minBufferSize,
                AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE);
        audioTrack.play();
        isPlaying = true;

        File file = new File(Environment.getExternalStorageDirectory().getPath()+"/video2.pcm");
        try {
            fileInputStream = new FileInputStream(file);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        byte[] tempBuffer = new byte[minBufferSize];
                        while (fileInputStream.available() > 0) {
                            int readCount = fileInputStream.read(tempBuffer);
                            if (readCount == AudioTrack.ERROR_INVALID_OPERATION ||
                                    readCount == AudioTrack.ERROR_BAD_VALUE) {
                                continue;
                            }
                            if (readCount != 0 && readCount != -1) {
                                audioTrack.write(tempBuffer, 0, readCount);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void stop(){
        if (audioTrack != null) {
            Log.d("player:", "Stopping");
            audioTrack.stop();
            Log.d("player:", "Releasing");
            audioTrack.release();
            Log.d("player:", "Nulling");

            btnPlay.setText("播放");
            isPlaying = false;
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeRecord();
    }
}
