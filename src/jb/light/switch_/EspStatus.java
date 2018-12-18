/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jb.light.switch_;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Jan
 */
public class EspStatus {
    public static final int cOK = 0;
    public static final int cNOK = 9;
    
    private static List<String> mSwitch;
    private static List<Integer> mNumberError;
    
    static {
        mSwitch = new ArrayList<>();
        mNumberError = new ArrayList<>();
    }
    
    private EspStatus(){}
    
    public static void xEspAction(String pSwitch, int pResult){
        int lCount;
        String lSwitch;
        Integer lNumberError;
        boolean lFound;
        
        lFound = false;
        for (lCount = 0; lCount < mSwitch.size(); lCount++){
            lSwitch = mSwitch.get(lCount);
            if (lSwitch.equals(pSwitch)){
                lFound = true;
                lNumberError = mNumberError.get(lCount);
                if (pResult == cOK){
                    lNumberError = 0;
                } else {
                    lNumberError++;
                }
                mNumberError.set(lCount, lNumberError);
                break;
            }
        }
        if (!lFound){
            mSwitch.add(pSwitch);
            mNumberError.add((pResult == cOK) ? 0 : 1);
        }
    }
    
    public static int xNumberError(String pSwitch){
        int lCount;
        String lSwitch;
        Integer lNumberError;

        lNumberError = 0;
        for (lCount = 0; lCount < mSwitch.size(); lCount++){
            lSwitch = mSwitch.get(lCount);
            if (lSwitch.equals(pSwitch)){
                lNumberError = mNumberError.get(lCount);
                break;
            }
        }
        return lNumberError;
    }
}
