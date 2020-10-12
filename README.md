## EurekaService服务端安装

### pom.xml依赖

```xml
<!-- eureka-server -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
</dependency>
```

### application.yml

```yml
server:
  port: 7001
eureka:
  instance:
    hostname: localhost
  client:
    #不要注册自己
    register-with-eureka: false
    #不需要检索服务，而是维护服务
    fetch-registry: true
    service-url:
      defaultZoom: http://${eureka.instance.hostname}:${server.port}/eureka/
```

### 主启动类

```java
@SpringBootApplication
@EnableEurekaServer //表明这个是Eureka服务中心
public class EurekaMain7001 {
    public static void main(String[] args) {
        SpringApplication.run(EurekaMain7001.class, args);
    }
}
```

## 将微服务注册到EurekaService服务端

### pom.xml

```xml
<!--eureka client-->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

### 主启动类

```Java
@SpringBootApplication
@EnableEurekaClient
public class PaymentMain8001 {
    public static void main(String[] args) {
        SpringApplication.run(PaymentMain8001.class,args);
    }
}
```

### application.yml

```yml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:7001/eueka
    register-with-eureka: true
    fetch-registry: true
```

**注意：配置文件中的defaultZone，两端需要一致**



## Eureka集群

相互注册，相互守望

即需要将defaultZone配置成对方的IP

```yml
server:
  port: 7002
eureka:
  instance:
    hostname: eureka7002.com
  client:
    #不要注册自己
    register-with-eureka: false
    #不需要检索服务，而是维护服务
    fetch-registry: false
    service-url:
      defaultZone: http://eureka7001.com:7001/eureka/
```





## 服务发现

**controller层**   

注意引包是入的import org.springframework.cloud.client.discovery.DiscoveryClient;

```java
/**
 * 查询本微服务的相关信息
 * @return
 */
@Resource
    private DiscoveryClient discoveryClient;

@GetMapping(value = "/payment/discovery")
@ResponseBody
public Object discovery(){
    //发现服务 cloud-payment-service cloud-oreder-service
    List<String> services = discoveryClient.getServices();
    for(String element: services){
        log.info("*****element:"+element);
    }

    //获取一个Instances下相关的信息
    List<ServiceInstance> instances = discoveryClient.getInstances("CLOUD-PAYMENT-SERVICE");
    for (ServiceInstance instance : instances){
        log.info(instance.getServiceId()+"\t"+ instance.getHost()+ "\t" + instance.getPort() + "\t"+instance.getUri());
    }

    return this.discoveryClient;
}
```

主启动类

```java
@SpringBootApplication
@EnableEurekaClient
@EnableDiscoveryClient//开启服务发现
public class PaymentMain8001 {
    public static void main(String[] args) {
        SpringApplication.run(PaymentMain8001.class,args);
    }
}
```



## Eureka自我保护机制

在某时刻某个服务不可用了，Eureka不会立即清理，依旧会对该服务的信息进行保存。

属于CAP中的AP





# zookeeper

## 服务注册进zookeeper

**pom.xml**

```xml
<!--SpringBoot整合Zookeeper客户端-->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-zookeeper-discovery</artifactId>
    <exclusions>
        <!--先排除自带的zookeeper3.5.3-->
        <exclusion>
            <groupId>org.apache.zookeeper</groupId>
            <artifactId>zookeeper</artifactId>
        </exclusion>
    </exclusions>
</dependency>
<!--添加zookeeper3.4.14版本-->
<dependency>
    <groupId>org.apache.zookeeper</groupId>
    <artifactId>zookeeper</artifactId>
    <version>3.4.9</version>
</dependency>
```

**application.yml**

```yml
server:
  port: 8004


#服务别名 --- 注册zookeeper到注册中心名称
spring:
  application:
    name: cloud-provider-payment
  cloud:
    zookeeper:
      connect-string: 47.96.27.160:2181
```

**controller层**

```java
@RestController
@Slf4j
public class PaymentController {
    @Value("${server.port}")
    private String serverPort;

    @RequestMapping(value = "/payment/zk")
    public String payment_zk(){
        return "springcloud with zookeeper:" + serverPort + "\t" + UUID.randomUUID().toString();
    }
}
```

**主启动类**

```java
@SpringBootApplication
@EnableDiscoveryClient
public class PaymentMain8006 {
    public static void main(String[] args) {
        SpringApplication.run(PaymentMain8006.class, args);
    }
}
```



**Docker中安装zookeeper**

1. 拉取：docker pull zookeeper:3.4.9
2. 运行：docker run --privileged=true -d --name zookeeper3 --publish 2181:2181  -d zookeeper:3.4.9



**查看服务：**

~~~java
//首先找到正在运行的容器di
docker ps

//然后执行如下操作
docker exec -it add905a36402（this is id） bash

cd bin

./zkCli.sh

ls /services
//参考：https://blog.csdn.net/qq_43357627/article/details/108816396
~~~

**查看信息**

get /services/cloud-provider-payment/db498dc4-d6b8-40a5-916f-b4010671d5fe

~~~jso
{"name":"cloud-provider-payment","id":"db498dc4-d6b8-40a5-916f-b4010671d5fe","address":"DESKTOP-O35174P","port":8004,"sslPort":null,"payload":{"@class":"org.
springframework.cloud.zookeeper.discovery.ZookeeperInstance","id":"application-1","name":"cloud-provider-payment","metadata":{}},"registrationTimeUTC":160238
4139101,"serviceType":"DYNAMIC","uriSpec":{"parts":[{"value":"scheme","variable":true},{"value":"://","variable":false},{"value":"address","variable":true},{
"value":":","variable":false},{"value":"port","variable":true}]}}
~~~



将Order服务注册进Zookeeper

pom.xml、application.yml、主启动类与上一节类似

配置类：

```java
@Configuration
public class ApplicationContextConfig {
    @Bean
    @LoadBalanced
    public RestTemplate getRestTemplate(){
        return new RestTemplate();
    }
}
```

controller类：

```java
@RestController
@Slf4j
public class OrderZKController {

    private static  final String INVOKE_URL = "http://cloud-provider-payment";

    @Resource
    private RestTemplate restTemplate;

    @GetMapping(value = "/consumer/payment/zk")
    public String paymentInfo() {
        String result = restTemplate.getForObject(INVOKE_URL+"/payment/zk",String.class);
        return result;
    }
}
```

返回：springcloud with zookeeper:8006 f45f26e8-379c-48f3-a789-c97b14a87b76

