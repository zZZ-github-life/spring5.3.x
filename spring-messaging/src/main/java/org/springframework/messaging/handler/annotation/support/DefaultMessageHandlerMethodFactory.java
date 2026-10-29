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

package org.springframework.messaging.handler.annotation.support;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.lang.Nullable;
import org.springframework.messaging.converter.GenericMessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolverComposite;
import org.springframework.messaging.handler.invocation.InvocableHandlerMethod;
import org.springframework.util.Assert;
import org.springframework.validation.Validator;

/**
 * 默认的 {@link MessageHandlerMethodFactory} 实现，创建 {@link InvocableHandlerMethod}
 * 并配有必要 {@link HandlerMethodArgumentResolver} 实例，以检测和处理
 * {@link org.springframework.messaging.handler.annotation.MessageMapping MessageMapping}
 * 定义的大多数用例。
 *
 * <p>可以添加额外的方法参数解析器，以自定义可处理的方法签名。
 *
 * <p>默认情况下，校验过程会重定向到空操作实现，参见
 * {@link #setValidator(Validator)} 进行自定义。可以采用类似方式自定义
 * {@link ConversionService}，以调整消息载荷的转换方式。
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @since 4.1
 * @see #setConversionService
 * @see #setValidator
 * @see #setCustomArgumentResolvers
 */
public class DefaultMessageHandlerMethodFactory
		implements MessageHandlerMethodFactory, BeanFactoryAware, InitializingBean {

	private ConversionService conversionService = new DefaultFormattingConversionService();

	@Nullable
	private MessageConverter messageConverter;

	@Nullable
	private Validator validator;

	@Nullable
	private List<HandlerMethodArgumentResolver> customArgumentResolvers;

	private final HandlerMethodArgumentResolverComposite argumentResolvers =
			new HandlerMethodArgumentResolverComposite();

	@Nullable
	private BeanFactory beanFactory;


	/**
	 * 设置用于转换原始消息载荷或头的 {@link ConversionService}。
	 * @see HeaderMethodArgumentResolver
	 * @see GenericMessageConverter
	 */
	public void setConversionService(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	/**
	 * 设置要使用的 {@link MessageConverter}。默认使用 {@link GenericMessageConverter}。
	 * @see GenericMessageConverter
	 */
	public void setMessageConverter(MessageConverter messageConverter) {
		this.messageConverter = messageConverter;
	}

	/**
	 * 设置用于校验 {@code @Payload} 参数的 Validator 实例。
	 * @see org.springframework.validation.annotation.Validated
	 * @see PayloadMethodArgumentResolver
	 */
	public void setValidator(Validator validator) {
		this.validator = validator;
	}

	/**
	 * 设置自定义 {@code HandlerMethodArgumentResolver} 列表，
	 * 这些解析器将在受支持参数类型的解析器之后使用。
	 * @param customArgumentResolvers the list of resolvers (never {@code null})
	 */
	public void setCustomArgumentResolvers(List<HandlerMethodArgumentResolver> customArgumentResolvers) {
		this.customArgumentResolvers = customArgumentResolvers;
	}

	/**
	 * 配置受支持参数类型的完整列表，实际上会覆盖默认配置的列表。
	 * 这是一个高级选项。对于大多数用例，使用
	 * {@link #setCustomArgumentResolvers(java.util.List)} 就足够了。
	 */
	public void setArgumentResolvers(@Nullable List<HandlerMethodArgumentResolver> argumentResolvers) {
		if (argumentResolvers == null) {
			this.argumentResolvers.clear();
			return;
		}
		this.argumentResolvers.addResolvers(argumentResolvers);
	}

	/**
	 * {@link BeanFactory} 仅需用于处理器方法参数中的占位符解析；
	 * 在其他情况下是可选的。
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	@Override
	public void afterPropertiesSet() {
		if (this.messageConverter == null) {
			this.messageConverter = new GenericMessageConverter(this.conversionService);
		}
		if (this.argumentResolvers.getResolvers().isEmpty()) {
			this.argumentResolvers.addResolvers(initArgumentResolvers());
		}
	}


	@Override
	public InvocableHandlerMethod createInvocableHandlerMethod(Object bean, Method method) {
		InvocableHandlerMethod handlerMethod = new InvocableHandlerMethod(bean, method);
		handlerMethod.setMessageMethodArgumentResolvers(this.argumentResolvers);
		return handlerMethod;
	}

	protected List<HandlerMethodArgumentResolver> initArgumentResolvers() {
		List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>();
		ConfigurableBeanFactory beanFactory = (this.beanFactory instanceof ConfigurableBeanFactory ?
				(ConfigurableBeanFactory) this.beanFactory : null);

		// Annotation-based argument resolution
		resolvers.add(new HeaderMethodArgumentResolver(this.conversionService, beanFactory));
		resolvers.add(new HeadersMethodArgumentResolver());

		// Type-based argument resolution
		resolvers.add(new MessageMethodArgumentResolver(this.messageConverter));

		if (this.customArgumentResolvers != null) {
			resolvers.addAll(this.customArgumentResolvers);
		}

		Assert.notNull(this.messageConverter, "MessageConverter not configured");
		resolvers.add(new PayloadMethodArgumentResolver(this.messageConverter, this.validator));

		return resolvers;
	}

}
