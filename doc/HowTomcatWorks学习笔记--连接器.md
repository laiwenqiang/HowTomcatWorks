
>这本书(How Tomcat Works [中文下载地址](https://github.com/laiwenqiang/HowTomcatWorks/blob/master/doc/how%20tomcat%20works中文版.pdf))之前就看过，然而也就开了个头就废弃了，所以一直耿耿于怀。这次决定重新开始，在此记录一些学习心得和本书的主要知识点。
所有代码也将托管在[GitHub](https://github.com/laiwenqiang/HowTomcatWorks)上面。O(∩_∩)O

# **上节回顾** #
在上一章节中，我们创建了一个安全性相对高一点的Servlet容器。
前面的两个章节的内容简单，而且提到概念性的东西也是熟悉的，比如说：

1. ``` ServerSocket ```、``` Socket ```。
2. ``` ServletRequest ```、 ``` ServletResponse ```。
3. ``` Servlet ```。
4. ``` Facade设计模式 ```。

这章节，讨论连接器。相对而言陌生不少。

---

# **概要** #

<font color=red>**在Tomcat里面，有两个主要的模块：连接器和容器**。</font>
本章节将会创建一个连接器，用来更好地处理处理请求和相应。
这个连接器将会，

1. 解析HTTP请求头部，
2. 并让Servlet能够获取头部、cookies、参数名/值等信息。在前两章节的程序里面，Servlet是无法获取到请求的信息的，本章已经可以了。

# **模块划分** #
本章节的程序将划分为三块：

1. startup启动模块，只有一个类：``` BootStrap```。
3. connector模块：
	- <font color=red>连接器和它的支撑类``` HttpConnector ``` 和 ``` HttpProcessor  ```。</font>
	- 指代 HTTP 请求的类(HttpRequest)和它的辅助类。
	- 指代 HTTP 响应的类(HttpResponse)和它的辅助类。
	- Facade 类(HttpRequestFacade 和 HttpResponseFacade)。
	- Constant 类。 
3. core模块，由``` ServletProcessor ```和``` StaticResourceProcessor ```组成。

<font color=blue>
等待HTTP请求的任务由``` HttpConnector ```完成，
而创建请求和响应对象的工作交给了``` HttpProcessor ```实例。
当然，解析HTTP请求的工作也是由``` HttpProcessor ```完成的。
</font>


# **StringManager类（消息处理工具类）** #

简要说一下这个工具类的功能，就是方便输出错误信息。

1.  需要在包下存放属性文件，里面配置错误信息。
2. 用 ``` StringManager sm = StringManager.getManager("包名称"); ``` 获取StringManager实例。
3. 用``` sm.getString("错误代号"); ```打印信息。

例如：
``` java
throw new ServletException(sm.getString("httpProcessor.parseHeaders.colon"));
```

# **启动类** #
程序的入口
``` java
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
```

# **HttpConnector类**#
用于开启Socket接收请求，并且增加了Thread。
``` java
package com.ex03.pyrmont.connector.http;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by laiwenqiang on 2017/5/18.
 */
public class HttpConnector implements Runnable {

    boolean stopped = false;

    private String scheme = "http";

    public String getScheme() {
        return scheme;
    }

    @Override
    public void run() {
        ServerSocket serverSocket = null;
        int port = 8080;

        try {
            serverSocket = new ServerSocket(port, 1, InetAddress.getByName("127.0.0.1"));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        while(!stopped) {
            Socket socket = null;
            try {
                socket = serverSocket.accept();
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }

            HttpProcessor processor = new HttpProcessor(this);
            processor.process(socket);
        }
    }

    public void start() {
        Thread thread = new Thread(this);
        thread.start();
    }
}
```
这个类和前两章的内容很相似。
对于HTTP的处理又``` HttpProcessor ```完成，入口是``` process ```方法。

# **HttpConnector类** #
<font color=red>**本章节的核心类。**</font>

这个类由``` process ```、``` parseHeaders ```、``` parseRequest ```、和 ```normalize ```方法构成。

## **process方法** ##
``` process ```方法会接收Socket实例，
1. 创建一个``` HttpRequest ```对象和``` HttpResponse ```对象。
2. 解析HTTP请求，并将它们放到``` HttpRequest ```对象中。
3. 根据请求的类型，将``` HttpRequest ```对象和``` HttpResponse ```传送到``` ServletProcessor ```/```StaticResourceProcessor ```里。

``` java 
public void process(Socket socket) {
        SocketInputStream input;
        OutputStream output;

        try {
            input = new SocketInputStream(socket.getInputStream(), 2048);
            output = socket.getOutputStream();

            request = new HttpRequest(input);

            response = new HttpResponse(output);
            response.setRequest(request);
            response.setHeader("Server", "Pyrmont Servlet Container");

            parseRequest(input, output);
            parseHeaders(input);

            if (request.getRequestURI().startsWith("/servlet/")) {
                ServletProcessor processor = new ServletProcessor();
                processor.process(request, response);
            } else {
                StaticResourceProcessor processor = new StaticResourceProcessor();
                processor.process(request, response);
            }

            socket.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
}
```

## **parseHeaders方法** ##
顾名思义，本方法用于解析HTTP请求的头部。
``` java
/**
 * 这个方法只解析
 * "cookie", "content-length", and "content-type",
 * 这些字段
 * @param input
 * @throws IOException
 * @throws ServletException
 */
private void parseHeaders(SocketInputStream input)
        throws IOException, ServletException {

    while (true) {
        HttpHeader header = new HttpHeader();

        // 这里会读取input的数据到header，每次读取一对 name/value。
        // 如果全部读取完成，nameEnd 和 valueEnd 会被设置为 0。
        input.readHeader(header);

        if (header.nameEnd == 0) {
            if (header.valueEnd == 0) {
                // 读取完所有数据，返回
                return;
            } else {
                throw new ServletException(sm.getString("httpProcessor.parseHeaders.colon"));
            }
        }

        String name = new String(header.name, 0, header.nameEnd);
        String value = new String(header.value, 0, header.valueEnd);
        request.addHeader(name, value);

        // cookie的格式：
        // Cookie: name=value; name2=value2
        if (name.equals("cookie")) {
            Cookie cookie[] = RequestUtil.parseCookieHeader(value);
            for (int i = 0; i < cookie.length; i++) {
                if (cookie[i].getName().equals("jsessionid")) {
                    if (!request.isRequestedSessionIdFromCookie()) {
                        // 只有缓存中没有时才会执行
                        request.setRequestedSessionId(cookie[i].getValue());
                        request.setRequestedSessionCookie(true);
                        request.setRequestedSessionURL(false);
                    }
                }
                request.addCookie(cookie[i]);
            }
        } else if (name.equals("content-length")) {
            int n = -1;
            try {
                n = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                e.printStackTrace();
                throw new ServletException(sm.getString("httpProcessor.parseHeaders.contentLength"));
            }
            request.setContentLength(n);

        } else if (name.equals("content-type")) {
            request.setContentType(value);
        }
    }
}
```

# **parseRequest方法** #
用于解析请求，这些解析类其实就是解析字符串，很繁琐。
```
private void parseRequest(SocketInputStream input, OutputStream output) throws IOException, ServletException {

        input.readRequestLine(requestLine);
        String method = new String(requestLine.method, 0, requestLine.methodEnd);
        String uri;
        String protocol = new String(requestLine.protocol, 0, requestLine.protocolEnd);

        if (method.length() < 1) {
            throw new ServletException("Missing HTTP request method");
        }
        if (requestLine.uriEnd < 1) {
            throw new ServletException("Missing HTTP request uri");
        }

        int question = requestLine.indexOf("?");
        if (question >= 0) {
            request.setQueryString(new String(requestLine.uri, question + 1,
                    requestLine.uriEnd - question - 1));
            uri = new String(requestLine.uri, 0, question);
        } else {
            request.setQueryString(null);
            uri = new String(requestLine.uri, 0, requestLine.uriEnd);
        }

        if (!uri.startsWith("/")) {
            int pos = uri.indexOf("://");
            if (pos != -1) {
                pos = uri.indexOf('/', pos + 3);
                if (pos == -1) {
                    uri = "";
                } else {
                    uri = uri.substring(pos);
                }
            }
        }

        // 可能会存在如下的uri
        // xxxx.jsp;jsessionid=xxxxx;
        // URL重写功能。为了防止一些用户把Cookie禁止而无法使用session而设置的功能。
        // jsessionid后面的一长串就是你服务器上的session的ID号,这样无需cookie也可以使用session.

        String match = ";jsessionid=";
        int semicolon = uri.indexOf(match);
        if (semicolon > 0) {
            String rest = uri.substring(semicolon + match.length());
            int semicolon2 = rest.indexOf(';');
            if (semicolon2 > 0) {
                request.setRequestedSessionId(rest.substring(0, semicolon2));
                rest = rest.substring(semicolon2);
            } else {
                request.setRequestedSessionId(rest);
                rest = "";
            }
            request.setRequestedSessionURL(true);
            uri = uri.substring(0, semicolon) + rest;
        } else {
            request.setRequestedSessionURL(false);
            request.setRequestedSessionId(null);
        }

        String normalizedUri = normalize(uri);
        request.setMethod(method);
        request.setProtocol(protocol);

        if (normalizedUri != null) {
            request.setRequestURI(normalizedUri);
        } else {
            request.setRequestURI(uri);
        }

        if (normalizedUri == null) {
            throw new ServletException("Invalid URI: " + uri + "'");
        }
}
```

## **normalize方法** ##
这个方法由上面那个方法调用，功能是处理URI地址。没有细看，对于整体理解无大碍。
``` java
/**
 * Return a context-relative path, beginning with a "/", that represents
 * the canonical version of the specified path after ".." and "." elements
 * are resolved out.  If the specified path attempts to go outside the
 * boundaries of the current context (i.e. too many ".." path elements
 * are present), return <code>null</code> instead.
 *
 * @param path Path to be normalized
 */
protected String normalize(String path) {
    if (path == null)
        return null;
    // Create a place for the normalized path
    String normalized = path;

    // Normalize "/%7E" and "/%7e" at the beginning to "/~"
    if (normalized.startsWith("/%7E") || normalized.startsWith("/%7e"))
        normalized = "/~" + normalized.substring(4);

    // Prevent encoding '%', '/', '.' and '\', which are special reserved
    // characters
    if ((normalized.indexOf("%25") >= 0)
            || (normalized.indexOf("%2F") >= 0)
            || (normalized.indexOf("%2E") >= 0)
            || (normalized.indexOf("%5C") >= 0)
            || (normalized.indexOf("%2f") >= 0)
            || (normalized.indexOf("%2e") >= 0)
            || (normalized.indexOf("%5c") >= 0)) {
        return null;
    }

    if (normalized.equals("/."))
        return "/";

    // Normalize the slashes and add leading slash if necessary
    if (normalized.indexOf('\\') >= 0)
        normalized = normalized.replace('\\', '/');
    if (!normalized.startsWith("/"))
        normalized = "/" + normalized;

    // Resolve occurrences of "//" in the normalized path
    while (true) {
        int index = normalized.indexOf("//");
        if (index < 0)
            break;
        normalized = normalized.substring(0, index) +
                normalized.substring(index + 1);
    }

    // Resolve occurrences of "/./" in the normalized path
    while (true) {
        int index = normalized.indexOf("/./");
        if (index < 0)
            break;
        normalized = normalized.substring(0, index) +
                normalized.substring(index + 2);
    }

    // Resolve occurrences of "/../" in the normalized path
    while (true) {
        int index = normalized.indexOf("/../");
        if (index < 0)
            break;
        if (index == 0)
            return (null);  // Trying to go outside our context
        int index2 = normalized.lastIndexOf('/', index - 1);
        normalized = normalized.substring(0, index2) +
                normalized.substring(index + 3);
    }

    // Declare occurrences of "/..." (three or more dots) to be invalid
    // (on some Windows platforms this walks the directory tree!!!)
    if (normalized.indexOf("/...") >= 0)
        return (null);

    // Return the normalized path that we have completed
    return (normalized);

}
```

# **ServletProcessor类 和StaticResourceProcessor类 ** #

这两个类和之前没有太大变化，在此就不赘述了。

---

一步一个脚印
本章完成，(^__^)