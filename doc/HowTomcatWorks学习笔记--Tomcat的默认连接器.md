>这本书(How Tomcat Works [中文下载地址](https://github.com/laiwenqiang/HowTomcatWorks/blob/master/doc/how%20tomcat%20works中文版.pdf))之前就看过，然而也就开了个头就废弃了，所以一直耿耿于怀。这次决定重新开始，在此记录一些学习心得和本书的主要知识点。
>所有代码也将托管在[GitHub](https://github.com/laiwenqiang/HowTomcatWorks)上面。O(∩_∩)O

# 上节回顾 #
我们创建了一个connector连接器。

1. 将之前的程序更加模块化，
2. 同时解析了HTTP请求，将这些信息封装到``` HttpRequest ```，这样Servlet就能够获取到这些信息了。

上一章节的主要工作在于解析HTTP请求上。


# 概要 #
主要剖析tomcat的 ``` HttpConnector ``` 和 ``` HttpProcessor ``` 的源代码。

# 连接器 #
一个 Tomcat 连接器必须符合以下条件:

1. 必须实现接口``` org.apache.catalina.Connector ```。
2. 必须创建请求对象，该请求对象的类必须实现接口``` org.apache.catalina.Request ```。
3. 必须创建响应对象，该响应对象的类必须实现接口 ``` org.apache.catalina.Response ```。

## Connector接口 ##
指的是``` org.apache.catalina.Connector ```，在这个接口里，最重要的方法是，
1. ``` getContainer ``` 返回一个容器。
2. ``` setContainer ``` 设置一个容器。
3. ``` createRequest ``` 为前来的 HTTP 请求构造一个请求对象。
4. ``` createResponse ``` 创建一个响应对象。

Connector有很多实现，``` org.apache.catalina.connector.http.HttpConnector ```是其中一个。

## HttpConnector类 #

这个类实现的接口有，Connector、Runnable和Lifecycle。
``` org.apache.catalina.Lifecycle ```用于维护生命周期。

**在前几章节，我们也实现了自己的HttpConnector类，现在来看一下它和Tomcat的有何不同。**

1. 创建ServerSocket方式不同；
2. 维护HttpProcessor对象池；
3. 处理HTTP不同。

### 创建ServerSocket ###

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

在```start```方法中创建（这是HttpConnector类的方法，不是用于启动线程的run方法。总是会被搞混。）：

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
```  newProcessor ```方法会创建```HttpProcessor```实例。

需要注意的是，它在该过程中会直接调用```HttpProcessor```的run方法。

如下：

``` java
private HttpProcessor newProcessor() {
    HttpProcessor processor = new HttpProcessor(this, curProcessors++);
    if (processor instanceof Lifecycle) {
        try {
            ((Lifecycle) processor).start();
        } catch (LifecycleException e) {
            log("newProcessor", e);
            return (null);
        }
    }
    created.addElement(processor);
    return (processor);
}
```

可以看到，如果创建HttpProcessor对象，就会调用```recycle```方法。这个方法的内容可以猜想到，就是把processor放到对象池中。

``` java
processors.push(processor);
```

### 处理HTTP ###

在上面的```start```方法中，有一个用于启动线程的方法：```threadStart```。

该方法会，

1. 调用run方法，创建socket。
2. 获取HttpProcessor，处理socket。

run方法会执行一个while循环：

``` java
// ...
while (!stopped) {
            // Accept the next incoming connection from the server socket
            Socket socket = null;
            try {
                socket = serverSocket.accept();
            // ...
```

而获取HttpProcessor的主要逻辑如下：

``` java
HttpProcessor processor = createProcessor();
if (processor == null) {
    try {
        log(sm.getString("httpConnector.noProcessor"));
        socket.close();
    } catch (IOException e) {
        ;
    }
    continue;
}
processor.assign(socket);
```

对于HTTP请求的具体处理细节，都在```HttpProcessor```中。这里会调用它的```assign```方法。

## HttpProcessor类##

下一章节继续。



---
一步一个脚印
本章未完成，(^__^)