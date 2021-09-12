package jb.light.switch_;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Random;
import jb.light.support.Action;
import jb.light.support.Current;
import jb.light.support.Data;
import jb.light.support.Setting;
import jb.light.support.Switch;

/**
 *
 * @author Jan
 */
public class Control {

    private final boolean cGpioOn = true; //Only turn off for testing on PC (No GPIO available)!
//    private final boolean cGpioOn = false; //Only turn off for testing on PC (No GPIO available)!

    private LocalDate mSysoutDate;
    private boolean mStop;

    private boolean mTest = false;
    private int mTestSunsetHour;
    private int mTestSunsetMin;
    private int mTestIntervalSec;

    private final SunriseSunset mSunset = new SunriseSunset();
    private final Data mData = new Data();
    private final LightSensor mOnSensor = new LightSensor(mData, cGpioOn);
    private final Current mCurrent = new Current();

    public void xStart() {
        sSetSysout();
        mTest = false;
        mStop = false;
        sUpdate();
        if (ZonedDateTime.now().isAfter(mCurrent.xLightOff().minusMinutes(15))) {
            mCurrent.xStartLightOnProcessed();
            mData.xCurrent(mCurrent);
        }
        sProcess();
    }

    public void xStart(int pSunsetHour, int pSunsetMin, int pIntervalSec) {
        sSetSysout();
        mTest = true;
        mStop = false;
        if (pSunsetHour < 0) {
            mTestSunsetHour = -1;
            mTestSunsetMin = -1;
        } else {
            mTestSunsetHour = pSunsetHour;
            mTestSunsetMin = pSunsetMin;
        }
        if (pIntervalSec < 0) {
            mTestIntervalSec = -1;
        } else {
            mTestIntervalSec = pIntervalSec;
        }
        sUpdate();
        sProcess();
    }

    public void xStop() {
        mStop = true;
    }

    private void sSetSysout() {
        File lFile = null;
        File lDir;
        int lCount;
        LocalDate lDate;
        String lBest;
        boolean lExists;

        lDir = new File("log");
        if (!lDir.exists()) {
            lDir.mkdir();
        }
        lCount = 0;
        lDate = LocalDate.now();
        lExists = true;
        while (lExists) {
            lBest = "LightSwitch_" + lDate.format(DateTimeFormatter.ISO_DATE) + "_" + String.format("%03d", lCount);
            lFile = new File(lDir, lBest);
            lExists = lFile.exists();
            lCount++;
        }
        try {
            System.setOut(new PrintStream(lFile));
            mSysoutDate = lDate;
        } catch (FileNotFoundException pExc) {
            mData.xWriteLog("Exception on sysout file: " + pExc.getMessage());
        }
    }

    private void sProcess() {
        List<Action> lActions;
        Action lAction;
        ZonedDateTime lNow;
        boolean lDoneSomething;

        mData.xWriteLog("Start background");
        while (!mStop) {
            lDoneSomething = false;
            lNow = ZonedDateTime.now();
            try {
                if (lNow.toLocalDate().isAfter(mSysoutDate)) {
                    sSetSysout();
                }

                lActions = mData.xActions();
                if (lActions.size() > 0) {
                    lAction = lActions.get(0);
                    if (lAction.xProcess() == null || lAction.xProcess().isBefore(lNow)) {
                        lDoneSomething = true;
                        sProcessAction(lAction);
                    }
                }

                if (mCurrent.xStartLightOn().isBefore(lNow)) {
                    lDoneSomething = true;
                    sTestOn();
                }

                if (mCurrent.xLightOff().isBefore(lNow)) {
                    lDoneSomething = true;
                    mCurrent.xLightOffProcessed();
                    mCurrent.xFase(Current.cFaseNight);
                    mData.xCurrent(mCurrent);
                    sSwitchAllOffAuto();
                }

                if (mCurrent.xUpdate().isBefore(lNow)) {
                    lDoneSomething = true;
                    sUpdate();
                }

                if (!lDoneSomething) {
                    Thread.sleep(100);
                }
            } catch (InterruptedException pExc) {
                mData.xWriteLog("InteruptException: " + pExc.getLocalizedMessage());
                mOnSensor.xClose();
                mData.xClose();
                mStop = true;
            }
        }
    }

    private void sTestOn() {
        boolean lOn;
        String lText;
        Setting lSetting;
        ZonedDateTime lOnTest;

        lSetting = mData.xSetting();

        if (ZonedDateTime.now().isAfter(mCurrent.xSunset())) {
            mData.xWriteLog("After sunset. Lights on.");
            lOn = true;
        } else {
            lOn = mOnSensor.xTestOn();
            lText = "Lightreading: " + mOnSensor.xLightReading();
            if (mOnSensor.xLightCount() > 0) {
                lText = lText + ". Dark enough. Counter: " + mOnSensor.xLightCount();
            }
            if (!lOn) {
                lOnTest = ZonedDateTime.now();
                if (mTest) {
                    if (mTestIntervalSec < 0) {
                        lOnTest = lOnTest.plusSeconds(lSetting.xPeriodSec());
                    } else {
                        lOnTest = lOnTest.plusSeconds(mTestIntervalSec);
                    }
                } else {
                    lOnTest = lOnTest.plusSeconds(lSetting.xPeriodSec());
                }
                lText = lText + ". Next test at " + lOnTest;
                mCurrent.xStartLightOn(lOnTest);
                mCurrent.xFase(Current.cFaseTwilight);
                mCurrent.xLightReading(mOnSensor.xLightReading());
                mData.xCurrent(mCurrent);
            }
            mData.xWriteLog(lText);
        }

        if (lOn) {
            mCurrent.xStartLightOnProcessed();
            mCurrent.xFase(Current.cFaseEvening);
            mData.xCurrent(mCurrent);
            sSwitchAllOnAuto();
        }
    }

    private void sUpdate() {
        Random lRnd;
        int lRndCorr;
        ZonedDateTime lSunset;
        Setting lSetting;
        int lDay;

        lSetting = mData.xSetting();

        mData.xWriteLog("Start update");

        lSunset = mSunset.xSunset(lSetting.xLattitude(), lSetting.xLongitude()).atZoneSameInstant(ZoneId.systemDefault());
        if (mTest) {
            if (mTestSunsetHour < 0) {
                mCurrent.xSunset(lSunset);
            } else {
                mCurrent.xSunset(ZonedDateTime.now().withHour(mTestSunsetHour).withMinute(mTestSunsetMin));
            }
        } else {
            mCurrent.xSunset(lSunset);
        }
        mCurrent.xSetStartLightOn();
        mOnSensor.xInit();
        mCurrent.xUpdate(ZonedDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0));
        mData.xWriteLog("Sunset at " + mCurrent.xSunset().toString());

        lRnd = new Random();
        lRndCorr = lRnd.nextInt(lSetting.xLightOffPeriod());
        if (lSetting.xLightOffHour() < 10) {
            lDay = 1;
        } else {
            lDay = 0;
        }
        mCurrent.xLightOff(ZonedDateTime.now().plusDays(lDay).withHour(lSetting.xLightOffHour()).withMinute(lSetting.xLightOffMin()).withSecond(0).withNano(0).plusMinutes(lRndCorr));
        mCurrent.xFase(Current.cFaseDay);
        mData.xCurrent(mCurrent);
        mData.xWriteLog("Lights off at " + mCurrent.xLightOff().toString());
        mData.xWriteLog("Next update " + mCurrent.xUpdate().toString());
    }

    private void sSwitchAll(boolean pOn) {
        List<Switch> lSwitches;
        Switch lSwitch;
        Transmitter lTrans;
        String lText;
        int lCount;

        if (pOn) {
            lText = "Lights on!";
        } else {
            lText = "Lights off!";
        }
        mData.xWriteLog(lText);
        lTrans = new Transmitter(true);
        lSwitches = mData.xSwitches();
        for (lCount = 0; lCount < lSwitches.size(); lCount++) {
            lSwitch = lSwitches.get(lCount);
            mData.xActionSwitchProcessed(lSwitch);
            if (lSwitch.xActive()){
                if (pOn){
                    lTrans.xSwitchOn(lSwitch, null);
                } else {
                    lTrans.xSwitchOff(lSwitch);
                }
            }
        }
    }

    private void sSwitchAllOnAuto() {
        List<Switch> lSwitches;
        Switch lSwitch;
        Transmitter lTrans;
        int lCount;
        ZonedDateTime lAutoOff;

        mData.xWriteLog("Lights on auto");
        lTrans = new Transmitter(true);
        lSwitches = mData.xSwitches();
        lAutoOff = mCurrent.xLightOff().plusHours(2);
        for (lCount = 0; lCount < lSwitches.size(); lCount++) {
            lSwitch = lSwitches.get(lCount);
            mData.xActionSwitchProcessed(lSwitch);
            if (lSwitch.xActive()) {
                sSwitchOnAuto(lTrans, lSwitch, lAutoOff);
            }
        }
    }

    private void sSwitchAllOffAuto() {
        List<Switch> lSwitches;
        Switch lSwitch;
        Transmitter lTrans;
        int lCount;
        ZonedDateTime lAutoEnd;

        mData.xWriteLog("Lights off auto");
        lTrans = new Transmitter(true);
        lSwitches = mData.xSwitches();
        lAutoEnd = ZonedDateTime.now().plusDays(1);
        for (lCount = 0; lCount < lSwitches.size(); lCount++) {
            lSwitch = lSwitches.get(lCount);
            mData.xActionSwitchProcessed(lSwitch);
            if (lSwitch.xActive()) {
                sSwitchOffAuto(lTrans, lSwitch, lAutoEnd);
            }
        }
    }

    private void sSwitchOnAuto(Transmitter pTrans, Switch pSwitch, ZonedDateTime pAutoOff) {
        boolean lResult;
        Action lCorrAction;
        ZonedDateTime lActionMoment;
        int lNumbError;
        int lInterval;

        if (pAutoOff.isAfter(ZonedDateTime.now().plusMinutes(15))) {
            lResult = pTrans.xSwitchOn(pSwitch, pAutoOff);
            EspStatus.xEspAction(pSwitch.xName(), (lResult) ? EspStatus.cOK : EspStatus.cNOK);
            if (!lResult) {
                lNumbError = EspStatus.xNumberError(pSwitch.xName());
                if (lNumbError > 10) {
                    if (lNumbError > 15) {
                        lInterval = 60;
                    } else {
                        lInterval = 10;
                    }
                } else {
                    lInterval = 1;
                }
                lActionMoment = ZonedDateTime.now().plusMinutes(lInterval);
                if (lActionMoment.isBefore(pAutoOff)) {
                    lCorrAction = new Action(lActionMoment, Action.cActionSwitchOn, pSwitch.xName(), pAutoOff);
                    mData.xNewAction(lCorrAction);
                }
            }
        }
    }

    private void sSwitchOffAuto(Transmitter pTrans, Switch pSwitch, ZonedDateTime pAutoEnd) {
        boolean lResult;
        Action lCorrAction;
        ZonedDateTime lActionMoment;
        int lNumbError;
        int lInterval;

        if (pAutoEnd.isAfter(ZonedDateTime.now())) {
            lResult = pTrans.xSwitchOff(pSwitch);
            EspStatus.xEspAction(pSwitch.xName(), (lResult) ? EspStatus.cOK : EspStatus.cNOK);
            if (!lResult) {
                lNumbError = EspStatus.xNumberError(pSwitch.xName());
                if (lNumbError > 10) {
                    if (lNumbError > 15) {
                        lInterval = 60;
                    } else {
                        lInterval = 10;
                    }
                } else {
                    lInterval = 1;
                }
                lActionMoment = ZonedDateTime.now().plusMinutes(lInterval);
                if (lActionMoment.isBefore(pAutoEnd)) {
                    lCorrAction = new Action(lActionMoment, Action.cActionSwitchOff, pSwitch.xName(), pAutoEnd);
                    mData.xNewAction(lCorrAction);
                }
            }
        }
    }

    private void sProcessAction(Action pAction) {
        ZonedDateTime lDateTime;
        Switch lSwitch;
        Transmitter lTrans;
        ZonedDateTime lEndTime;

        if (!pAction.xReady()) {
            switch (pAction.xType()) {
                case Action.cActionRefresh:
                    sUpdate();
                    break;
                case Action.cActionSwitchLightOff: {
                    try {
                        lDateTime = ZonedDateTime.parse(pAction.xPar(), DateTimeFormatter.ISO_ZONED_DATE_TIME);
                        mCurrent.xLightOff(lDateTime);
                        if (mCurrent.xFase() == Current.cFaseNight) {
                            mCurrent.xFase(Current.cFaseEvening);
                        }
                        mData.xCurrent(mCurrent);
                        mData.xWriteLog("Switch light off at " + lDateTime.toString());
                    } catch (DateTimeParseException pExc) {
                        mData.xWriteLog("Invalid date/time " + pAction.xPar() + ", Action not executed");
                    }
                    break;
                }
                case Action.cActionSwitchOn: {
                    lTrans = new Transmitter(false);
                    lSwitch = mData.xSwitch(pAction.xSwitch());
                    mData.xActionSwitchProcessed(lSwitch);
                    if (pAction.xPar().equals("")) {
                        lTrans.xSwitchOn(lSwitch, null);
                        mData.xWriteLog("Switch on " + pAction.xSwitch() + "!");
                    } else {
                        try {
                            lEndTime = ZonedDateTime.parse(pAction.xPar(), DateTimeFormatter.ISO_ZONED_DATE_TIME);
                            sSwitchOnAuto(lTrans, lSwitch, lEndTime);
                            mData.xWriteLog("Switch on auto " + pAction.xSwitch() + "!");
                        } catch (DateTimeParseException pExc) {
                            mData.xWriteLog("Invalid AutoOff time " + pAction.xPar() + ", Action not executed");
                        }
                    }
                    break;
                }
                case Action.cActionSwitchOff: {
                    lTrans = new Transmitter(false);
                    lSwitch = mData.xSwitch(pAction.xSwitch());
                    mData.xActionSwitchProcessed(lSwitch);
                    if (pAction.equals("")) {
                        lTrans.xSwitchOff(lSwitch);
                        mData.xWriteLog("Switch off " + pAction.xSwitch() + "!");
                    } else {
                        try {
                            lEndTime = ZonedDateTime.parse(pAction.xPar(), DateTimeFormatter.ISO_ZONED_DATE_TIME);
                            sSwitchOffAuto(lTrans, lSwitch, lEndTime);
                            mData.xWriteLog("Switch off auto " + pAction.xSwitch() + "!");
                        } catch (DateTimeParseException pExc) {
                            mData.xWriteLog("Invalid End Retry time " + pAction.xPar() + ", Action not executed");
                        }
                    }
                    break;
                }
                case Action.cActionSwitchAllOn: {
                    sSwitchAll(true);
                    break;
                }
                case Action.cActionSwitchAllOff: {
                    sSwitchAll(false);
                    break;
                }
                case Action.cActionNone: {
                    mData.xWriteLog("Dummy action. Done nothing!");
                    break;
                }
                default: {
                    mData.xWriteLog("Unknown action " + pAction.xType() + ". Done nothing!");
                    break;
                }
            }
            mData.xActionProcessed(pAction);
        }
    }
}
