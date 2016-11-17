#This software is modified from RobotCA
##Original Readme

University of South Carolina  
CSCE 490 & 492 Capstone Design  

Our project is to design an Android application to control a robot. Specifically, the goal is to create a native Android tablet application to interface with a Robot Operating System (ROS) robot, Clearpath Robotics' HUSKY model, for direct drive, diagnostic, and navigation information. The application will have to process data from various sensors on the robot and display the information in a user understandable fashion. For example, the robot will have a laser ranging system which will be collecting data on the environment. The application must parse this data and display the environment that the robot "sees".

Dependencies:  
+ ROS
+ ROSJava
+ Android 4.0+ (API level 13+)

![Master Chooser](https://cloud.githubusercontent.com/assets/8508489/14839465/021d5f80-0bf9-11e6-9580-10fa54de7cfc.png) 

![Main Layout](https://cloud.githubusercontent.com/assets/8508489/14839460/0201419c-0bf9-11e6-82c9-8e51ce85d48c.png)  

Client Information:  
Dr. Ioannis Rekleitis  
Assistant Professor  
University of South Carolina

#Modify Note
This software have been modified to control NEU household robot.
A servo control function was added to help control 2DOF servo driven platform.
Auido input function will be added to let user control the robot by voice command.
If no camera image is recieved, make sure ROS_HOSTNAME is not set.

2016/11/16
Now in 'Camera' mode user can send target for object recognition and tracking by input the label of the target. One can also tracking the target by drawing rectangle upon the image.

Author:
Zhipeng Dong
Ph.D student
https://github.com/DrawZeroPoint