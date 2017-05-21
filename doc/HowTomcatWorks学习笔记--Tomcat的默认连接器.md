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

这个类实现的接口有，Connector、Runnable和Lifecycle。
``` org.apache.catalina.Lifecycle ```用于维护生命周期。

**在前几章节，我们也实现了自己的HttpConnector类，现在来看一下它和Tomcat的有何不同。**

1. 创建Socket方式不同；
2. 维护HttpProcessor对象池；
3. 处理HTTP不同。

### 创建Socket ###

入口是```initialize```方法，会判断是否已经初始化过，没有的话就调用```open```方法。

```initialize```的主要代码如下：

```java
//...
if (initialized)
            throw new LifecycleException (
                sm.getString("httpConnector.alreadyInitialized"));

        this.initialized=true;
        Exception eRethrow = null;

        // Establish a server socket on the specified port
        try {
            serverSocket = open();
//...
```

```open```方法会返回一个ServerSocket实例。它会运用工厂来获取这个实例。主要代码如下：

```java
// ...
// Acquire the server socket factory for this Connector
ServerSocketFactory factory = getFactory();

// If no address is specified, open a connection on all addresses
if (address == null) {
  	log(sm.getString("httpConnector.allAddresses"));
  	try {
    	return (factory.createSocket(port, acceptCount));
  	} catch (BindException be) {
    	throw new BindException(be.getMessage() + ":" + port);
  	}
}

// Open a server socket on the specified address
try {
  	InetAddress is = InetAddress.getByName(address);
  	log(sm.getString("httpConnector.anAddress", address));
  	try {
    	return (factory.createSocket(port, acceptCount, is));
// ...      
```
*acceptCount表示连接数量，这里默认是10个。可以设置。*

### 维护HttpProcessor线程池 ###

对我之前我们自己的程序，每次只能处理一个HTTP请求。

在默认的连接器中，```HttpConnector```拥有一个```HttpProcessor```对象池，可以处理多个HTTP请求。

对象池是放在```Stack```中的：

```java
private Stack processors = new Stack();
```

创建的HttpProcessor实例数量是可以控制的。

介绍几个相关的变量：

1. minProcessors，最小的实例数量，默认5个。可以设置。
2. maxProcessors，最大的实例数量，默认20个。可以设置。
3. curProcessors，当前对象池中的实例数量。

刚开始的时候，会创建minProcessors个实例。

随着时间推移，当请求的数量大于实例的数量时，会创建更多的实例，直到数量抵达maxProcessors个。

如果请求继续增多，将会被忽略。

在```start```方法中创建：

```java
//...
if (started)
  	throw new LifecycleException
  	(sm.getString("httpConnector.alreadyStarted"));
	threadName = "HttpConnector[" + port + "]";
lifecycle.fireLifecycleEvent(START_EVENT, null);
started = true;

// Start our background thread
threadStart();

// Create the specified minimum number of processors
while (curProcessors < minProcessors) {
  	if ((maxProcessors > 0) && (curProcessors >= maxProcessors))
    	break;
  	HttpProcessor processor = newProcessor();
  	recycle(processor);
}
//...
```

可以看到，如果创建HttpProcessor对象，就会调用```recycle```方法。这个方法的内容可以才想到，就是把processor放到对象池中。

``` java
processors.push(processor);
```



















---
一步一个脚印
本章完成，(^__^)