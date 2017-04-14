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
import com.mbientlab.metawear.Subscriber;
import com.mbientlab.metawear.builder.RouteBuilder;
import com.mbientlab.metawear.builder.RouteComponent;
import com.mbientlab.metawear.data.Acceleration;
import com.mbientlab.metawear.module.Accelerometer;
import com.thingworx.communications.client.ConnectedThingClient;
import com.thingworx.communications.client.things.VirtualThing;
import com.thingworx.metadata.annotations.ThingworxPropertyDefinition;
import com.thingworx.metadata.annotations.ThingworxPropertyDefinitions;
import com.thingworx.metadata.annotations.ThingworxServiceDefinition;
import com.thingworx.metadata.annotations.ThingworxServiceResult;
import com.thingworx.types.constants.CommonPropertyNames;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
		@ThingworxPropertyDefinition(name="Height", description="Data from the sensor", baseType="NUMBER", category="Aggregates", aspects={"isReadOnly:true"}),
		@ThingworxPropertyDefinition(name="isLanded", description="Data from the sensor", baseType="BOOLEAN", category="Aggregates", aspects={"isReadOnly:true"}),
		@ThingworxPropertyDefinition(name="AccelerometerRawZValue", description="Data from the sensor", baseType="NUMBER", category="Aggregates", aspects={"isReadOnly:true"}),
		@ThingworxPropertyDefinition(name="DeltaTime", description="Data from the sensor", baseType="NUMBER", category="Aggregates", aspects={"isReadOnly:true"}),
		@ThingworxPropertyDefinition(name="XOffset", description="Data from the sensor", baseType="NUMBER", category="Aggregates", aspects={"isReadOnly:true"}),
		@ThingworxPropertyDefinition(name="YOffset", description="Data from the sensor", baseType="NUMBER", category="Aggregates", aspects={"isReadOnly:true"}),
		@ThingworxPropertyDefinition(name="ZOffset", description="Data from the sensor", baseType="NUMBER", category="Aggregates", aspects={"isReadOnly:true"}),
		@ThingworxPropertyDefinition(name="CurrentZData", description="Data from the sensor", baseType="NUMBER", category="Aggregates", aspects={"isReadOnly:true"})
})

public class BottleFlipRemoteThing extends VirtualThing
{
	private static final Logger LOGGER = LoggerFactory.getLogger(BottleFlipRemoteThing.class);
	public Accelerometer accelerometer;
	private boolean isActivelyGatheringData = false;
    private float acceleration = 0.0f;
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
	private long millisecondsTimeOfLastRead = 0L;

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
        if(isActivelyGatheringData)
        {
            super.setProperty("Speed",speed);
            super.setProperty("Acceleration", acceleration);
            super.setProperty("Height", height);
            super.setProperty("isLanded", true);
			super.setProperty("AccelerometerRawZValue", accelerationZRawValue);
			super.setProperty("DeltaTime", deltaTimeInMills);
			super.setProperty("CurrentZData", currentZData);
			super.setProperty("XOffset", xOffset);
			super.setProperty("YOffset", yOffset);
			super.setProperty("ZOffset", zOffset);
        }


        super.updateSubscribedProperties(15000);
        super.updateSubscribedEvents(60000);
    }

	//Service to Add Numbers Together example
	@ThingworxServiceDefinition( name="StartGatheringData_BottleFlip_PTC", description="Starts the gathering of data on each refresh.")
	@ThingworxServiceResult( name=CommonPropertyNames.PROP_RESULT, description="Result", baseType="BOOLEAN" )
	public Boolean StartGatheringData_BottleFlip_PTC() throws Exception
	{
		isActivelyGatheringData = true;
        startAccelerometer();
		return isActivelyGatheringData;
	}

	//Service to Add Numbers Together example
	@ThingworxServiceDefinition( name="StopGatheringData_BottleFlip_PTC", description="Stops the gathering of data on each refresh.")
	@ThingworxServiceResult( name=CommonPropertyNames.PROP_RESULT, description="Result", baseType="BOOLEAN" )
	public Boolean StopGatheringData_BottleFlip_PTC() throws Exception
	{
		isActivelyGatheringData = false;
        stopAccelerometer();
		return isActivelyGatheringData;
	}

	private void startAccelerometer()
	{
		if(accelerometer != null)
		{
			accelerometer.acceleration().start();
			accelerometer.start();
		}
	}

	private void stopAccelerometer()
	{
		accelerometer.acceleration().stop();
		accelerometer.stop();

		acceleration = 0.0f;
		accelerationZRawValue = 0.0f;
		speed = 0.0f;
		height = 0.0f;
		deltaTime = 0.000f;
		tempSpeed = 0.0f;
		deltaTimeInMills = 0.0f;
		tempHeight = 0.0f;
		millisecondsTimeOfLastRead = 0L;
	}

	protected void createAccelerometerStream(Accelerometer acc)
    {
        accelerometer = acc;
        accelerometer.acceleration().addRouteAsync(new RouteBuilder() {
            @Override
            public void configure(RouteComponent source) {
                source.stream(new Subscriber() {
                    @Override
                    public void apply(Data data, Object... env) {
                        calculateBottlePhysicsAttributes(data.value(Acceleration.class));
                    }
                });
            }
        });
    }

    private void calculateBottlePhysicsAttributes(Acceleration sensorData) {


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
			currentZData = sensorData.z() - zOffset;
			long currentTime = new Date().getTime();
			deltaTime = (currentTime - millisecondsTimeOfLastRead)/1000.000f;
			deltaTimeInMills = (currentTime - millisecondsTimeOfLastRead);
			acceleration = calculateAcceleration(sensorData);
			tempSpeed = tempSpeed + (acceleration * deltaTime);
			tempHeight = tempHeight + (tempSpeed * deltaTime);
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
		zOffset = currentZData % zIdealValue;
	}

	private float calculateAcceleration(Acceleration data)
	{
		float xData = data.x() - xOffset;
		float yData = data.y() - yOffset;
		float zData = data.z() - zOffset;

		float accelerationTotal = (xData*xData)+(yData*yData)+(zData*zData);
		accelerationTotal = (float)Math.sqrt(accelerationTotal) * zData;
		accelerationTotal = (accelerationTotal * 9.81f) - 9.81f;
		accelerationZRawValue = accelerationTotal;
		return accelerationTotal;
	}
}
