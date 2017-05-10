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
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.GyroBmi160;
import com.mbientlab.metawear.module.Led;
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
		@ThingworxPropertyDefinition(name="Acceleration", description="Data from the sensor", baseType="NUMBER", category="Aggregates", aspects={"isReadOnly:true"}),
		@ThingworxPropertyDefinition(name="BottleFlips", description="Data from the sensor", baseType="NUMBER", category="Aggregates", aspects={"isReadOnly:true"}),
		@ThingworxPropertyDefinition(name="AccelerometerLiveData", description="Data from the sensor", baseType="STRING", category="Aggregates", aspects={"isReadOnly:true"}),
        @ThingworxPropertyDefinition(name="CurrentZData", description="Data from the sensor", baseType="NUMBER", category="Aggregates", aspects={"isReadOnly:true"}),
		@ThingworxPropertyDefinition(name="Theta", description="Data from the sensor", baseType="NUMBER", category="Aggregates", aspects={"isReadOnly:true"}),
		@ThingworxPropertyDefinition(name="IsThrowStarted", description="Data from the sensor", baseType="BOOLEAN", category="Aggregates", aspects={"isReadOnly:true"}),
		@ThingworxPropertyDefinition(name="IsThrowEnded", description="Data from the sensor", baseType="BOOLEAN", category="Aggregates", aspects={"isReadOnly:true"}),
		@ThingworxPropertyDefinition(name="IsBottleMoving", description="Data from the sensor", baseType="BOOLEAN", category="Aggregates", aspects={"isReadOnly:true"}),
		@ThingworxPropertyDefinition(name="IsFlipDone", description="Data from the sensor", baseType="BOOLEAN", category="Aggregates", aspects={"isReadOnly:true"})
})

public class BottleFlipRemoteThing extends VirtualThing {
	private static final Logger LOGGER = LoggerFactory.getLogger(BottleFlipRemoteThing.class);
	private Accelerometer accelerometer;
	private GyroBmi160 gyroscope;
	private MetaWearBoard board;
	private boolean isActivelyGatheringData = false;

	//Physical Attributes of the bottle
	private float acceleration = 0.0f;
	private float currentZData = 0.0f;
	private float thetaFromGyroscope = 0.0f;
	private float xAnglePosition = 0.0f;
	private float yAnglePosition = 0.0f;
	private float thresholdToTriggerThrow = 1.4f;
	private float numberOfFlips = 0.0f;

	//Variables for calibrating the accelerometer.
	private float xOffset = 0.0f;
	private float yOffset = 0.0f;
	private float zOffset = 0.0f;

	//keeps track of delta time and throw duration
	private long millisecondsTimeOfLastRead = 0L;
	private long millisecondsTimeOfLastReadGyroscope = 0L;
	private long duration = 0L;
	private long timeInterval = 0L;

	//Strings to hold the live data of the sensor modules
	private String accelerometerData = "";

	//logic gates to tell when the bottle is done moving
	private boolean isThrowEnded = false;
	private boolean isThrowStarted = false;
	private boolean isBottleMoving = false;
	private boolean isFlipDone = false;

	private final float roundingModifier = 1000.0f;

	//Number of times to run the calculations after the bottle has stopped, to allow for the bottle to settle
	private int count = 30;

	//Constructor - used to initialize Virtual Thing
	public BottleFlipRemoteThing(String name, String description, String identifier, ConnectedThingClient client) {
		super(name, description, identifier, client);
		this.init();
	}

	// From the VirtualThing class
	// This method will get called when a connect or reconnect happens
	// Need to send the values when this happens
	// This is more important for a solution that does not send its properties on a regular basis
	public void synchronizeState() {
		// Be sure to call the base class
		super.synchronizeState();
		// Send the property values to Thingworx when a synchronization is required
		super.syncProperties();
	}

	//Called from the Constructor - used to initialize any variables the Thing will use.
	private void init() {
		//initializes VirtualThing from the Annotations in the class.
		try {
			initializeFromAnnotations();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	//This method will be called every update. It is overridden from the parent VirtualThing class.
	@Override
	public void processScanRequest() throws Exception {
		// Be sure to call the base classes scan request
		super.processScanRequest();
		// Execute the code for this simulation every scan
		this.getDataFromDevice();
	}

	// [This is where you would gather data from your sensor and set the properties.]
	private void getDataFromDevice() throws Exception
	{
		super.setProperty("Acceleration", acceleration);
		super.setProperty("AccelerometerLiveData", accelerometerData);
		super.setProperty("CurrentZData", currentZData);
		super.setProperty("IsBottleMoving", isBottleMoving);
		super.setProperty("Theta", thetaFromGyroscope);
		super.setProperty("IsThrowStarted", isThrowStarted);
		super.setProperty("IsThrowEnded", isThrowEnded);
		super.setProperty("IsFlipDone", isFlipDone);
		super.setProperty("BottleFlips", numberOfFlips);


		super.updateSubscribedProperties(15000);
		super.updateSubscribedEvents(60000);
	}

	//When invoked, will turn on all sensor modules, and start calculating properties based on readings. Also initializes variables to handle the turn on and off logic
	@ThingworxServiceDefinition(name = "StartGatheringData_BottleFlip_PTC", description = "Starts the gathering of data on each refresh.")
	@ThingworxServiceResult(name = CommonPropertyNames.PROP_RESULT, description = "Result", baseType = "BOOLEAN")
	public Boolean StartGatheringData_BottleFlip_PTC() throws Exception
	{
		return startGatheringData();
	}

	//When invoked, will turn off all sensor modules, and return data members to their default values.
	@ThingworxServiceDefinition(name = "StopGatheringData_BottleFlip_PTC", description = "Stops the gathering of data on each refresh.")
	@ThingworxServiceResult(name = CommonPropertyNames.PROP_RESULT, description = "Result", baseType = "BOOLEAN")
	public Boolean StopGatheringData_BottleFlip_PTC() throws Exception
	{
		return stopGatheringData();
	}

	//method that handles the work for the StartGatheringData_BottleFlip_PTC wrapper method.
	private Boolean startGatheringData()
	{
		isActivelyGatheringData = true;
		count = 30;
		acceleration = 0.0f;
		millisecondsTimeOfLastRead = 0L;
		isThrowEnded = false;
		isThrowStarted = false;
		isBottleMoving = false;
		isFlipDone = false;
		xAnglePosition = 0.0f;
		yAnglePosition = 0.0f;
		thetaFromGyroscope = 0.0f;
		thresholdToTriggerThrow = 1.4f;
		timeInterval = System.currentTimeMillis();
		numberOfFlips = 0.0f;
		startModules();
		return isActivelyGatheringData;
	}

	//Starts the Accelerometer and Gyroscope Modules
	private void startModules()
	{
		accelerometer.acceleration().start();
		accelerometer.start();

		gyroscope.angularVelocity().start();
		gyroscope.start();
	}

	//method that handles the work for the StopGatheringData_BottleFlip_PTC wrapper method.
	private Boolean stopGatheringData()
	{
		isActivelyGatheringData = false;
		if (timeInterval != 0L)
		{
			duration = (System.currentTimeMillis() - timeInterval) / 1000;
		}

		stopModules();

		return (currentZData == 1.0f || currentZData == -1.0f);
	}

	//Starts the Accelerometer and Gyroscope Modules
	private void stopModules()
	{
		accelerometer.acceleration().stop();
		accelerometer.stop();

		gyroscope.angularVelocity().stop();
		gyroscope.stop();
	}

	protected void addBoard_InitiModules(MetaWearBoard theBoard, List<MetaWearBoard.Module> modules) {
		board = theBoard;

		for (MetaWearBoard.Module tempModule : modules) {
			if (tempModule instanceof Accelerometer) {
				((Accelerometer) tempModule).configure().odr(25f).commit();
				((Accelerometer) tempModule).acceleration().addRouteAsync(new RouteBuilder() {
					@Override
					public void configure(RouteComponent source) {
						source.stream(new Subscriber() {
							@Override
							public void apply(Data data, Object... env) {
								if (isActivelyGatheringData) {
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
					public Object then(Task<Route> task) throws Exception
					{
						accelerometer = ((Accelerometer) tempModule);
						return null;
					}
				});
			} else if (tempModule instanceof GyroBmi160) {
				((GyroBmi160) tempModule).configure()
						.odr(GyroBmi160.OutputDataRate.ODR_25_HZ)
						.range(GyroBmi160.Range.values()[0])
						.commit();
				((GyroBmi160) tempModule).angularVelocity().addRouteAsync(source -> source.stream((data, env) ->
				{
					final AngularVelocity value = data.value(AngularVelocity.class);
					calculateAngle(value);

				})).continueWith(new Continuation<Route, Object>() {
					@Override
					public Object then(Task<Route> task) throws Exception
					{
						gyroscope = ((GyroBmi160)tempModule);
						return null;
					}
				});
			} else if (tempModule instanceof Led) {

			} else {
				System.out.println("Module Not accounted for");
			}
		}
		//acc, bar, mag, gyr

	}

	//Main Gyroscope method. Calculates the orientation of the bottle with respect to its starting position.
	private void calculateAngle(AngularVelocity value)
	{
		float xAngleSpeed = value.x();
		float yAngleSpeed = value.y();

		long currentTime = System.currentTimeMillis();
		if(millisecondsTimeOfLastReadGyroscope == 0L)
		{
			millisecondsTimeOfLastReadGyroscope = currentTime;
			thetaFromGyroscope = 0;
		}
		else
		{
			//Calculates bottle Angle
			float gyroscopeDeltaTime = (currentTime - millisecondsTimeOfLastReadGyroscope)/1000.0f;
			xAnglePosition = xAnglePosition + xAngleSpeed * gyroscopeDeltaTime;
			yAnglePosition = yAnglePosition + yAngleSpeed * gyroscopeDeltaTime;
			float op1 = (float)Math.cos(Math.toRadians(xAnglePosition));
			float op2 = (float)Math.cos(Math.toRadians(yAnglePosition));
			thetaFromGyroscope = (float)Math.acos(op1*op2);

			//Calculate Flips
			//numberOfFlips = (float)Math.floor(thetaFromGyroscope / Math.PI * 2);
		}

		millisecondsTimeOfLastReadGyroscope = currentTime;

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
			isFlipDone = true;
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
		}
		//Set Z position based on sensor
		currentZData = Math.round((sensorData.z() - zOffset)*10)/10;

		//Calculations to gather acceleration, speed, and height.
		float calculatedAcceleration = calculateAcceleration(sensorData);
		if(calculatedAcceleration > acceleration)
		{
			acceleration = calculatedAcceleration;
		}


		millisecondsTimeOfLastRead = System.currentTimeMillis();

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
		float xData = Math.round((data.x() - xOffset)*roundingModifier)/roundingModifier;
		float yData = Math.round((data.y() - yOffset)*roundingModifier)/roundingModifier;
		float zData = Math.round((data.z() - zOffset)*roundingModifier)/roundingModifier;
        float returnFloat;

		//Calculates the acceleration in the Throw direction
		float accelerationTotal = (float)(Math.sqrt((xData*xData)+(yData*yData)+(zData*zData)));

		if(accelerationTotal <= thresholdToTriggerThrow || isThrowEnded)
		{
			returnFloat = 0.0f;
            if(isThrowStarted)
            {
                isThrowEnded = true;
            }
		}
		else
		{
			returnFloat = ((accelerationTotal - 0.8f) * 9.81f);
            isThrowStarted = true;
			isBottleMoving = true;
			thresholdToTriggerThrow = 1.0f;
		}
		return returnFloat;
	}
}

