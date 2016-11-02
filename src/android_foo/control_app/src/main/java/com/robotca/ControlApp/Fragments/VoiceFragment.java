package com.robotca.ControlApp.Fragments;

/**
 * Created by Zhipeng Dong on 16-10-31.
 */

import android.app.Fragment;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.robotca.ControlApp.Core.ControlMode;
import com.robotca.ControlApp.R;
import com.robotca.ControlApp.Views.VoiceView;

public class VoiceFragment extends Fragment {
    private VoiceView virtualVoiceView;
    private View view;
    private ControlMode controlMode = ControlMode.Joystick;

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

        // Inflate the view if needed
        if (view == null) {
            view = inflater.inflate(R.layout.fragment_voice_view, container, true);

            // Grab the VoiceView and set its topic
            virtualVoiceView = (VoiceView) view.findViewById(R.id.voice_view);

            (view.findViewById(R.id.voice_button)).setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            virtualVoiceView.onContactChanged();
                        }
                    }
            );
        }

        return view;
    }


    /**
     * Returns the virtualVoiceView
     * @return The virtualVoiceView
     */
    public VoiceView getJoystickView() {
        return virtualVoiceView;
    }

    /**
     * Get the currently active ControlMode.
     * @return The current ControlMode
     */
    public ControlMode getControlMode() {
        return controlMode;
    }

    /**
     * Set the ControlMode for controlling the Joystick.
     * @param controlMode The new ControlMode
     */
    public void setControlMode(ControlMode controlMode) {
        this.controlMode = controlMode;
        this.invalidate();
    }

    /**
     * Invalidate the Fragment, updating the visibility of the Voice Button based on the ControlMode.
     *
     */
    public void invalidate() {

        switch (controlMode) {
            case Joystick:
                show();
                break;

            case Tilt:
                show();
                break;

            default:
                hide();
                break;
        }

        virtualVoiceView.setControlMode(controlMode);
    }

    /**
     * Stops the VoiceFragment.
     */
    public void stop() {
        virtualVoiceView.stop();
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
