package com.aceft.data;

import android.app.Activity;
import android.graphics.Point;
import android.support.annotation.Nullable;
import android.view.Display;

public final class LayoutTasks {

    private LayoutTasks() {
    }

    public static int getWindowWidth(Activity a) {
        int width;
        Display display = a.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        width = size.x;
        return width;
    }

    public static String formatNumber(String number) {
        if (number.length() > 3)
            number = number.substring(0,number.length()-3) + "," + number.substring(number.length()-3);
        if (number.length() > 7)
            number = number.substring(0,number.length()-7) + "," + number.substring(number.length()-7);
        return number;
    }

    public static String formatNumber(int number) {
        String s = Integer.toString(number);
        return formatNumber(s);
    }

    public static String formatTime(int hour, int minutes) {
        String sHour = hour < 10 ? "0" + hour : "" + hour;
        String sMinute = minutes < 10 ? "0" + minutes : "" + minutes;
        return  sHour + ":" + sMinute;
    }

    public static int[] stringTimeToInt(String s) {
        if (s.isEmpty()) return  new int[]{0, 0};
        int hour = Integer.parseInt(s.substring(0,2));
        int min = Integer.parseInt(s.substring(3,5));
        return new int[]{hour, min};
    }

    public static boolean timeBetween(int fh, int fm, int uh, int um, int h, int m) {
        if (fh < uh) {
            if (h < fh || h > uh) return false;
            if (h == fh && m < fm) return false;
            if (h == uh && m > um) return false;
            return true;
        } else {
            if (h > fh || h < uh) return true;
            if (h == fh && m >= fm) return true;
            if (h == uh && m <= um) return true;
            return false;
        }
    }
}
