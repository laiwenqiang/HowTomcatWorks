> 这本书(How Tomcat Works [中文下载地址](https://github.com/laiwenqiang/HowTomcatWorks/blob/master/doc/how%20tomcat%20works中文版.pdf))之前就看过，然而也就开了个头就废弃了，所以一直耿耿于怀。这次决定重新开始，在此记录一些学习心得和本书的主要知识点。所有代码也将托管在[GitHub](https://github.com/laiwenqiang/HowTomcatWorks)上面。O(∩_∩)O

# **上节回顾** #
构建了一个简单的Servlet容器，

1. 运用``` javax.servlet.ServletRequest ```和``` javax.servlet.ServletResponse ```，我们实现了自己的Request和Response。
2. 我们把请求划分为两类，一种是请求静态资源，读取文件后返回消息；
3. 另外一种是Servlet请求，包含业务逻辑。
4. 对于Servlet请求，我们的容器会调用该Servlet的service方法进行处理。
5. 值得注意的是，调用Servlet实例的时候，我们的容器会启用类加载机制动态的把Servlet类load进来，从而实例化它。

---

# **概要** #
上节还有个尾巴，对于这个Servlet容器存在不安全的地方，下面详细改善。

---

# **不安全的地方** #
在``` HttpServer1.java ```这个类里面。有这样一段代码：
``` java
try {
	      servlet = (Servlet) myClass.newInstance();

	      //3. 调用Servlet的service方法

	     servlet.service(request, response);
     } catch (Exception e) {
            e.printStackTrace();
}
```
我们的代码会将编写好的Servlet加载进来，然后将Request和Response作为参数传递进去。

我们在编写在Servlet的时候，可以接收到类型为``` ServletRequest ```和``` ServletResponse ```的实例（他们作为参数传递进来了）。即使如此，依旧可以把他们强制转化为``` com.ex02.pyrmont.Request ```和``` com.ex02.pyrmont.Response ```。这两个类里面有``` parse ```、``` parseUri ```、``` sendStaticResource ```，这些核心方法。所以在Servlet里面，能够很轻松地对这些方法进行调用。显然是不安全的。

---

# **改进方法** #
## **添加默认修饰符** ##
这个方法是，
让 ``` Request ``` 和``` Response```类拥有默认访问修饰，所以它们不能在``` ex02.pyrmont ```包的外部使用。

## **使用Facade（外观）设计模式** ##

书上说这种方式更为优雅，而上面那个更为简单。
但是我觉得，这种方式是在某些不能够更改源码的情况下，会显得很眼前一亮。

主要的思路就是，

1. 创建RequestFacade，实现ServletRequest接口。
2. 创建ResponseFacade，实现ServletResponse接口。
3. 在Facade里面，持有对``` Request/Response ```的引用，并且设置为**private**的。

这样，Servlet获取到的是Facade，无法对里面的``` Request/Response ```进行操作。



## **封装Request** ##
``` java
package com.ex02.pyrmont;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

/**
 * Created by laiwenqiang on 2017/5/17.
 */
public class RequestFacade implements ServletRequest {

    private ServletRequest request;
    public RequestFacade (ServletRequest request) {
        this.request = request;
    }

    @Override
    public Object getAttribute(String s) {
        return request.getAttribute(s);
    }

    @Override
    public Enumeration getAttributeNames() {
        return request.getAttributeNames();
    }

    //后面省略
}
```

## **封装Response** ##
``` java
package com.ex02.pyrmont;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

/**
 * Created by laiwenqiang on 2017/5/17.
 */
public class ResponseFacade implements ServletResponse {

    private ServletResponse response = null;

    public ResponseFacade(Response response) {
        this.response = response;
    }

    @Override
    public String getCharacterEncoding() {
        return response.getCharacterEncoding();
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return response.getOutputStream();
    }
    
	//后面省略
}
```
## **修改ServletProcessor** ##
在调用Servlet的地方改为：
``` java
//这里和ServletProcessor1不同，运用外观设计模式
Servlet servlet = null;
RequestFacade requestFacade = new RequestFacade(request);
ResponseFacade responseFacade = new ResponseFacade(response);
try {
       servlet = (Servlet) myClass.newInstance();

       //3. 调用Servlet的service方法

       servlet.service(requestFacade, responseFacade);
    } catch (Exception e) {
            e.printStackTrace();
}

```
##**修改HttpServer** ##
最后，将主程序调用``` ServletProcessor1.java ```的地方修改下即可。
``` java
if(request.getUri().startsWith("/servlet/")) {
                    //启用相对安全的ServletProcessor2
        ServletProcessor2 processor = new ServletProcessor2();
		processor.process(request, response);

} else {
		StaticResourceProcessor processor = new StaticResourceProcessor();
		processor.process(request, response);
}
```

---
一步一个脚印

本章完成，(^__^)