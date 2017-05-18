package com.ex03.pyrmont.connector;


import com.ex03.pyrmont.connector.http.HttpRequest;
import com.ex03.pyrmont.connector.http.HttpResponse;

/**
 * Created by laiwenqiang on 2017/5/18.
 */
public class StaticResourceProcessor {
    public void process(HttpRequest request, HttpResponse response) {
        try {
            response.sendStaticResource();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
