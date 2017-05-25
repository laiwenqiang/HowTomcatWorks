package com.ext04.pyrmont.startup;

import com.ext04.pyrmont.core.SimpleContainer;
import org.apache.catalina.connector.http.HttpConnector;


/**
 * Created by laiwenqiang on 2017/5/25.
 */
public class BootStrap {
    public static void main(String[] args) {
        HttpConnector connector = new HttpConnector();
        SimpleContainer container = new SimpleContainer();
        connector.setContainer(container);

        try {
            connector.initialize();
            connector.start();

            // 加上这句话，防止main方法退出。
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
