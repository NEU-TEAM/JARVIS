/*
 * Copyright (C) 2014 noda.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.robotca.ControlApp.Core;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.NoiseSuppressor;
import android.util.Log;

import org.jboss.netty.buffer.ChannelBuffers;
import org.ros.concurrent.CancellableLoop;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.topic.Publisher;

import java.nio.ByteOrder;


public class AudioPublisher extends AbstractNodeMain {
    private static final String LOG_TAG = "ROS AUDIO";
    private String topicName;
    private boolean userPaused = false;

    AudioPublisher(String topicName) {
        this.topicName = topicName;
    }

    private AudioRecord audioRecord;
    private static final int SAMPLE_RATE = 8000;

    void pause() {
        userPaused = true;
    }

    void play() {
        userPaused = false;
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of(topicName);
    }

    @Override
    public void onShutdown(Node node) {
        if (null != audioRecord) audioRecord.stop();
    }

    void onShutdown() {
        if (audioRecord != null) {
            audioRecord.stop();
        }
    }

    @Override
    public void onStart(final ConnectedNode connectedNode) {
//        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
//        final int bufferSize = AudioRecord.getMinBufferSize(32000,
//                AudioFormat.CHANNEL_OUT_MONO,//CHANNEL_OUT_STEREO
//                AudioFormat.ENCODING_PCM_16BIT);

        final int bufferSize = 32000;

        audioRecord = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT, bufferSize);

//        if (NoiseSuppressor.isAvailable()) {
//            NoiseSuppressor ns = NoiseSuppressor.create(audioRecord.getAudioSessionId());
//            ns.setEnabled(true);
//        }

        audioRecord.startRecording();

        final Publisher<audio_common_msgs.AudioData> publisher = connectedNode
                .newPublisher(topicName, audio_common_msgs.AudioData._TYPE);

        connectedNode.executeCancellableLoop(new CancellableLoop() {
            final byte[] buffer = new byte[bufferSize];

            @Override
            protected void setup() {
            }

            @Override
            protected void loop() throws InterruptedException {
                if (!userPaused) {
                    try {
                        audioRecord.read(buffer, 0, bufferSize);
                    } catch (Throwable t) {
                        Log.e("Error", "Read write failed");
                        t.printStackTrace();
                        return;
                    }
                    audio_common_msgs.AudioData data = publisher.newMessage();
                    data.setData(ChannelBuffers.copiedBuffer(ByteOrder.LITTLE_ENDIAN, buffer, 0, buffer.length));
                    publisher.publish(data);
                }
            }
        });
    }
}
