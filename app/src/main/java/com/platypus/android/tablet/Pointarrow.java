package com.platypus.android.tablet;

/**
 * Created by zeshengxi on 3/7/16.
 */
public class Pointarrow {

    public int getIcon (float degree){
        int Index = 0;
        // 360/8=45, each Icon covers 45 degree
        if((degree >= 0 & degree < 22.5) || (degree <= 360 & degree > 337.5) ){
            Index = 0;
        }
        else if (degree >= 22.5 & degree < 67.5){
            Index = 1;
        }
        else if (degree >= 67.5 & degree < 112.5){
            Index = 2;
        }
        else if (degree >= 112.5 & degree < 157.5){
            Index = 3;
        }
        else if (degree >= 157.5 & degree < 202.5){
            Index = 4;
        }
        else if (degree >= 202.5 & degree < 247.5){
            Index = 5;
        }
        else if (degree >= 247.5 & degree < 292.5){
            Index = 6;
        }
        else if (degree >= 292.5 & degree <= 337.5){
            Index = 7;
        }

        return Index;
    }
}
