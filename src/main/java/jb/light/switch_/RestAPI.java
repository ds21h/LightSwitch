package jb.light.switch_;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;

/**
 * Created by Jan on 17-9-2015.
 */
public class RestAPI {
    public static final String cMethodGet = "GET";
    public static final String cMethodPut = "PUT";

    public static final String cMediaJSON = "application/json";
    public static final String cMediaText = "text/plain";

    private String mMethod;
    private String mMediaRequest;
    private String mMediaReply;
    private String mUrl;
    private String mAction;

    public RestAPI(){
        mMethod = cMethodGet;
        mMediaRequest = cMediaText;
        mMediaReply = cMediaJSON;
        mUrl = "";
        mAction = "";
    }

    public void xMethod(String pMethod){
        mMethod = pMethod;
    }

    public void xMediaRequest(String pMedia){
        mMediaRequest = pMedia;
    }

    public void xMediaReply(String pMedia){
        mMediaReply = pMedia;
    }

    public void xUrl(String pUrl){
        mUrl = pUrl;
    }

    public void xAction(String pAction){
        mAction = pAction;
    }
    public RestResult xCallApi(){
        String lOutput;
        StringBuilder lStr;
        String lUrlS;
        String lAction;
        URL lUrl;
        HttpURLConnection lConn = null;
        DataOutputStream lOutStream;
        BufferedReader lBufRead;
        String lLine;
        int lRespCode;
        RestResult lRestResult;

        lStr = new StringBuilder();
        if (mMethod.equals(cMethodGet)){
            if (!mAction.equals("")){
                lUrlS = mUrl + "?" + mAction;
                lAction = "";
            } else {
                lUrlS = mUrl;
                lAction = "";
            }
        } else {
            lUrlS = mUrl;
            lAction = mAction;
        }
        try {
            lUrl = new URL(lUrlS);
            lConn = (HttpURLConnection) lUrl.openConnection();
            lConn.setRequestMethod(mMethod);
            lConn.setRequestProperty("Accept", mMediaReply);
            lConn.setConnectTimeout(5000);
            lConn.setReadTimeout(5000);
            if (!lAction.equals("")) {
                lConn.setRequestProperty("Content-Type", mMediaRequest);
                lConn.setDoOutput(true);
                lOutStream = new DataOutputStream(lConn.getOutputStream());
                lOutStream.writeBytes(lAction);
                lOutStream.flush();
                lOutStream.close();
            }

            lRespCode = lConn.getResponseCode();
            if (lRespCode == 200) {
                lBufRead = new BufferedReader(new InputStreamReader(
                        (lConn.getInputStream())));
                while ((lLine = lBufRead.readLine()) != null) {
                    lStr.append(lLine);
                }
                lOutput = lStr.toString();
                lRestResult = new RestResult(Result.cResultOK, lOutput);
            } else {
                lRestResult = new RestResult("Wrong responsecode: " + lRespCode, Result.cResultError);
            }
        } catch (MalformedURLException e) {
            lRestResult = new RestResult("URL malformed: " + e.getLocalizedMessage(), Result.cResultError);
        } catch (SocketTimeoutException pExc){
            if (pExc.getLocalizedMessage() == null){
                lRestResult = new RestResult("Read Time-Out", Result.cResultReadTimeOut);
            } else {
                lRestResult = new RestResult("Connect Time-Out", Result.cResultConnectTimeOut);
            }
        } catch (IOException e) {
            lRestResult = new RestResult("IO Exception:" + e.getLocalizedMessage(), Result.cResultError);
        } finally {
            if (lConn != null){
                lConn.disconnect();
            }
        }
        return lRestResult;
    }

    public class RestResult {
        private int mResult;
        private String mText;
        private JSONObject mReplyJ;
        private String mReplyS;
        private boolean mJson;

        public int xResult(){
            return mResult;
        }

        public String xText() {
            return mText;
        }

        public JSONObject xReplyJ(){
            if (mJson){
                return mReplyJ;
            } else {
                return null;
            }
        }

        public String xReplyS(){
            return mReplyS;
        }

        public RestResult(int pResult, String pOutput){
            mReplyS = pOutput;
            if (mMediaReply.equals(cMediaJSON)){
                try {
                    mReplyJ = new JSONObject(pOutput);
                    mResult = pResult;
                    mText = "OK";
                    mJson = true;
                }catch (JSONException pExc){
                    mResult = Result.cResultOutputError;
                    mText = "No valid JSON Object received";
                    mReplyJ = null;
                    mJson = false;
                }
            } else {
                mReplyJ = null;
                mResult = Result.cResultOK;
                mText = "OK";
                mJson = false;
            }
        }

        public RestResult(String pMelding, int pResult){
            mReplyJ = null;
            mReplyS = null;
            mResult = pResult;
            mText = pMelding;
        }
    }
}
