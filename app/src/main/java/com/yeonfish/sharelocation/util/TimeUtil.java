package com.yeonfish.sharelocation.util;

import java.util.Timer;
import java.util.TimerTask;

public class TimeUtil {
    public static Timer setInterval(TimerTask task, long time) {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(task,0L,time);
        return timer;
    }
    public static void setTimeout(Runnable task, int time) {
        new android.os.Handler().postDelayed(task, time);
    }
}
