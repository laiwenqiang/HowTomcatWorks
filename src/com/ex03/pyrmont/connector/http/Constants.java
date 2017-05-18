package com.ex03.pyrmont.connector.http;

import java.io.File;

/**
 * Created by laiwenqiang on 2017/5/18.
 */
public class Constants {
    public static final String WEB_ROOT =
            System.getProperty("user.dir") + File.separator  + "webroot";
    public static final String Package = "com.ex03.pyrmont.connector.http";
    public static final int DEFAULT_CONNECTION_TIMEOUT = 60000;
    public static final int PROCESSOR_IDLE = 0;
    public static final int PROCESSOR_ACTIVE = 1;
}
