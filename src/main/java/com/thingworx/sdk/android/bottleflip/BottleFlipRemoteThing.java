/* bcwti
*
* Copyright (c) 2016 Parametric Technology Corporation (PTC). All Rights
* Reserved.
*
* This software is the confidential and proprietary information of PTC
* and is subject to the terms of a software license agreement. You shall
* not disclose such confidential information and shall use it only in accordance
* with the terms of the license agreement.
*
* ecwti
*/

package com.thingworx.sdk.android.bottleflip;


import com.mbientlab.metawear.Data;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.Subscriber;
import com.mbientlab.metawear.builder.RouteBuilder;
import com.mbientlab.metawear.builder.RouteComponent;
import com.mbientlab.metawear.data.Acceleration;
import com.mbientlab.metawear.data.AngularVelocity;
import com.mbientlab.metawear.data.MagneticField;
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.BarometerBosch;
import com.mbientlab.metawear.module.GyroBmi160;
import com.mbientlab.metawear.module.Led;
import com.mbientlab.metawear.module.MagnetometerBmm150;
import com.thingworx.communications.client.ConnectedThingClient;
import com.thingworx.communications.client.things.VirtualThing;
import com.thingworx.metadata.annotations.ThingworxPropertyDefinition;
import com.thingworx.metadata.annotations.ThingworxPropertyDefinitions;
import com.thingworx.metadata.annotations.ThingworxServiceDefinition;
import com.thingworx.metadata.annotations.ThingworxServiceResult;
import com.thingworx.types.constants.CommonPropertyNames;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import bolts.Continuation;
import bolts.Task;


/**
 * BottleFlipRemoteThing
 * @author mvulcano
 *
 * @Created: Apr 6, 2016
 * Uniontown Solution Center
 *
 *	Virtual Thing object. Used to represent the Bottle in Thingworx. Includes property definitions and service definitions, as well as handling the physics calculations of the bottle's trajectory
 */

@SuppressWarnings("serial")
@ThingworxPropertyDefinitions(properties = {
		@ThingworxPropertyDefinition(name="Speed", description="Data from the sensor", baseType="NUMBER", category="Aggregates", aspects={"isReadOnly:true"}),
		@ThingworxPropertyDefinition(name="Acceleration", description="Data from the sensor", baseType="NUMBER", category="Aggregates", aspects={"isReadOnly:true"}),
		@ThingworxPropertyDefinition(name="Height", description="Data from the sensor", baseType="NUMBER", category="Aggregates", aspects={"isReadOnly:true"}),
		@ThingworxPropertyDefinition(name="BarometerHeight", description="Data from the sensor", baseType="NUMBER", category="Aggregates", aspects={"isReadOnly:true"}),
		@ThingworxPropertyDefinition(name="AccelerometerLiveData", description="Data from the sensor", baseType="STRING", category="Aggregates", aspects={"isReadOnly:true"}),
		@ThingworxPropertyDefinition(name="BarometerLiveData", description="Data from the sensor", baseType="STRING", category="Aggregates", aspects={"isReadOnly:true"}),
		@ThingworxPropertyDefinition(name="TotalTime", description="Data from the sensor", baseType="NUMBER", category="Aggregates", aspects={"isReadOnly:true"}),
        @ThingworxPropertyDefinition(name="CurrentZData", description="Data from the sensor", baseType="NUMBER", category="Aggregates", aspects={"isReadOnly:true"}),
		@ThingworxPropertyDefinition(name="Theta", description="Data from the sensor", baseType="NUMBER", category="Aggregates", aspects={"isReadOnly:true"}),
		@ThingworxPropertyDefinition(name="IsThrowStarted", description="Data from the sensor", baseType="BOOLEAN", category="Aggregates", aspects={"isReadOnly:true"}),
		@ThingworxPropertyDefinition(name="IsThrowEnded", description="Data from the sensor", baseType="BOOLEAN", category="Aggregates", aspects={"isReadOnly:true"}),
		@ThingworxPropertyDefinition(name="IsBottleMoving", description="Data from the sensor", baseType="BOOLEAN", category="Aggregates", aspects={"isReadOnly:true"})
})

public class BottleFlipRemoteThing extends VirtualThing
{
	private static final Logger LOGGER = LoggerFactory.getLogger(BottleFlipRemoteThing.class);
	private Accelerometer accelerometer;
	private MetaWearBoard board;
	private boolean isActivelyGatheringData = false;

	//Physical Attributes of the bottle
    private float acceleration = 0.0f;
    private float heightAcceleration = 0.0f;
    private float heightSpeed = 0.0f;
	private float speed = 0.0f;
	private float height = 0.0f;
	private float currentZData = 0.0f;
	private float thetaVar = 0.0f;
	private float barometerHeight = 0.0f;
	private float barometerFloorHeight = 0.0f;

	//Variables for calibrating the accelerometer.
	private float xOffset = 0.0f;
	private float yOffset = 0.0f;
	private float zOffset = 0.0f;

	//keeps track of delta time and throw duration
	private long millisecondsTimeOfLastRead = 0L;
	private long duration = 0L;
	private float deltaTime = 0.000f;
	private long timeInterval = 0L;

    private String accelerometerData = "";
	private String barometerData = "";

	//logic gates to tell when the bottle is done moving
    private boolean isThrowEnded = false;
    private boolean isThrowStarted = false;
	private boolean isBottleMoving = false;

	//Number of times to run the calculations after the bottle has stopped, to allow for the bottle to settle
	private int count = 10;

	//Constructor - used to initialize Virtual Thing
	public BottleFlipRemoteThing(String name, String description, String identifier, ConnectedThingClient client)
	{
		super(name,description,identifier,client);
		this.init();
	}

	// From the VirtualThing class
	// This method will get called when a connect or reconnect happens
	// Need to send the values when this happens
	// This is more important for a solution that does not send its properties on a regular basis
	public void synchronizeState()
	{
		// Be sure to call the base class
		super.synchronizeState();
		// Send the property values to Thingworx when a synchronization is required
		super.syncProperties();
	}

	//Called from the Constructor - used to initialize any variables the Thing will use.
	private void init()
	{
		//initializes VirtualThing from the Annotations in the class.
		try
		{
			initializeFromAnnotations();
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	//This method will be called every update. It is overridden from the parent VirtualThing class.
	@Override
	public void processScanRequest() throws Exception
	{
		// Be sure to call the base classes scan request
		super.processScanRequest();
		// Execute the code for this simulation every scan
		this.getDataFromDevice();
	}

	// [This is where you would gather data from your sensor and set the properties.]
	private void getDataFromDevice() throws Exception
	{
        if(isActivelyGatheringData) {
            super.setProperty("Speed", speed);
            super.setProperty("Acceleration", acceleration);
            super.setProperty("Height", height);
			super.setProperty("Height", barometerHeight);
            super.setProperty("AccelerometerLiveData", accelerometerData);
			super.setProperty("BarometerLiveData", accelerometerData);
            super.setProperty("CurrentZData", currentZData);
            super.setProperty("TotalTime", duration);
			super.setProperty("IsBottleMoving", isBottleMoving);
			super.setProperty("Theta", thetaVar);
			super.setProperty("IsThrowStarted", isThrowStarted);
			super.setProperty("IsThrowEnded", isThrowEnded);
        }


        super.updateSubscribedProperties(15000);
        super.updateSubscribedEvents(60000);
    }

    //When invoked, will turn on all sensor modules, and start calculating properties based on readings. Also initializes variables to handle the turn on and off logic
	@ThingworxServiceDefinition( name="StartGatheringData_BottleFlip_PTC", description="Starts the gathering of data on each refresh.")
	@ThingworxServiceResult( name=CommonPropertyNames.PROP_RESULT, description="Result", baseType="BOOLEAN" )
	public Boolean StartGatheringData_BottleFlip_PTC() throws Exception
	{
		isActivelyGatheringData = true;
		count = 10;

		timeInterval = System.currentTimeMillis();
		return isActivelyGatheringData;
	}

	//When invoked, will turn off all sensor modules, and return data members to their default values.
	@ThingworxServiceDefinition( name="StopGatheringData_BottleFlip_PTC", description="Stops the gathering of data on each refresh.")
	@ThingworxServiceResult( name=CommonPropertyNames.PROP_RESULT, description="Result", baseType="BOOLEAN" )
	public Boolean StopGatheringData_BottleFlip_PTC() throws Exception
	{
		return stopGatheringData();
	}

	//method that handles the work for the StopGatheringData_BottleFlip_PTC wrapper method.
	private Boolean stopGatheringData()
	{
		isActivelyGatheringData = false;

		if(timeInterval != 0L)
		{
			duration  = (System.currentTimeMillis() - timeInterval)/1000;
		}

		acceleration = 0.0f;
		speed = 0.0f;
		height = 0.0f;
		deltaTime = 0.000f;
		barometerHeight = 0.0f;
		millisecondsTimeOfLastRead = 0L;
		heightAcceleration = 0.0f;
		heightSpeed = 0.0f;
		isThrowEnded = false;
		isThrowStarted = false;
		isBottleMoving = false;
		return (currentZData == 1.0f || currentZData == -1.0f);
	}




	protected void addBoard_InitiModules(MetaWearBoard theBoard, List<MetaWearBoard.Module> modules)
	{
		board = theBoard;

		for(MetaWearBoard.Module tempModule : modules)
		{
			if(tempModule instanceof Accelerometer)
			{
				((Accelerometer) tempModule).configure().odr(5f).commit();
				((Accelerometer) tempModule).acceleration().addRouteAsync(new RouteBuilder() {
					@Override
					public void configure(RouteComponent source) {
						source.stream(new Subscriber() {
							@Override
							public void apply(Data data, Object... env) {
								if(isActivelyGatheringData) {
									Acceleration acc1 = data.value(Acceleration.class);
									accelerometerData = acc1.toString();
									calculateBottlePhysicsAttributes(acc1);
									if (isThrowEnded && isActivelyGatheringData) {
										checkIfBottleLanded(acc1);
									}
								}
							}
						});
					}
				}).continueWith(new Continuation<Route, Object>() {

					@Override
					public Object then(Task<Route> task) throws Exception {
						((Accelerometer) tempModule).packedAcceleration().start();
						((Accelerometer) tempModule).start();
						return null;
					}
				});
			}
			else if(tempModule instanceof BarometerBosch)
			{
				((BarometerBosch) tempModule).configure().pressureOversampling(BarometerBosch.OversamplingMode.STANDARD)
					.filterCoeff(BarometerBosch.FilterCoeff.AVG_4)
					.standbyTime(0.5f)
					.commit();
				((BarometerBosch) tempModule).altitude().addRouteAsync(new RouteBuilder() {
					@Override
					public void configure(RouteComponent source) {
						source.stream(new Subscriber() {
							@Override
							public void apply(Data data, Object... env) {
								barometerData = "Altitude: "+data.value(Float.class)+"m";
								calculateBarometerHeight(data.value(Float.class));
							}
						});
					}
				}).continueWith(new Continuation<Route, Object>() {

					@Override
					public Object then(Task<Route> task) throws Exception {

						((BarometerBosch) tempModule).altitude().start();
						((BarometerBosch) tempModule).start();
						return null;
					}
				});
			}
			else if(tempModule instanceof GyroBmi160)
			{
				((GyroBmi160) tempModule).configure()
						.odr(GyroBmi160.OutputDataRate.ODR_25_HZ)
						.range(GyroBmi160.Range.values()[0])
						.commit();
				((GyroBmi160) tempModule).angularVelocity().addRouteAsync(source -> source.stream((data, env) -> {
					final AngularVelocity value = data.value(AngularVelocity.class);

				})).continueWith(new Continuation<Route, Object>() {
					@Override
					public Object then(Task<Route> task) throws Exception {

						((GyroBmi160) tempModule).angularVelocity().start();
						((GyroBmi160) tempModule).start();

						return null;
					}
				});
			}
			else if(tempModule instanceof MagnetometerBmm150)
			{
				((MagnetometerBmm150) tempModule).usePreset(MagnetometerBmm150.Preset.ENHANCED_REGULAR);
				((MagnetometerBmm150) tempModule).packedMagneticField().addRouteAsync(source -> source.stream((data, env) -> {
					final MagneticField value = data.value(MagneticField.class);
				})).continueWith(new Continuation<Route, Object>() {
					@Override
					public Object then(Task<Route> task) throws Exception {
						((MagnetometerBmm150) tempModule).packedMagneticField().start();
						((MagnetometerBmm150) tempModule).start();
						return null;
					}
				});
			}
			else if(tempModule instanceof Led)
			{

			}
			else
			{
				System.out.println("Module Not accounted for");
			}
		}
		//acc, bar, mag, gyr

	}

	private void calculateBarometerHeight(Float value)
	{
		if(barometerFloorHeight == 0.0f)
		{
			barometerFloorHeight = value;
		}
		else
		{
			float tempheight = value - barometerFloorHeight;
			if(barometerHeight < tempheight)
			{
				barometerHeight = tempheight;
			}
		}
	}

	//Checks for a sudden increase in acceleration. The idea is that, once the bottle is in flight, it won't have a high acceleration till it impacts the ground. Once this happens, the isBottleMoving
	// flag is set to false. From there, this service decrements the count till it reaches zero, then turn off the sensor.
	private void checkIfBottleLanded(Acceleration acc1)
	{
		float x = acc1.x();
		float y = acc1.y();
		float z = acc1.z();
		if(isBottleMoving) {
			isBottleMoving = (x > -1f && x < 1f && y > -1f && y < 1f && z > -1f && z < 1f);
		}
		else
		{
			count--;
		}
		if(count <= 0)
		{
			stopGatheringData();
		}
	}

	//Calculates physical attributes of the bottle based on the acceleration sensor.
    private void calculateBottlePhysicsAttributes(Acceleration sensorData) {

		//If first read, initialize the calculations and do accelerometer calibration
		if(millisecondsTimeOfLastRead == 0)
		{
			float currentYData = sensorData.y();
			float currentXData = sensorData.x();
			float zData = sensorData.z();
			calculateCalibrationOffsets(currentXData, currentYData, zData);
			currentZData = sensorData.z() - zOffset;
			millisecondsTimeOfLastRead = System.currentTimeMillis();
			acceleration = calculateAcceleration(sensorData);
			speed = 0.0f;
			height = 0.0f;
			deltaTime = 0.000f;
		}
		//Else, start calculating the acceleration, speed, and height for each interval of delta time.
		else
		{
			//Set Z position based on sensor
			currentZData = Math.round((sensorData.z() - zOffset)*10)/10;

			//Get time and calculate delta time
			long currentTime = System.currentTimeMillis();
			deltaTime = (currentTime - millisecondsTimeOfLastRead)/1000.000f;

			//Calculations to gather acceleration, speed, and height.
			acceleration = calculateAcceleration(sensorData);
			float tempSpeed = speed;
			float tempHeight = height;

			//Vf = Vi + AT
			tempSpeed = tempSpeed + (acceleration * deltaTime);

			//Vfup = Viup + AupT
            heightSpeed = heightSpeed + (heightAcceleration*deltaTime);

			//Xf = Xi + ViT + 1/2AT^2
            tempHeight = tempHeight + (heightSpeed * deltaTime) + ((heightAcceleration*(deltaTime*deltaTime))/2);

			if(tempSpeed > speed)
			{
				speed = tempSpeed;
			}
            if(tempHeight > height)
            {
                height = tempHeight;
            }

			millisecondsTimeOfLastRead = currentTime;
		}
	}

	//Calibration method to allow the sensor to zero out on initial run. This will kill some of the sensor noise.
	private void calculateCalibrationOffsets(float currentXData, float currentYData, float currentZData)
	{
		float zIdealValue = 1.0f;
		xOffset = currentXData;
		yOffset = currentYData;
		zOffset = currentZData - zIdealValue;
	}

	//Main calculation method. Figures out the height acceleration and the angular acceleration based on sensor data, Pythagorean Theorem, and Trigonometry
	private float calculateAcceleration(Acceleration data)
	{
		//Acceleration in each direction, rounded to 3 significant figures
		float xData = Math.round((data.x() - xOffset)*1000)/1000;
		float yData = Math.round((data.y() - yOffset)*1000)/1000;
		float zData = Math.round((data.z() - zOffset)*1000)/1000;
        float returnFloat;

		//Calculates the angle between the z axis, and the direction of gravity
        float theta = calculateAngleOffset(xData, yData, zData);
		thetaVar = theta;

		//Calculates the acceleration in the Throw direction
		float accelerationTotal = (xData*xData)+(yData*yData)+(zData*zData);

		//Calculates the acceleration in the 'up' direction
		accelerationTotal = (float)(Math.sqrt(accelerationTotal));
        heightAcceleration = accelerationTotal * (float)Math.cos(theta);

		//If the bottle is in motion, 0 out the acceleration, as the only force acting on the bottle at this point is gravity.
		if(accelerationTotal <= 1.0f || isThrowEnded)
		{
			returnFloat = 0.0f;
            heightAcceleration = 0.0f;
            if(isThrowStarted)
            {
                heightAcceleration = -9.81f;
                isThrowEnded = true;
            }
		}
		//else, return the current read acceleration, minus the acceleration due to gravity. Also turns accelerometer 'Gs' to m/s^2
		else
		{
			returnFloat = (accelerationTotal * 9.81f) - (9.81f*(float)Math.cos(theta));
            heightAcceleration = (heightAcceleration * 9.81f) - 9.81f;
            isThrowStarted = true;
			isBottleMoving = true;
		}

		return returnFloat;
	}

	//Calculates the pitch of the sensor via pitch formula.
	private float calculateAngleOffset(float xData, float yData, float zData)
    {
        float op1 = (float)Math.sqrt((xData*xData) + (yData*yData));
        float op2 = (float)Math.sqrt((yData*yData)+(zData*zData));
        return (float)Math.atan2(op1,op2);
    }
}
