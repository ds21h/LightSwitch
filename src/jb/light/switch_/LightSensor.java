/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jb.light.switch_;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalMultipurpose;
import com.pi4j.io.gpio.PinMode;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.RaspiPin;
import jb.light.support.Data;
import jb.light.support.Setting;

/**
 *
 * @author Jan
 */
public class LightSensor {

    private final GpioController mGpio;
    private final GpioPinDigitalMultipurpose mLive;
    private final GpioPinDigitalInput mLDRsensor;
    private int mLightCount = 0;
    private int mLightReading = 0;
//    private int mBaseLevel = 0;
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
            mLive = mGpio.provisionDigitalMultipurposePin(RaspiPin.GPIO_01, "Live", PinMode.DIGITAL_INPUT);
            mLive.setPullResistance(PinPullResistance.PULL_DOWN);
            mLDRsensor = mGpio.provisionDigitalInputPin(RaspiPin.GPIO_04, "LDR sensor");
            mLDRsensor.setPullResistance(PinPullResistance.PULL_DOWN);
        } else {
            mGpio = null;
            mLive = null;
            mLDRsensor = null;
        }
    }

    public void xInit() {
        mLightCount = 0;
        //    mBaseLevel = 0;
    }

    public boolean xTestOn() {
        boolean lOn = false;
        Setting lSetting;

        lSetting = mData.xSetting();

        mLightReading = sReadLight();
//        if (mBaseLevel == 0) {
//            if (mLightReading > lSetting.xSensorLimit() + 5) {
//                mBaseLevel = lSetting.xSensorLimit() + 5;
//            } else {
//                mBaseLevel = mLightReading;
//            }
//        } else {
//            if (mLightReading < mBaseLevel) {
//                mBaseLevel = mLightReading;
//            }
//        }
        if (mLightReading > lSetting.xSensorLimit()) {
//            if (mLightReading > mBaseLevel + lSetting.xSensorTreshold()) {
            mLightCount++;
            if (mLightCount > lSetting.xPeriodDark()) {
                lOn = true;
            }
//            } else {
//                mLightCount = 0;
//        }
        } else {
            mLightCount = 0;
        }

        return lOn;
    }

    private int sReadLight() {
        int lCount;
        int lResult;
        boolean lStop;
        long lStartTimeMili;
        long lStartTimeNano;

        if (cGpioOn) {
            mLive.setPullResistance(PinPullResistance.OFF);
            mLDRsensor.setPullResistance(PinPullResistance.OFF);
            mLive.setMode(PinMode.DIGITAL_OUTPUT);
            mLive.high();
            lStartTimeMili = System.currentTimeMillis();
            lStartTimeNano = System.nanoTime();
            lCount = 0;
            lStop = false;
            while (!lStop) {
                if (mLDRsensor.isLow()) {
                    lCount++;
                    if (lCount > 2500) {
                        if ((System.currentTimeMillis() - lStartTimeMili) > 1000) {
                            lStop = true;
                        } else {
                            lCount = 0;
                        }
                    }
                } else {
                    lStop = true;
                }
            }
            lResult = (int) ((System.nanoTime() - lStartTimeNano) / 10000);
            mLive.low();
            mLive.setMode(PinMode.DIGITAL_INPUT);
            mLive.setPullResistance(PinPullResistance.PULL_DOWN);
            mLDRsensor.setPullResistance(PinPullResistance.PULL_DOWN);
        } else {
            lResult = 0;
        }

        return lResult;
    }

    public void xClose() {
        if (mGpio != null) {
            mGpio.shutdown();
        }
    }
}
