package com.dici.process;

public class ProcessUtils {
    public static void killProcess(Process proc) { if (proc != null) proc.destroyForcibly(); }
}
