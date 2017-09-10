package com.robotca.ControlApp;

import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Subscriber;

/**
 * Created by pedestrian-username on 17-8-23.
 */

public class Listener extends AbstractNodeMain{
    private String topic_name;
    private String message_type;
    private MessageListener messageListener;

    public Listener(String topic, String message_type, MessageListener messageListener) {
        this.topic_name = topic;
        this.message_type = message_type;
        this.messageListener = messageListener;
    }

    public void onStart(ConnectedNode connectedNode) {
        final Subscriber subscriber = connectedNode.newSubscriber(this.topic_name, this.message_type);
        subscriber.addMessageListener(this.messageListener);
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("android_remote_control");
    }
}
