# wpclient-agent
  是一个异常日志收集的客户端，用javaagent实现，可以收集error级别(也可以配置info和debug级别日志)
  的所有日志发送到redis队列里。这个是客户端，需要自己实现服务端消费获取队列里的消息，处理并展示。
 

# 如何使用

## 1.clone代码（可选，已经发布到中央仓库，可以直接依赖中央仓库的稳定版本）
    git clone git@github.com:guoyang1982/wpclient-agent.git
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
## 5.远程控制
    客户端开了一个端口，可以用telnet进行远程控制，端口号可以在配置文件里配置，远程可以修改配置文件里的必要信息，命令：
    telnet ip port
    进去后，输入help，可以看到所有命令和命令的解释。主要控制项为：心跳检查redis开关，线程池队列监控，发送消息开关等。
# 案例
    以下是实际项目使用的异常收集，是项目使用的分为wp-server，这个是分布式取消息服务，每个线程池对应一个应用(每个应用是redis里一个key,看客户端代码)，获取消息存到mongodb里
    ，还有wp-web是web服务，可以按照用户注册应用并添加报警邮件信息，监听每个应用，可以看到自己权限下的应用的异常详细信息，并按异常类型汇总，提供报表图进行统计。
   
   



