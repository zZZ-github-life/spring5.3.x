/*
 * Copyright 2002-2019 the original author or authors.
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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.framework.AopInfrastructureBean;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.EmbeddedValueResolver;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.jms.config.JmsListenerConfigUtils;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerEndpointRegistrar;
import org.springframework.jms.config.JmsListenerEndpointRegistry;
import org.springframework.jms.config.MethodJmsListenerEndpoint;
import org.springframework.lang.Nullable;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;
import org.springframework.messaging.handler.invocation.InvocableHandlerMethod;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;

/**
 * Bean 后处理器，注册带 {@link JmsListener} 注解的方法，
 * 使其按照注解的属性，由
 * {@link org.springframework.jms.config.JmsListenerContainerFactory}
 * 在底层创建的 JMS 消息监听器容器调用。
 *
 * <p>带注解的方法可以使用 {@link JmsListener} 所定义的灵活参数。
 *
 * <p>此后期处理器由 Spring 的 {@code <jms:annotation-driven>} XML 元素
 * 以及 {@link EnableJms} 注解自动注册。
 *
 * <p>自动检测容器中的任何 {@link JmsListenerConfigurer} 实例，
 * 从而可以自定义要使用的注册表、默认容器工厂，
 * 或对端点注册进行细粒度控制。完整的使用细节请参见
 * {@link EnableJms} 的 javadoc。
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @since 4.1
 * @see JmsListener
 * @see EnableJms
 * @see JmsListenerConfigurer
 * @see JmsListenerEndpointRegistrar
 * @see JmsListenerEndpointRegistry
 * @see org.springframework.jms.config.JmsListenerEndpoint
 * @see MethodJmsListenerEndpoint
 */
public class JmsListenerAnnotationBeanPostProcessor
		implements MergedBeanDefinitionPostProcessor, Ordered, BeanFactoryAware, SmartInitializingSingleton {

	/**
	 * 默认 {@link JmsListenerContainerFactory} 的 bean 名称。
	 */
	static final String DEFAULT_JMS_LISTENER_CONTAINER_FACTORY_BEAN_NAME = "jmsListenerContainerFactory";


	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private String containerFactoryBeanName = DEFAULT_JMS_LISTENER_CONTAINER_FACTORY_BEAN_NAME;

	@Nullable
	private JmsListenerEndpointRegistry endpointRegistry;

	private final MessageHandlerMethodFactoryAdapter messageHandlerMethodFactory =
			new MessageHandlerMethodFactoryAdapter();

	@Nullable
	private BeanFactory beanFactory;

	@Nullable
	private StringValueResolver embeddedValueResolver;

	private final JmsListenerEndpointRegistrar registrar = new JmsListenerEndpointRegistrar();

	private final AtomicInteger counter = new AtomicInteger();

	private final Set<Class<?>> nonAnnotatedClasses = Collections.newSetFromMap(new ConcurrentHashMap<>(64));


	@Override
	public int getOrder() {
		return LOWEST_PRECEDENCE;
	}

	/**
	 * 设置默认要使用的 {@link JmsListenerContainerFactory} 的名称。
	 * <p>如果未指定，则假定已定义 "jmsListenerContainerFactory"。
	 */
	public void setContainerFactoryBeanName(String containerFactoryBeanName) {
		this.containerFactoryBeanName = containerFactoryBeanName;
	}

	/**
	 * 设置将持有已创建端点并管理相关监听器容器生命周期的
	 * {@link JmsListenerEndpointRegistry}。
	 */
	public void setEndpointRegistry(JmsListenerEndpointRegistry endpointRegistry) {
		this.endpointRegistry = endpointRegistry;
	}

	/**
	 * 设置用于配置消息监听器的 {@link MessageHandlerMethodFactory}，
	 * 该监听器负责服务此处理器检测到的端点。
	 * <p>默认使用 {@link DefaultMessageHandlerMethodFactory}，
	 * 可以进一步配置它，以支持额外的方法参数，
	 * 或自定义转换和校验支持。详情请参见
	 * {@link DefaultMessageHandlerMethodFactory} 的 Javadoc。
	 */
	public void setMessageHandlerMethodFactory(MessageHandlerMethodFactory messageHandlerMethodFactory) {
		this.messageHandlerMethodFactory.setMessageHandlerMethodFactory(messageHandlerMethodFactory);
	}

	/**
	 * 提供 {@link BeanFactory} 是可选的；如果未设置，
	 * 将不会自动检测 {@link JmsListenerConfigurer} bean，
	 * 并且必须显式配置 {@link #setEndpointRegistry 端点注册表}。
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
		if (beanFactory instanceof ConfigurableBeanFactory) {
			this.embeddedValueResolver = new EmbeddedValueResolver((ConfigurableBeanFactory) beanFactory);
		}
		this.registrar.setBeanFactory(beanFactory);
	}


	@Override
	public void afterSingletonsInstantiated() {
		// Remove resolved singleton classes from cache
		this.nonAnnotatedClasses.clear();

		if (this.beanFactory instanceof ListableBeanFactory) {
			// Apply JmsListenerConfigurer beans from the BeanFactory, if any
			Map<String, JmsListenerConfigurer> beans =
					((ListableBeanFactory) this.beanFactory).getBeansOfType(JmsListenerConfigurer.class);
			List<JmsListenerConfigurer> configurers = new ArrayList<>(beans.values());
			AnnotationAwareOrderComparator.sort(configurers);
			for (JmsListenerConfigurer configurer : configurers) {
				configurer.configureJmsListeners(this.registrar);
			}
		}

		if (this.containerFactoryBeanName != null) {
			this.registrar.setContainerFactoryBeanName(this.containerFactoryBeanName);
		}

		if (this.registrar.getEndpointRegistry() == null) {
			// Determine JmsListenerEndpointRegistry bean from the BeanFactory
			if (this.endpointRegistry == null) {
				Assert.state(this.beanFactory != null, "BeanFactory must be set to find endpoint registry by bean name");
				this.endpointRegistry = this.beanFactory.getBean(
						JmsListenerConfigUtils.JMS_LISTENER_ENDPOINT_REGISTRY_BEAN_NAME, JmsListenerEndpointRegistry.class);
			}
			this.registrar.setEndpointRegistry(this.endpointRegistry);
		}


		// Set the custom handler method factory once resolved by the configurer
		MessageHandlerMethodFactory handlerMethodFactory = this.registrar.getMessageHandlerMethodFactory();
		if (handlerMethodFactory != null) {
			this.messageHandlerMethodFactory.setMessageHandlerMethodFactory(handlerMethodFactory);
		}

		// Actually register all listeners
		this.registrar.afterPropertiesSet();
	}


	@Override
	public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof AopInfrastructureBean || bean instanceof JmsListenerContainerFactory ||
				bean instanceof JmsListenerEndpointRegistry) {
			// Ignore AOP infrastructure such as scoped proxies.
			return bean;
		}

		Class<?> targetClass = AopProxyUtils.ultimateTargetClass(bean);
		if (!this.nonAnnotatedClasses.contains(targetClass) &&
				AnnotationUtils.isCandidateClass(targetClass, JmsListener.class)) {
			Map<Method, Set<JmsListener>> annotatedMethods = MethodIntrospector.selectMethods(targetClass,
					(MethodIntrospector.MetadataLookup<Set<JmsListener>>) method -> {
						Set<JmsListener> listenerMethods = AnnotatedElementUtils.getMergedRepeatableAnnotations(
								method, JmsListener.class, JmsListeners.class);
						return (!listenerMethods.isEmpty() ? listenerMethods : null);
					});
			if (annotatedMethods.isEmpty()) {
				this.nonAnnotatedClasses.add(targetClass);
				if (logger.isTraceEnabled()) {
					logger.trace("No @JmsListener annotations found on bean type: " + targetClass);
				}
			}
			else {
				// Non-empty set of methods
				annotatedMethods.forEach((method, listeners) ->
						listeners.forEach(listener -> processJmsListener(listener, method, bean)));
				if (logger.isDebugEnabled()) {
					logger.debug(annotatedMethods.size() + " @JmsListener methods processed on bean '" + beanName +
							"': " + annotatedMethods);
				}
			}
		}
		return bean;
	}

	/**
	 * 处理给定方法上的给定 {@link JmsListener} 注解，
	 * 为给定 bean 实例注册相应的端点。
	 * @param jmsListener 要处理的注解
	 * @param mostSpecificMethod 被注解的方法
	 * @param bean 要在其上调用该方法的实例
	 * @see #createMethodJmsListenerEndpoint()
	 * @see JmsListenerEndpointRegistrar#registerEndpoint
	 */
	protected void processJmsListener(JmsListener jmsListener, Method mostSpecificMethod, Object bean) {
		Method invocableMethod = AopUtils.selectInvocableMethod(mostSpecificMethod, bean.getClass());

		MethodJmsListenerEndpoint endpoint = createMethodJmsListenerEndpoint();
		endpoint.setBean(bean);
		endpoint.setMethod(invocableMethod);
		endpoint.setMostSpecificMethod(mostSpecificMethod);
		endpoint.setMessageHandlerMethodFactory(this.messageHandlerMethodFactory);
		endpoint.setEmbeddedValueResolver(this.embeddedValueResolver);
		endpoint.setBeanFactory(this.beanFactory);
		endpoint.setId(getEndpointId(jmsListener));
		endpoint.setDestination(resolve(jmsListener.destination()));
		if (StringUtils.hasText(jmsListener.selector())) {
			endpoint.setSelector(resolve(jmsListener.selector()));
		}
		if (StringUtils.hasText(jmsListener.subscription())) {
			endpoint.setSubscription(resolve(jmsListener.subscription()));
		}
		if (StringUtils.hasText(jmsListener.concurrency())) {
			endpoint.setConcurrency(resolve(jmsListener.concurrency()));
		}

		JmsListenerContainerFactory<?> factory = null;
		String containerFactoryBeanName = resolve(jmsListener.containerFactory());
		if (StringUtils.hasText(containerFactoryBeanName)) {
			Assert.state(this.beanFactory != null, "BeanFactory must be set to obtain container factory by bean name");
			try {
				factory = this.beanFactory.getBean(containerFactoryBeanName, JmsListenerContainerFactory.class);
			}
			catch (NoSuchBeanDefinitionException ex) {
				throw new BeanInitializationException("Could not register JMS listener endpoint on [" +
						mostSpecificMethod + "], no " + JmsListenerContainerFactory.class.getSimpleName() +
						" with id '" + containerFactoryBeanName + "' was found in the application context", ex);
			}
		}

		this.registrar.registerEndpoint(endpoint, factory);
	}

	/**
	 * 实例化一个空的 {@link MethodJmsListenerEndpoint}，
	 * 以便在 {@link #processJmsListener} 中使用提供的参数进行进一步配置。
	 * @return 新的 {@code MethodJmsListenerEndpoint} 或其子类
	 * @since 4.1.9
	 * @see MethodJmsListenerEndpoint#createMessageListenerInstance()
	 */
	protected MethodJmsListenerEndpoint createMethodJmsListenerEndpoint() {
		return new MethodJmsListenerEndpoint();
	}

	private String getEndpointId(JmsListener jmsListener) {
		if (StringUtils.hasText(jmsListener.id())) {
			String id = resolve(jmsListener.id());
			return (id != null ? id : "");
		}
		else {
			return "org.springframework.jms.JmsListenerEndpointContainer#" + this.counter.getAndIncrement();
		}
	}

	@Nullable
	private String resolve(String value) {
		return (this.embeddedValueResolver != null ? this.embeddedValueResolver.resolveStringValue(value) : value);
	}


	/**
	 * {@link MessageHandlerMethodFactory} 适配器，提供可配置的底层实例供使用。
	 * 如果要使用的工厂在端点已注册但尚未创建时才确定，此适配器非常有用。
	 * @see JmsListenerEndpointRegistrar#setMessageHandlerMethodFactory
	 */
	private class MessageHandlerMethodFactoryAdapter implements MessageHandlerMethodFactory {

		@Nullable
		private MessageHandlerMethodFactory messageHandlerMethodFactory;

		public void setMessageHandlerMethodFactory(MessageHandlerMethodFactory messageHandlerMethodFactory) {
			this.messageHandlerMethodFactory = messageHandlerMethodFactory;
		}

		@Override
		public InvocableHandlerMethod createInvocableHandlerMethod(Object bean, Method method) {
			return getMessageHandlerMethodFactory().createInvocableHandlerMethod(bean, method);
		}

		private MessageHandlerMethodFactory getMessageHandlerMethodFactory() {
			if (this.messageHandlerMethodFactory == null) {
				this.messageHandlerMethodFactory = createDefaultJmsHandlerMethodFactory();
			}
			return this.messageHandlerMethodFactory;
		}

		private MessageHandlerMethodFactory createDefaultJmsHandlerMethodFactory() {
			DefaultMessageHandlerMethodFactory defaultFactory = new DefaultMessageHandlerMethodFactory();
			if (beanFactory != null) {
				defaultFactory.setBeanFactory(beanFactory);
			}
			defaultFactory.afterPropertiesSet();
			return defaultFactory;
		}
	}

}
