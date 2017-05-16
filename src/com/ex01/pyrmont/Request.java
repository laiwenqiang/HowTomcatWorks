package com.ex01.pyrmont;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by laiwenqiang on 2017/5/16.
 */
public class Request {
    private InputStream inputStream;
    private String uri;

    public Request(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    //从输入流中获取uri
    public void parse() {
        StringBuffer request = new StringBuffer(2048);
        int i;
        byte[] buffer = new byte[2048];
        try {
            i = inputStream.read(buffer);
        } catch (IOException e) {
            e.printStackTrace();
            //i = -1时，下面for循环条件则不会成立。
            i = -1;
        }
        for(int j=0; j<i; j++){
            request.append((char)buffer[j]);
        }
        System.out.println(request.toString());
        uri = parseUri(request.toString());
    }

    public String getUri() {
        return uri;
    }

    //获取Uri
    private String parseUri(String requestString){
        int index1,index2;
        index1 = requestString.indexOf(' ');
        if(index1 != -1){
            index2 = requestString.indexOf(' ', index1 + 1);
            if(index2 > index1){
                return requestString.substring(index1 + 1, index2);
            }
        }
        return null;
    }

}
