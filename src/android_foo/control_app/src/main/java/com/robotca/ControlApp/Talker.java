package com.robotca.ControlApp;

import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;

/**
 * Created by pedestrian-username on 17-8-23.
 */

public class Talker extends AbstractNodeMain {
    private String topic_name;
    private String message_type;
    private Publisher publisher;


    public Talker(String topic_name, String message_type) {
        this.topic_name = topic_name;
        this.message_type = message_type;
    }

    public GraphName getDefaultNodeName() {
        return GraphName.of("rosjava_tutorial_pubsub/Publisher");
    }

    public void onStart(ConnectedNode connectedNode) {
        publisher = connectedNode.newPublisher(this.topic_name, this.message_type);
    }

    public void publish(String Text){
        std_msgs.String str = (std_msgs.String) publisher.newMessage();
        str.setData(Text);
        publisher.publish(str);
    }
}
