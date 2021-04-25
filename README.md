## 简介
logserver-spring-boot-starter自动配置logback发送日志到[logserver](https://gitee.com/xlongwei/logserver)，从而实现聚合多个项目的日志输出到文件系统的功能。[logserver](https://gitee.com/xlongwei/logserver)实现了简易的日志实时跟踪、grep关键字搜索、log文件下载等功能，在浏览器上分析日志比较困难时建议下载文件到本地使用Notepad++等工具处理。

## 使用

### Maven
```xml
<dependency>
    <groupId>com.xlongwei.logserver</groupId>
    <artifactId>logserver-spring-boot-starter</artifactId>
    <version>0.0.2</version>
</dependency>
```

### Gradle
```
compile 'com.xlongwei.logserver:logserver-spring-boot-starter:0.0.1'
```

## 配置
```yml
logserver: 
  enabled: true #false停用logserver功能
  remoteHost: logserver #默认logserver配合/etc/hosts使用，也可以直接配置ip
  port: 6000 #默认端口6000
  queueSize: 10240 #默认队列大小10240，配置SocketAppender的队列大小
  reconnectionDelay: 10000 #默认重试间隔10秒
  token: xlongwei #安全校验
  remoteAddress: #默认http://logserver:9880，向logserver注册自己
management: #需要依赖spring-boot-starter-actuator
  endpoints:
    web:
      exposure:
        include: logserver #开启LogserverEndpoint，让logserver变更日志级别
```

### jvm

-Dlogserver.remoteHost=192.168.1.99
