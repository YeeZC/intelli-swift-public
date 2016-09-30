package com.fr.bi.stable.utils.code;

import com.fr.bi.manager.PerformancePlugManager;
import com.fr.bi.stable.utils.time.BIDateUtils;

/**
 * BI日志输出
 */
public class BILogger {
    boolean verbose = true;
    public static BILogger logger = null;

    public static BILogger getLogger() {
        if (logger != null) {
            return logger;
        }
        synchronized (BILogger.class) {
            if (logger == null) {
                logger = new BILogger();
            }
        }
        return logger;
    }

    public void error(String message) {
        System.err.println(message);

        errorOut(message);
    }

    public void error(String message, Throwable e) {
        System.err.println(message);
        e.printStackTrace();
        errorOut(message);
        String out = BIPrintUtils.outputException(e);
        errorOut(out);
    }

    private void errorOut(String message) {
        if (PerformancePlugManager.getInstance().useStandardOutError()) {
            System.out.println(message);
        } else {
            System.err.println(message);
        }
    }

    public void info(String message) {
        System.out.println(BIDateUtils.getCurrentDateTime() + ": " + message);
    }

    public void debug(String message) {
        if (verbose) {
            System.out.println(BIDateUtils.getCurrentDateTime() + ": " + message);
        }
    }
}
