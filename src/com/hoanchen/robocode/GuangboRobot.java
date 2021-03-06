package com.hoanchen.robocode;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;

import robocode.HitByBulletEvent;
import robocode.HitWallEvent;
import robocode.ScannedRobotEvent;
import robocode.TeamRobot;
import robocode.util.Utils;

public class GuangboRobot extends TeamRobot {

	//These are constants. One advantage of these is that the logic in them (such as 20-3*BULLET_POWER)
	//does not use codespace, making them cheaper than putting the logic in the actual code.
 
	final static double BULLET_POWER=3;//Our bulletpower.
	final static double BULLET_DAMAGE=BULLET_POWER*4;//Formula for bullet damage.
	final static double BULLET_SPEED=20-3*BULLET_POWER;//Formula for bullet speed.
	
	//Variables
	static double dir=1;
	static double oldEnemyHeading;
	//static double enemyEnergy;
	
	public void run() {
		
		//set my robot colors
		setColors();
		
		//Sets the gun and radar to turn independent from the robot's turn.
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);
		
        while (true) {
			turnRadarRightRadians(Double.POSITIVE_INFINITY);
        }
    }
	/**
	 * onScannedRobot: What to do when you see another robot
	 */
	public void onScannedRobot(ScannedRobotEvent e) {
		
		//if is team mate hold the fire
		if (isTeammate(e.getName())) {
			return;
		}
		
		//movement method
		setTurnRight(90+e.getBearing()-(25*dir));
		if (Math.random() < 0.05) {
			dir=-dir;
		}
		setAhead((e.getDistance()/1.75)*dir);
		
		
		
		Graphics2D g=getGraphics();

		double absBearing=e.getBearingRadians()+getHeadingRadians();
 
 
		/*This method of targeting is know as circular targeting; you assume your enemy will
		 *keep moving with the same speed and turn rate that he is using at fire time.The 
		 *base code comes from the wiki.
		*/
		
		//Finding the heading and heading change.
		double enemyHeading = e.getHeadingRadians();
		double enemyHeadingChange = enemyHeading - oldEnemyHeading;
		oldEnemyHeading = enemyHeading;
 
		//predict enemy's movement
		double deltaTime = 0;
		double predictedX = getX()+e.getDistance()*Math.sin(absBearing);
		double predictedY = getY()+e.getDistance()*Math.cos(absBearing);
		while((++deltaTime) * BULLET_SPEED <  Point2D.Double.distance(getX(), getY(), predictedX, predictedY)){	
 
			//Add the movement we think our enemy will make to our enemy's current X and Y
			predictedX += Math.sin(enemyHeading) * e.getVelocity();
			predictedY += Math.cos(enemyHeading) * e.getVelocity();
 
 
			//Find our enemy's heading changes.
			enemyHeading += enemyHeadingChange;
			
			g.setColor(Color.red);
			g.drawRect((int)predictedX-2,(int)predictedY-2,4,4);
 
			//If our predicted coordinates are outside the walls, put them 18 distance units away from the walls as we know 
			//that that is the closest they can get to the wall
			predictedX=Math.max(Math.min(predictedX,getBattleFieldWidth()-18),18);
			predictedY=Math.max(Math.min(predictedY,getBattleFieldHeight()-18),18);
 
		}
		//Find the bearing of our predicted coordinates from us.
		double aim = Utils.normalAbsoluteAngle(Math.atan2(  predictedX - getX(), predictedY - getY()));
 
		//Aim and fire.
		setTurnGunRightRadians(Utils.normalRelativeAngle(aim - getGunHeadingRadians()));
		setFire(BULLET_POWER);
 
 
 
 
 
 
		setTurnRadarRightRadians(Utils.normalRelativeAngle(absBearing-getRadarHeadingRadians())*2);
		
//	    double distance = e.getDistance();
//
//		if(distance<200)
//		{
//		   fire(3.5);
//		}
//		else if(distance<500)
//		{
//		   fire(2.5);
//		}
//		else if(distance<800)
//		{
//		   fire(1.5);
//		}
//		else
//		{
//		   fire(0.5);
//		}
	}
 
    
    public void onHitByBullet(HitByBulletEvent e) {
		//enemyEnergy-=BULLET_DAMAGE;
	}
    
    //this method detects whether the robot hits the wall or hits another robot
    public void onHitWall(HitWallEvent e)
    {
    	//if hits the wall turn the direction opponent
		dir=-dir;
    }
    
    
    private void setColors() {
		setBodyColor(Color.yellow);
		setGunColor(Color.black);
		setRadarColor(Color.blue);
		setBulletColor(Color.yellow);
		setScanColor(Color.green);
    }
}
