package com.robotca.ControlApp.Fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.robotca.ControlApp.ControlApp;
import com.robotca.ControlApp.Core.RobotController;
import com.robotca.ControlApp.R;

import org.ros.android.BitmapFromCompressedImage;
import org.ros.android.MessageCallable;
import org.ros.android.view.RosImageView;
import org.ros.android.view.RosTextView;
import org.ros.message.MessageListener;

import java.util.Objects;

import sensor_msgs.CompressedImage;

/**
 * Fragment for showing the view from the Robot's camera.
 *
 * Created by Michael Brunson on 11/7/15.
 */
public class CameraViewFragment extends RosFragment {

    private RosImageView<sensor_msgs.CompressedImage> cameraView;
    private TextView noCameraTextView;
    private RosTextView<std_msgs.String> statusView;
    private RosTextView<std_msgs.String> consoleView;
    private RobotController controller;

    /**
     * Default Constructor.
     */
    public CameraViewFragment(){}

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        @SuppressLint("InflateParams") View view = inflater.inflate(R.layout.fragment_camera_view, null);
        noCameraTextView = (TextView)view.findViewById(R.id.noCameraTextView);
        //noinspection unchecked
        cameraView = (RosImageView<sensor_msgs.CompressedImage>) view.findViewById(R.id.camera_fragment_camera_view);

        cameraView.setTopicName(PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("edittext_camera_topic", getString(R.string.camera_topic)));
        cameraView.setMessageType(CompressedImage._TYPE);
        cameraView.setMessageToBitmapCallable(new BitmapFromCompressedImage());

        try {
            controller = ((ControlApp) getActivity()).getRobotController();
        }
        catch(Exception ignore){
        }

        // Create a message listener for getting camera data
        if(controller != null){
            controller.setCameraMessageReceivedListener(new MessageListener<CompressedImage>() {
                @Override
                public void onNewMessage(CompressedImage compressedImage) {
                    if (compressedImage != null) {
                        controller.setCameraMessageReceivedListener(null);
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                noCameraTextView.setVisibility(View.GONE);
                            }
                        });
                    }
                }
            });
        }

        //initialize console
        statusView = (RosTextView<std_msgs.String>) view.findViewById(R.id.camera_fragment_status_text_view);
        statusView.setTopicName(PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("edittext_status_topic", getString(R.string.vision_status_topic)));
        statusView.setMessageType(std_msgs.String._TYPE);
        statusView.setMessageToStringCallable(new MessageCallable<String, std_msgs.String>() {
            @Override
            public String call(std_msgs.String message) {
                String s;
                if (Objects.equals(message.getData(), "")) {
                    s = getString(R.string.vision_common_pref) + " " + getString(R.string.vision_wander_mode);
                }
                else if (Objects.equals(message.getData(), getString(R.string.vision_wander_mode))) {
                    s = getString(R.string.vision_common_pref) + " " + getString(R.string.vision_wander_mode);
                }
                else {
                    s = getString(R.string.vision_common_pref) + " " + message.getData() + " " + getString(R.string.vision_common_surf);
                }
                return s; // this directly set text on view
            }
        });

        consoleView = (RosTextView<std_msgs.String>) view.findViewById(R.id.camera_fragment_console_text_view);
        consoleView.setTopicName(PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("edittxet_console_topic", getString(R.string.vision_console_topic)));
        consoleView.setMessageType(std_msgs.String._TYPE);
        consoleView.setMessageToStringCallable(new MessageCallable<String, std_msgs.String>() {
            @Override
            public String call(std_msgs.String message) {
                String s;
                if (Objects.equals(message.getData(), "")) {
                    s = getString(R.string.vision_initial);
                }
                else {
                    s = message.getData();
                }
                return s;
            }
        });


        if (nodeConfiguration != null) {
            nodeMainExecutor.execute(cameraView, nodeConfiguration.setNodeName("android/fragment_camera_view"));
            nodeMainExecutor.execute(statusView, nodeConfiguration.setNodeName("android/fragment_camera_status"));
            nodeMainExecutor.execute(consoleView, nodeConfiguration.setNodeName("android/fragment_camera_console"));
        }

        return view;
    }

    @Override
    void shutdown() {
        nodeMainExecutor.shutdownNodeMain(cameraView);
    }
}