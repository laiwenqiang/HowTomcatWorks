> 这本书(How Tomcat Works [中文下载地址](https://github.com/laiwenqiang/HowTomcatWorks/blob/master/doc/how%20tomcat%20works中文版.pdf))之前就看过，然而也就开了个头就废弃了，所以一直耿耿于怀。这次决定重新开始，在此记录一些学习心得和本书的主要知识点。所有代码也将托管在[GitHub](https://github.com/laiwenqiang/HowTomcatWorks)上面。O(∩_∩)O

本章节，简要介绍HTTP协议，并且实现一个简陋的web服务器。
主要运用到java.net.Socket和java.net.ServerSocket，并且通过Http消息和客户端进行通信。

---

# **HTTP协议** #
（基本上是书上的概念搬过来。）


## **HTTP Request** ##
一个HTTP请求，包含三部分：
1. Method--URI--Protocol/version
2. Request headers
3. Entity body

例如：
``` http
POST /examples/default.jsp HTTP/1.1 Accept: text/plain; text/html Accept-Language: en-gb
Connection: Keep-Alive
Host: localhost
User-Agent: Mozilla/4.0 (compatible; MSIE 4.01; Windows 98) Content-Length: 33
Content-Type: application/x-www-form-urlencoded Accept-Encoding: gzip, deflate

lastName=Franks&firstName=Michael
```

请求方法包括：GET、POST、HEAD、OPTIONS、PUT、DELETE 和 TRACE。

URI 指明了请求资源的地址，通常是从网站更目录开始计算的一个相对路径，因此它总是以斜线“ /”开头的。
URL 实际上是 URI 的一种类型。

请求头(header)中包含了一些关于客户端环境和请求实体(entity)的有用的信息。
例如，客户端浏览器所使用的语言，请求实体信息的长度等。
每个请求头使用 CRLF(回车换行符，“\r\n”)分隔。

注意请求头的格式:
请求头名+英文空格+请求头值

请求头和请求实体之间有一个空白行(CRLF)。这是 HTTP 协议规定的格式。HTTP 服务器，以此确定请求实体是从哪里开始的。

上面的例子中，请求实体是:
``` lastName=Franks&firstName=Michael```

## **HTTP Response**
HTTP Response也有三部分，
1. Protocol--Status Code --Description
2. Response heads
3. Entity body

例如：
``` http
HTTP/1.1 200 OK
Server: Microsoft-IIS/4.0
Date: Mon, 5 Jan 2004 13:13:33 GMT Content-Type: text/html
Last-Modified: Mon, 5 Jan 2004 13:13:12 GMT Content-Length: 112
<html>
<head>
<title>HTTP Response Example</title> </head>
<body>
Welcome to Brainy Software
</body>
</html>
```
注意响应实体(entity)与响应头(header)之间有一个空白行(CRLF)。

---

# **Socket和ServerSocket**
Socket类表示一个客户端，而ServerSocket表示一个服务端。连接的流程如下，
1. 在服务端建立ServerSocket，等待请求。
2. 一旦请求来，则创建一个Socket连接，进行通信。

---

# **简陋的web服务器**
这里我们创建一个服务器，功能很简单，就是在浏览器上显示几句话。我们用到上面HTTP Response的例子，返回“Welcome to Brainy Software”这句话。
``` java
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by laiwenqiang on 2017/5/16.
 */
public class SimpleServer {

    public static void main(String[] args)  {

        ServerSocket serverSocket = null;
        int port = 8080;
        try {
            serverSocket = new ServerSocket(port, 1, InetAddress.getByName("127.0.0.1"));
        }  catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        while(true){
            Socket socket = null;
            OutputStream outputStream = null;

            try {
                socket = serverSocket.accept();
                outputStream = socket.getOutputStream();

                String msg = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: text/html\r\n" +
                        "\r\n" +
                        "<html>\r\n" +
                        "<head>\r\n" +
                        "<title>HTTP Response Example</title>\r\n" +
                        "</head>\r\n" +
                        "<body>\r\n" +
                        "Welcome to Brainy Software\r\n" +
                        "</body>\r\n" +
                        "</html>";
                outputStream.write(msg.getBytes());

                Thread.sleep(50);//由于是阻塞写入，暂停 50ms，保证可以写入。

                socket.close();

            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
        }
    }
}
```
<font color=red>上面的休眠50毫秒，是必要的。如果没有那段代码的话，客户端可能会接收不到返回的数据。</font>

运行后，我们在浏览器输入：http://127.0.0.1:8080，得到结果：
```
Welcome to Brainy Software
```
---

# **稍微复杂一点的web服务器**
这个就是书上的例子，和上面的代码本质上是一样的，区别在于他把请求和相应做了封装。功能上也有所不同，他请求webroot目录下的文件，如果不存在返回404。
## **创建服务端**
```java
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by laiwenqiang on 2017/5/16.
 */
public class HttpServer {

    public static final String WEB_ROOT = System.getProperty("user.dir") + File.separator + "webroot";
    private boolean shutdown = false;
    private static final String SHUTDOWN_COMMAND = "/SHUTDOWN";

    public static void main(String[] args) {
        HttpServer server = new HttpServer();
        server.await();
    }

    public void await(){
        ServerSocket serverSocket = null;
        int port = 8080;
        try {
            serverSocket = new ServerSocket(port, 1, InetAddress.getByName("127.0.0.1"));
        }  catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        while(!shutdown){
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
                response.sendStaticResource();
                socket.close();

                //request.getUri可能会返回null值。由于做了异常处理，所以循环会继续。
                shutdown = request.getUri().equals(SHUTDOWN_COMMAND);
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
        }
    }
}
```
## **封装请求**
```java
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
```
## **封装相应**
这个返回Response的类，主要功能在sendStaticResource方法里。原理就是拼接HTTP Response相应。如果存在该文件，则读取文件内容，然后往OutputStream里面放；不存在的话，就返回一个404。

<font color=blue>*** 我的电脑是mac操作系统，在浏览器上测试会出现异常错误。原因还未知。故添加一个返回的相应头。 ***</font>
```java
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by laiwenqiang on 2017/5/16.
 */
public class Response {
    private static final int BUFFER_SIZE = 1024;
    private OutputStream output;
    private Request request;

    public Response(OutputStream outputStream) {
        this.output = outputStream;
    }

    public void setRequest(Request request) {
        this.request = request;
    }

    public void sendStaticResource() throws IOException {
        FileInputStream fileInputStream = null;
        byte[] bytes = new byte[BUFFER_SIZE];
        try {
            //获取request中的uri，找到对应的文件。
            File file = new File(HttpServer.WEB_ROOT, request.getUri());
            if(file.exists()){

                fileInputStream = new FileInputStream(file);

                //源代码中没有下面这段话，导致请求可能会出现错误。现在添加。
                String msg = "HTTP/1.1 404 File Not Found\r\n" +
                        "Content-Type: text/html\r\n" +
                        "\r\n";
                output.write(msg.getBytes());
                
                while(i != -1){
                    output.write(bytes, 0, i);
                    i = fileInputStream.read(bytes, 0, BUFFER_SIZE);
                }
            }
            //404错误
            else{
                String errorMessage = "HTTP/1.1 404 File Not Found\r\n" +
                        "Content-Type: text/html\r\n" +
                        "Content-Length: 23\r\n" +
                        "\r\n" +
                        "<h1> File Not Found!</h1>";
                output.write(errorMessage.getBytes());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally{
            if(fileInputStream != null){
                fileInputStream.close();
            }
        }

    }

}
```
## **创建文件index.html**
现在需要新建一个文件夹webroot，并且在里面创建一个index.html文件，内容很简单：
```html
<html>
<head>
    <title>Welcome to BrainySoftware</title>
</head>
<body>
Welcome to BrainySoftware.
</body>
</html>
```
运行后，在浏览器输入：localhost:8080/index.html，得到结果：
```
Welcome to BrainySoftware.
```
如果输入不存在的文件路径，会返回：
```
File Not Found
```
---
一步一个脚印

本章节完(*^__^*) 
