/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jb.light.switch_;

/**
 * Sole purpose is to start this function as a daemon (Linux service).
 * 
 * For the functionality mainline see class Control
 */
public class SwitchLight {

    private static final Control mControl = new Control();

    public static void main(String[] args) {
        Thread lRunHook;
        String lArg;
        String lParam;
        String lHourS;
        String lSep;
        String lMinS;
        int lHour = 0;
        int lMin = 0;
        boolean lArgOK = false;

        if (args.length > 0) {
            lArg = args[0].trim();
            System.out.println("Received parameter: " + lArg);
            if (lArg.length() == 12) {
                lParam = lArg.substring(0, 7);
                lHourS = lArg.substring(7, 9);
                lSep = lArg.substring(9, 10);
                lMinS = lArg.substring(10);

                if (lParam.equals("Sunset=")) {
                    if (lSep.equals(":")) {
                        try {
                            lHour = Integer.parseInt(lHourS);
                            lMin = Integer.parseInt(lMinS);
                            lArgOK = true;
                        } catch (NumberFormatException pExc) {

                        }
                    }
                }
            }
            if (lArgOK) {
//                mControl.xStart(lHour, lMin, 0, 15);
                mControl.xStart(lHour, lMin, 15);
            } else {
                System.out.println("Correct format: Sunset=hh:mm");
            }
        } else {

            lRunHook = new Thread() {
                @Override
                public void run() {
                    sShutDown();
                }
            };
            Runtime.getRuntime().addShutdownHook(lRunHook);

            mControl.xStart();
        }
    }

    private static void sShutDown() {
        mControl.xStop();
    }
}
