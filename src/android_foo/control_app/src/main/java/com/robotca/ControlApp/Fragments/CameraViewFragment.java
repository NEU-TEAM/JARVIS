package com.robotca.ControlApp.Fragments;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.view.MotionEventCompat;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;

import com.robotca.ControlApp.ControlApp;
import com.robotca.ControlApp.Core.RobotController;
import com.robotca.ControlApp.R;

import org.ros.android.BitmapFromCompressedImage;
import org.ros.android.MessageCallable;
import org.ros.android.view.RosImageView;
import org.ros.android.view.RosTextView;
import org.ros.message.MessageListener;
import org.ros.node.parameter.ParameterTree;

import java.util.Objects;

import sensor_msgs.CompressedImage;
import std_msgs.UInt32;

/**
 * Fragment for showing the view from the Robot's camera.
 *
 * Created by Michael Brunson on 11/7/15.
 */
public class CameraViewFragment extends RosFragment {

    private RosImageView<sensor_msgs.CompressedImage> cameraView;
    private RosImageView<sensor_msgs.CompressedImage> cameraLabeledView;
    private TextView noCameraTextView;
    private RosTextView<std_msgs.String> statusView;
    private RosTextView<std_msgs.String> consoleView;
    private EditText editText;
    private ToggleButton toggleButton;
    private RobotController controller;
    private int maxValue = 99;

    private float x_start, y_start, x_end, y_end;
    private Rect rectangle;
    private Paint paint;

    /**
     * Default Constructor.
     */
    public CameraViewFragment(){
        rectangle = new Rect();
        paint = new Paint();
        paint.setColor(Color.GRAY);
        paint.setStrokeWidth(5);
        paint.setStyle(Paint.Style.STROKE);
    }


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

        cameraView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        x_start = event.getX();
                        y_start = event.getY();
                        v.setPressed(true);
                        break;

                    case MotionEvent.ACTION_MOVE:
                        float x = event.getX();
                        float y = event.getY();
                        boolean isInside = (x > 0 && x < v.getWidth() && y > 0 && y < v.getHeight());
                        if (v.isPressed() != isInside) {
                            v.setPressed(isInside);
                        }
                        break;
                    case MotionEvent.ACTION_CANCEL:
                        v.setPressed(false);
                        break;
                    case MotionEvent.ACTION_UP:
                        if (v.isPressed()) {
//                            v.performClick();
                            v.setPressed(false);
                            x_end = event.getX();
                            y_end = event.getY();
                            publishROI();
                        }
                        break;
                }
                return true;
            }
        });



        cameraLabeledView = (RosImageView<sensor_msgs.CompressedImage>) view.findViewById(R.id.camera_fragment_camera_labeled_view);
        cameraLabeledView.setTopicName(PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("edittext_camera_labeled_topic",
                getString(R.string.camera_labeled_topic)));
        cameraLabeledView.setMessageType(CompressedImage._TYPE);
        cameraLabeledView.setMessageToBitmapCallable(new BitmapFromCompressedImage());

        editText = (EditText) view.findViewById(R.id.camera_fragment_edit_text);

//        cameraLabeledView.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View var1, MotionEvent var2) {
//                if(MotionEventCompat.getActionMasked(var2) == MotionEvent.ACTION_DOWN)
//                    cameraView.setVisibility(View.GONE);
//                return true;
//            }
//        });

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

            controller.setLabelCameraMessageReceivedListener(new MessageListener<CompressedImage>() {
                @Override
                public void onNewMessage(CompressedImage compressedImage) {
                    if (compressedImage != null) {
                        controller.setLabelCameraMessageReceivedListener(null);
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                cameraView.setVisibility(View.GONE);
                            }
                        });
                    }
                }
            });

            controller.setSelectRequestReceivedListener(new MessageListener<UInt32>() {
                @Override
                public void onNewMessage(final UInt32 uInt32) {
                    if (uInt32 != null) {
                        controller.setSelectRequestReceivedListener(null);
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                maxValue = uInt32.getData();
                            }
                        });
                    }
                }
            });
        }

        ImageButton confirmButton = (ImageButton) view.findViewById(R.id.camera_fragment_image_button_confirm);
        confirmButton.setImageResource(R.drawable.check);
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onConfirm();
            }
        });

        ImageButton cancelButton = (ImageButton) view.findViewById(R.id.camera_fragment_image_button_cancel);
        cancelButton.setImageResource(R.drawable.cross);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCancel();
            }
        });

        Button button = (Button) view.findViewById(R.id.camera_fragment_clear_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClear();
            }
        });

        Button sendButton = (Button) view.findViewById(R.id.camera_fragment_send_target_button);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onSend();
            }
        });

        // switch between camera live view and search result view
        toggleButton = (ToggleButton) view.findViewById(R.id.camera_fragment_toggle_button);
        toggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cameraLabeledView.isShown()) {
                    cameraLabeledView.setVisibility(View.GONE);
                    cameraView.setVisibility(View.VISIBLE);
                }
                else {
                    cameraView.setVisibility(View.GONE);
                    cameraLabeledView.setVisibility(View.VISIBLE);
                }
            }
        });

        //initialize status and console display
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
                    if (Objects.equals(message.getData(), "CHOOSE")) {
                        s = getString(R.string.vision_choose);
                        if (!cameraLabeledView.isShown()) {
                            toggleButton.callOnClick(); // show labeled image(search result)
                        }
                    }
                    else {
                        s = message.getData();
                    }
                }
                return s;
            }
        });


        if (nodeConfiguration != null) {
            nodeMainExecutor.execute(cameraView, nodeConfiguration.setNodeName("android/fragment_camera_view"));
            nodeMainExecutor.execute(cameraLabeledView, nodeConfiguration.setNodeName("android/fragment_camera_label_view"));
            nodeMainExecutor.execute(statusView, nodeConfiguration.setNodeName("android/fragment_camera_status"));
            nodeMainExecutor.execute(consoleView, nodeConfiguration.setNodeName("android/fragment_camera_console"));
        }

        return view;
    }

    @Override
    void shutdown() {
        nodeMainExecutor.shutdownNodeMain(cameraView);
        nodeMainExecutor.shutdownNodeMain(cameraLabeledView);
    }


    /**
     * Confirm current search result by selecting the target's number
     */
    void onConfirm() {
        ParameterTree params = ((ControlApp) getActivity()).getRobotController().connectedNode.getParameterTree();
        if (params.has(getString(R.string.param_search_select_lock))){
            Boolean is_lock = params.getBoolean(getString(R.string.param_search_select_lock));
            if (is_lock){
                return;
            }
        }
        if (maxValue == 1) {
            params.set(getString(R.string.param_user_selected), maxValue);
            return;
        }
        if (Objects.equals(editText.getText().toString(), "")) {
            consoleView.setText(R.string.select_not_valid);
        }
        else {
            int selected_num = -1;
            try {
                selected_num = Integer.parseInt(editText.getText().toString());
            }
            catch (Exception ex) {
                consoleView.setText(R.string.select_not_valid);
            }
            if (maxValue >= selected_num && selected_num >= 0) {
                params.set(getString(R.string.param_user_selected), selected_num);
            }
            else
                consoleView.setText(R.string.select_not_valid);
        }
    }

    /**
     * Deny current search result and continue
     */
    void onCancel() {
        ParameterTree params = ((ControlApp) getActivity()).getRobotController().connectedNode.getParameterTree();
        if (params.has(getString(R.string.param_search_select_lock))){
            Boolean is_lock = params.getBoolean(getString(R.string.param_search_select_lock));
            if (!is_lock){
                params.set(getString(R.string.param_user_selected), 0);
            }
        }
    }

    void onSend() {
        ParameterTree params = ((ControlApp) getActivity()).getRobotController().connectedNode.getParameterTree();
        if (params.has(getString(R.string.param_track_send_lock))){
            Boolean is_lock = params.getBoolean(getString(R.string.param_track_send_lock));
            if (is_lock){
                consoleView.setText(R.string.send_not_valid);
                return;
            }
        }
        if (Objects.equals(editText.getText().toString(), "")) {
            consoleView.setText(R.string.need_set_target_label);
        }
        else {
            consoleView.setText(R.string.target_confirmed);
            params.set(getString(R.string.param_target_label), editText.getText().toString());
            params.set(getString(R.string.param_target_is_set), true);
        }
        editText.setText("");
    }

    /**
     * Clear current target
     */
    void onClear() {
        ParameterTree params = ((ControlApp) getActivity()).getRobotController().connectedNode.getParameterTree();
        params.set(getString(R.string.param_target_label), "");
        params.set(getString(R.string.param_target_is_set), false);
        params.set(getString(R.string.param_action_target_label), "");
        params.set(getString(R.string.param_action_target_is_set), false);
        params.set(getString(R.string.param_user_selected), 0);
        consoleView.setText(R.string.target_canceled);
        editText.setText("");
    }

    void publishROI() {
        ParameterTree params = ((ControlApp) getActivity()).getRobotController().connectedNode.getParameterTree();
        if (x_start < x_end && y_start < y_end) {
            if (cameraView.getWidth() < 2 * cameraView.getHeight()) {
                float x_s = x_start / cameraView.getWidth() * 640;
                float x_e = x_end / cameraView.getWidth() * 640;
                float y_s = (float)(y_start - (cameraView.getHeight() - 0.75 * cameraView.getWidth()) / 2 ) / cameraView.getWidth() * 640;
                float y_e = (float)(y_end - (cameraView.getHeight() - 0.75 * cameraView.getWidth()) / 2 ) / cameraView.getWidth() * 640;

                params.set(getString(R.string.param_target_label), "user selected object");
                params.set(getString(R.string.param_target_is_set), true);
                controller.publishROI(x_s, y_s, x_e, y_e);
            }
        }
    }
}