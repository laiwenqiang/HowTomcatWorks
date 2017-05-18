package com.ex03.pyrmont.startup;

import com.ex03.pyrmont.connector.http.HttpConnector;

/**
 * Created by laiwenqiang on 2017/5/18.
 */
public class BootStartup {
    public static void main(String[] args) {
        HttpConnector connector = new HttpConnector();
        connector.start();
    }


}
