package com.ex03.pyrmont.connector;

import com.ex03.pyrmont.connector.http.*;

import javax.servlet.Servlet;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandler;

/**
 * Created by laiwenqiang on 2017/5/16.
 */
public class ServletProcessor {
    //接收request，处理业务后，返回response
    public void process(HttpRequest request, HttpResponse response) {

        //1. 获取Servlet的名字

        String uri = request.getRequestURI();
        // 将“/servlet/servletName” 转化为： “servletName”
        String servletName = uri.substring(uri.lastIndexOf("/" ) + 1);

        //2. 使用类加载器加载Servlet

        URLClassLoader loader = null;
        URL[] urls = new URL[1];
        URLStreamHandler handler = null;
        File classPath = new File(Constants.WEB_ROOT);

        try {
            String repository = (new URL("file", null, classPath.getCanonicalPath() +
                    File.separator)).toString();
            urls[0] = new URL(null, repository, handler);
            loader = new URLClassLoader(urls);

        } catch (IOException e) {
            e.printStackTrace();
        }

        Class myClass = null;
        try {
            myClass = loader.loadClass(servletName);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        //这里和ServletProcessor1不同，运用外观设计模式
        Servlet servlet = null;
        HttpRequestFacade requestFacade = new HttpRequestFacade(request);
        HttpResponseFacade responseFacade = new HttpResponseFacade(response);

        try {
            servlet = (Servlet) myClass.newInstance();

            //3. 调用Servlet的service方法

            servlet.service(requestFacade, responseFacade);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
