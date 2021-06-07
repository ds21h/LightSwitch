/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jb.light.switch_;

import java.time.ZonedDateTime;
import jb.light.support.Action;
import jb.light.support.Data;
import jb.light.support.Switch;
import org.json.JSONObject;

/**
 *
 * @author Jan
 */
public class Transmitter {

    private final boolean mWithPause;
//    private final boolean mIoOn;
    private final Data mData;

    public Transmitter(Data pData, boolean pIoOn, boolean pWithPause) {
        mData = pData;
//        mIoOn = pIoOn;
        mWithPause = pWithPause;
    }

    public void xSwitch(Switch pSwitch, boolean pOn) {
        if (pSwitch.xActive()) {
                sSwitchIot(pSwitch, pOn);
//            if (pSwitch.xType().equals("esp")) {
//                sSwitchIot(pSwitch, pOn);
//            } else {
//                if (mIoOn) {
//                    sSwitchFM(pSwitch, pOn);
//                }
//            }
        }
    }

//    private void sSwitchFM(Switch pSwitch, boolean pOn) {
//        Runtime lRun;
//        Process lProcess;
//        String lCommand;
//        String lAction;
//
//        if (mWithPause) {
//            try {
//                Thread.sleep(pSwitch.xPause());
//            } catch (InterruptedException pExc) {
//            }
//        }
//
//        lRun = Runtime.getRuntime();
//        if (pOn) {
//            lAction = "on";
//        } else {
//            lAction = "off";
//        }
//
//        lCommand = "/usr/local/licht/schakelen/" + pSwitch.xType() + " " + pSwitch.xGroup() + " " + pSwitch.xPoint() + " " + lAction;
//
//        try {
//            lProcess = lRun.exec(lCommand);
//            lProcess.waitFor();
//            Thread.sleep(1000);
//            lProcess = lRun.exec(lCommand);
//            lProcess.waitFor();
//        } catch (IOException | InterruptedException pExc) {
//        }
//    }

    private void sSwitchIot(Switch pSwitch, boolean pOn) {
        RestAPI lRestAPI;
        JSONObject lRequest;
        String lAction;
        String lUrl;
        RestAPI.RestResult lResult;
        boolean lError;
        JSONObject lAnswer;
        String lResultStr;
        String lStatus;
        Action lCorrAction;
        String lActionType;
        ZonedDateTime lActionMoment;
        int lNumbError;
        int lInterval;

        if (mWithPause) {
            try {
                Thread.sleep(pSwitch.xPause());
            } catch (InterruptedException pExc) {
            }
        }

        lUrl = "http://" + pSwitch.xIP() + "/Switch";
        if (pOn) {
            lAction = "on";
        } else {
            lAction = "off";
        }
        lRequest = new JSONObject();
        lRequest.put("status", lAction);
        lRestAPI = new RestAPI();
        lRestAPI.xUrl(lUrl);
        lRestAPI.xMethod(RestAPI.cMethodPut);
        lRestAPI.xMediaRequest(RestAPI.cMediaJSON);
        lRestAPI.xMediaReply(RestAPI.cMediaJSON);
        lRestAPI.xAction(lRequest.toString());

        lResult = lRestAPI.xCallApi();

        if (lResult.xResult() == Result.cResultOK) {
            if (lResult.xReplyJ() == null) {
                lError = true;
            } else {
                lAnswer = lResult.xReplyJ();
                lResultStr = lAnswer.optString("result", "");
                lStatus = lAnswer.optString("status", "");
                if (lResultStr.equals("OK")) {
                    if (lStatus.equals(lAction)) {
                        lError = false;
                    } else {
                        lError = true;
                    }
                } else {
                    lError = true;
                }
            }
        } else {
            lError = true;
        }

        EspStatus.xEspAction(pSwitch.xName(), (lError) ? EspStatus.cNOK : EspStatus.cOK);
        if (lError) {
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
            if (pOn) {
                lActionType = Action.cActionSwitchOn;
            } else {
                lActionType = Action.cActionSwitchOff;
            }
            lActionMoment = ZonedDateTime.now().plusMinutes(lInterval);
            lCorrAction = new Action(lActionMoment, lActionType, pSwitch.xName());
            mData.xNewAction(lCorrAction);
        }
    }
}
