/*
 *  Copyright (C) 2019 Docobo Ltd - All Rights Reserved
 *
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 */

package com.docobo.device;

import android.os.Build;

/**
 * <b>Class PlatformInfo</b>
 * <br><br>
 * TODO - Add class definition here.
 */
public class PlatformInfo {

    public static boolean isDocoboDevice() {
        return "DOCOBO".equalsIgnoreCase(Build.MANUFACTURER);
    }
}
