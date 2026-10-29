/*
 * Copyright 2002-2023 the original author or authors.
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
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.messaging.handler.annotation.MessageMapping;

/**
 * 将方法标记为指定 {@link #destination} 上 JMS 消息监听器目标的注解。
 * {@link #containerFactory} 指定用于构建 JMS 监听器容器的
 * {@link org.springframework.jms.config.JmsListenerContainerFactory}。
 * 如果未设置，则假定存在 bean 名称为 {@code jmsListenerContainerFactory}
 * 的<em>默认</em>容器工厂，除非已通过配置提供了显式的默认值。
 *
 * <p><b>建议设置一个自定义的
 * {@link org.springframework.jms.config.DefaultJmsListenerContainerFactory} bean。</b>
 * 对于生产用途，通常需要微调超时和恢复设置。
 * 最重要的是，默认的 'AUTO_ACKNOWLEDGE' 模式不提供可靠性保证，
 * 因此如有可靠性需求，请务必使用事务性会话。
 *
 * <p>对 {@code @JmsListener} 注解的处理通过注册
 * {@link JmsListenerAnnotationBeanPostProcessor} 来完成。这可以手动完成，
 * 也可以（更方便地）通过 {@code <jms:annotation-driven/>} 元素或
 * {@link EnableJms @EnableJms} 注解完成。
 *
 * <p>带注解的 JMS 监听器方法可以具有灵活的方法签名，与
 * {@link MessageMapping} 所提供的类似：
 * <ul>
 * <li>{@link javax.jms.Session}，用于访问 JMS 会话</li>
 * <li>{@link javax.jms.Message} 或其子类之一，用于访问原始 JMS 消息</li>
 * <li>{@link org.springframework.messaging.Message}，用于使用 Spring 消息抽象中的对应类型</li>
 * <li>带 {@link org.springframework.messaging.handler.annotation.Payload @Payload} 注解的方法参数，
 * 包括对校验的支持</li>
 * <li>带 {@link org.springframework.messaging.handler.annotation.Header @Header} 注解的方法参数，
 * 用于提取特定的头值，包括由
 * {@link org.springframework.jms.support.JmsHeaders} 定义的标准 JMS 头</li>
 * <li>带 {@link org.springframework.messaging.handler.annotation.Headers @Headers} 注解的方法参数，
 * 该参数还必须可赋值给 {@link java.util.Map}，以便访问所有头</li>
 * <li>{@link org.springframework.messaging.MessageHeaders} 参数，用于访问所有头</li>
 * <li>{@link org.springframework.messaging.support.MessageHeaderAccessor} 或
 * {@link org.springframework.jms.support.JmsMessageHeaderAccessor}，
 * 用于便捷地访问所有方法参数</li>
 * </ul>
 *
 * <p>带注解的方法可以具有非 {@code void} 返回类型。此时，方法调用的结果
 * 将作为 JMS 回复发送到由传入消息的 {@code JMSReplyTO} 头所定义的目标。
 * 如果未设置该头，可以通过在方法声明中添加
 * {@link org.springframework.messaging.handler.annotation.SendTo @SendTo}
 * 来提供默认目标。
 *
 * <p>此注解可用作<em>元注解</em>，以创建带属性覆盖的自定义<em>组合注解</em>。
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 4.1
 * @see EnableJms
 * @see JmsListenerAnnotationBeanPostProcessor
 * @see JmsListeners
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(JmsListeners.class)
@MessageMapping
public @interface JmsListener {

	/**
	 * 管理此端点的容器的唯一标识符。
	 * <p>如果未指定，则会提供一个自动生成的标识符。
	 * @see org.springframework.jms.config.JmsListenerEndpointRegistry#getListenerContainer(String)
	 */
	String id() default "";

	/**
	 * 用于创建服务于此端点的消息监听器容器的
	 * {@link org.springframework.jms.config.JmsListenerContainerFactory} 的 bean 名称。
	 * <p>如果未指定，则使用默认容器工厂（如果有）。
	 */
	String containerFactory() default "";

	/**
	 * 此监听器的目标名称，通过容器范围的
	 * {@link org.springframework.jms.support.destination.DestinationResolver} 策略解析。
	 */
	String destination();

	/**
	 * 持久订阅的名称（如果有）。
	 * <p>从 Spring Framework 5.3.26 开始，如果未指定显式的订阅名称，
	 * 将基于带注解的监听器方法的完全限定名生成默认订阅名称 &mdash;
	 * 例如，对于 {@code org.example.jms.ProductListener} 类中的
	 * {@code processRequest(...)} 监听器方法，为
	 * {@code "org.example.jms.ProductListener.processRequest"}。
	 */
	String subscription() default "";

	/**
	 * JMS 消息选择器表达式（如果有）。
	 * <p>有关选择器表达式的详细定义，请参见 JMS 规范。
	 */
	String selector() default "";

	/**
	 * 监听器的并发上限（如果有）。覆盖用于创建监听器容器的
	 * 容器工厂所定义的值。
	 * <p>并发上限可以是 "lower-upper"（下限-上限）形式的字符串 &mdash;
	 * 例如 "5-10" &mdash; 也可以只是上限字符串 &mdash; 例如 "10"，
	 * 此时下限为 1。
	 * <p>请注意，底层容器可能支持也可能不支持所有特性。
	 * 例如，它可能无法伸缩，此时只使用上限。
	 */
	String concurrency() default "";

}
