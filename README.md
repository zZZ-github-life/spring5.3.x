# Spring 源码解析
    解读spring源码时，添加的一些中文注释
# 涉及的包
spring源码过于庞大，目前解析的源码暂时涉及了以下几个包：
- spring-context，
- spring-core,
- spring-beans


# 各顶层接口的设计目的
描述：就从新建的测试包 spring-a-test包下这段代码来逐步解析：AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ZTest1Config.class);

### ApplicationContext：
    我们平常所说的ioc容器就是它，提供了 Bean 管理、国际化、事件传播、资源加载等功能。利用它可以方便地管理 Spring 应用中的所有组件和资源。 
### ApplicationStartup：
    来标记应用程序启动期间的步骤，并收集有关执行上下文或处理时间的数据。
### EnvironmentCapable
    实现这个接口类，表示能够提供应用程序的环境（即Environment的对象）
### Environment
    提供解析和访问系统属性、环境变量和配置文件中的属性，从而提高应用程序的可配置性和适应性
### BeanDefinitionRegistry
    BeanDefinition的注册中心。在容器启动和运行期间动态地注册和管理 Bean 
### BeanDefinition
    spring会将我们在xml或者注解配置的class转为BeanDefinition，这个对象主要记录class的配置信息，主要用于创建bean
### ConditionEvaluator 类
    用户处理@Condition注解，根据这个注解配置条件决定一个bean是否注入到spring容器当中
### BeanFactory
    提供了管理和访问 Bean 的基本方法.主要负责实例化、配置和管理这些 Bean 的生命周期


    