package com.liu.autiorecord;

import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.nfc.Tag;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.liu.autiorecord.utils.G711Code;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import static com.liu.autiorecord.utils.GlobalConfig.AUDIO_FORMAT;
import static com.liu.autiorecord.utils.GlobalConfig.CHANNEL_CONFIG;
import static com.liu.autiorecord.utils.GlobalConfig.SAMPLE_RATE_INHZ;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
public class PCM2AACActivity extends AppCompatActivity {


    private Button btn;
    private TextView tv;


    private MediaCodec mMediaCodec;
    private MediaCodec.BufferInfo mBufferInfo;
    private final String mime = "audio/mp4a-latm";
    private int bitRate = 96000;
    private ByteBuffer[] inputBufferArrary;
    private ByteBuffer[] outputBufferArrary;
    private FileOutputStream fileOutputStream;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pcm2_aac);
        btn = findViewById(R.id.btn1);
        tv = findViewById(R.id.url_tv);
//        initMediaCodec();

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
    private void initMediaCodec(){
        try {
            File root = Environment.getExternalStorageDirectory();
            File fileAAC = new File(root,"scv_a.aac");
            if (!fileAAC.exists()){
                fileAAC.createNewFile();
            }
            fileOutputStream = new FileOutputStream(fileAAC.getAbsoluteFile());
            mMediaCodec = MediaCodec.createEncoderByType(mime);
            MediaFormat mediaFormat = new MediaFormat();
            mediaFormat.setString(MediaFormat.KEY_MIME,mime);
            mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectERLC);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
            mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 2);
            mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 1024 * 1024);
            mediaFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, 44100);

            mMediaCodec.configure(mediaFormat,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);
            mMediaCodec.start();
            inputBufferArrary = mMediaCodec.getInputBuffers();
            outputBufferArrary = mMediaCodec.getOutputBuffers();
            mBufferInfo = new MediaCodec.BufferInfo();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public void  encodeData(byte[] data){
        //dequeueInputBuffer（time）需要传入一个时间值，-1表示一直等待，0表示不等待有可能会丢帧，其他表示等待多少毫秒
        int inputIndex = mMediaCodec.dequeueInputBuffer(-1);//获取输入缓存的index
        if (inputIndex >= 0) {
            ByteBuffer inputByteBuf = inputBufferArrary[inputIndex];
            inputByteBuf.clear();
            inputByteBuf.put(data);//添加数据
            inputByteBuf.limit(data.length);//限制ByteBuffer的访问长度
            mMediaCodec.queueInputBuffer(inputIndex, 0, data.length, 0, 0);//把输入缓存塞回去给MediaCodec
        }

        int outputIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, 0);//获取输出缓存的index
        while (outputIndex >= 0) {
            //获取缓存信息的长度
            int byteBufSize = mBufferInfo.size;
            //添加ADTS头部后的长度
            int bytePacketSize = byteBufSize + 7;

            ByteBuffer  outPutBuf = outputBufferArrary[outputIndex];
            outPutBuf.position(mBufferInfo.offset);
            outPutBuf.limit(mBufferInfo.offset+mBufferInfo.size);

            byte[]  targetByte = new byte[bytePacketSize];
            //添加ADTS头部
            addADTStoPacket(targetByte,bytePacketSize);
            /*
            get（byte[] dst,int offset,int length）:ByteBuffer从position位置开始读，读取length个byte，并写入dst下
            标从offset到offset + length的区域
             */
            outPutBuf.get(targetByte,7,byteBufSize);

            outPutBuf.position(mBufferInfo.offset);

            try {
                fileOutputStream.write(targetByte);
            } catch (IOException e) {
                e.printStackTrace();
            }
            //释放
            mMediaCodec.releaseOutputBuffer(outputIndex,false);
            outputIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo,0);
        }
    }
    /**
     * 给编码出的aac裸流添加adts头字段
     *
     * @param packet    要空出前7个字节，否则会搞乱数据
     * @param packetLen
     */
    private void addADTStoPacket(byte[] packet, int packetLen) {
        int profile = 2;  //AAC LC
        int freqIdx = 4;  //44.1KHz
        int chanCfg = 2;  //CPE
        packet[0] = (byte) 0xFF;
        packet[1] = (byte) 0xF9;
        packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
        packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;
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

                try {
                    os = new FileOutputStream(file);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                if (null != os){
                    while (isRecording){
                        int read = audioRecord.read(data,0,minBufferSize);
                        if (AudioRecord.ERROR_INVALID_OPERATION!=read){
                            //调用aac编码
                            encodeData(data);
                            try {
                                //写pcm本地
                                //os.write(data);
                                //写G711本地
                                os.write(data);
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
