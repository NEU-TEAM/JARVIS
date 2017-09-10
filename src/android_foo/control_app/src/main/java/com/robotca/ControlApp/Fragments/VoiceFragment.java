package com.robotca.ControlApp.Fragments;

/**
 * Created by Zhipeng Dong on 16-10-31.
 */

import android.app.Fragment;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.SynthesizerListener;
import com.robotca.ControlApp.JsonParser;
import com.robotca.ControlApp.Listener;
import com.robotca.ControlApp.R;
import com.robotca.ControlApp.Talker;

import org.json.JSONException;
import org.json.JSONObject;
import org.ros.message.MessageListener;

import java.util.HashMap;
import java.util.LinkedHashMap;

import static com.iflytek.cloud.SpeechConstant.ENGINE_TYPE;
import static com.iflytek.cloud.SpeechConstant.KEY_REQUEST_FOCUS;
import static com.iflytek.cloud.SpeechConstant.PARAMS;
import static com.iflytek.cloud.SpeechConstant.PITCH;
import static com.iflytek.cloud.SpeechConstant.SPEED;
import static com.iflytek.cloud.SpeechConstant.STREAM_TYPE;
import static com.iflytek.cloud.SpeechConstant.TYPE_LOCAL;
import static com.iflytek.cloud.SpeechConstant.VOICE_NAME;
import static com.iflytek.cloud.SpeechConstant.VOLUME;

public class VoiceFragment extends Fragment {


    private ImageButton virtualVoiceView;

    // 语音对象
    private SpeechRecognizer mIat;
    private SpeechSynthesizer mTts;
    private Toast mToast;
    private HashMap<String, String> mIatResults = new LinkedHashMap<String, String>();

    private String Text;
    // 函数调用返回值
    private int ret = 0;

    public Talker talker;
    public Listener listener;

    /**
     * Default Constructor.
     */
    public VoiceFragment() {
    }


    /**
     * Create this Fragments View.
     */
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_voice_view, container, false);
        SpeechUtility.createUtility(getActivity(), SpeechConstant.APPID+"=581c7563");

        listener = new Listener("android_remote_control", std_msgs.String._TYPE, new MessageListener<std_msgs.String>() {
            @Override
            public void onNewMessage(std_msgs.String message) {
                String str = message.getData();
                mTts.startSpeaking(str, mTtsListener);
            }
        });

        talker = new Talker("pc_remote_control", std_msgs.String._TYPE);

        virtualVoiceView = (ImageButton) view.findViewById(R.id.voice_button);

        mToast = Toast.makeText(getActivity(),null,Toast.LENGTH_SHORT);

        // 初始化对象
        mIat = SpeechRecognizer.createRecognizer(getActivity(), mInitListener);
        mTts = SpeechSynthesizer.createSynthesizer(getActivity(), mInitListener);

        virtualVoiceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mTts.isSpeaking())
                    mTts.stopSpeaking();
                iat();
            }
        });

        iat_setParam();
        tts_setParam();

        return view;
    }

    /**
     * 初始化监听器。
     */
    private InitListener mInitListener = new InitListener() {

        @Override
        public void onInit(int code) {
            if (code != ErrorCode.SUCCESS) {
                mToast.setText("初始化失败,错误码："+code);
                mToast.show();
            }
        }
    };

    /**
     * 听写监听器。
     */
    private RecognizerListener iat_mRecognizerListener = new RecognizerListener() {

        @Override
        public void onBeginOfSpeech() {
            mToast.setText("语音听写开始");
            mToast.show();
        }

        @Override
        public void onError(SpeechError error) {
        }

        @Override
        public void onEndOfSpeech() {
            mToast.setText("语音听写结束");
            mToast.show();
        }

        @Override
        public void onResult(RecognizerResult results, boolean isLast) {
            java.lang.String text = JsonParser.parseIatResult(results.getResultString());
            java.lang.String sn = null;
            // 读取json结果中的sn字段
            try {
                JSONObject resultJson = new JSONObject(results.getResultString());
                sn = resultJson.optString("sn");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            mIatResults.put(sn, text);

            StringBuffer resultBuffer = new StringBuffer();
            for (java.lang.String key : mIatResults.keySet()) {
                resultBuffer.append(mIatResults.get(key));
            }
            if (isLast) {
                Text = resultBuffer.toString();
                talker.publish(Text);
            }
        }

        @Override
        public void onVolumeChanged(int volume, byte[] data) {
            mToast.setText("当前正在说话，音量大小：" + volume);
            mToast.show();
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
        }
    };


    /**
     * 合成监听器。
     */
    private SynthesizerListener mTtsListener = new SynthesizerListener() {

        @Override
        public void onSpeakBegin() {
            mToast.setText("开始播放");
            mToast.show();
        }

        @Override
        public void onSpeakPaused() {
            mToast.setText("暂停播放");
            mToast.show();
        }

        @Override
        public void onSpeakResumed() {
            mToast.setText("继续播放");
            mToast.show();
        }

        @Override
        public void onBufferProgress(int percent, int beginPos, int endPos,
                                     java.lang.String info) {
        }

        @Override
        public void onSpeakProgress(int percent, int beginPos, int endPos) {
        }

        @Override
        public void onCompleted(SpeechError error) {
            if (error == null) {
                mToast.setText("播放完成");
                mToast.show();
            } else if (error != null) {
                mToast.setText(error.getPlainDescription(true));
                mToast.show();
            }
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
        }
    };

    private void iat(){
        // 开始听写
        // 如何判断一次听写结束：OnResult isLast=true 或者 onError
        mIatResults.clear();
        // 不显示听写对话框
        ret = mIat.startListening(iat_mRecognizerListener);
        if (ret != ErrorCode.SUCCESS) {
            mToast.setText("听写失败,错误码：" + ret);
            mToast.show();
        }
    }

    public void iat_setParam() {
        mIat.setParameter(SpeechConstant.PARAMS, null);
        mIat.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_LOCAL);
        mIat.setParameter(SpeechConstant.RESULT_TYPE, "json");
        mIat.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
        mIat.setParameter(SpeechConstant.ACCENT, "mandarin ");
        mIat.setParameter(SpeechConstant.VAD_BOS,"4000");
        mIat.setParameter(SpeechConstant.VAD_EOS,"1000");
        // 设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
        mIat.setParameter(SpeechConstant.ASR_PTT,"1");
        mIat.setParameter(SpeechConstant.AUDIO_FORMAT,"wav");
        mIat.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory()+"/讯飞语音平台/iat.wav");
    }

    private void tts_setParam(){
        mTts.setParameter(PARAMS, null);
        mTts.setParameter(ENGINE_TYPE, TYPE_LOCAL);
        mTts.setParameter(VOICE_NAME, "xiaoyan");
        mTts.setParameter(SPEED, "50");
        mTts.setParameter(PITCH, "50");
        mTts.setParameter(VOLUME, "50");
        //设置播放器音频流类型
        mTts.setParameter(STREAM_TYPE, "3");
        // 设置播放合成音频打断音乐播放，默认为true
        mTts.setParameter(KEY_REQUEST_FOCUS, "true");
        mTts.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
        mTts.setParameter(SpeechConstant.TTS_AUDIO_PATH, Environment.getExternalStorageDirectory()+"/讯飞语音平台/tts.wav");
    }

    /**
     * Shows the VoiceFragment.
     */
    public void show(){
        getFragmentManager()
                .beginTransaction()
                .show(this)
                .commit();
    }

    /**
     * Hides the VoiceFragment.
     */
    public void hide(){
        getFragmentManager()
                .beginTransaction()
                .hide(this)
                .commit();
    }
}
