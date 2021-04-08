# rest-proxy-client
## 主要功能：
### 1.简化RestTemplate的使用
### 2.集成公司的封装RestTemplate组件，在不破坏封装的RestTemplate前提下定制化自己的功能
   > 2.1.加入自己的请求、响应转换converter  
   > 2.2.加入自己的拦截器  
   > 2.3.个性化输出自己的日志（AOP）  
  
****** 
## 使用方法:
 * deploy到自己的仓库
 * 在pom.xml里加入引用
 * 在启动类加上@EnbaleRestClient
 * 调用远程api直接定义接口，在接口上添加注解@RestClient(配置远程服务)
   > 声明接口方法，加上org.springframework.web.bind.annotation包下的注解
   > 在业务类中定义成员变量，加上注解@Resoure或者@Autowired即可像本地服务调用一样使用
 
***
