/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.messaging.handler.annotation.reactive;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.ValueConstants;
import org.springframework.messaging.handler.invocation.reactive.SyncHandlerMethodArgumentResolver;
import org.springframework.util.ClassUtils;

/**
 * 从命名值（例如消息头或目标变量）解析方法参数的抽象基类。
 * 命名值可以包含名称、必需标志和默认值中的一项或多项。
 *
 * <p>子类只需定义具体步骤，例如如何从方法参数获取命名值详情、
 * 如何解析参数值，或如何处理缺失值。
 *
 *  <p>默认值字符串可以包含 ${...} 占位符和 Spring 表达式语言 {@code #{...}} 表达式；
 * 如果向类构造函数提供了 {@link ConfigurableBeanFactory}，这些内容将被解析。
 *
 * <p>使用 {@link ConversionService} 将解析出的 String 参数值转换为
 * 预期的目标方法参数类型。
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
public abstract class AbstractNamedValueMethodArgumentResolver implements SyncHandlerMethodArgumentResolver {

	private final ConversionService conversionService;

	@Nullable
	private final ConfigurableBeanFactory configurableBeanFactory;

	@Nullable
	private final BeanExpressionContext expressionContext;

	private final Map<MethodParameter, NamedValueInfo> namedValueInfoCache = new ConcurrentHashMap<>(256);


	/**
	 * 使用 {@link ConversionService} 和 {@link BeanFactory} 的构造函数。
	 * @param conversionService conversion service for converting String values
	 * to the target method parameter type
	 * @param beanFactory a bean factory for resolving {@code ${...}}
	 * placeholders and {@code #{...}} SpEL expressions in default values
	 */
	protected AbstractNamedValueMethodArgumentResolver(ConversionService conversionService,
			@Nullable ConfigurableBeanFactory beanFactory) {

		this.conversionService = conversionService;
		this.configurableBeanFactory = beanFactory;
		this.expressionContext = (beanFactory != null ? new BeanExpressionContext(beanFactory, null) : null);
	}


	@Override
	public Object resolveArgumentValue(MethodParameter parameter, Message<?> message) {
		NamedValueInfo namedValueInfo = getNamedValueInfo(parameter);
		MethodParameter nestedParameter = parameter.nestedIfOptional();

		Object resolvedName = resolveEmbeddedValuesAndExpressions(namedValueInfo.name);
		if (resolvedName == null) {
			throw new IllegalArgumentException(
					"Specified name must not resolve to null: [" + namedValueInfo.name + "]");
		}

		Object arg = resolveArgumentInternal(nestedParameter, message, resolvedName.toString());
		if (arg == null) {
			if (namedValueInfo.defaultValue != null) {
				arg = resolveEmbeddedValuesAndExpressions(namedValueInfo.defaultValue);
			}
			else if (namedValueInfo.required && !nestedParameter.isOptional()) {
				handleMissingValue(namedValueInfo.name, nestedParameter, message);
			}
			arg = handleNullValue(namedValueInfo.name, arg, nestedParameter.getNestedParameterType());
		}
		else if ("".equals(arg) && namedValueInfo.defaultValue != null) {
			arg = resolveEmbeddedValuesAndExpressions(namedValueInfo.defaultValue);
		}

		if (parameter != nestedParameter || !ClassUtils.isAssignableValue(parameter.getParameterType(), arg)) {
			arg = this.conversionService.convert(arg, TypeDescriptor.forObject(arg), new TypeDescriptor(parameter));
			// Check for null value after conversion of incoming argument value
			if (arg == null && namedValueInfo.defaultValue == null &&
					namedValueInfo.required && !nestedParameter.isOptional()) {
				handleMissingValue(namedValueInfo.name, nestedParameter, message);
			}
		}

		return arg;
	}

	/**
	 * 获取给定方法参数的命名值。
	 */
	private NamedValueInfo getNamedValueInfo(MethodParameter parameter) {
		NamedValueInfo namedValueInfo = this.namedValueInfoCache.get(parameter);
		if (namedValueInfo == null) {
			namedValueInfo = createNamedValueInfo(parameter);
			namedValueInfo = updateNamedValueInfo(parameter, namedValueInfo);
			this.namedValueInfoCache.put(parameter, namedValueInfo);
		}
		return namedValueInfo;
	}

	/**
	 * 为给定方法参数创建 {@link NamedValueInfo} 对象。
	 * 实现通常通过 {@link MethodParameter#getParameterAnnotation(Class)}
	 * 获取方法注解。
	 * @param parameter the method parameter
	 * @return the named value information
	 */
	protected abstract NamedValueInfo createNamedValueInfo(MethodParameter parameter);

	/**
	 * 如有必要，回退到类文件中的参数名称，并将
	 * {@link ValueConstants#DEFAULT_NONE} 替换为 null。
	 */
	private NamedValueInfo updateNamedValueInfo(MethodParameter parameter, NamedValueInfo info) {
		String name = info.name;
		if (info.name.isEmpty()) {
			name = parameter.getParameterName();
			if (name == null) {
				throw new IllegalArgumentException(
						"Name for argument of type [" + parameter.getNestedParameterType().getName() +
						"] not specified, and parameter name information not found in class file either.");
			}
		}
		return new NamedValueInfo(name, info.required,
				ValueConstants.DEFAULT_NONE.equals(info.defaultValue) ? null : info.defaultValue);
	}

	/**
	 * 解析给定的注解指定值，该值可能包含占位符和表达式。
	 */
	@Nullable
	private Object resolveEmbeddedValuesAndExpressions(String value) {
		if (this.configurableBeanFactory == null || this.expressionContext == null) {
			return value;
		}
		String placeholdersResolved = this.configurableBeanFactory.resolveEmbeddedValue(value);
		BeanExpressionResolver exprResolver = this.configurableBeanFactory.getBeanExpressionResolver();
		if (exprResolver == null) {
			return value;
		}
		return exprResolver.evaluate(placeholdersResolved, this.expressionContext);
	}

	/**
	 * 将给定参数类型和值名称解析为参数值。
	 * @param parameter the method parameter to resolve to an argument value
	 * @param message the current request
	 * @param name the name of the value being resolved
	 * @return the resolved argument. May be {@code null}
	 */
	@Nullable
	protected abstract Object resolveArgumentInternal(MethodParameter parameter, Message<?> message, String name);

	/**
	 * 当需要值，但 {@link #resolveArgumentInternal} 返回 {@code null}
	 * 且没有默认值时调用。子类可以为此情况抛出适当的异常。
	 * @param name the name for the value
	 * @param parameter the target method parameter
	 * @param message the message being processed
	 */
	protected abstract void handleMissingValue(String name, MethodParameter parameter, Message<?> message);

	/**
	 * 处理可能为 null 的值的最后一次机会。
	 * 特别是对于 boolean 方法参数，使用 {@link Boolean#FALSE}。
	 * 对于原始类型，还会引发 ISE。
	 */
	@Nullable
	private Object handleNullValue(String name, @Nullable Object value, Class<?> paramType) {
		if (value == null) {
			if (Boolean.TYPE.equals(paramType)) {
				return Boolean.FALSE;
			}
			else if (paramType.isPrimitive()) {
				throw new IllegalStateException("Optional " + paramType + " parameter '" + name +
						"' is present but cannot be translated into a null value due to being " +
						"declared as a primitive type. Consider declaring it as object wrapper " +
						"for the corresponding primitive type.");
			}
		}
		return value;
	}


	/**
	 * 表示命名值声明。
	 */
	protected static class NamedValueInfo {

		private final String name;

		private final boolean required;

		@Nullable
		private final String defaultValue;

		protected NamedValueInfo(String name, boolean required, @Nullable String defaultValue) {
			this.name = name;
			this.required = required;
			this.defaultValue = defaultValue;
		}
	}

}
