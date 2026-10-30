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

package org.springframework.web.reactive.result.method.annotation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import reactor.core.publisher.Mono;

import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.lang.Nullable;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolverSupport;
import org.springframework.web.server.ServerErrorException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

/**
 * 用于从命名值解析方法参数的抽象基类。请求参数、请求头和路径变量都是命名值的示例。每个命名值可以具有名称、必需标志和默认值。
 *
 * <p>子类定义如何执行以下操作：
 * <ul>
 * <li>获取方法参数的命名值信息
 * <li>将名称解析为参数值
 * <li>在需要参数值但值缺失时进行处理
 * <li>可选地处理已解析的值
 * </ul>
 *
 * <p>默认值字符串可以包含 ${...} 占位符和 Spring 表达式语言 #{...} 表达式。为此，必须向类构造函数提供 {@link ConfigurableBeanFactory}。
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public abstract class AbstractNamedValueArgumentResolver extends HandlerMethodArgumentResolverSupport {

	@Nullable
	private final ConfigurableBeanFactory configurableBeanFactory;

	@Nullable
	private final BeanExpressionContext expressionContext;

	private final Map<MethodParameter, NamedValueInfo> namedValueInfoCache = new ConcurrentHashMap<>(256);


	/**
	 * 创建新的 {@link AbstractNamedValueArgumentResolver} 实例。
	 * @param factory 用于解析默认值中的 {@code ${...}} 占位符
	 * 和 {@code #{...}} SpEL 表达式的 Bean 工厂；
	 * 如果默认值预计不包含表达式则为 {@code null}
	 * @param registry 用于检查响应式类型包装
	 */
	public AbstractNamedValueArgumentResolver(@Nullable ConfigurableBeanFactory factory,
			ReactiveAdapterRegistry registry) {

		super(registry);
		this.configurableBeanFactory = factory;
		this.expressionContext = (factory != null ? new BeanExpressionContext(factory, null) : null);
	}


	@Override
	public Mono<Object> resolveArgument(
			MethodParameter parameter, BindingContext bindingContext, ServerWebExchange exchange) {

		NamedValueInfo namedValueInfo = getNamedValueInfo(parameter);
		MethodParameter nestedParameter = parameter.nestedIfOptional();

		Object resolvedName = resolveEmbeddedValuesAndExpressions(namedValueInfo.name);
		if (resolvedName == null) {
			return Mono.error(new IllegalArgumentException(
					"Specified name must not resolve to null: [" + namedValueInfo.name + "]"));
		}

		Model model = bindingContext.getModel();

		return resolveName(resolvedName.toString(), nestedParameter, exchange)
				.flatMap(arg -> {
					if ("".equals(arg) && namedValueInfo.defaultValue != null) {
						arg = resolveEmbeddedValuesAndExpressions(namedValueInfo.defaultValue);
					}
					arg = applyConversion(arg, namedValueInfo, parameter, bindingContext, exchange);
					handleResolvedValue(arg, namedValueInfo.name, parameter, model, exchange);
					return Mono.justOrEmpty(arg);
				})
				.switchIfEmpty(getDefaultValue(
						namedValueInfo, parameter, bindingContext, model, exchange));
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
	 * 为给定方法参数创建 {@link NamedValueInfo} 对象。实现通常通过 {@link MethodParameter#getParameterAnnotation(Class)} 获取方法注解。
	 * @param parameter 方法参数
	 * @return 命名值信息
	 */
	protected abstract NamedValueInfo createNamedValueInfo(MethodParameter parameter);

	/**
	 * 基于给定的 NamedValueInfo 创建具有净化值的新 NamedValueInfo。
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
		String defaultValue = (ValueConstants.DEFAULT_NONE.equals(info.defaultValue) ? null : info.defaultValue);
		return new NamedValueInfo(name, info.required, defaultValue);
	}

	/**
	 * 解析给定的注解指定值，其中可能包含占位符和表达式。
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
	 * 将给定的参数类型和值名称解析为参数值。
	 * @param name 正在解析的值的名称
	 * @param parameter 要解析为参数值的方法参数
	 * （在 {@link java.util.Optional} 声明的情况下已预先嵌套）
	 * @param exchange 当前交换
	 * @return 解析后的参数（可能为空 {@link Mono}）
	 */
	protected abstract Mono<Object> resolveName(String name, MethodParameter parameter, ServerWebExchange exchange);

	/**
	 * 如有必要，应用类型转换。
	 */
	@Nullable
	private Object applyConversion(@Nullable Object value, NamedValueInfo namedValueInfo, MethodParameter parameter,
			BindingContext bindingContext, ServerWebExchange exchange) {

		WebDataBinder binder = bindingContext.createDataBinder(exchange, namedValueInfo.name);
		try {
			value = binder.convertIfNecessary(value, parameter.getParameterType(), parameter);
		}
		catch (ConversionNotSupportedException ex) {
			throw new ServerErrorException("Conversion not supported.", parameter, ex);
		}
		catch (TypeMismatchException ex) {
			throw new ServerWebInputException("Type mismatch.", parameter, ex);
		}
		return value;
	}

	/**
	 * 解析默认值（如果有）。
	 */
	private Mono<Object> getDefaultValue(NamedValueInfo namedValueInfo, MethodParameter parameter,
			BindingContext bindingContext, Model model, ServerWebExchange exchange) {

		return Mono.fromSupplier(() -> {
			Object value = null;
			if (namedValueInfo.defaultValue != null) {
				value = resolveEmbeddedValuesAndExpressions(namedValueInfo.defaultValue);
			}
			else if (namedValueInfo.required && !parameter.isOptional()) {
				handleMissingValue(namedValueInfo.name, parameter, exchange);
			}
			value = handleNullValue(namedValueInfo.name, value, parameter.getNestedParameterType());
			value = applyConversion(value, namedValueInfo, parameter, bindingContext, exchange);
			handleResolvedValue(value, namedValueInfo.name, parameter, model, exchange);
			return value;
		});
	}

	/**
	 * 在需要命名值，但 {@link #resolveName(String, MethodParameter, ServerWebExchange)} 返回 {@code null} 且没有默认值时调用。子类通常在此情况下抛出异常。
	 * @param name 值的名称
	 * @param parameter 方法参数
	 * @param exchange 当前交换
	 */
	@SuppressWarnings("UnusedParameters")
	protected void handleMissingValue(String name, MethodParameter parameter, ServerWebExchange exchange) {
		handleMissingValue(name, parameter);
	}

	/**
	 * 在需要命名值，但 {@link #resolveName(String, MethodParameter, ServerWebExchange)} 返回 {@code null} 且没有默认值时调用。子类通常在此情况下抛出异常。
	 * @param name 值的名称
	 * @param parameter 方法参数
	 */
	protected void handleMissingValue(String name, MethodParameter parameter) {
		String typeName = parameter.getNestedParameterType().getSimpleName();
		throw new ServerWebInputException("Missing argument '" + name + "' for method " +
				"parameter of type " + typeName, parameter);
	}

	/**
	 * {@code null} 会导致 {@code boolean} 的值为 {@code false}，或对其他原始类型抛出异常。
	 */
	@Nullable
	private Object handleNullValue(String name, @Nullable Object value, Class<?> paramType) {
		if (value == null) {
			if (Boolean.TYPE.equals(paramType)) {
				return Boolean.FALSE;
			}
			else if (paramType.isPrimitive()) {
				throw new IllegalStateException("Optional " + paramType.getSimpleName() +
						" parameter '" + name + "' is present but cannot be translated into a" +
						" null value due to being declared as a primitive type. " +
						"Consider declaring it as object wrapper for the corresponding primitive type.");
			}
		}
		return value;
	}

	/**
	 * 在值解析后调用。
	 * @param arg 解析后的参数值
	 * @param name 参数名称
	 * @param parameter 参数的类型
	 * @param model 模型
	 * @param exchange 当前交换
	 */
	@SuppressWarnings("UnusedParameters")
	protected void handleResolvedValue(
			@Nullable Object arg, String name, MethodParameter parameter, Model model, ServerWebExchange exchange) {
	}


	/**
	 * 表示命名值的信息，包括名称、是否必需以及默认值。
	 */
	protected static class NamedValueInfo {

		private final String name;

		private final boolean required;

		@Nullable
		private final String defaultValue;

		public NamedValueInfo(String name, boolean required, @Nullable String defaultValue) {
			this.name = name;
			this.required = required;
			this.defaultValue = defaultValue;
		}
	}

}
