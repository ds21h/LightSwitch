/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jb.light.switch_;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalMultipurpose;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinMode;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import jb.light.support.Data;
import jb.light.support.Setting;

/**
 *
 * @author Jan
 */
public class LightSensor {

    private final GpioController mGpio;
    private final GpioPinDigitalOutput mLive;
    private final GpioPinDigitalMultipurpose mLDRsensor;
    private int mLightCount = 0;
    private int mLightReading = 0;
    private int mBaseLevel = 0;
    private final boolean cGpioOn;
    private final Data mData;

    public int xLightReading() {
        return mLightReading;
    }

    public int xLightCount() {
        return mLightCount;
    }

    public LightSensor(Data pData, boolean pGpioOn) {
        mData = pData;
        cGpioOn = pGpioOn;
        if (cGpioOn) {
            mGpio = GpioFactory.getInstance();
            mLive = mGpio.provisionDigitalOutputPin(RaspiPin.GPIO_01, "Live", PinState.LOW);
            mLive.setShutdownOptions(true, PinState.LOW);
            mLDRsensor = mGpio.provisionDigitalMultipurposePin(RaspiPin.GPIO_04, "LDR sensor", PinMode.DIGITAL_OUTPUT);
        } else {
            mGpio = null;
            mLive = null;
            mLDRsensor = null;
        }
    }

    public void xInit() {
        mLightCount = 0;
        mBaseLevel = 0;
    }

    public boolean xTestOn() throws InterruptedException {
        boolean lOn = false;
        Setting lSetting;

        lSetting = mData.xSetting();

        mLightReading = sReadLight(lSetting.xMaxSensor());
        if (mBaseLevel == 0) {
            if (mLightReading > lSetting.xSensorLimit() + 5) {
                mBaseLevel = lSetting.xSensorLimit() + 5;
            } else {
                mBaseLevel = mLightReading;
            }
        } else if (mLightReading < mBaseLevel) {
            mBaseLevel = mLightReading;
        }
        if (mLightReading > lSetting.xSensorLimit()) {
            if (mLightReading > mBaseLevel + lSetting.xSensorTreshold()) {
                mLightCount++;
                if (mLightCount > lSetting.xPeriodDark()) {
                    lOn = true;
                }
            } else {
                mLightCount = 0;
            }
        } else {
            mLightCount = 0;
        }

        return lOn;
    }

    private int sReadLight(int pMaxSensor) throws InterruptedException {
        int lCount;
        boolean lStop;

        if (cGpioOn) {
            mLDRsensor.setMode(PinMode.DIGITAL_OUTPUT);
            mLDRsensor.low();
            Thread.sleep(100);
            mLDRsensor.setMode(PinMode.DIGITAL_INPUT);
            mLive.high();
            lCount = 0;
            lStop = false;
            while (!lStop) {
                if (mLDRsensor.isLow()) {
                    lCount++;
                    if (lCount > pMaxSensor) {
                        lStop = true;
                    }
                } else {
                    lStop = true;
                }
            }
            mLive.low();
        } else {
            lCount = 0;
        }

        return lCount;
    }

    public void xClose() {
        if (mGpio != null) {
            mGpio.shutdown();
        }
    }
}
