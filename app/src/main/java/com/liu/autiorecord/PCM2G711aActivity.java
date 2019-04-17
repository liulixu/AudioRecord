package com.liu.autiorecord;

import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.liu.autiorecord.utils.G711Code;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import static com.liu.autiorecord.utils.GlobalConfig.AUDIO_FORMAT;
import static com.liu.autiorecord.utils.GlobalConfig.CHANNEL_CONFIG;
import static com.liu.autiorecord.utils.GlobalConfig.SAMPLE_RATE_INHZ;

public class PCM2G711aActivity extends AppCompatActivity {
    private Button btn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pcm2_g711a);

        btn = findViewById(R.id.btn1);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("isRecording:",isRecording+"");
                if (!isRecording){
                    creatAutiRecord();
                    btn.setText("停止录音");
                }else {
                    closeRecord();
                    btn.setText("开始录音");
                }
            }
        });
    }

    /**
     * 创建------------------------------------------------
     */
    private AudioRecord audioRecord = null;//声明AudioRecord对象
    private boolean isRecording = false;//是否录音
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
        final File file = new File(Environment.getExternalStorageDirectory().getPath(),"test.g711");
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
                short[] inG711Buffer = new short[minBufferSize];
                byte[] outG711Buffer = new byte[minBufferSize];

                try {
                    os = new FileOutputStream(file);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                if (null != os){
                    while (isRecording){
                        int read = audioRecord.read(data,0,minBufferSize);
                        int nReadBytes = audioRecord.read(inG711Buffer,0,inG711Buffer.length);
                        if (AudioRecord.ERROR_INVALID_OPERATION!=read){
                            //调用G711编码
                            G711Code.G711aEncoder(inG711Buffer,outG711Buffer,nReadBytes);
                            try {
                                //写pcm本地
                                //os.write(data);
                                //写G711本地
                                os.write(outG711Buffer);
                            } catch (IOException e) {
                                e.printStackTrace();
                                Log.e("写g711异常",e.toString()+"");
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
    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeRecord();
    }
}
