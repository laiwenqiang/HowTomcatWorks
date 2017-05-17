package com.ex02.pyrmont;

/**
 * Created by laiwenqiang on 2017/5/17.
 */
public class StaticResourceProcessor {
    public void process(Request request, Response response) {
        try {
            response.sendStaticResource();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
