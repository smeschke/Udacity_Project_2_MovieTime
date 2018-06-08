package com.example.stephen.movietime;

import android.content.res.Resources;

public class ViewUtils {

    public static int get_width(){
        int width = Resources.getSystem().getDisplayMetrics().widthPixels; //screen width
        int display_size = 92; //smallest size
        if (width / 2 > 154) {
            display_size = 154;
        }
        if (width / 2 > 185) {
            display_size = 180;
        }
        if (width / 2 > 342) {
            display_size = 342;
        }
        if (width / 2 > 500) {
            display_size = 500;
        }
        if (width / 2 > 780) {
            display_size = 780;
        }
        return display_size;
    }

}
