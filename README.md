# rest-proxy-client
## 简介：
### 1.基于Spring框架简化RestTemplate的使用
### 2.集成公司封装的RestTemplate组件，在不破坏封装的RestTemplate前提下定制化自己的功能
   > 2.1.加入自己的请求、响应转换converter  
   > 2.2.加入自己的拦截器  
   > 2.3.个性化输出自己的日志（AOP）  
  
## 目标：
### 1.使用RestTemplate调用远程服务时就像本地服务调用一样简单
  
****** 
## 使用方法:
 * deploy到自己的仓库
 * 在pom.xml里加入引用
 * 在启动类加上@EnbaleRestClient
 * 调用远程api直接定义接口，在接口上添加注解@RestClient(配置远程服务)
   > 声明接口方法，加上org.springframework.web.bind.annotation包下的注解 (本项目引入@RestRequestBody，@RestRequestFile，不在spring包里)  
   > 在业务类中定义成员变量，加上注解@Resource或者@Autowired即可像本地服务调用一样使用
  
***
### 注解说明:
* @RestClient 远程调用定义  
  > route 可以动态配置${} 将从配置上下文里取；也可以直接配置服务地址  
  > value 内容组装（拼接）到 route 值里  
  > name 服务名称  
* @RestRequestBody 参数不封装 例如：list 直接传 [a,b] 
  > 如果参数没有任何注解，则默认放在map里 例如：list 传值 {"list":[a,b]}
* @RestRequestFile 表示文件
  > 可以使用二进制数组(byte[])、File类型
  
***
### 示例
##### 启动类
![启动类](https://github.com/niwzb/rest-proxy-client/blob/master/src/main/resources/image/start.png)  
##### 接口定义
![远程调用接口](https://github.com/niwzb/rest-proxy-client/blob/master/src/main/resources/image/service.png)  
##### 引用
![引用](https://github.com/niwzb/rest-proxy-client/blob/master/src/main/resources/image/refrence.png)
