package com.robotca.ControlApp.Views;

/**
 * Created by Zhipeng Dong on 16-10-31.
 */

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageButton;

import com.robotca.ControlApp.ControlApp;
import com.robotca.ControlApp.Core.ControlMode;
import com.robotca.ControlApp.R;

import org.ros.message.MessageListener;

public class VoiceView extends RelativeLayout implements AnimationListener,
        MessageListener<nav_msgs.Odometry>/*, NodeMain*/ {
    /**
     * TAG Debug Log tag.
     */
    private static final String TAG = "VoiceView";

    private static final int INVALID_POINTER_ID = -1;
    /**
     * mainLayout The parent layout that contains all the elements of the virtual
     * joystick.
     */
    private RelativeLayout mainLayout;

    private ImageButton imageButton = null;

    /**
     * parentSize The length (width==height ideally) of a side of the parent
     * container that holds the virtual joystick.
     */
    private float parentSize = Float.NaN;

    /**
     * pointerId Used to keep track of the contact that initiated the interaction
     * with the virtual joystick. All other contacts are ignored.
     */
    private int pointerId = INVALID_POINTER_ID;

    /**
     * Used for tilt sensor control.
     */
    private ControlMode controlMode = ControlMode.Joystick;


    public VoiceView(Context context) {
        super(context);

        init(context);
    }

    public VoiceView(Context context, AttributeSet attrs) {
        super(context, attrs);

        init(context);
    }

    public VoiceView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        init(context);
    }

    /*
     * Prevents redundancies in the three constructors
     */
    private void init(Context context) {
        initVirtualVoiceButton(context);
    }


    @Override
    public void onAnimationEnd(Animation animation) {
    }

    @Override
    public void onAnimationRepeat(Animation animation) {
    }

    @Override
    public void onAnimationStart(Animation animation) {
    }

    @Override
    public void onNewMessage(final nav_msgs.Odometry message) {
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        // Touch events indicate user wants to reset calibration in Tilt mode
        if (controlMode == ControlMode.Tilt) {
            return true;
        }

        final int action = event.getAction();

        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_MOVE: {
                // If the primary contact point is no longer on the screen then ignore
                // the event.
                if (pointerId != INVALID_POINTER_ID) {

                }
                break;
            }
            case MotionEvent.ACTION_DOWN: {
                // Get the coordinates of the pointer that is initiating the
                // interaction.
                pointerId = event.getPointerId(event.getActionIndex());
                onContactDown();
                break;
            }
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_UP: {
                // Check if the contact that initiated the interaction is up.
                //noinspection deprecation
                if ((action & MotionEvent.ACTION_POINTER_ID_MASK) >> MotionEvent.ACTION_POINTER_ID_SHIFT == pointerId) {
                    onContactUp();
                }
                break;
            }
        }
        return true;
    }


    /**
     * Initialize the fields with values that can only be determined once the
     * layout for the views has been determined.
     */
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        // Call the parent's onLayout to using the views.
        super.onLayout(changed, l, t, r, b);
    }


    /**
     * Sets up the visual elements of the virtual joystick.
     */
    private void initVirtualVoiceButton(Context context) {
        // All the virtual joystick elements must be centered on the parent.
        setGravity(Gravity.CENTER);

        LayoutInflater.from(context).inflate(R.layout.virtual_voice_button, this, true);
        mainLayout = (RelativeLayout) findViewById(R.id.fragment_voice_view);
    }

//    void ib_init(){
//        imageButton = new ImageButton(this);
//        RelativeLayout.LayoutParams lp=new RelativeLayout.LayoutParams(-2, -2);
//        lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
//        lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
//        imageButton.setLayoutParams(lp);
//
//        imageButton.setImageResource(this.getResources().getIdentifier("icon", "drawable", "jarvis"));
//
//        imageButton.setOnClickListener(new OnClickListener(){
//            public void onClick(View v) {
//            }
//        });
//    }

    /**
     * Update the virtual joystick to indicate a contact down has occurred.
     */
    private void onContactDown() {
        //TODO start record voice
    }

    /**
     * The divets and the ring are made transparent to reflect that the virtual
     * joystick is no longer active. The intensity circle is slowly scaled to 0.
     */
    private void onContactUp() {
        // TODO send voice

        // Reset the pointer id.
        pointerId = INVALID_POINTER_ID;
    }


    public void setControlMode(ControlMode controlMode) {

        this.controlMode = controlMode;
    }

    public void stop() {
    }
}
