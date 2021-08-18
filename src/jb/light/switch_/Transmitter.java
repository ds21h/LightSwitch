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

    public Transmitter(boolean pWithPause) {
        mWithPause = pWithPause;
    }

    public void xSwitch(Switch pSwitch, boolean pOn) {
        if (pSwitch.xActive()) {
            if (pOn){
                xSwitchOn(pSwitch, null);
            } else {
                xSwitchOff(pSwitch);
            }
        }
    }

    boolean xSwitchOn(Switch pSwitch, ZonedDateTime pAutoOff){
        RestAPI lRestAPI;
        JSONObject lRequest;
        int lAutoOffSec;
        String lUrl;
        RestAPI.RestResult lResult;
        boolean lError;
        JSONObject lAnswer;
        String lResultStr;
        String lStatus;

        if (mWithPause) {
            try {
                Thread.sleep(pSwitch.xPause());
            } catch (InterruptedException pExc) {
            }
        }

        lUrl = "http://" + pSwitch.xIP() + "/Switch";
        lRequest = new JSONObject();
        lRequest.put("status", "on");
        if (pAutoOff != null){
            lAutoOffSec = (int)(pAutoOff.toEpochSecond() - ZonedDateTime.now().toEpochSecond());
            if (lAutoOffSec > 0){
                lRequest.put("auto-off", lAutoOffSec);
            }
        }
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
                    if (lStatus.equals("on")) {
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
        return !lError;
    }
    
    boolean xSwitchOff(Switch pSwitch){
        RestAPI lRestAPI;
        JSONObject lRequest;
        String lUrl;
        RestAPI.RestResult lResult;
        boolean lError;
        JSONObject lAnswer;
        String lResultStr;
        String lStatus;

        if (mWithPause) {
            try {
                Thread.sleep(pSwitch.xPause());
            } catch (InterruptedException pExc) {
            }
        }

        lUrl = "http://" + pSwitch.xIP() + "/Switch";
        lRequest = new JSONObject();
        lRequest.put("status", "off");
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
                    if (lStatus.equals("off")) {
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
        return !lError;
    }
}
