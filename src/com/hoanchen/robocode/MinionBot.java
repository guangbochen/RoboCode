package com.hoanchen.robocode;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.util.Enumeration;
import java.util.Hashtable;

import robocode.HitByBulletEvent;
import robocode.HitWallEvent;
import robocode.RobotDeathEvent;
import robocode.ScannedRobotEvent;
import robocode.TeamRobot;
import robocode.util.Utils;

public class MinionBot extends TeamRobot {

	//These are constants. One advantage of these is that the logic in them (such as 20-3*BULLET_POWER)
	//does not use codespace, making them cheaper than putting the logic in the actual code.
 
	final static double BULLET_POWER=3;//Our bulletpower.
	final static double BULLET_DAMAGE=BULLET_POWER*4;//Formula for bullet damage.
	final static double BULLET_SPEED=20-3*BULLET_POWER;//Formula for bullet speed.
	
	/**
	 * run: SnippetBot's default behavior
	 */
	Hashtable targets;				//all enemies are stored in the hashtable
	Enemy target;					//our current enemy
	final double PI = Math.PI;		//just a constant
	int direction = 1;				//direction we are heading... 1 = forward, -1 = backwards
	double firePower;				//the power of the shot we will be using
	double midpointstrength = 0;	//The strength of the gravity point in the middle of the field
	int midpointcount = 0;			//Number of turns since that strength was changed.
	
	//Variables
	static double dir=1;
	static double oldEnemyHeading;
	//static double enemyEnergy;
	
	public void run() {
		
		targets = new Hashtable();
		target = new Enemy();
		target.distance = 100000;						//initialise the distance so that we can select a target
		
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
		
	    double distance = e.getDistance();
		if(distance>400)
		{
			
			Enemy en;
			if (targets.containsKey(e.getName())) {
				en = (Enemy)targets.get(e.getName());
			} else {
				en = new Enemy();
				targets.put(e.getName(),en);
			}
			//the next line gets the absolute bearing to the point where the bot is
			double absbearing_rad = (getHeadingRadians()+e.getBearingRadians())%(2*PI);
			//this section sets all the information about our target
			en.name = e.getName();
			double h = normaliseBearing(e.getHeadingRadians() - en.heading);
			h = h/(getTime() - en.ctime);
			en.changehead = h;
			en.x = getX()+Math.sin(absbearing_rad)*e.getDistance(); //works out the x coordinate of where the target is
			en.y = getY()+Math.cos(absbearing_rad)*e.getDistance(); //works out the y coordinate of where the target is
			en.bearing = e.getBearingRadians();
			en.heading = e.getHeadingRadians();
			en.ctime = getTime();				//game time at which this scan was produced
			en.speed = e.getVelocity();
			en.distance = e.getDistance();	
			en.live = true;
			if ((en.distance < target.distance)||(target.live == false)) {
				target = en;
			}
			
			antiGravMove();					//Move the bot
			doFirePower();					//select the fire power to use
			doScanner();					//Oscillate the scanner over the bot
			doGun();
			fire(firePower);
			execute();						//execute all commands
		}
		else
		{
			CicularTargeting(e);
		}
	    
		
	}
	
	public void CicularTargeting(ScannedRobotEvent e)
	{
		
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
    
    
    /**
     * methods for anti-gravity movement
     */
    
	void doFirePower() {
		firePower = 450/target.distance;//selects a bullet power based on our distance away from the target
		System.out.println(target.distance);
		if (firePower > 3) {
			firePower = 3;
		}
		//firePower = 0;
	}
	
	void antiGravMove() {
   		double xforce = 0;
	    double yforce = 0;
	    double force;
	    double ang;
	    GravPoint p;
		Enemy en;
    	Enumeration e = targets.elements();
	    //cycle through all the enemies.  If they are alive, they are repulsive.  Calculate the force on us
		while (e.hasMoreElements()) {
    	    en = (Enemy)e.nextElement();
			if (en.live) {
				p = new GravPoint(en.x,en.y, -1000);
		        force = p.power/Math.pow(getRange(getX(),getY(),p.x,p.y),2);
		        //Find the bearing from the point to us
		        ang = normaliseBearing(Math.PI/2 - Math.atan2(getY() - p.y, getX() - p.x)); 
		        //Add the components of this force to the total force in their respective directions
		        xforce += Math.sin(ang) * force;
		        yforce += Math.cos(ang) * force;
			}
	    }
	    
		/**The next section adds a middle point with a random (positive or negative) strength.
		The strength changes every 5 turns, and goes between -1000 and 1000.  This gives a better
		overall movement.**/
		midpointcount++;
		if (midpointcount > 5) {
			midpointcount = 0;
			midpointstrength = (Math.random() * 2000) - 1000;
		}
		p = new GravPoint(getBattleFieldWidth()/2, getBattleFieldHeight()/2, midpointstrength);
		force = p.power/Math.pow(getRange(getX(),getY(),p.x,p.y),1.5);
	    ang = normaliseBearing(Math.PI/2 - Math.atan2(getY() - p.y, getX() - p.x)); 
	    xforce += Math.sin(ang) * force;
	    yforce += Math.cos(ang) * force;
	   
	    /**The following four lines add wall avoidance.  They will only affect us if the bot is close 
	    to the walls due to the force from the walls decreasing at a power 3.**/
	    xforce += 5000/Math.pow(getRange(getX(), getY(), getBattleFieldWidth(), getY()), 3);
	    xforce -= 5000/Math.pow(getRange(getX(), getY(), 0, getY()), 3);
	    yforce += 5000/Math.pow(getRange(getX(), getY(), getX(), getBattleFieldHeight()), 3);
	    yforce -= 5000/Math.pow(getRange(getX(), getY(), getX(), 0), 3);
	    
	    //Move in the direction of our resolved force.
	    goTo(getX()-xforce,getY()-yforce);
	}
	
	/**Move towards an x and y coordinate**/
	void goTo(double x, double y) {
	    double dist = 20; 
	    double angle = Math.toDegrees(absbearing(getX(),getY(),x,y));
	    double r = turnTo(angle);
	    setAhead(dist * r);
	}


	/**Turns the shortest angle possible to come to a heading, then returns the direction the
	the bot needs to move in.**/
	int turnTo(double angle) {
	    double ang;
    	int dir;
	    ang = normaliseBearing(getHeading() - angle);
	    if (ang > 90) {
	        ang -= 180;
	        dir = -1;
	    }
	    else if (ang < -90) {
	        ang += 180;
	        dir = -1;
	    }
	    else {
	        dir = 1;
	    }
	    setTurnLeft(ang);
	    return dir;
	}

	/**keep the scanner turning**/
	void doScanner() {
		setTurnRadarLeftRadians(2*PI);
//		turnRadarLeftRadians(Double.POSITIVE_INFINITY);
	}
	
	/**Move the gun to the predicted next bearing of the enemy**/
	void doGun() {
		long time = getTime() + (int)Math.round((getRange(getX(),getY(),target.x,target.y)/(20-(3*firePower))));
		Point2D.Double p = target.guessPosition(time);
		
		//offsets the gun by the angle to the next shot based on linear targeting provided by the enemy class
		double gunOffset = getGunHeadingRadians() - (Math.PI/2 - Math.atan2(p.y - getY(), p.x - getX()));
		setTurnGunLeftRadians(normaliseBearing(gunOffset));
	}
	
	
	//if a bearing is not within the -pi to pi range, alters it to provide the shortest angle
	double normaliseBearing(double ang) {
		if (ang > PI)
			ang -= 2*PI;
		if (ang < -PI)
			ang += 2*PI;
		return ang;
	}
	
	//if a heading is not within the 0 to 2pi range, alters it to provide the shortest angle
	double normaliseHeading(double ang) {
		if (ang > 2*PI)
			ang -= 2*PI;
		if (ang < 0)
			ang += 2*PI;
		return ang;
	}
	
	//returns the distance between two x,y coordinates
	public double getRange( double x1,double y1, double x2,double y2 )
	{
		double xo = x2-x1;
		double yo = y2-y1;
		double h = Math.sqrt( xo*xo + yo*yo );
		return h;	
	}
	
	//gets the absolute bearing between to x,y coordinates
	public double absbearing( double x1,double y1, double x2,double y2 )
	{
		double xo = x2-x1;
		double yo = y2-y1;
		double h = getRange( x1,y1, x2,y2 );
		if( xo > 0 && yo > 0 )
		{
			return Math.asin( xo / h );
		}
		if( xo > 0 && yo < 0 )
		{
			return Math.PI - Math.asin( xo / h );
		}
		if( xo < 0 && yo < 0 )
		{
			return Math.PI + Math.asin( -xo / h );
		}
		if( xo < 0 && yo > 0 )
		{
			return 2.0*Math.PI - Math.asin( -xo / h );
		}
		return 0;
	}
     
	public void onRobotDeath(RobotDeathEvent e) {
		Enemy en = (Enemy)targets.get(e.getName());
		en.live = false;		
	}	
}


class Enemy {
	/*
	 * ok, we should really be using accessors and mutators here,
	 * (i.e getName() and setName()) but life's too short.
	 */
	String name;
	public double bearing,heading,speed,x,y,distance,changehead;
	public long ctime; 		//game time that the scan was produced
	public boolean live; 	//is the enemy alive?
	public Point2D.Double guessPosition(long when) {
		double diff = when - ctime;
		double newY = y + Math.cos(heading) * speed * diff;
		double newX = x + Math.sin(heading) * speed * diff;
		
		return new Point2D.Double(newX, newY);
	}
}

	/**Holds the x, y, and strength info of a gravity point**/
class GravPoint {
    public double x,y,power;
    public GravPoint(double pX,double pY,double pPower) {
        x = pX;
        y = pY;
        power = pPower;
    }
}