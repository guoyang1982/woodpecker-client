# wpclient-agent
  是一个异常日志收集的客户端，用javaagent实现，可以收集error级别(也可以配置info和debug级别日志)
  的所有日志发送到redis队列里（采用二进制传输协议 更小更快）。这个是客户端，需要自己实现服务端消费获取队列里的消息，处理并展示（[woodpecker服务端地址](https://github.com/guoyang1982/woodpecker)）。而且可以支持线上debug，
  性能等分析命令，可以应用运行后attach分析。

# 如何使用

## 1.clone代码（可选，已经发布到中央仓库，可以直接依赖中央仓库的稳定版本）
    git clone git@github.com:guoyang1982/woodpecker-client.git
## 2.编译安装（可选）

    cd wpclient-agent/bin
    ./woodpecker-packages.sh
    会在target生成wpclient-agent.zip运行文件，解压配置即可
## 3.项目配置
    解压wpclient-agent.zip包修改里面的woodpecker.properties文件，需要配置application.name 这个是项目名称，
    agent.log.name是项目所使用的日志，目前支持logbak和log4j，redis.cluster.host集群服务器(ip:port,ip:port)
    redis.cluster.password集群服务器的密码。其他都是可配置项，有默认，看注释。
## 4.运行
   ### spring-boot:
    java -javaagent:/jar包路径/wpclient-agent.jar=/配置文件路径/woodpecker.properties 
    -jar 运行的jar包.jar
   ### tomcat:
    加入agent到CATALINA_OPTS 在 Tomcat 启动脚本 (catalina.sh).
    CATALINA_OPTS="$CATALINA_OPTS -javaagent:/jar包路径/wpclient-agent.jar=/配置文件路径/woodpecker.properties"
   ### resin:
    加入以下配置到 /conf/resin.xml:
    <jvm-arg>-javaagent:/jar包路径/wpclient-agent.jar=/配置文件路径/woodpecker.properties</jvm-arg>
   ### 如果只想使用命令工具，可以应用启动后，随时attach:
   jps 获取应用的pid
   然后./woodpecker-launch.sh pid
   最后执行./woodpecker-run.sh ip port 就ok了

## 5.远程控制
   ### 客户端开了一个端口，可以用telnet进行远程控制，端口号可以在配置文件里配置，远程可以修改配置文件里的必要信息，命令：
      telnet ip port
   ### 推荐用客户端：
      ./woodpecker-run.sh ip port
 
    进去后，输入help，可以看到所有命令和命令的解释。主要控制项为：心跳检查redis开关，线程池队列监控，发送消息开关，日志级别控制，jvm信息展示，
    内存使用信息展示，耗时线程栈展示，跟踪类方法耗时调用信息，查看类方法入参和返回值，查看已加载类详细信息，查看已加载类的方法，查看方法的堆栈信息。
## 6.主要命令举例
  客户端支持命令补全(tab),还有历史记录(上下建)。
### 1.help命令
![Aaron Swartz](https://github.com/guoyang1982/woodpecker-client/blob/master/doc/help.jpg)
查看详细命令：
注意：memory已经改为了jstat
![Aaron Swartz](https://github.com/guoyang1982/woodpecker-client/blob/master/doc/help1.jpg)

### 2.loglevel命令
如下设置root级别为debug,设置com.gy.woo.classname打印日志级别为debug
![Aaron Swartz](https://github.com/guoyang1982/woodpecker-client/blob/master/doc/loglevel.jpg)

### 3.trace命令
查看方法调用路径和耗时
![Aaron Swartz](https://github.com/guoyang1982/woodpecker-client/blob/master/doc/trace.jpg)
根据条件过滤，如：只打印大于耗时1ms以上的，支持各种表达式
![Aaron Swartz](https://github.com/guoyang1982/woodpecker-client/blob/master/doc/trace_cost.jpg)
线上方法调用量太大，跟踪方法会刷屏，并且不能跟踪特定的某个方法调用，可以使用参数表达式解决：
![Aaron Swartz](https://github.com/guoyang1982/woodpecker-client/blob/master/doc/params1.jpg)
如果是对象的话：
![Aaron Swartz](https://github.com/guoyang1982/woodpecker-client/blob/master/doc/params2.jpg)
    params代表参数，是个数组有几个参数，数组长度就为几，params[0]代表第一个参数，如果这个参数是个list对象，可以params[0][0],代表list
对象里的第一个值，如果这个值是对象，可以params[0][0].name代表这个对象里的name属性。如上面的params[0]==227854954,我跟踪的方法第一个参数是
userid，也就是只跟踪userid==227854954的方法调用。而params[0][0].name=='lms'，是我跟踪的方法第一个参数是list，而list里有对象，对象里有属性name，所以
指的是只跟踪方法第一个参数，list里第一个对象的属性值name为lms的方法调用。依此类推。表达式是用mvel实现的。

说明：
    []中括号里以逗号分隔成两个耗时，前面表示从调用链第1条到本方法执行完毕的总耗时，后面表示本方法自身的耗时，@后面是行号，第一条括号里面的是总共耗时。
    trace命令可以看方法内部的执行路径。如果发生异常可以提示异常类型。目前只支持一级方法内部情况。

### 4.watch命令
如果方法发生异常等错误，需要查看方法的入参和返回值，可以用这个命令。也可以支持参数表达式，如trace命令。
参数p是返回所有入参，x是层次展示
![Aaron Swartz](https://github.com/guoyang1982/woodpecker-client/blob/master/doc/watch-px.jpg)
参数r是返回返回值 包括异常，也会返回入参(但是代码中对入参进行修改了，就是修改后的值)
![Aaron Swartz](https://github.com/guoyang1982/woodpecker-client/blob/master/doc/watch-r.jpg)

### 5.invoke命令
方法调用命令，可以在客户端远程调用应用内的方法。

### 6.sc 命令
查询类的详细信息，如类有哪些的加载器，类的静态属性值等。
![Aaron Swartz](https://github.com/guoyang1982/woodpecker-client/blob/master/doc/class.jpg)

### 7.sm命令
查询类的某个方法的详细信息。

### 8.jvm命令
查询jvm的详细信息。

### 9.top命令
查看应用线程占用cpu时间，能够排查占用cpu时间最大的线程栈，进而追踪到有问题的代码，同jstack
![Aaron Swartz](https://github.com/guoyang1982/woodpecker-client/blob/master/doc/top.jpg)

### 10.jstat
查看内存占用情况,以及垃圾收集情况，和jdk的jstat功能一样
![Aaron Swartz](https://github.com/guoyang1982/woodpecker-client/blob/master/doc/jstat.jpg)

### 11.stack命令
查看方法的线程栈，当不知道方法的调用路径时，这个比较不错，尤其对二方包等。
![Aaron Swartz](https://github.com/guoyang1982/woodpecker-client/blob/master/doc/stack.jpg)
### 12.print命令
pring命令可以在类的某个方法里打印变量值包括局部和成员变量，支持打印对象，int，long，等值，看命令参数。也可以支持参数表达式，如trace命令。
适合：服务在启动后，出现问题急切需要知道变量的值而不需要重新打印log日志重启服务。
![Aaron Swartz](https://github.com/guoyang1982/woodpecker-client/blob/master/doc/print.jpg)
说明：
print：命令名
-o:为参数，支持其他参数 查 help print
com.*.ope.controller.*:类名 全路径 目前不支持正则匹配
channelList:方法名
80:插入代码的行号
user:要打印的变量
### 13.monitor监控命令
监控命令,监控接口调用率。
参数：-c 时间秒
![Aaron Swartz](https://github.com/guoyang1982/woodpecker-client/blob/master/doc/monitor.jpg)


# 日志案例
    以下是实际项目使用的异常收集服务：
   [woodpecker服务端地址](https://github.com/guoyang1982/woodpecker)
    
   * wp-server模块：这个是分布式取消息服务，每个线程池对应一个应用(每个应用是redis里一个key,看客户端代码)，获取消息存到mongodb里。
   * wp-web模块：是web服务，可以按照用户注册应用并添加报警邮件信息，监听每个应用，可以看到自己权限下的应用的异常详细信息，并按异常类型汇总，提供报表图进行统计。

#命令后期支持
1.增加监控标示 只对这个标示进行监控（解决大并发量）
