>这本书(How Tomcat Works [中文下载地址](https://github.com/laiwenqiang/HowTomcatWorks/blob/master/doc/how%20tomcat%20works中文版.pdf))之前就看过，然而也就开了个头就废弃了，所以一直耿耿于怀。这次决定重新开始，在此记录一些学习心得和本书的主要知识点。
>所有代码也将托管在[GitHub](https://github.com/laiwenqiang/HowTomcatWorks)上面。O(∩_∩)O

# **上节回顾** #
我们创建了一个connector连接器。

1. 将之前的程序更加模块化，
2. 同时解析了HTTP请求，将这些信息封装到``` HttpRequest ```，这样Servlet就能够获取到这些信息了。

上一章节的主要工作在于解析HTTP请求上。


# **概要** #
剖析tomcat的 ``` HttpConnector ``` 和 ``` HttpProcessor ``` 的源代码。

# **连接器** #
一个 Tomcat 连接器必须符合以下条件:

1. 必须实现接口``` org.apache.catalina.Connector ```。
2. 必须创建请求对象，该请求对象的类必须实现接口``` org.apache.catalina.Request ```。
3. 必须创建响应对象，该响应对象的类必须实现接口 ``` org.apache.catalina.Response ```。

## **Connector接口** ##
指的是``` org.apache.catalina.Connector ```，在这个接口里，最重要的方法是，
1. ``` getContainer ``` 返回一个容器。
2. ``` setContainer ``` 设置一个容器。
3. ``` createRequest ``` 为前来的 HTTP 请求构造一个请求对象。
4. ``` createResponse ``` 创建一个响应对象。

Connector有很多实现，``` org.apache.catalina.connector.http.HttpConnector ```是其中一个。

## **HttpConnector类** #























---
一步一个脚印
本章完成，(^__^)