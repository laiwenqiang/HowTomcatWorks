> 这本书(How Tomcat Works [中文下载地址](https://github.com/laiwenqiang/HowTomcatWorks/blob/master/doc/how%20tomcat%20works中文版.pdf))之前就看过，然而也就开了个头就废弃了，所以一直耿耿于怀。这次决定重新开始，在此记录一些学习心得和本书的主要知识点。所有代码也将托管在[GitHub](https://github.com/laiwenqiang/HowTomcatWorks)上面。O(∩_∩)O

# **上节回顾**
在上面一章节[HowTomcatWorks学习笔记--一个简单的Web容器](http://blog.csdn.net/laiwenqiang/article/details/72265716)中，
1. 运用Socket和ServerSocket，
2. 并且在遵循HTTP协议的规范下，创建了一个简单的Web容器。

<font color=red>**该容器的主要工作流程是，**</font>

1. 用ServerSocket创建服务端，启动8080端口监听请求，
2. 客户端一旦发送Socket请求，
3. 解析HTTP Request，获取URI路径。
4. 依据URI寻找资源，
5. 如果存在该资源，按照HTTP Response规范返回内容；
6. 如果不存在，按照HTTP Response规范返回404。

这个容器还抽象出了Request和Response类，用来处理消息。然而，只能返回本地文件内容，不能做业务逻辑处理。并且，我们是用自己的HttpServer这个类来直接操作，显然不符合设计规范。因此，引入Servlet这个概念，用来处理业务逻辑。

---
# **概要**
本章节，主要通过两个程序来开发自己的Servlet容器。
1. 第一个简单，用于理解一个Servlet容器的工作原理。
2. 第二个稍微复杂。

---

# **javax.servlet.Servlet接口**

谈到Servlet，自然就要运用到``` javax.servlet.Servlet ```接口。我们接下来用到的Servlet就要实现这个接口，该接口拥有五个方法。

1. ``` public void init(ServletConfig config) throws ServletException ```
2. ``` public void service(ServletRequest request, ServletResponse response) 
		 throws ServletException, java.io.IOException ```
3. ``` public void destroy() ```
4. ``` public ServletConfig getServletConfig() ```
5. ``` public java.lang.String getServletInfo() ```


## **Servlet生命周期** ##
一个老生常谈的话题了。
简要来说，就是：
<font color=red>init初始化 ---> service处理请求 ---> destroy销毁。</font>

在处理service请求的时候，**Servlet容器**会传递一个``` javax.servlet.ServletRequest ```对象和 ``` javax.servlet.ServletResponse ``` 对象，用来处理请求和相应。在一个Servlet生命周期里，service方法会被多次调用。

当从服务器中移除一个Servlet实例的时候，会调用destroy方法。
这通常发生在 servlet 容器正在被关闭或者 servlet 容器需要一些空闲内存的时候。

具体点，就是：
<font color=red>
1. 客户端发送对某个Servlet的请求，
2. 如果服务器里没有该Servlet实例，会调用该Servlet的init方法进行初始化；
3. 如果有，init方法不被调用。
3. 处理请求，调用service方法。返回结果。
4. 如果Servlet容器（服务器）被关闭，或者内存不足，会调用Servlet的destroy方法。
</font>

这里的PrimitiveServlet类，就是一个很简单的Servlet实例。
``` java
package com.ex02.pyrmont;

import javax.servlet.*;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Created by laiwenqiang on 2017/5/17.
 */
public class PrimitiveServlet implements Servlet {

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        System.out.println("init");
    }

    @Override
    public ServletConfig getServletConfig() {
        return null;
    }

    @Override
    public void service(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException, IOException {
        System.out.println("from service");
        PrintWriter out = servletResponse.getWriter();
		
        //由于浏览器无法解析出相应，故添加如下一段代码。原因未知。
        String msg = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html\r\n" +
                "\r\n";
        out.write(msg);
        
        out.write("Hello. Rose are red.");
        out.write("Violets are blue.");
		out.close();
    }

    @Override
    public String getServletInfo() {
        return null;
    }

    @Override
    public void destroy() {
        System.out.println("destroy");
    }
}
```
---
# **Servlet容器的工作内容** #
对Servlet的操作需要由Servlet容器来完成。<font color=red>Servlet容器的工作内容（和Servlet的生命周期是对应的），</font>

1. 当第一次调用Servlet的时候，加载该Servlet并调用init方法。
2. 对每次请求，都会构造一个``` javax.servlet.ServletRequest ```和``` javax.servlet.ServletResponse ```。
3. 调用Servlet的service方法，同时传递ServletRequest和ServletResponse对象。
4. 当Servlet类被关闭的时候，调用destroy方法并且卸载Servlet类。

# **Servlet容器的简单版本01** #
现在创建的这个容器相对简单，很简单。没有实现Servlet容器的所有工作。
<font color=blue>我们会发现，它和第一章实现的web服务器很类似，不过现在能够处理本地文件和Servlet。</font>内容是，

1. 等待HTTP请求。
2. 构造一个ServletRequest和ServletResponse对象。
3. 如果请求的是一个静态资源（本地文件），调用StaticResourceProcessor实例的process 方法，同时传递 ServletRequest 和 ServletResponse 对象。
4. 如果请求一个Servlet的话，加载Servlet类并调用service方法，同时传递 ServletRequest 和 ServletResponse 对象。

在这个容器中，每次请求Servlet的时候，都会加载Servlet。（真实情况是只会加载一次）

## **创建容器**
和之前的区别就是增加了对请求的判断，

1. 如果URI是以``` /servlet/ ```开头，则会加载相应的Servlet，调用相应的service方法。
2. 如果不是，表示请求的是静态资源，读取内容后直接方法。

处理请求的时候，都会传递Request和Response给对应的处理方法。
``` java
package com.ex02.pyrmont;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by laiwenqiang on 2017/5/17.
 */
public class HttpServer1 {
    private static final String SHUTDOWN_COMMAND = "/SHUTDOWN";
    private boolean shutdown = false;

    public static void main(String[] args) {
        HttpServer1 server = new HttpServer1();
        server.await();
    }

    private void await() {
        ServerSocket serverSocket = null;
        int port = 8080;

        try {
            serverSocket = new ServerSocket(port, 1, InetAddress.getByName("127.0.0.1"));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        while (!shutdown) {
            Socket socket = null;
            InputStream inputStream = null;
            OutputStream outputStream = null;

            try {
                socket = serverSocket.accept();
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();

                Request request = new Request(inputStream);
                request.parse();
                Response response = new Response(outputStream);
                response.setRequest(request);

                if(request.getUri().startsWith("/servlet/")) {
                    ServletProcessor1 processor = new ServletProcessor1();
                    processor.process(request, response);
                } else {
                    StaticResourceProcessor processor = new StaticResourceProcessor();
                    processor.process(request, response);
                }

                socket.close();
                shutdown = request.getUri().equals(SHUTDOWN_COMMAND);


            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
    }
}
```
## **创建请求Request** ##
和之前的主要区别就是，

1. 实现了``` ServletRequest ```接口。
``` java
package com.ex02.pyrmont;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

/**
 * Created by laiwenqiang on 2017/5/17.
 */
public class Request implements ServletRequest {

    private InputStream inputStream;
    private String uri;

    public Request(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public String getUri() {
        return uri;
    }

    public void parse() {
        StringBuilder request = new StringBuilder(2048);
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

    @Override
    public Object getAttribute(String s) {
        return null;
    }

    @Override
    public Enumeration getAttributeNames() {
        return null;
    }

    @Override
    public String getCharacterEncoding() {
        return null;
    }

    @Override
    public void setCharacterEncoding(String s) throws UnsupportedEncodingException {

    }

    @Override
    public int getContentLength() {
        return 0;
    }

    @Override
    public String getContentType() {
        return null;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        return null;
    }

    @Override
    public String getParameter(String s) {
        return null;
    }

    @Override
    public Enumeration getParameterNames() {
        return null;
    }

    @Override
    public String[] getParameterValues(String s) {
        return new String[0];
    }

    @Override
    public Map getParameterMap() {
        return null;
    }

    @Override
    public String getProtocol() {
        return null;
    }

    @Override
    public String getScheme() {
        return null;
    }

    @Override
    public String getServerName() {
        return null;
    }

    @Override
    public int getServerPort() {
        return 0;
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return null;
    }

    @Override
    public String getRemoteAddr() {
        return null;
    }

    @Override
    public String getRemoteHost() {
        return null;
    }

    @Override
    public void setAttribute(String s, Object o) {

    }

    @Override
    public void removeAttribute(String s) {

    }

    @Override
    public Locale getLocale() {
        return null;
    }

    @Override
    public Enumeration getLocales() {
        return null;
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String s) {
        return null;
    }

    @Override
    public String getRealPath(String s) {
        return null;
    }

}
```

## **创建相应Response** ##
和Request类似。
``` java
package com.ex02.pyrmont;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import java.io.*;
import java.util.Locale;

/**
 * Created by laiwenqiang on 2017/5/17.
 */
public class Response implements ServletResponse {

    private static final int BUFFER_SIZE = 1024;
    Request request;
    OutputStream output;
    PrintWriter writer;

    public Response(OutputStream output) {
        this.output = output;
    }

    public void setRequest(Request request) {
        this.request = request;
    }

    /* This method is used to serve a static page */
    public void sendStaticResource() throws IOException {
        byte[] bytes = new byte[BUFFER_SIZE];
        FileInputStream fis = null;
        try {
      /* request.getUri has been replaced by request.getRequestURI */
            File file = new File(Constants.WEB_ROOT, request.getUri());
            fis = new FileInputStream(file);
      /*
         HTTP Response = Status-Line
           *(( general-header | response-header | entity-header ) CRLF)
           CRLF
           [ message-body ]
         Status-Line = HTTP-Version SP Status-Code SP Reason-Phrase CRLF
      */
            int ch = fis.read(bytes, 0, BUFFER_SIZE);

            //源代码中没有下面这段话，导致请求会出现错误。现在添加。
            String msg = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: text/html\r\n" +
                    "\r\n";
            output.write(msg.getBytes());

            while (ch!=-1) {
                output.write(bytes, 0, ch);
                ch = fis.read(bytes, 0, BUFFER_SIZE);
            }
        }
        catch (FileNotFoundException e) {
            String errorMessage = "HTTP/1.1 404 File Not Found\r\n" +
                    "Content-Type: text/html\r\n" +
                    "Content-Length: 23\r\n" +
                    "\r\n" +
                    "<h1>File Not Found</h1>";
            output.write(errorMessage.getBytes());
        }
        finally {
            if (fis!=null)
                fis.close();
        }
    }


    /** implementation of ServletResponse  */
    public void flushBuffer() throws IOException {
    }

    public int getBufferSize() {
        return 0;
    }

    public String getCharacterEncoding() {
        return null;
    }

    public Locale getLocale() {
        return null;
    }

    public ServletOutputStream getOutputStream() throws IOException {
        return null;
    }

    public PrintWriter getWriter() throws IOException {
        // autoflush is true, println() will flush,
        // but print() will not.
        writer = new PrintWriter(output, true);
        return writer;
    }

    public boolean isCommitted() {
        return false;
    }

    public void reset() {
    }

    public void resetBuffer() {
    }

    public void setBufferSize(int size) {
    }

    public void setContentLength(int length) {
    }

    public void setContentType(String type) {
    }

    public void setLocale(Locale locale) {
    }
}
```
## **对静态资源的处理** ##
这里封装了一个类，调用Response里对静态资源处理的方法。
``` java
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
```
## **对Servlet的处理** ##
这里的处理流程是，

1. 解析URI，获取ServletName。
2. 运用类加载机制，从对应的目录下load进所需的Servlet。
3. 调用该Servlet的service方法，返回消息。

``` java
package com.ex02.pyrmont;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandler;

/**
 * Created by laiwenqiang on 2017/5/16.
 */
public class ServletProcessor1 {
    //接收request，处理业务后，返回response
    public void process(Request request, Response response) {

        //1. 获取Servlet的名字

        String uri = request.getUri();
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

        Servlet servlet = null;
        try {
            servlet = (Servlet) myClass.newInstance();

            //3. 调用Servlet的service方法

            servlet.service(request, response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```
## **工具类** ##
``` java
package com.ex02.pyrmont;

import java.io.File;

/**
 * Created by laiwenqiang on 2017/5/17.
 */
public class Constants {
    public static final String WEB_ROOT = System.getProperty("user.dir") + File.separator + "webroot";
}
```
---
现在，这个容器已经完成。
在浏览器中输入入：http://www.localhost.com:8080/servlet/PrimitiveServlet，得到结果：
``` 
Hello. Rose are red.Violets are blue. 
```

输入：http://localhost:8080/index.html，得到结果：
```
Welcome to BrainySoftware.
```
---
一步一个脚印

本章未完成，待续(^__^)

