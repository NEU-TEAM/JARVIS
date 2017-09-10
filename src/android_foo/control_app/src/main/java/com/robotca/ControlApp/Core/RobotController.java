package com.robotca.ControlApp.Core;

import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;

import com.robotca.ControlApp.ControlApp;
import com.robotca.ControlApp.Core.Plans.RobotPlan;
import com.robotca.ControlApp.R;

import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMain;
import org.ros.node.NodeMainExecutor;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;
import org.ros.node.parameter.ParameterTree;
import org.ros.time.TimeProvider;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.lang.Math;

import std_msgs.Int32MultiArray;
import std_msgs.Float32MultiArray;
import std_msgs.UInt32;
import geometry_msgs.Point;
import geometry_msgs.Pose;
import geometry_msgs.Quaternion;
import geometry_msgs.Twist;
import nav_msgs.Odometry;
import sensor_msgs.CompressedImage;
import sensor_msgs.LaserScan;
import sensor_msgs.NavSatFix;

/**
 * Manages receiving data from, and sending commands to, a connected Robot.
 *
 * Created by Michael Brunson on 2/13/16.
 */
public class RobotController implements NodeMain, Savable {

    // Logcat Tag
    private static final String TAG = "RobotController";

    // The parent Context
    private final ControlApp context;

    // Whether the RobotController has been initialized
    private boolean initialized;

    // Timer for periodically publishing velocity commands
    private Timer velocityPublisherTimer;

    // Timer for periodically publishing servo commands
    private Timer servoPublisherTimer;

    private Publisher<std_msgs.Float32MultiArray> roiPublisher;
    private Float32MultiArray currentRoi;

    // Indicates when a velocity command should be published
    private boolean publishVelocity;
    // Publisher for velocity commands
    private Publisher<Twist> movePublisher;
    // Contains the current velocity plan to be published
    private Twist currentVelocityCommand;

    // Indicates when a servo command should be published
    private boolean publishServo;
    // Publisher for velocity commands
    private Publisher<Int32MultiArray> servoPublisher;
    // Contains the current velocity plan to be published
    private Int32MultiArray currentServoCommand;

    // Class for publish voice
//    private AudioPublisher audioPublisher;
//    private boolean voice_mode_isCommand = true;

//    private AudioManager audioManager;
//    private AudioSubscriber audioSubscriber;
    // Is robot talk by phone speaker
//    private boolean speakerOn = false;

    // Subscriber to NavSatFix data
    private Subscriber<NavSatFix> navSatFixSubscriber;
    // The most recent NavSatFix
    private NavSatFix navSatFix;
    // Lock for synchronizing accessing and receiving the current NatSatFix
    private final Object navSatFixMutex = new Object();

    // Subscriber to LaserScan data
    private Subscriber<LaserScan> laserScanSubscriber;
    // The most recent LaserScan
    private LaserScan laserScan;
    // Lock for synchronizing accessing and receiving the current LaserScan
    private final Object laserScanMutex = new Object();

    // Subscriber to Odometry data
    private Subscriber<Odometry> odometrySubscriber;
    // The most recent Odometry
    private Odometry odometry;
    // Lock for synchronizing accessing and receiving the current Odometry
    private final Object odometryMutex = new Object();

    // Subscriber to Pose data
    private Subscriber<Pose> poseSubscriber;
    // The most recent Pose
    private Pose pose;
    // Lock for synchronizing accessing and receiving the current Pose
    private final Object poseMutex = new Object();

    private Subscriber<UInt32> selectRequestSubscriber;

    private Subscriber<CompressedImage> imageSubscriber;
    private CompressedImage image;

    private Subscriber<CompressedImage> labelImageSubscriber;
    private CompressedImage labelImage;

    private final Object imageMutex = new Object();
    private MessageListener<CompressedImage> imageMessageReceived;
    private MessageListener<CompressedImage> labelImageMessageReceived;

    private MessageListener<UInt32> selectRequestRecieved;

    // The currently running RobotPlan
    private RobotPlan motionPlan;
    // The currently paused RobotPlan
    private int pausedPlanId;

    // The node connected to the Robot on which data can be sent and received
    public ConnectedNode connectedNode;

    // Listener for LaserScans
    private final ArrayList<MessageListener<LaserScan>> laserScanListeners;
    // Listener for Odometry
    private ArrayList<MessageListener<Odometry>> odometryListeners;
    // Listener for NavSatFix
    private ArrayList<MessageListener<NavSatFix>> navSatListeners;

    /**
     * LocationProvider subscribers can register to to receive location updates.
     */
    public final LocationProvider LOCATION_PROVIDER;

    // The Robot's starting position
    private static Point startPos;
    // The Robot's last recorded position
    private static Point currentPos;
    // The Robot's last recorded orientation
    private static Quaternion rotation;
    // The Robot's last recorded speed
    private static double speed;
    // The Robot's last recorded turn rate
    private static double turnRate;

    // Bundle ID for pausedPlan
    private static final String PAUSED_PLAN_BUNDLE_ID = "com.robotca.ControlApp.Core.RobotController.pausedPlan";

    // Constant for no motion plan
    private static final int NO_PLAN = -1;

    /**
     * Creates a RobotController.
     * @param context The Context the RobotController belongs to.
     */
    public RobotController(ControlApp context) {
        this.context = context;

        this.initialized = false;

        this.laserScanListeners = new ArrayList<>();
        this.odometryListeners = new ArrayList<>();
        this.navSatListeners = new ArrayList<>();

        this.LOCATION_PROVIDER = new LocationProvider();
        this.addNavSatFixListener(this.LOCATION_PROVIDER);

        pausedPlanId = NO_PLAN;

        startPos = null;
        currentPos = null;
        rotation = null;
    }

    /**
     * Adds an Odometry listener.
     * @param l The listener
     * @return True on success
     */
    public boolean addOdometryListener(MessageListener<Odometry> l) {
        return odometryListeners.add(l);
    }

    /**
     * Adds a NavSatFix listener.
     * @param l The listener
     * @return True on success
     */
    public boolean addNavSatFixListener(MessageListener<NavSatFix> l) {
        return navSatListeners.add(l);
    }

    /**
     * Adds a LaserScan listener.
     * @param l The listener
     * @return True on success
     */
    public boolean addLaserScanListener(MessageListener<LaserScan> l) {
        synchronized (laserScanListeners) {
            return laserScanListeners.add(l);
        }
    }

    /**
     * Removes a LaserScan listener.
     * @param l The listener
     * @return True if the listener was removed
     */
    public boolean removeLaserScanListener(MessageListener<LaserScan> l) {

        synchronized (laserScanListeners) {
            return laserScanListeners.remove(l);
        }
    }

    /**
     * Initializes the RobotController.
     * @param nodeMainExecutor The NodeMainExecutor on which to execute the NodeConfiguration.
     * @param nodeConfiguration The NodeConfiguration to execute
     */
    public void initialize(NodeMainExecutor nodeMainExecutor, NodeConfiguration nodeConfiguration) {
        nodeMainExecutor.execute(this, nodeConfiguration.setNodeName("android/robot_controller"));
    }

    /**
     * Runs the specified RobotPlan on the Robot.
     * @param plan The RobotPlan
     */
    public void runPlan(RobotPlan plan) {
        stop(true);
        pausedPlanId = NO_PLAN;

        publishVelocity = true;

        motionPlan = plan;
        if (motionPlan != null)
            motionPlan.run(this);
    }

    /**
     * Attempts to resume a stopped RobotPlan.
     * @return True if a RobotPlan was resumed
     */
    public boolean resumePlan() {
        if (pausedPlanId != NO_PLAN) {
            Log.d(TAG, "Resuming paused plan");
            runPlan(ControlMode.getRobotPlan(context, ControlMode.values()[pausedPlanId]));
            return true;
        }

        return false;
    }

    /**
     * @return The current RobotPlan
     */
    public RobotPlan getMotionPlan() {
        return motionPlan;
    }

    /**
     * @return Whether there is a paused motion plan
     */
    public boolean hasPausedPlan() {
        return pausedPlanId != NO_PLAN;
    }

    /**
     * Stops the Robot's current motion and any RobotPlan that may be running.
     *
     * @return True if a resumable RobotPlan was cancelled
     */
    public boolean stop() {
        return stop(true);
    }

    /**
     * Stops the Robot's current motion and optionally any RobotPlan that may be running.
     *
     * @param cancelMotionPlan Whether to cancel the current motion plan
     *
     * @return True if a resumable RobotPlan was cancelled
     */
    public boolean stop(boolean cancelMotionPlan) {

        if (cancelMotionPlan || pausedPlanId == NO_PLAN) {
            pausedPlanId = NO_PLAN;

            if (motionPlan != null) {
                motionPlan.stop();

                if (motionPlan.isResumable()) {
                    pausedPlanId = motionPlan.getControlMode() == null ? NO_PLAN : motionPlan.getControlMode().ordinal();
                }

                motionPlan = null;
            }
        }

        publishVelocity = false;
        publishVelocity(0.0, 0.0, 0.0);

        publishServo = false;
        publishServo(0.0, 0.0);

        if (movePublisher != null){
            movePublisher.publish(currentVelocityCommand);
        }

        if (servoPublisher != null){
            servoPublisher.publish(currentServoCommand);
        }

        return pausedPlanId != NO_PLAN && cancelMotionPlan;
    }

    /**
     * Sets the next values of the next velocity to publish.
     * @param linearVelocityX Linear velocity in the x direction
     * @param linearVelocityY Linear velocity in the y direction
     * @param angularVelocityZ Angular velocity about the z axis
     */
    public void publishVelocity(double linearVelocityX, double linearVelocityY, double angularVelocityZ) {
        if (currentVelocityCommand != null) {

            float scale = 1.0f;

            try {
                // Safe Mode
                if (context.getWarningSystem().isSafemodeEnabled() && linearVelocityX >= 0.0) {
                    scale = (float) Math.pow(1.0f - context.getHUDFragment().getWarnAmount(), 2.0);
                }
            }
            catch(Exception e){
                scale = 0;
                Log.e("Emergency Stop", e.getMessage());
            }

            if(PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.prefs_invert_x_axis_key), false)){
                linearVelocityX *= -1;
            }

            if(PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.prefs_invert_y_axis_key), false)){
                linearVelocityY *= -1;
            }

            if(PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.prefs_invert_angular_velocity_key), false)){
                angularVelocityZ *= -1;
            }

            currentVelocityCommand.getLinear().setX(linearVelocityX * scale);
            currentVelocityCommand.getLinear().setY(-linearVelocityY * scale);
            currentVelocityCommand.getLinear().setZ(0.0);
            currentVelocityCommand.getAngular().setX(0.0);
            currentVelocityCommand.getAngular().setY(0.0);
            currentVelocityCommand.getAngular().setZ(-angularVelocityZ);
        } else {
            Log.w("Emergency Stop", "currentVelocityCommand is null");
        }
    }

    /**
     * Sets the next values of the next velocity to publish.
     * @param pitch servo pitch angle increasement
     * @param yaw servo yaw angle increasement
     */
    public void publishServo(double pitch, double yaw) {
        if (currentServoCommand != null) {
            int pi = (int)(pitch + 0.5 * Math.abs(pitch) / pitch);
            int yi = (int)(yaw + 0.5 * Math.abs(yaw) / yaw);
            int p;
            int y;
            if (pi == 0) {
                p = 0;
            }
            else {
                p = Math.abs(pi) / pi;
            }
            if (yi == 0) {
                y = 0;
            }
            else {
                y = Math.abs(yi) / yi;
            }
            int[] array;
            array = new int[2];
            array[0] = p;
            array[1] = y;
            currentServoCommand.setData(array);
        } else {
            Log.w("Emergency Stop", "currentServoCommand is null");
        }
    }

    /**
     * Same as above, but forces the velocity to be published.
     * @param linearVelocityX Linear velocity in the x direction
     * @param linearVelocityY Linear velocity in the y direction
     * @param angularVelocityZ Angular velocity about the z axis
     */
    public void forceVelocity(double linearVelocityX, double linearVelocityY,
                              double angularVelocityZ) {
        publishVelocity = true;
        publishVelocity(linearVelocityX, linearVelocityY, angularVelocityZ);
    }

    /**
     * Same as above, but forces the velocity to be published.
     * @param pitch Linear velocity in the x direction
     * @param yaw Linear velocity in the y direction
     */
    public void forceServo(double pitch, double yaw) {
        publishServo = true;
        publishServo(pitch, yaw);
    }

//    public void publishVoice(boolean isPub) {
//        if (isPub) {
//            audioPublisher.play();
//        }
//        else {
//            audioPublisher.pause();
//        }
//    }

//    /**
//     * Turn ON/OFF the speaker
//     */
//    public void changeSpeakerMode() {
//        speakerOn = !speakerOn;
//        if (speakerOn) {
//            audioSubscriber.play();
//        }
//        else {
//            audioSubscriber.pause();
//        }
//    }
//
//    public void changeVoiceMode() {
//        if (audioPublisher != null) {
//            audioPublisher.onShutdown();
//        }
//        String userCommandTopic = PreferenceManager.getDefaultSharedPreferences(context)
//                .getString(context.getString(R.string.prefs_user_command_topic_edittext_key),
//                        context.getString(R.string.user_command_topic));
//
//        String userTalkTopic = PreferenceManager.getDefaultSharedPreferences(context)
//                .getString(context.getString(R.string.prefs_user_talk_topic_edittext_key),
//                        context.getString(R.string.user_talk_topic));
//
//        voice_mode_isCommand = !voice_mode_isCommand;
//        if (voice_mode_isCommand)
//            audioPublisher = new AudioPublisher(userCommandTopic);
//        else
//            audioPublisher = new AudioPublisher(userTalkTopic);
//        audioPublisher.pause();
//        audioPublisher.onStart(this.connectedNode);
//    }

    /**
     * @return The default node name for the RobotController
     */
    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("android/robot_controller");
    }

    /**
     * Callback for when the RobotController is connected.
     * @param connectedNode The ConnectedNode the RobotController is connected through
     */
    @Override
    public void onStart(ConnectedNode connectedNode) {
        this.connectedNode = connectedNode;
        initialize();
    }

    /*
     * Initializes the RobotController.
     */
    public void initialize() {
        if (!initialized && connectedNode != null) {

            // Create audioManager
//            audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            // Start the topics
            refreshTopics();

            initialized = true;
        }
    }

    /**
     * Refreshes all topics, recreating them if there topic names have been changed.
     */
    public void refreshTopics() {

        // Get the correct topic names
        String moveTopic = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.prefs_joystick_topic_edittext_key),
                        context.getString(R.string.joy_topic));

        String servoTopic = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.prefs_servo_topic_edittext_key),
                        context.getString(R.string.servo_topic));

        String userCommandTopic = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.prefs_user_command_topic_edittext_key),
                        context.getString(R.string.user_command_topic));

        String userTalkTopic = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.prefs_user_talk_topic_edittext_key),
                        context.getString(R.string.user_talk_topic));

        String voiceOutputTopic = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.prefs_voice_output_topic_edittext_key),
                        context.getString(R.string.voice_output_topic));

        String navSatTopic = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.prefs_navsat_topic_edittext_key),
                        context.getString(R.string.navsat_topic));

        String laserScanTopic = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.prefs_laserscan_topic_edittext_key),
                        context.getString(R.string.laser_scan_topic));

        String odometryTopic = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.prefs_odometry_topic_edittext_key),
                        context.getString(R.string.odometry_topic));

        String poseTopic = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.prefs_pose_topic_edittext_key),
                        context.getString(R.string.pose_topic));

        String selectRequestTopic = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.prefs_select_request_topic_edittext_key),
                        context.getString(R.string.select_request_topic));

        String imageTopic = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.prefs_camera_topic_edittext_key),
                        context.getString(R.string.camera_topic));

        String labelImageTopic = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.prefs_label_image_topic_edittext_key),
                        context.getString(R.string.camera_labeled_topic));

        String roiTopic = PreferenceManager.getDefaultSharedPreferences(context).getString(
                context.getString(R.string.prefs_roi_topic_edittext_key),
                context.getString(R.string.roi_topic));

        //Refresh the roi publisher
        if (roiPublisher == null || !roiTopic.equals(roiPublisher.getTopicName().toString())) {
            if (roiPublisher != null) {
                roiPublisher.shutdown();
            }
            roiPublisher = connectedNode.newPublisher(roiTopic, Float32MultiArray._TYPE);
            currentRoi = roiPublisher.newMessage();
        }

        // Refresh the Servo Publisher
        if (servoPublisher == null
                || !servoTopic.equals(servoPublisher.getTopicName().toString())) {

            if (servoPublisherTimer != null) {
                servoPublisherTimer.cancel();
            }

            if (servoPublisher != null) {
                servoPublisher.shutdown();
            }

            // Start the servo publisher
            servoPublisher = connectedNode.newPublisher(servoTopic, Int32MultiArray._TYPE);
            currentServoCommand = servoPublisher.newMessage();

            servoPublisherTimer = new Timer();
            servoPublisherTimer.schedule(new TimerTask() {
                @Override
                public void run() { if (publishServo) {
                    servoPublisher.publish(currentServoCommand);
                }
                }
            }, 0, 50);
            publishServo = false;
        }

        // Refresh the Move Publisher
        if (movePublisher == null
                || !moveTopic.equals(movePublisher.getTopicName().toString())) {

            if (velocityPublisherTimer != null) {
                velocityPublisherTimer.cancel();
            }

            if (movePublisher != null) {
                movePublisher.shutdown();
            }

            // Start the move publisher
            movePublisher = connectedNode.newPublisher(moveTopic, Twist._TYPE);
            currentVelocityCommand = movePublisher.newMessage();

            velocityPublisherTimer = new Timer();
            velocityPublisherTimer.schedule(new TimerTask() {
                @Override
                public void run() { if (publishVelocity) {
                    movePublisher.publish(currentVelocityCommand);
                }
                }
            }, 0, 50);
            publishVelocity = false;
        }

        // Refresh the NavSat Subscriber
        if (navSatFixSubscriber == null
                || !navSatTopic.equals(navSatFixSubscriber.getTopicName().toString())) {

            if (navSatFixSubscriber != null)
                navSatFixSubscriber.shutdown();

            // Start the NavSatFix subscriber
            navSatFixSubscriber = connectedNode.newSubscriber(navSatTopic, NavSatFix._TYPE);
            navSatFixSubscriber.addMessageListener(new MessageListener<NavSatFix>() {
                @Override
                public void onNewMessage(NavSatFix navSatFix) {
                    setNavSatFix(navSatFix);
                }
            });
        }

        // Refresh the LaserScan Subscriber
        if (laserScanSubscriber == null
                || !laserScanTopic.equals(laserScanSubscriber.getTopicName().toString())) {

            if (laserScanSubscriber != null)
                laserScanSubscriber.shutdown();

            // Start the LaserScan subscriber
            laserScanSubscriber = connectedNode.newSubscriber(laserScanTopic, LaserScan._TYPE);
            laserScanSubscriber.addMessageListener(new MessageListener<LaserScan>() {
                @Override
                public void onNewMessage(LaserScan laserScan) {
                    setLaserScan(laserScan);
                }
            });
        }

        // Refresh the Odometry Subscriber
        if (odometrySubscriber == null
                || !odometryTopic.equals(odometrySubscriber.getTopicName().toString())) {

            if (odometrySubscriber != null)
                odometrySubscriber.shutdown();

            // Start the Odometry subscriber
            odometrySubscriber = connectedNode.newSubscriber(odometryTopic, Odometry._TYPE);
            odometrySubscriber.addMessageListener(new MessageListener<Odometry>() {
                @Override
                public void onNewMessage(Odometry odometry) {
                    setOdometry(odometry);
                }
            });
        }

        // Refresh the Pose Subscriber
        if (poseSubscriber == null
                || !poseTopic.equals(poseSubscriber.getTopicName().toString())) {

            if (poseSubscriber != null)
                poseSubscriber.shutdown();

            // Start the Pose subscriber
            poseSubscriber = connectedNode.newSubscriber(poseTopic, Pose._TYPE);
            poseSubscriber.addMessageListener(new MessageListener<Pose>() {
                @Override
                public void onNewMessage(Pose pose) {
                    setPose(pose);
                }
            });
        }

        if(selectRequestSubscriber == null || !selectRequestTopic.equals(selectRequestSubscriber.getTopicName().toString())){
            if(selectRequestSubscriber != null)
                selectRequestSubscriber.shutdown();

            selectRequestSubscriber = connectedNode.newSubscriber(selectRequestTopic, UInt32._TYPE);
            selectRequestSubscriber.addMessageListener(new MessageListener<UInt32>() {
                @Override
                public void onNewMessage(UInt32 uInt32) {
                    if (selectRequestRecieved != null) {
                        selectRequestRecieved.onNewMessage(uInt32);
                    }
                }
            });
        }

        if(imageSubscriber == null || !imageTopic.equals(imageSubscriber.getTopicName().toString())){
            if(imageSubscriber != null)
                imageSubscriber.shutdown();

            imageSubscriber = connectedNode.newSubscriber(imageTopic, CompressedImage._TYPE);

            imageSubscriber.addMessageListener(new MessageListener<CompressedImage>() {
                @Override
                public void onNewMessage(CompressedImage image) {
                    setImage(image);
                    synchronized (imageMutex) {
                        if (imageMessageReceived != null) {
                            imageMessageReceived.onNewMessage(image);
                        }
                    }
                }
            });
        }

        if(labelImageSubscriber == null || !labelImageTopic.equals(labelImageSubscriber.getTopicName().toString())){
            if(labelImageSubscriber != null)
                labelImageSubscriber.shutdown();

            labelImageSubscriber = connectedNode.newSubscriber(labelImageTopic, CompressedImage._TYPE);

            labelImageSubscriber.addMessageListener(new MessageListener<CompressedImage>() {
                @Override
                public void onNewMessage(CompressedImage image) {
                    synchronized (imageMutex) {
                        if (labelImageMessageReceived != null) {
                            labelImageMessageReceived.onNewMessage(image);
                        }
                    }
                }
            });
        }
    }

    /**
     * Shuts down all topics.
     */
    public void shutdownTopics() {
        if(velocityPublisherTimer != null) {
            velocityPublisherTimer.cancel();
        }

        if(servoPublisherTimer != null) {
            servoPublisherTimer.cancel();
        }

        if (movePublisher != null) {
            movePublisher.shutdown();
        }

//        if (audioPublisher != null) {
//            audioPublisher.onShutdown();
//        }
//
//        if (audioSubscriber != null) {
//            audioSubscriber.onShutdown();
//        }

        if (navSatFixSubscriber != null) {
            navSatFixSubscriber.shutdown();
        }

        if (laserScanSubscriber != null) {
            laserScanSubscriber.shutdown();
        }

        if(odometrySubscriber != null){
            odometrySubscriber.shutdown();
        }

        if(poseSubscriber != null){
            poseSubscriber.shutdown();
        }
    }

    /**
     * Callback for when the RobotController is shutdown.
     * @param node The Node
     */
    @Override
    public void onShutdown(Node node) {
        shutdownTopics();
    }

    /**
     * Callback for when the shutdown is complete.
     * @param node The Node
     */
    @Override
    public void onShutdownComplete(Node node) {
        this.connectedNode = null;
    }

    /**
     * Callback indicating an error has occurred.
     * @param node The Node
     * @param throwable The error
     */
    @Override
    public void onError(Node node, Throwable throwable) {
        Log.e(TAG, "", throwable);
    }

    /**
     * @return The most recently received LaserScan
     */
    public LaserScan getLaserScan() {
        synchronized (laserScanMutex) {
            return laserScan;
        }
    }

    /**
     * Sets the current LaserScan.
     * @param laserScan The LaserScan
     */
    protected void setLaserScan(LaserScan laserScan) {
        synchronized (laserScanMutex) {
            this.laserScan = laserScan;

            boolean invertLaserScan = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.prefs_reverse_angle_reading_key), false);

            if(invertLaserScan) {
                float[] ranges = this.laserScan.getRanges();

                for (int i = 0; i < this.laserScan.getRanges().length / 2; i++) {
                    float range = ranges[i];
                    ranges[i] = ranges[ranges.length - i - 1];
                    ranges[ranges.length - i - 1] = range;
                }
            }
        }

        // Call the listener callbacks
        synchronized (laserScanListeners) {
            for (MessageListener<LaserScan> listener : laserScanListeners) {
                listener.onNewMessage(laserScan);
            }
        }
    }

    /**
     * @return The most recently received NavSatFix
     */
    @SuppressWarnings("unused")
    public NavSatFix getNavSatFix() {
        synchronized (navSatFixMutex) {
            return navSatFix;
        }
    }

    /**
     * Sets the current NavSatFix.
     * @param navSatFix The NavSatFix
     */
    protected void setNavSatFix(NavSatFix navSatFix) {
        synchronized (navSatFixMutex) {
            this.navSatFix = navSatFix;

            // Call the listener callbacks
            for (MessageListener<NavSatFix> listener: navSatListeners) {
                listener.onNewMessage(navSatFix);
            }
        }
    }

    /**
     * @return The most recently received Odometry.
     */
    @SuppressWarnings("unused")
    public Odometry getOdometry() {
        synchronized (odometryMutex) {
            return odometry;
        }
    }

    /**
     * Sets the current Odometry.
     * @param odometry The Odometry
     */
    protected void setOdometry(Odometry odometry) {
        synchronized (odometryMutex) {
            this.odometry = odometry;

            // Call the listener callbacks
            for (MessageListener<Odometry> listener: odometryListeners) {
                listener.onNewMessage(odometry);
            }

            // Record position TODO this should be moved to setPose() but that's not being called for some reason
            if (startPos == null) {
                startPos = odometry.getPose().getPose().getPosition();
            } else {
                currentPos = odometry.getPose().getPose().getPosition();
            }
            rotation = odometry.getPose().getPose().getOrientation();

            // Record speed and turnrate
            speed = odometry.getTwist().getTwist().getLinear().getX();
            turnRate = odometry.getTwist().getTwist().getAngular().getZ();
        }
    }

    /**
     * @return The most recently received Pose.
     */
    @SuppressWarnings("unused")
    public Pose getPose() {
        synchronized (poseMutex) {
            return pose;
        }
    }

    /**
     * Sets the current Pose.
     * @param pose The Pose
     */
    public void setPose(Pose pose){
        synchronized (poseMutex){
            this.pose = pose;
        }

        Log.d("RobotController", "Pose Set");
//        // Record position
//        if (startPos == null) {
//            startPos = pose.getPosition();
//        } else {
//            currentPos = pose.getPosition();
//        }
//        rotation = pose.getOrientation();
    }

    /**
     * Load from a Bundle.
     *
     * @param bundle The Bundle
     */
    @Override
    public void load(@NonNull Bundle bundle) {
        pausedPlanId = bundle.getInt(PAUSED_PLAN_BUNDLE_ID, NO_PLAN);
    }

    /**
     * Save to a Bundle.
     *
     * @param bundle The Bundle
     */
    @Override
    public void save(@NonNull Bundle bundle) {
        bundle.putInt(PAUSED_PLAN_BUNDLE_ID, pausedPlanId);
    }

    /**
     * @return The Robot's last reported x position
     */
    public static double getX() {
        if (currentPos == null)
            return 0.0;
        else
            return currentPos.getX() - startPos.getX();
    }

    /**
     * @return The Robot's last reported y position
     */
    public static double getY() {
        if (currentPos == null)
            return 0.0;
        else
            return currentPos.getY() - startPos.getY();
    }

    /**
     * @return The Robot's last reported heading in radians
     */
    public static double getHeading() {
        if (rotation == null)
            return 0.0;
        else
            return Utils.getHeading(org.ros.rosjava_geometry.Quaternion.fromQuaternionMessage(rotation));
    }

    /**
     * @return The Robot's last reported speed in the range [-1, 1].
     */
    public static double getSpeed() {
        return speed;
    }

    /**
     * @return The Robot's last reported turn rate in the range[-1, 1].
     */
    public static double getTurnRate() {
        return turnRate;
    }


    public void setCameraMessageReceivedListener(MessageListener<CompressedImage> cameraMessageReceived) {
        this.imageMessageReceived = cameraMessageReceived;
    }

    public void setLabelCameraMessageReceivedListener(MessageListener<CompressedImage> labelCameraMessageReceived) {
        this.labelImageMessageReceived = labelCameraMessageReceived;
    }

    public void setSelectRequestReceivedListener(MessageListener<UInt32> selectRequestRecieved) {
        this.selectRequestRecieved = selectRequestRecieved;
    }

    public void setImage(CompressedImage image) {
        synchronized (imageMutex) {
            this.image = image;
        }
    }

    public CompressedImage getImage(){
        synchronized (imageMutex) {
            return this.image;
        }
    }

    public void publishROI(float x_s, float y_s, float x_e, float y_e) {
        float[] array;
        array = new float[4];
        array[0] = x_s;
        array[1] = y_s;
        array[2] = x_e;
        array[3] = y_e;
        currentRoi.setData(array);
        roiPublisher.publish(currentRoi);
    }
}