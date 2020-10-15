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



# consul

**下载**

下载地址：https://www.consul.io/downloads

**安装运行**

解压，运行 consul agent -dev

**管理地址**

http://localhost:8500/

## 服务注册进Consul

**pom.xml**

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-consul-discovery</artifactId>
</dependency>
```

**application.yml**

```yml
server:
  port: 80
spring:
  application:
    name: cloud-consumer-order

  cloud:
    consul:
      host: localhost
      port: 8500
      discovery:
        service-name: ${spring.application.name}
```

**主启动类**

```java
@SpringBootApplication
@EnableDiscoveryClient
public class OrderConsulMain80 {
    public static void main(String[] args) {
        SpringApplication.run(OrderConsulMain80.class, args);
    }
}
```

controller类

```java
@RestController
@Slf4j
public class OrderConsulController {
    public static final String PAYMENT_URL = "http://consul-provider-payment"; 
    @Resource
    private RestTemplate restTemplate;

    @GetMapping(value = "/consumer/payment/consul")
    public String paymentInfo() {
        String result = restTemplate.getForObject(PAYMENT_URL+"/payment/consul",String.class);
        return result;
    }
}
```



三个注册中心异同点

| 组件名    | 语言 | CAP            | 服务器健康检查 | 对外暴露接口 | Spring Cloud 集成 |
| --------- | ---- | -------------- | -------------- | ------------ | ----------------- |
| Eureka    | java | AP（高可用）   | 可配支持       | HTTP         | 已集成            |
| Consul    | Go   | CP（数据一致） | 支持           | HTTP/DNS     | 已集成            |
| Zookeeper | Java | CP（数据一致） | 支持           | HTTP         | 已集成            |

CAP理论的核心是：一个分布式系统不可能同时很好的满足一致性、可用性和分区容错性这三个需求。



# Ribbon

## 基本概念

LB负载均衡：就是将用户的请求平摊到多个服务上，从而达到系统的HA（高可用）

**Ribbon本地负载均衡客户端** VS **Nginx服务端负载均衡**区别
Nginx是服务器负载均衡，客户端所有请求都会交给nginx，然后由nginx实现转发请求。即负载均衡是由服务端实现的。

Ribbon本地负载均衡，在调用微服务接口时候，会在注册中心上获取注册信息服务列表之后缓存到VM本地，从而在本地实现RPC远
程服务调用技术。



**集中式LB**
即在服务的消费方和提供方之间使用独立的LB设施(可以是硬件，如F5,也可以是软件，如nginx)由该设施负责把访问请求通过某种策
咯转发至服务的提供方；

**进程内LB**
将LB逻辑集成到消费方，消费方从服务注册中心获知有哪些地址可用，然后自己再从这些地址中选择出一个合适的服务器。

Ribbon就属于进程内LB，它只是一个类库，集成于消费方进程，消费方通过它来获取到服务提供方的地址.

**Ribbon在工作时分成两步**
第一步先选择EurekaServer，它优先选择在同一个区域内负载较少的server.
第二步再根据用户指定的策略，在从server取到的服务注册列表中选择一个地址。
其中Ribbon提供了多种策略:比如轮询、随机和根据响应时间加权。

引入Eureka的Dependence就包含了Ribbon

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

## RestTemplate的使用

getForObject方法/getForEntity方法

getForObject方法：返回对象为响应体中数据转化为对象，基本上可以理解为Json

```java
public CommonResult<Paymet> getPayment(@PathVariable("id") Long id){
    return restTemplate.getForObject(PAYMENT_URL+"/payment/get/"+id,CommonResult.class);
}
```

getForObject方法：返回对象为ResponseEntity对象，包含响应中的一些重要信息，比如响应头、响应状态代码、响应体等。

```java
public CommonResult<Paymet> getPayment2(@PathVariable("id") Long id){
    ResponseEntity<CommonResult> entity = restTemplate.getForEntity(PAYMENT_URL + "/payment/get/" + id, CommonResult.class);
    if(entity.getStatusCode().is2xxSuccessful()){ //如果状态码是2XX，则返回响应体
        return entity.getBody();
    }else {
        return new CommonResult<>(404,"操作失败");
    }
}
```

## Ribbon默认自带的负载规则

IRule：根据特定算法中从服务列表中选取一个要访问的服务

```Java
public interface IRule {
    Server choose(Object var1);

    void setLoadBalancer(ILoadBalancer var1);

    ILoadBalancer getLoadBalancer();
}
```

负载规则替换

自定义规则（注意不要放在主启动类扫描的包下，新建包）

```java
@Configuration
public class MyselfRule {
    @Bean
    public IRule myRule(){
        return new RandomRule();//定义为随机
    }
}
```

主启动类需要添加注释

```Java
@SpringBootApplication
@EnableEurekaClient
@RibbonClient(name = "CLOUD-PAYMENT-SERVICE", configuration = MyselfRule.class)
public class OrderMain80 {
    public static void main(String[] args) {
        SpringApplication.run(OrderMain80.class, args);
    }
}
```

## 默认负载轮询算法原理

**负载均衡算法：**rest接口第几次请求数%服务器集群总数量=实际调用服务器位置下标，每次服务重启动后rest接口计数从1开始.