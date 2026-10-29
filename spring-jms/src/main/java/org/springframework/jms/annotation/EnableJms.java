/*
 * Copyright 2002-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.jms.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

/**
 * 启用带 JMS 监听器注解的端点，这些端点由
 * {@link org.springframework.jms.config.JmsListenerContainerFactory
 * JmsListenerContainerFactory} 在底层创建。像下面这样在
 * {@link org.springframework.context.annotation.Configuration Configuration}
 * 类上使用：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableJms
 * public class AppConfig {
 *
 *     &#064;Bean
 *     public DefaultJmsListenerContainerFactory myJmsListenerContainerFactory() {
 *       DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
 *       factory.setConnectionFactory(connectionFactory());
 *       factory.setDestinationResolver(destinationResolver());
 *       factory.setSessionTransacted(true);
 *       factory.setConcurrency("5");
 *       return factory;
 *     }
 *
 *     // other &#064;Bean definitions
 * }</pre>
 *
 * {@code JmsListenerContainerFactory} 负责创建服务于特定端点的监听器容器。
 * 典型实现（如上例中使用的
 * {@link org.springframework.jms.config.DefaultJmsListenerContainerFactory DefaultJmsListenerContainerFactory}）
 * 提供了底层 {@link org.springframework.jms.listener.MessageListenerContainer MessageListenerContainer}
 * 所支持的必需配置选项。
 *
 * <p>{@code @EnableJms} 可以检测容器中任何 Spring 管理 bean 上的
 * {@link JmsListener} 注解。例如，给定类 {@code MyService}：
 *
 * <pre class="code">
 * package com.acme.foo;
 *
 * public class MyService {
 *
 *     &#064;JmsListener(containerFactory = "myJmsListenerContainerFactory", destination="myQueue")
 *     public void process(String msg) {
 *         // process incoming message
 *     }
 * }</pre>
 *
 * 要使用的容器工厂由 {@link JmsListener#containerFactory() containerFactory}
 * 属性标识，该属性定义要使用的 {@code JmsListenerContainerFactory} bean 的名称。
 * 如果未设置，则假定存在名为 {@code jmsListenerContainerFactory} 的
 * {@code JmsListenerContainerFactory} bean。
 *
 * <p>下面的配置可确保每当在名为 "myQueue" 的 {@link javax.jms.Destination}
 * 上接收到 {@link javax.jms.Message} 时，都会以消息内容调用
 * {@code MyService.process()}：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableJms
 * public class AppConfig {
 *
 *     &#064;Bean
 *     public MyService myService() {
 *         return new MyService();
 *     }
 *
 *     // JMS infrastructure setup
 * }</pre>
 *
 * 或者，如果 {@code MyService} 带有 {@code @Component} 注解，则下面的配置
 * 可确保其 {@code @JmsListener} 注解的方法会以匹配的传入消息被调用：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableJms
 * &#064;ComponentScan(basePackages="com.acme.foo")
 * public class AppConfig {
 * }</pre>
 *
 * 请注意，创建的容器不会注册到应用上下文，但可以通过
 * {@link org.springframework.jms.config.JmsListenerEndpointRegistry JmsListenerEndpointRegistry}
 * 轻松定位以便进行管理。
 *
 * <p>带注解的方法可以使用灵活的方法签名；特别是可以使用
 * {@link org.springframework.messaging.Message Message} 抽象及相关注解，
 * 详情请参见 {@link JmsListener} 的 Javadoc。例如，下面的代码会注入消息内容
 * 以及自定义的 "myCounter" JMS 头：
 *
 * <pre class="code">
 * &#064;JmsListener(containerFactory = "myJmsListenerContainerFactory", destination="myQueue")
 * public void process(String msg, @Header("myCounter") int counter) {
 *     // process incoming message
 * }</pre>
 *
 * 这些功能由 {@link org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory}
 * 抽象，它负责构建处理带注解方法所需的调用器。默认情况下，使用
 * {@link org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory}。
 *
 * <p>如果需要更多控制，{@code @Configuration} 类可以实现
 * {@link JmsListenerConfigurer}。这允许访问底层的
 * {@link org.springframework.jms.config.JmsListenerEndpointRegistrar JmsListenerEndpointRegistrar}
 * 实例。下面的示例演示如何指定显式的默认
 * {@code JmsListenerContainerFactory}：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableJms
 * public class AppConfig implements JmsListenerConfigurer {
 *
 *     &#064;Override
 *     public void configureJmsListeners(JmsListenerEndpointRegistrar registrar) {
 *         registrar.setContainerFactory(myJmsListenerContainerFactory());
 *     }
 *
 *     &#064;Bean
 *     public JmsListenerContainerFactory&lt;?&gt; myJmsListenerContainerFactory() {
 *         // factory settings
 *     }
 *
 *     &#064;Bean
 *     public MyService myService() {
 *         return new MyService();
 *     }
 * }</pre>
 *
 * 作为参考，可以将上面的示例与以下 Spring XML 配置进行比较：
 *
 * <pre class="code">
 * &lt;beans&gt;
 *
 *     &lt;jms:annotation-driven container-factory="myJmsListenerContainerFactory"/&gt;
 *
 *     &lt;bean id="myJmsListenerContainerFactory" class="org.springframework.jms.config.DefaultJmsListenerContainerFactory"&gt;
 *           // factory settings
 *     &lt;/bean&gt;
 *
 *     &lt;bean id="myService" class="com.acme.foo.MyService"/&gt;
 *
 * &lt;/beans&gt;
 * }</pre>
 *
 * 如果需要对容器的创建和管理方式进行更多控制，也可以指定自定义的
 * {@link org.springframework.jms.config.JmsListenerEndpointRegistry
 * JmsListenerEndpointRegistry}。下面的示例还演示了如何自定义
 * {@code JmsHandlerMethodFactory}，使其与自定义的
 * {@link org.springframework.validation.Validator
 * Validator} 配合使用，从而对带有
 * {@link org.springframework.validation.annotation.Validated
 * Validated} 注解的负载先按自定义 {@code Validator} 进行校验。
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableJms
 * public class AppConfig implements JmsListenerConfigurer {
 *
 *     &#064;Override
 *     public void configureJmsListeners(JmsListenerEndpointRegistrar registrar) {
 *         registrar.setEndpointRegistry(myJmsListenerEndpointRegistry());
 *         registrar.setMessageHandlerMethodFactory(myJmsHandlerMethodFactory);
 *     }
 *
 *     &#064;Bean
 *     public JmsListenerEndpointRegistry&lt;?&gt; myJmsListenerEndpointRegistry() {
 *         // registry configuration
 *     }
 *
 *     &#064;Bean
 *     public JmsHandlerMethodFactory myJmsHandlerMethodFactory() {
 *        DefaultJmsHandlerMethodFactory factory = new DefaultJmsHandlerMethodFactory();
 *        factory.setValidator(new MyValidator());
 *        return factory;
 *     }
 *
 *     &#064;Bean
 *     public MyService myService() {
 *         return new MyService();
 *     }
 * }</pre>
 *
 * 作为参考，可以将上面的示例与以下 Spring XML 配置进行比较：
 * <pre class="code">
 * &lt;beans&gt;
 *
 *     &lt;jms:annotation-driven registry="myJmsListenerEndpointRegistry"
 *         handler-method-factory="myJmsHandlerMethodFactory"/&gt;
 *
 *     &lt;bean id="myJmsListenerEndpointRegistry"
 *           class="org.springframework.jms.config.JmsListenerEndpointRegistry"&gt;
 *           // registry configuration
 *     &lt;/bean&gt;
 *
 *     &lt;bean id="myJmsHandlerMethodFactory"
 *           class="org.springframework.messaging.handler.support.DefaultJmsHandlerMethodFactory"&gt;
 *         &lt;property name="validator" ref="myValidator"/&gt;
 *     &lt;/bean&gt;
 *
 *     &lt;bean id="myService" class="com.acme.foo.MyService"/&gt;
 *
 * &lt;/beans&gt;
 * </pre>
 *
 * 实现 {@code JmsListenerConfigurer} 还可以通过
 * {@code JmsListenerEndpointRegistrar} 对端点注册进行细粒度控制。
 * 例如，下面的代码配置了一个额外的端点：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableJms
 * public class AppConfig implements JmsListenerConfigurer {
 *
 *     &#064;Override
 *     public void configureJmsListeners(JmsListenerEndpointRegistrar registrar) {
 *         SimpleJmsListenerEndpoint myEndpoint = new SimpleJmsListenerEndpoint();
 *         // ... configure the endpoint
 *         registrar.registerEndpoint(endpoint, anotherJmsListenerContainerFactory());
 *     }
 *
 *     &#064;Bean
 *     public MyService myService() {
 *         return new MyService();
 *     }
 *
 *     &#064;Bean
 *     public JmsListenerContainerFactory&lt;?&gt; anotherJmsListenerContainerFactory() {
 *         // ...
 *     }
 *
 *     // JMS infrastructure setup
 * }</pre>
 *
 * 请注意，所有实现了 {@code JmsListenerConfigurer} 的 bean 都会被检测到
 * 并以类似方式调用。如果使用 XML 配置，上面的示例可以转换为在上下文中
 * 注册的常规 bean 定义。
 *
 * @author Stephane Nicoll
 * @since 4.1
 * @see JmsListener
 * @see JmsListenerAnnotationBeanPostProcessor
 * @see org.springframework.jms.config.JmsListenerEndpointRegistrar
 * @see org.springframework.jms.config.JmsListenerEndpointRegistry
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(JmsBootstrapConfiguration.class)
public @interface EnableJms {
}
