package com.hoanchen.robocode;

import java.awt.Color;

import robocode.Robot;
import robocode.ScannedRobotEvent;

public class GuangboRobot extends Robot {

	public void run() {
		//set my robot colors
		setBodyColor(Color.yellow);
		setGunColor(Color.black);
		setRadarColor(Color.blue);
		setBulletColor(Color.yellow);
		setScanColor(Color.green);
		
        while (true) {
            ahead(100);
            turnGunRight(360);
            back(100);
            turnGunRight(360);
            turnRight(10.0);
        }
    }
 
    public void onScannedRobot(ScannedRobotEvent e) {
        fire(1);
    }
    
    
    //this method detects whether the robot hits the wall or hits another robot
    private void hitsWallOrRobot()
    {
    	
    }
}
