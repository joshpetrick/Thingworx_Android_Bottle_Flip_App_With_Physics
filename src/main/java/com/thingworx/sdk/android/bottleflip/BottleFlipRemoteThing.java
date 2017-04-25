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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mbientlab.metawear.builder.RouteComponent;
import com.mbientlab.metawear.data.Acceleration;
import com.mbientlab.metawear.data.AngularVelocity;
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.Led;
import com.thingworx.communications.client.ConnectedThingClient;
import com.thingworx.communications.client.things.VirtualThing;
import com.thingworx.metadata.annotations.ThingworxPropertyDefinition;
import com.thingworx.metadata.annotations.ThingworxPropertyDefinitions;
import com.thingworx.metadata.annotations.ThingworxServiceDefinition;
import com.thingworx.metadata.annotations.ThingworxServiceResult;
import com.thingworx.types.constants.CommonPropertyNames;

import com.mbientlab.metawear.Data;
import com.mbientlab.metawear.Subscriber;
import com.mbientlab.metawear.builder.RouteBuilder;
import com.mbientlab.metawear.module.BarometerBosch;
import com.mbientlab.metawear.module.GyroBmi160;

import java.util.Date;


/**
 * BottleFlipRemoteThing
 * @author mvulcano
 *
 * @Created: Apr 6, 2016
 * Uniontown Solution Center
 *
 *
 */

@SuppressWarnings("serial")
@ThingworxPropertyDefinitions(properties = {
		@ThingworxPropertyDefinition(name="Speed", description="Data from the sensor", baseType="NUMBER", category="Aggregates", aspects={"isReadOnly:true"}),
		@ThingworxPropertyDefinition(name="Acceleration", description="Data from the sensor", baseType="NUMBER", category="Aggregates", aspects={"isReadOnly:true"}),
        @ThingworxPropertyDefinition(name="BarometerLiveData", description="Data from the sensor", baseType="STRING", category="Aggregates", aspects={"isReadOnly:true"}),
        @ThingworxPropertyDefinition(name="GyroscopeLiveData", description="Data from the sensor", baseType="STRING", category="Aggregates", aspects={"isReadOnly:true"}),
		@ThingworxPropertyDefinition(name="Height", description="Data from the sensor", baseType="NUMBER", category="Aggregates", aspects={"isReadOnly:true"}),
		@ThingworxPropertyDefinition(name="isLanded", description="Data from the sensor", baseType="BOOLEAN", category="Aggregates", aspects={"isReadOnly:true"}),
		@ThingworxPropertyDefinition(name="AccelerometerLiveData", description="Data from the sensor", baseType="STRING", category="Aggregates", aspects={"isReadOnly:true"}),
		@ThingworxPropertyDefinition(name="DeltaTime", description="Data from the sensor", baseType="NUMBER", category="Aggregates", aspects={"isReadOnly:true"}),
		@ThingworxPropertyDefinition(name="XOffset", description="Data from the sensor", baseType="NUMBER", category="Aggregates", aspects={"isReadOnly:true"}),
		@ThingworxPropertyDefinition(name="YOffset", description="Data from the sensor", baseType="NUMBER", category="Aggregates", aspects={"isReadOnly:true"}),
		@ThingworxPropertyDefinition(name="ZOffset", description="Data from the sensor", baseType="NUMBER", category="Aggregates", aspects={"isReadOnly:true"}),
		@ThingworxPropertyDefinition(name="ZAngleOffset", description="Data from the sensor", baseType="NUMBER", category="Aggregates", aspects={"isReadOnly:true"}),
		@ThingworxPropertyDefinition(name="TotalTime", description="Data from the sensor", baseType="NUMBER", category="Aggregates", aspects={"isReadOnly:true"}),
		@ThingworxPropertyDefinition(name="BarometerHeight", description="Data from the sensor", baseType="NUMBER", category="Aggregates", aspects={"isReadOnly:true"}),
        @ThingworxPropertyDefinition(name="CurrentZData", description="Data from the sensor", baseType="NUMBER", category="Aggregates", aspects={"isReadOnly:true"}),
		@ThingworxPropertyDefinition(name="IsBottleMoving", description="Data from the sensor", baseType="BOOLEAN", category="Aggregates", aspects={"isReadOnly:true"}),
        @ThingworxPropertyDefinition(name="AccelerometerRawZValue", description="Data from the sensor", baseType="NUMBER", category="Aggregates", aspects={"isReadOnly:true"})
})

public class BottleFlipRemoteThing extends VirtualThing
{
	private static final Logger LOGGER = LoggerFactory.getLogger(BottleFlipRemoteThing.class);
	private Accelerometer accelerometer;
	//private BarometerBosch barometer;
	private boolean isActivelyGatheringData = false;
    private float acceleration = 0.0f;
    private float heightAcceleration = 0.0f;
    private float heightSpeed = 0.0f;
	private float accelerationZRawValue = 0.0f;
	private float speed = 0.0f;
	private float height = 0.0f;
	private float deltaTime = 0.000f;
	private float tempSpeed = 0.0f;
	private float deltaTimeInMills = 0.0f;
	private float tempHeight = 0.0f;
	private float currentZData = 0.0f;
	private float xOffset = 0.0f;
	private float yOffset = 0.0f;
	private float zOffset = 0.0f;
	private float zAngleOffset = 0.0f;
	private long millisecondsTimeOfLastRead = 0L;
	private long duration = 0L;
    private String barometerData = "";
    private String gyroscopeData = "";
    private String accelerometerData = "";
	private Date timeInterval;
    private float barometerHeight = 0.0f;
    private float barometerOffset = 0.0f;
    private boolean isThrowEnded = false;
    private boolean isThrowStarted = false;
	private float xAccelerometerData = 0.0f;
	private float yAccelerationData = 0.0f;
	private float zAccelerationData = 0.0f;
	private boolean isBottleMoving = false;
	private int count = 10;
    //private Led led;

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
            super.setProperty("isLanded", true);
            super.setProperty("AccelerometerLiveData", accelerometerData);
            super.setProperty("DeltaTime", deltaTimeInMills);
            super.setProperty("CurrentZData", currentZData);
            super.setProperty("XOffset", xOffset);
            super.setProperty("YOffset", yOffset);
            super.setProperty("ZOffset", zOffset);
            super.setProperty("ZAngleOffset", zAngleOffset);
            super.setProperty("TotalTime", duration);
            super.setProperty("BarometerLiveData", barometerData);
            super.setProperty("GyroscopeLiveData", gyroscopeData);
            super.setProperty("BarometerHeight", barometerHeight);
            super.setProperty("AccelerometerRawZValue", accelerationZRawValue);
			super.setProperty("IsBottleMoving", isBottleMoving);
        }


        super.updateSubscribedProperties(15000);
        super.updateSubscribedEvents(60000);
    }

    //When invoked, will turn on all sensor modules, and start calculating properties based on readings.
	@ThingworxServiceDefinition( name="StartGatheringData_BottleFlip_PTC", description="Starts the gathering of data on each refresh.")
	@ThingworxServiceResult( name=CommonPropertyNames.PROP_RESULT, description="Result", baseType="BOOLEAN" )
	public Boolean StartGatheringData_BottleFlip_PTC() throws Exception
	{
		isActivelyGatheringData = true;
		count = 10;
		startAccelerometer();
        //startBarometer();
        //startGyroscope();
		timeInterval = new Date();
		return isActivelyGatheringData;
	}

	//When invoked, will turn off all sensor modules, and return data members to their default values.
	@ThingworxServiceDefinition( name="StopGatheringData_BottleFlip_PTC", description="Stops the gathering of data on each refresh.")
	@ThingworxServiceResult( name=CommonPropertyNames.PROP_RESULT, description="Result", baseType="BOOLEAN" )
	public Boolean StopGatheringData_BottleFlip_PTC() throws Exception
	{
		return stopGatheringData();
	}

	private Boolean stopGatheringData()
	{
		isActivelyGatheringData = false;
		stopAccelerometer();
		//stopBarometer();
		//stopGyroscope();

		if(timeInterval != null)
		{
			duration  = (new Date().getTime() - timeInterval.getTime())/1000;
		}
		acceleration = 0.0f;
		accelerationZRawValue = 0.0f;
		speed = 0.0f;
		height = 0.0f;
		deltaTime = 0.000f;
		tempSpeed = 0.0f;
		deltaTimeInMills = 0.0f;
		tempHeight = 0.0f;
		millisecondsTimeOfLastRead = 0L;
		barometerHeight = 0.0f;
		barometerOffset = 0.0f;
		heightAcceleration = 0.0f;
		heightSpeed = 0.0f;
		isThrowEnded = false;
		isThrowStarted = false;
		isBottleMoving = false;
		return (currentZData == 1.0f || currentZData == -1.0f);
	}

	//Starts the accelerometer module
	private void startAccelerometer()
	{
		if(accelerometer != null)
		{
			accelerometer.acceleration().start();
			accelerometer.start();
		}
	}

	//Stops the accelerometer module
	private void stopAccelerometer()
	{
		accelerometer.acceleration().stop();
		accelerometer.stop();

	}

    //Starts the accelerometer module
    /*
    private void startGyroscope()
    {
        if(gyroscope != null)
        {
            gyroscope.angularVelocity().start();
            gyroscope.start();
        }
    }
    */
    //Stops the accelerometer module
    /*
    private void stopGyroscope()
    {
        gyroscope.angularVelocity().stop();
        gyroscope.stop();

    }
    */
    //Starts the accelerometer module
    /*
    private void startBarometer()
    {
        if(barometer != null)
        {
            barometer.altitude().start();
            barometer.start();
        }
    }
    */
    //Stops the accelerometer module
    /*
    private void stopBarometer()
    {
        barometer.altitude().stop();
        barometer.stop();

    }
    */

	//Creates the acceleration stream. This is an asynchronous task that runs in the background, gathering data from the sensor and setting it as a property.
	protected void createAccelerometerStream(Accelerometer acc)
    {
        accelerometer = acc;
        accelerometer.acceleration().addRouteAsync(source -> source.stream(new Subscriber() {
            @Override
            public void apply(Data data, Object... env) {
                    Acceleration acc1 = data.value(Acceleration.class);
                    accelerometerData = acc1.toString();
                    calculateBottlePhysicsAttributes(acc1);
					if(isThrowEnded && isActivelyGatheringData)
					{
						checkIfBottleLanded(acc1);
					}
            }
        }));
    }

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

	//Creates the gyroscope stream. This is an asynchronous task that runs in the background, gathering data from the sensor and setting it as a property.
    /*
    protected void createGyroscopeStream(GyroBmi160 gyro)
	{
		gyroscope = gyro;
		gyro.angularVelocity().addRouteAsync(new RouteBuilder() {
			@Override
			public void configure(RouteComponent source) {
			source.stream(new Subscriber() {
				@Override
				public void apply(Data data, Object... env) {
                        AngularVelocity value = data.value(AngularVelocity.class);
                        gyroscopeData = value.toString();
				}
			});
			}
		});
	}
    */

    //Creates the barometer stream. This is an asynchronous task that runs in the background, gathering data from the sensor and setting it as a property.
/*	protected void createBarometerStream(BarometerBosch baro)
	{
		barometer = baro;
		barometer.altitude().addRouteAsync(new RouteBuilder() {
			@Override
			public void configure(RouteComponent source) {
				source.stream(new Subscriber() {
					@Override
					public void apply(Data data, Object... env) {
                            float value = data.value(Float.class);
                            barometerData = "Altitude (m) " + value;
                            if (barometerOffset == 0.0f) {
                                barometerOffset = value;
                            }
                            if(barometerHeight < (value - barometerOffset)) {
                                barometerHeight = value - barometerOffset;
                            }
					}
				});
			}
		});
	}
*/
	//Calculates physical attributes of the bottle based on the acceleration sensor.
    private void calculateBottlePhysicsAttributes(Acceleration sensorData) {

        accelerationZRawValue = sensorData.z();
		if(millisecondsTimeOfLastRead == 0)
		{
			float currentYData = sensorData.y();
			float currentXData = sensorData.x();
			float zData = sensorData.z();
			calculateCalibrationOffsets(currentXData, currentYData, zData);
			currentZData = sensorData.z() - zOffset;
			millisecondsTimeOfLastRead = new Date().getTime();
			acceleration = calculateAcceleration(sensorData);
			speed = 0.0f;
			height = 0.0f;
			deltaTime = 0.000f;
		}
		else
		{
			currentZData = Math.round((sensorData.z() - zOffset)*10)/10;
			long currentTime = new Date().getTime();
			deltaTime = (currentTime - millisecondsTimeOfLastRead)/1000.000f;
			deltaTimeInMills = (currentTime - millisecondsTimeOfLastRead);
			acceleration = calculateAcceleration(sensorData);
			tempSpeed = tempSpeed + (acceleration * deltaTime);
            heightSpeed = heightSpeed + (heightAcceleration*deltaTime);
            tempHeight = tempHeight + (heightSpeed * deltaTime) + ((heightAcceleration*(deltaTime*deltaTime))/2);
			if(tempSpeed > speed)
			{
				speed = tempSpeed;
			}
            if(tempHeight > height)
            {
                height = tempHeight;
            }
			if(speed == 0.0f)
			{
				tempSpeed = 0.0f;
			}
            if(height == 0.0f)
            {
                tempHeight = 0.0f;
            }
			millisecondsTimeOfLastRead = currentTime;
		}
	}

	private void calculateCalibrationOffsets(float currentXData, float currentYData, float currentZData)
	{
		float zIdealValue = 1.0f;
		xOffset = currentXData;
		yOffset = currentYData;
		zOffset = currentZData - zIdealValue;
	}

	private float calculateAcceleration(Acceleration data)
	{
		float xData = Math.round((data.x() - xOffset)*1000)/1000;
		float yData = Math.round((data.y() - yOffset)*1000)/1000;
		float zData = Math.round((data.z() - zOffset)*1000)/1000;
        float returnFloat;
        float theta = calculateAngleOffset(xData, yData, zData);

		float accelerationTotal = (xData*xData)+(yData*yData)+(zData*zData);
		heightAcceleration = (float)(Math.sqrt(accelerationTotal));
        accelerationTotal = heightAcceleration * (float)Math.cos(theta);
        LOGGER.error("Height Acceleration = "+heightAcceleration);
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
		else
		{
			returnFloat = (accelerationTotal * 9.81f) - 9.81f;
            heightAcceleration = (heightAcceleration * 9.81f) - 9.81f;
            isThrowStarted = true;
			isBottleMoving = true;
		}
		accelerationZRawValue = accelerationTotal;
		return returnFloat;
	}

	private float calculateAngleOffset(float xData, float yData, float zData)
    {
        float op1 = (float)Math.sqrt((xData*zData) + (yData*yData));
        float op2 = (float)Math.sqrt((yData*yData)+(zData*zData));
        return (float)Math.atan2(op1,op2);
    }
}
