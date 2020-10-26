# 1. Eureka

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





# 2. zookeeper

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



# 3. consul

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



# 4. Ribbon

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

## 手写轮询算法

1. ApplicationContextConfig去掉@LoadBalanced

2. 8001服务

```Java
@GetMapping(value = "/payment/lb")
@ResponseBody
public String getPaymentLB(){
    return serverPort;
}
```

3. 实现类

接口

```java
public interface LoadBalancer {
    //获取机器总数
    ServiceInstance instance(List<ServiceInstance> serviceInstances);
}
```

实现类

```Java
@Component
public class MyLB implements LoadBalancer{

    private AtomicInteger atomicInteger = new AtomicInteger(0);//原子操作类

    public final int getAndIncrement(){
        int current;
        int next;
        do{
            current = this.atomicInteger.get();
            next = current >= 2147483647 ? 0 : current + 1; //判断是否达到最大
        }while (!this.atomicInteger.compareAndSet(current, next));//当目前的值和下一个值不相等，那么自询
        System.out.println("*****next:" +next);
        return next;
    }
    @Override
    public ServiceInstance instance(List<ServiceInstance> serviceInstances) {
        int index = getAndIncrement() % serviceInstances.size(); //负载均衡算法
        return serviceInstances.get(index);//返回访问的主机序号
    }
}
```

4. 80服务调用

```Java
//自己的轮询规则
@GetMapping(value = "/consumer/payment/lb")
public String getPaymentLB(){
    //获取一个Instances下相关的信息
    List<ServiceInstance> instances = discoveryClient.getInstances("CLOUD-PAYMENT-SERVICE");
    if(instances == null || instances.size() <=0){ //w无效的实例
        return null;
    }
    ServiceInstance instance = loadBalancer.instance(instances);//将instances传入到自己的规则中
    URI uri = instance.getUri();

    return restTemplate.getForObject(uri + "/payment/lb", String.class);
}
```

# 5. OpenFeign

## 概述

Feign是一个声明式WebService**客户端**。使用Feign能让编写Web Service客户端更加简单。 

**它的使用方法是定义一个服务接口然后在上面添加注解。**Feign也支持可拔插式的编码器和解码器。Spring Cloud对Feign进行了封装，使其支持了Spring MVC标准注解和HttpMessageConverters。Feign可以与Eureka和Ribbon组合使用以支持负载均衡

**Feign能干什么**
**Feign旨在使编写Java Http客户端变得更容易。**
前面在使用Rjbbon+RestTemplate时，利用RestTemplate对http请求的封装处理，形成了一套模版化的调用方法。但是在实际开发中，由于对服务依赖的调用可能不止一处，**往往一个接口会被多处调用**，**所以通常都会针对每个微服务自行封装一些客户端类来包装这些依赖服务的调用。**所以，Feign在此基础上做了进一步封装，由他来帮助我们定义和实现依赖服务接口的定义。在Feign的实现下，**我们只需创建一个接口并使用注解的方式来配置它(以前是Dao接口上面标注Mapper注解,现在是一个微服务接口上面标注一个Feign注解即可)，**即可完成对服务提供方的接口绑定，简化了使用Spring cloud Ribbon时，自动封装服务调用客户端的开发量。

**Feign集成了Ribbon**
利用Ribbon维护了Payment的服务列表信息，并且通过轮询实现了客户端的负载均衡。而与Ribbon不同的是，通过feign只需要定义
服务绑定接口且以声明式的方法，优雅而简单的实现了服务调用

## 服务调用

**业务类**

业务逻辑接口+@FeignClient配置调用provider服务

新建PaymentFeignService接口并新增注解@FeignClient

```Java
@Component
@FeignClient(value = "CLOUD-PAYMENT-SERVICE")
public interface PaymentFeignService {
    @GetMapping(value = "/payment/get/{id}")
    public CommonResult<Paymet> getPaymentById(@PathVariable("id") Long id);//这个服务必须是8001所包含的接口
}
```

**控制层**

```Java
@RestController
@Slf4j
public class OrderFeignController {
    @Resource
    private PaymentFeignService paymentFeignService;

    @GetMapping(value = "/consumer/payment/get/{id}")
    public CommonResult<Paymet> getPaymentById(@PathVariable("id") Long id){
        return paymentFeignService.getPaymentById(id);
    }
}
```

pom.xml

```xml
<!-- openfeign -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
```

application.yml

```
server:
  port: 80

eureka:
  client:
    register-with-eureka: true
    service-url:
      defaultZone: http://eureka7002.com:7002/eureka/,http://eureka7001.com:7001/eureka/
```

## 日志增强

**配置类**

```Java
@Configuration
public class FeignConfig {
    @Bean
    Logger.Level feignLoggerLevel(){
        return Logger.Level.FULL;
    }
}
```

**yml**

```yaml
logging:
  level:
    #feign日志以什么级别健康哪个接口
    com.study.springcloud.service.PaymentFeignService: debug
```

**效果**

DEBUG 6376 --- [p-nio-80-exec-1] c.s.s.service.PaymentFeignService        : [PaymentFeignService#getPaymentById] <--- HTTP/1.1 200 (420ms)
2020-10-16 15:22:00.611 DEBUG 6376 --- [p-nio-80-exec-1] c.s.s.service.PaymentFeignService        : [PaymentFeignService#getPaymentById] connection: keep-alive
2020-10-16 15:22:00.611 DEBUG 6376 --- [p-nio-80-exec-1] c.s.s.service.PaymentFeignService        : [PaymentFeignService#getPaymentById] content-type: application/json
2020-10-16 15:22:00.611 DEBUG 6376 --- [p-nio-80-exec-1] c.s.s.service.PaymentFeignService        : [PaymentFeignService#getPaymentById] date: Fri, 16 Oct 2020 07:22:00 GMT
2020-10-16 15:22:00.611 DEBUG 6376 --- [p-nio-80-exec-1] c.s.s.service.PaymentFeignService        : [PaymentFeignService#getPaymentById] keep-alive: timeout=60
2020-10-16 15:22:00.611 DEBUG 6376 --- [p-nio-80-exec-1] c.s.s.service.PaymentFeignService        : [PaymentFeignService#getPaymentById] transfer-encoding: chunked
2020-10-16 15:22:00.611 DEBUG 6376 --- [p-nio-80-exec-1] c.s.s.service.PaymentFeignService        : [PaymentFeignService#getPaymentById] 
2020-10-16 15:22:00.614 DEBUG 6376 --- [p-nio-80-exec-1] c.s.s.service.PaymentFeignService        : [PaymentFeignService#getPaymentById] {"code":200,"message":"查询成功,serverPort:8001","data":{"id":1,"serial":"22"}}
2020-10-16 15:22:00.614 DEBUG 6376 --- [p-nio-80-exec-1] c.s.s.service.PaymentFeignService        : [PaymentFeignService#getPaymentById] <--- END HTTP (83-byte body)
2020-10-16 15:22:01.472  INFO 6376 --- [erListUpdater-0] c.netflix.config.ChainedDynamicProperty  : Flipping property: CLOUD-PAYMENT-SERVICE.ribbon.ActiveConnectionsLimit to use NEXT property: niws.loadbalancer.availabilityFilteringRule.activeConnectionsLimit = 2147483647

# 6. Hystrix

## 概述

Hystrix是一个用于处理分布式系统的**延迟和容错**的开源库，在分布式系统里，许多依赖不可避免的会调用失败，比如超时、异常等，
Hystrix能够保证在一个依赖出问题的情况下，**不会导致整体服务失败，避免级联故障，以提高分布式系统的弹性**。

"断路器”本身是一种开关装置，当某个服务单元发生故障之后，通过断路器的故障监控〔类似熔断保险丝)，**向调用方返回一个符合**
**预期的、可处理的备选响应(FallBack)，而不是长时间的等待或者抛出调用方无法处理的异常**，这样就保证了服务调用方的线程不会
被长时间、不必要地占用，从而避免了故障在分布式系统中的蔓延，乃至雪崩。

**服务降级：**不让客户端等待并立刻返回一个友好提示

**服务熔断：**达到最大服务访问后，直接拒绝访问，然后调用服务降级的方法返回提示

**服务限流：**秒杀高并发操作，严禁一窝蜂的拥挤，一秒钟N个，有序进行

## Hystrix支付微服务构建

pom.xml

```xml
<!-- hystrix-->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-hystrix</artifactId>
</dependency>
```

application.yml

```yml
server:
  port: 8001

spring:
  application:
    name: cloud-provider-hystrix-payment

eureka:
  client:
    register-with-eureka: true
    fetch-registry: true
    service-url:
      defaultZone: http://eureka7001.com:7001/eureka/
```

主启动类

```Java
@SpringBootApplication
@EnableEurekaClient
public class PaymentHystrixMain8001 {
    public static void main(String[] args) {
        SpringApplication.run(PaymentHystrixMain8001.class, args);
    }
}
```

服务层

```Java
@Service
public class PaymentService {
    //不会出异常的程序
    public String paymentInfo_OK(Integer id){
        return "线程池：" + Thread.currentThread().getName() + "paymentInfo_OK, id:" + id;
    }
    //会出异常的程序
    public String paymentInfo_TimeOut(Integer id){
        int timeNumber = 3;
        try {
            TimeUnit.SECONDS.sleep(timeNumber);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "线程池：" + Thread.currentThread().getName() + "paymentInfo_TimeOut, id:" + id + "耗时：" + timeNumber;
    }
}
```

控制层

```Java
@RestController
@Slf4j
public class PaymentController {
    @Resource
    PaymentService paymentService;

    @Value("$(server.port)")
    private String serverPort;

    @GetMapping(value = "/payment/hystrix/ok/{id}")
    public String paymentInfo_OK(@PathVariable("id") Integer id){
        String info_ok = paymentService.paymentInfo_OK(id);
        log.info("****result:" + info_ok);
        return info_ok;
    }
    @GetMapping(value = "/payment/hystrix/timeout/{id}")
    public String paymentInfo_TimeOut(@PathVariable("id") Integer id){
        String info_ok = paymentService.paymentInfo_TimeOut(id);
        log.info("****result:" + info_ok);
        return info_ok;
    }
}
```



## 服务降级

在提供服务的方法中，添加**@HystrixCommand注解**

```Java
@HystrixCommand(fallbackMethod = "paymentInfo_TimeOutHandler",commandProperties = {
        @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "3000")
})//服务降级到指定方法
```

添加保底方法

```Java
public String paymentInfo_TimeOutHandler(Integer id){
    return "线程池：" + Thread.currentThread().getName() + "paymentInfo_TimeOut, id:" + id + "出错";
}
```

主函数中添加**@EnableCircuitBreaker注解**

## 全局服务降级方法

1. 在整个Controller类上添加@DefaultProperties注解

```Java
@DefaultProperties(defaultFallback = "payment_Global_FallBackMethod")
```

2. 在需要降级的方法上添加@HystrixCommand注解

3. 添加全局保底方法

```java
public String payment_Global_FallBackMethod(){
    return "全局FallBack方法";
}
```

## 通配服务降级FeignFallBack

1. Feign调用的接口：**需要添加fallback属性**

```Java
@Component
@FeignClient(value = "CLOUD-PROVIDER-HYSTRIX-PAYMENT", fallback = PaymentHystrixFallback.class)
public interface PaymentHystrixService {
    @GetMapping(value = "/payment/hystrix/ok/{id}")
    public String paymentInfo_OK(@PathVariable("id") Integer id);

    @GetMapping(value = "/payment/hystrix/timeout/{id}")
    public String paymentInfo_TimeOut(@PathVariable("id") Integer id);
}
```

2. 实现该接口（重新方法）

```java
@Component
public class PaymentHystrixFallback implements PaymentHystrixService{
    @Override
    public String paymentInfo_OK(Integer id) {
        return "paymentInfo_OK 服务降级方法";
    }

    @Override
    public String paymentInfo_TimeOut(Integer id) {
        return "paymentInfo_TimeOut 服务降级方法";
    }
}
```

**熔断机制概述**
熔断机制是应对雪崩效应的一种微服务链路保护机制。当扇出链路的某个微服务出错不可用或者响应时间太长时，
会进行服务的降级，进而熔断该节点微服务的调用，快速返回错误的响应信息。
**当检测到该节点微服务调用响应正常后，恢复调用链路。**

在Spring Cloud框架里，熔断机制通过Hystrix实现。Hystrix会监控微服务间调用的状况，
当失败的调用到一定阈值，缺省是5秒内20次调用失败，就会启动熔断机制。熔断机制的注解是@HystrixCommand。

案例：

**服务层**

```java
//****服务熔断****
@HystrixCommand(fallbackMethod = "paymentCircuitBreaker_fallback",commandProperties = {
        @HystrixProperty(name = "circuitBreaker.enabled", value = "true"),              //是否开启断路器
        @HystrixProperty(name = "circuitBreaker.requestVolumeThreshold", value = "10"),    //请求数达到后才计算
        @HystrixProperty(name = "circuitBreaker.sleepWindowInMilliseconds", value = "10000"), //休眠时间窗
        @HystrixProperty(name = "circuitBreaker.errorThresholdPercentage", value = "60"),  //错误率达到多少跳闸
})
public String paymentCircuitBreaker(@PathVariable("id") Integer id){
    if(id < 0){
        throw new RuntimeException("id不能为负数");
    }
    String serialNumber = IdUtil.simpleUUID();

    return Thread.currentThread().getName() + "\t" + "调用成功，流水号：" + serialNumber;
}
public String paymentCircuitBreaker_fallback(@PathVariable("id") Integer id){
    return "id不能为负数，请重试";
}
```

**业务层**

```java
//===服务熔断
@GetMapping(value = "/payment/circuit/{id}")
public String paymentCircuitBreaker(@PathVariable("id") Integer id){
    String result = paymentService.paymentCircuitBreaker(id);
    log.info("info:" + result);
    return result;
}
```

## Hystrix图形化监控

**1.新建一个Module**

application.yml

```yml
server:
  port: 9001
```

依赖

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-hystrix-dashboard</artifactId>
</dependency>
```

主启动类

```Java
@SpringBootApplication
@EnableHystrixDashboard //开启Hystrix图形化界面
public class HystrixDashboard9001 {
    public static void main(String[] args) {
        SpringApplication.run(HystrixDashboard9001.class, args);
    }
}
```

2. 被监控的服务

依赖

```xml
<!--监控-->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

主启动类

```Java
@SpringBootApplication
@EnableEurekaClient
@EnableCircuitBreaker
public class PaymentHystrixMain8001 {
    public static void main(String[] args) {
        SpringApplication.run(PaymentHystrixMain8001.class, args);
    }
    /**
     * 此配置是为了服务监控而配置，与服务容错本身无关，springcloud升级后的坑
     * ServletRegistrationBean因为SpringBoot的默认路径不是 “/hystrix.stream"
     * 只要在自己的项目里配置上下的servlet就可以了
     */
    @Bean
    public ServletRegistrationBean getServlet() {
        HystrixMetricsStreamServlet streamServlet = new HystrixMetricsStreamServlet() ;
        ServletRegistrationBean registrationBean = new ServletRegistrationBean(streamServlet);
        registrationBean.setLoadOnStartup(1);
        registrationBean.addUrlMappings("/hystrix.stream");
        registrationBean.setName("HystrixMetricsStreamServlet");
        return  registrationBean;
    }
}
```

3. 测试地址

图形化地址：http://localhost:9001/hystrix

待测地址：http://localhost:8001/hystrix.stream



# 7. GateWay

Gateway是在Spring生态系统之上构建的API网关服务，基于Spring 5，Spring Boot.2和Project Reactor等技术。
Gateway旨在提供一种简单而有效的方式来对API进行路由，以及提供一些强大的过滤器功能，例如:熔断、限流、重试等

**SpringCloud Gateway使用的Webflux中的reactor-netty响应式编程组件，底层使用了Netty通讯框架。**

**路由：**路由是构建网关的基本模块，它由ID，目标URI，一系列的断言和过滤器组成，如果断言为true则匹配该路由；

**断言：**参考的是Java8的java.util.function.Predicate
开发人员可以匹配HTTP请求中的所有内容(例如请求头或请求参数)，如果请求与断言相匹配则进行路由

**过滤：**指的是Spring框架中GatewayFilter的实例，使用过滤器，可以在请求被路由前或者之后对请求进行修改。



## GateWay9527搭建

依赖：

```xml
<!--gateway-->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway</artifactId>
</dependency>
```

配置：

```yml
server:
  port: 9527

spring:
  application:
    name: cloud-gateway
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true # 开启从注册中心动态创建路由的功能，利用微服务名称进行路由
      routes:
        - id: payment_route # 路由的id,没有规定规则但要求唯一,建议配合服务名
          #匹配后提供服务的路由地址
          #uri: http://localhost:8001
          uri: lb://cloud-payment-service
          predicates:
            - Path=/payment/get/** # 断言，路径相匹配的进行路由

        - id: payment_route2
          #uri: http://localhost:8001
          uri: lb://cloud-payment-service
          predicates:
            - Path=/payment/lb/** #断言,路径相匹配的进行路由
            - After=2020-03-12T15:44:15.064+08:00[Asia/Shanghai]
          #- Cookie=username,eiletxie   #带Cookie，并且username的值为eiletxie
          #- Header=X-Request-Id,\d+ #请求头要有 X-Request-Id属性并且值为整数的正则表达式

eureka:
  instance:
    hostname: cloud-gateway-service
  client:
    fetch-registry: true
    register-with-eureka: true
    service-url:
      defaultZone: http://eureka7001.com:7001/eureka/
```

主启动类

```java
@SpringBootApplication
@EnableEurekaClient
public class GatewayMain9527 {
    public static void main(String[] args) {
        SpringApplication.run(GatewayMain9527.class, args);
    }
}
```

代码配置网关方式

```java
@Configuration
public class MyGateWayConfig {
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder){
        RouteLocatorBuilder.Builder routes = builder.routes();
        routes.route("mypath",r->r.path("/guonei").uri("http://news.baidu.com/guonei")).build();

        return routes.build();
    }
}
```

# 8. Config

## 概述

SpringCloud Config为微服务架构中的微服务提供集中化的外部配置支持，配置服务器为各个不同微服务应用的所有环境提供了一个中心化的外部配置。

**服务端**也称为分布式配置中心，**它是一个独立的微服务应用，用来连接配置服务器并为客户端提供获取配置信息，加密/解密信息等访问接口**
**客户端**则是通过指定的配置中心来管理应用资源，以及与业务相关的配置内容，并**在启动的时候从配置中心获取和加载配置信息**配置服务器默认采用git来存储配置信息，这样就有助于对环境配置进行版本管理，并且可以通过git客户端工具来方便的管理和访问配置内容

**服务器端**

配置：

```yaml
spring:
  application:
    name: cloud-config-center
  cloud:
    config:
      server:
        git:
          #uri: git@github.com:EiletXie/config-repo.git #Github上的git仓库名字
          uri: https://github.com/yanglei2018/springcloud-config.git
          ##搜索目录.这个目录指的是github上的目录
          search-paths:
            - config-repo
      ##读取分支
      label: master
```

依赖

```xml
<!--config server-->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-config-server</artifactId>
</dependency>
```

**客户端**

依赖

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-config</artifactId>
</dependency>
```

配置

bootstrap.yml

```yaml
server:
  port: 3355

spring:
  application:
    name: config-client
  cloud:
    #Config客户端配置
    config:
      label: master #分支名称
      name: config #配置文件名称
      profile: dev #读取后缀名称 上诉3个综合就是 master分支上 config-dev.yml
      uri: http://config-3344.com:3344/

eureka:
  client:
    service-url:
      defaultZone: http://eureka7001.com:7001/eureka/
```

业务类

```java
@RestController
@RefreshScope //刷新配置
public class ConfigClientController {
    // 因为config仓库以rest形式暴露，所以所有客户端都可以通过config服务端访问到github上对应的文件信息
    @Value("${config.info}")
    private String configInfo;

    @Value("${server.port}")
    private String serverPort;

    @GetMapping("/configInfo")
    public String getConfigInfo() {
        return "serverPort: " + serverPort + "\t\n\n configInfo:" + configInfo;
    }
}
```

存在的问题：客户端不能动态的刷新GitHub上更新的配置，需要重新启动服务后才能获取到最新的配置。

需要修改的配置：

bootstrap.yml

```yaml
#暴露监控端点
management:
  endpoints:
    web:
      exposure:
        include: "*"
```

业务类需要增加的注解：**@RefreshScope**

最后，需要手动运行命令：curl -X POST "http:localhost:3355/actuator/refresh"

# 9. Bus消息总线

## 概述

**什么是总线**
在微服务架构的系统中，通常会使用**轻量级的消息代理来构建一个共用的消息主题**，并让系统中所有微服务实例都连接上来。由于**该主题中产生的消息会被所有实例监听和消费**，所以称它为消息总线。在总线上的各个实例，都可以方便地广播一些需要让其他连接在该主题上的实例都知道的消息。
**基本原理**
ConfigClient实例都监听MQ中同一个topic(默认是springCloudBus)。当一个服务刷新数据的时候，它会把这个信息放入到Topic中，这样其它监听同—Topic的服务就能得到通知，然后去更新自身的配置。

## **一次配置，处处生效**

**1. 服务端：**

依赖

```xml
<!-- 添加消息总线RabbitMQ支持 -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-bus-amqp</artifactId>
</dependency>
```

配置

```yaml
#rabbit相关配置 15672是web管理界面的端口，5672是MQ访问的端口
rabbitmq:
  host: 47.96.27.160
  port: 5672
  username: guest
  password: guest
  
#rabbitmq相关设置 ，暴露 bus刷新配置的端点
management:
  endpoints:
    web:
      exposure:
        include: 'bus-refresh'
```

**2. 客户端:**

依赖

~~~xml
<!-- 添加消息总线RabbitMQ支持 -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-bus-amqp</artifactId>
</dependency>
~~~

配置：

~~~yaml
#rabbit相关配置 15672是web管理界面的端口，5672是MQ访问的端口
rabbitmq:
  host: 47.96.27.160
  port: 5672
  username: guest
  password: guest
  
#rabbitmq相关设置 ，暴露 bus刷新配置的端点
management:
  endpoints:
    web:
      exposure:
        include: "*"
~~~

**3. 执行的命令**

curl -X POST "http:localhost:3355/actuator/bus-refresh"

# 10. Steam

## 生产者

依赖

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <!--监控-->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    <!--eureka client-->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
    </dependency>
    <!--stream rabbit -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-stream-rabbit</artifactId>
    </dependency>

    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

配置

```yaml
server:
  port: 8801

spring:
  application:
    name: cloud-stream-provider
  cloud:
    stream:
      binders: #在此处配置要绑定的rabbitmq的服务信息
        defaultRabbit: #表示定义的名称，用于binding整合
          type: rabbit #消息组件类型
          environment: #设置rabbitmq的相关环境配置
            spring:
              rabbitmq:
                host: 47.96.27.160
                port: 5672
                username: guest
                password: guest
      bindings: #服务的整合处理
        output: #这个名字是一个通道的名称
          destination: studyExchange #表示要使用的Exchange名称定义
          content-type: application/json #设置消息类型，本次为json，本文要设置为“text/plain”
          binder: defaultRabbit #设置要绑定的消息服务的具体设置

eureka:
  client:
    service-url:
      defaultZone: http://localhost:7001/eureka
  instance:
    lease-renewal-interval-in-seconds: 2 #设置心跳的时间间隔（默认是30S)
    lease-expiration-duration-in-seconds: 5 #如果超过5S间隔就注销节点 默认是90s
    instance-id: send-8801.com #在信息列表时显示主机名称
    prefer-ip-address: true #访问的路径变为IP地址
```

服务层：

```java
public interface IMessageProvider {
    public String send();
}
```

```java
@EnableBinding(Source.class)
@Slf4j
public class MessageProviderImpl implements IMessageProvider {
    @Resource
    private MessageChannel output;
    @Override
    public String send() {
        String serial = UUID.randomUUID().toString();
        output.send(MessageBuilder.withPayload(serial).build());//发送消息
        log.info("****serial:" + serial);
        return serial;
    }
}
```

业务层

```java
@RestController
public class SendMessageController {
    @Resource
    private IMessageProvider messageProvider;

    @GetMapping(value = "/send")
    public String send(){
        return messageProvider.send();
    }
}
```

## 消费者

依赖

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <!--监控-->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    <!--eureka client-->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
    </dependency>
    <!--stream rabbit -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-stream-rabbit</artifactId>
    </dependency>

    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
```

配置

```yaml
server:
  port: 8802

spring:
  application:
    name: cloud-stream-provider
  cloud:
    stream:
      binders: #在此处配置要绑定的rabbitmq的服务信息
        defaultRabbit: #表示定义的名称，用于binding整合
          type: rabbit #消息组件类型
          environment: #设置rabbitmq的相关环境配置
            spring:
              rabbitmq:
                host: 47.96.27.160
                port: 5672
                username: guest
                password: guest
      bindings: #服务的整合处理
        input: #这个名字是一个通道的名称
          destination: studyExchange #表示要使用的Exchange名称定义
          content-type: application/json #设置消息类型，本次为json，本文要设置为“text/plain”
          binder: defaultRabbit #设置要绑定的消息服务的具体设置

eureka:
  client:
    service-url:
      defaultZone: http://localhost:7001/eureka
  instance:
    lease-renewal-interval-in-seconds: 2 #设置心跳的时间间隔（默认是30S)
    lease-expiration-duration-in-seconds: 5 #如果超过5S间隔就注销节点 默认是90s
    instance-id: receive-8802.com #在信息列表时显示主机名称
    prefer-ip-address: true #访问的路径变为IP地址
```

业务类

```java
@Component
@EnableBinding(Sink.class)
public class MessageListenerController {
    @Value("${server.port}")
    private  String serverPort;

    @StreamListener(Sink.INPUT)
    public void input(Message<String> message) {
        System.out.println("消费者1号， -----> 接受到的消息： " + message.getPayload()
                + "\t port: " + serverPort);
    }
}
```