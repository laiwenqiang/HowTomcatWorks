>这本书(How Tomcat Works [中文下载地址](https://github.com/laiwenqiang/HowTomcatWorks/blob/master/doc/how%20tomcat%20works中文版.pdf))之前就看过，然而也就开了个头就废弃了，所以一直耿耿于怀。这次决定重新开始，在此记录一些学习心得和本书的主要知识点。
>所有代码也将托管在[GitHub](https://github.com/laiwenqiang/HowTomcatWorks)上面。O(∩_∩)O

# 上节回顾 #

剖析了Tomcat的```HttpConnector```类。

工作原理梗概如下：

1. 用工厂模式创建出ServerSocket。
2. 创建特定数量的HttpProcessor对象池，用于处理Socket请求；
3. 同时，会启动HttpProcessor线程。由于没有请求，所以所有的HttpProcessor都会阻塞在那里。
4. 调用HttpConnector的run方法，等待Socket请求。
5. 当请求来临，从对象池中pop出一个HttpProcessor实例。
6. 调用HttpProcessor的```assign```方法，将Socket实例传递给HttpProcessor；
7. 并且，唤醒在HttpProcessor中被阻塞的线程，用于处理Socket请求。

# 概要 #
这里会主要关注```HttpProcessor```异步处理请求的方法。

# HttpProcessor类 #
在前几个章节中，我们自己写的处理请求的代码是同步的。他必须等待处理完```process```方法，才能接收新的Socket。

```java
public void run() {
    ...
    while (!stopped) {
        Socket socket = null;
        try {
			socket = serversocket.accept(); 
        }
        catch (Exception e) {
            continue;
		}
		// Hand this socket off to an Httpprocessor
       HttpProcessor processor = new Httpprocessor(this);
 	   processor.process(socket);
	} 
}
```

而Tomcat中，这是异步的。

## run方法 ##

它的逻辑是，接收Socket，然后处理请求，最后将HttpConnector实例放回到对象池中。

``` java
public void run() {
    // Process requests until we receive a shutdown signal
    while (!stopped) {
        // Wait for the next socket to be assigned
        Socket socket = await();
        if (socket == null)
            continue;
        // Process the request from this socket
        try {
            process(socket);
        } catch (Throwable t) {
            log("process.invoke", t);
        }
        // Finish up this request
        connector.recycle(this);
    }
    // Tell threadStop() we have shut ourselves down successfully
    synchronized (threadSync) {
        threadSync.notifyAll();
    }
}
```

while方法做循环，很常见。

**在上一章节中我们已经说过，HttpConnector会创建HttpProcessor，并且启动线程的run方法，同时它被放在对象池中，一直阻塞着。**

那么run方法为何会一直阻塞着呢？很简单，因为在while循环中的```await```方法。

## await方法 ##

执行的流程如下：

1. while循环中```available```条件的初始状态是false，表示没有请求。所以它就一直处于```wait```等待状态。
2. 一旦有请求（在assign方法中，会将available设置为true，并且释放线程锁。await得以调用。），await将会获取线程锁，代码跳出循环得以向下执行。
3. 然后，它将```available```条件设置为false，通过```notifyAll```释放线程锁，返回Socket实例。
4. 这样，当前Scoket的await方法就执行完成，返回run方法中。
5. 在run方法中，继续其他操作。

### 一个问题 ###

这里有个问题值得注意一下，**为何需要调用notifyAll方法，作用是什么，是否多此一举呢？**

书上是这么解释的：

> 为什么 await 需要使用一个本地变量(socket)而不是返回实例的 socket 变量呢?因为这样一来，在当前 socket 被完全处理之前，实例的 socket 变量可以赋给下一个前来的 socket。
>
> 为什么 await 方法需要调用 notifyAll 呢? 这是为了防止在 available 为 true 的时候另一 个 socket 到来。在这种情况下，连接器线程将会在 assign 方法的 while 循环中停止，直到接收到处理器线程的 notifyAll 调用。

意思大概就是，一个HttpProcessor实例在同一时间只能处理一个Socket的请求，但是

``` java
private synchronized Socket await() {
    // Wait for the Connector to provide a new Socket
    while (!available) {
        try {
            wait();
        } catch (InterruptedException e) {
        }
    }
    // Notify the Connector that we have received this Socket
    Socket socket = this.socket;
    available = false;
    notifyAll();
  
    if ((debug >= 1) && (socket != null))
        log("  The incoming request has been awaited");
    return (socket);
}
```

## assign方法##

刚刚我们说了，await方法会阻塞。直到assign方法被调用，释放了线程锁，await才得以继续执行。

```assign```方法的作用就是，

1. 接收由HttpConnector传递的Socket实例。
2. 释放线程锁，让```await```方法得以调用。

```available```属性是false，我们得知。

``` java
synchronized void assign(Socket socket) {
    // Wait for the Processor to get the previous Socket
    while (available) {
        try {
            	wait();
            } catch (InterruptedException e) {
        }
    }
    // Store the newly available Socket and notify our thread
    this.socket = socket;
    available = true;
    notifyAll();

    if ((debug >= 1) && (socket != null))
        log(" An incoming request is being assigned");
}
```



下一章节继续。



---
一步一个脚印
本章未完成，(^__^)