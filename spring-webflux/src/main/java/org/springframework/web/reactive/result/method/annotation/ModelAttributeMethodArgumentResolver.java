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

package org.springframework.web.reactive.result.method.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import org.springframework.beans.BeanUtils;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.annotation.ValidationAnnotationUtils;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.bind.support.WebExchangeDataBinder;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolverSupport;
import org.springframework.web.server.ServerWebExchange;

/**
 * 解析带有 {@code @ModelAttribute} 注解的方法参数。
 *
 * <p>模型属性来自模型，或使用默认构造函数创建后添加到模型。创建后，通过数据绑定将请求（表单数据、查询参数）填充到该属性。如果参数带有 {@code @javax.validation.Valid} 或 Spring 自有的 {@code @org.springframework.validation.annotation.Validated} 注解，还可能应用校验。
 *
 * <p>当使用 {@code useDefaultResolution=true} 创建此处理器时，任何非简单类型的参数和返回值都被视为模型属性，无论是否存在 {@code @ModelAttribute}。
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 5.0
 */
public class ModelAttributeMethodArgumentResolver extends HandlerMethodArgumentResolverSupport {

	private final boolean useDefaultResolution;


	/**
	 * 带默认解析模式标志的类构造函数。
	 * @param adapterRegistry 用于在 Mono 与其他响应式类型之间进行适配
	 * @param useDefaultResolution 如果为 "true"，则不论是否存在 {@code @ModelAttribute} 注解，
	 * 非简单方法参数和返回值都被视为模型属性。
	 */
	public ModelAttributeMethodArgumentResolver(ReactiveAdapterRegistry adapterRegistry,
			boolean useDefaultResolution) {

		super(adapterRegistry);
		this.useDefaultResolution = useDefaultResolution;
	}


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		if (parameter.hasParameterAnnotation(ModelAttribute.class)) {
			return true;
		}
		else if (this.useDefaultResolution) {
			return checkParameterType(parameter, type -> !BeanUtils.isSimpleProperty(type));
		}
		return false;
	}

	@Override
	public Mono<Object> resolveArgument(
			MethodParameter parameter, BindingContext context, ServerWebExchange exchange) {

		ResolvableType type = ResolvableType.forMethodParameter(parameter);
		Class<?> resolvedType = type.resolve();
		ReactiveAdapter adapter = (resolvedType != null ? getAdapterRegistry().getAdapter(resolvedType) : null);
		ResolvableType valueType = (adapter != null ? type.getGeneric() : type);

		Assert.state(adapter == null || !adapter.isMultiValue(),
				() -> getClass().getSimpleName() + " does not support multi-value reactive type wrapper: " +
						parameter.getGenericParameterType());

		String name = ModelInitializer.getNameForParameter(parameter);
		Mono<?> valueMono = prepareAttributeMono(name, valueType, context, exchange);

		// unsafe(): we're intercepting, already serialized Publisher signals
		Sinks.One<BindingResult> bindingResultSink = Sinks.unsafe().one();

		Map<String, Object> model = context.getModel().asMap();
		model.put(BindingResult.MODEL_KEY_PREFIX + name, bindingResultSink.asMono());

		return valueMono.flatMap(value -> {
			WebExchangeDataBinder binder = context.createDataBinder(exchange, value, name);
			return (bindingDisabled(parameter) ? Mono.empty() : bindRequestParameters(binder, exchange))
					.doOnError(bindingResultSink::tryEmitError)
					.doOnSuccess(aVoid -> {
						validateIfApplicable(binder, parameter);
						BindingResult bindingResult = binder.getBindingResult();
						model.put(BindingResult.MODEL_KEY_PREFIX + name, bindingResult);
						model.put(name, value);
						// Ignore result: serialized and buffered (should never fail)
						bindingResultSink.tryEmitValue(bindingResult);
					})
					.then(Mono.fromCallable(() -> {
						BindingResult errors = binder.getBindingResult();
						if (adapter != null) {
							return adapter.fromPublisher(errors.hasErrors() ?
									Mono.error(new WebExchangeBindException(parameter, errors)) : valueMono);
						}
						else {
							if (errors.hasErrors() && !hasErrorsArgument(parameter)) {
								throw new WebExchangeBindException(parameter, errors);
							}
							return value;
						}
					}));
		});
	}

	/**
	 * 根据 {@link ModelAttribute#binding} 注解属性，确定是否应对提供的 {@link MethodParameter} 禁用绑定。
	 * @since 5.2.15
	 */
	private boolean bindingDisabled(MethodParameter parameter) {
		ModelAttribute modelAttribute = parameter.getParameterAnnotation(ModelAttribute.class);
		return (modelAttribute != null && !modelAttribute.binding());
	}

	/**
	 * 用于将请求绑定到目标对象的扩展点。
	 * @param binder 用于绑定的数据绑定器实例
	 * @param exchange 当前请求
	 * @since 5.2.6
	 */
	protected Mono<Void> bindRequestParameters(WebExchangeDataBinder binder, ServerWebExchange exchange) {
		return binder.bind(exchange);
	}

	private Mono<?> prepareAttributeMono(String attributeName, ResolvableType attributeType,
			BindingContext context, ServerWebExchange exchange) {

		Object attribute = context.getModel().asMap().get(attributeName);

		if (attribute == null) {
			attribute = findAndRemoveReactiveAttribute(context.getModel(), attributeName);
		}

		if (attribute == null) {
			return createAttribute(attributeName, attributeType.toClass(), context, exchange);
		}

		ReactiveAdapter adapter = getAdapterRegistry().getAdapter(null, attribute);
		if (adapter != null) {
			Assert.isTrue(!adapter.isMultiValue(), "Data binding only supports single-value async types");
			return Mono.from(adapter.toPublisher(attribute));
		}
		else {
			return Mono.justOrEmpty(attribute);
		}
	}

	@Nullable
	private Object findAndRemoveReactiveAttribute(Model model, String attributeName) {
		return model.asMap().entrySet().stream()
				.filter(entry -> {
					if (!entry.getKey().startsWith(attributeName)) {
						return false;
					}
					ReactiveAdapter adapter = getAdapterRegistry().getAdapter(null, entry.getValue());
					if (adapter == null) {
						return false;
					}
					String name = attributeName + ClassUtils.getShortName(adapter.getReactiveType());
					return entry.getKey().equals(name);
				})
				.findFirst()
				.map(entry -> {
					// Remove since we will be re-inserting the resolved attribute value
					model.asMap().remove(entry.getKey());
					return entry.getValue();
				})
				.orElse(null);
	}

	private Mono<?> createAttribute(
			String attributeName, Class<?> clazz, BindingContext context, ServerWebExchange exchange) {

		Constructor<?> ctor = BeanUtils.getResolvableConstructor(clazz);
		return constructAttribute(ctor, attributeName, context, exchange);
	}

	private Mono<?> constructAttribute(Constructor<?> ctor, String attributeName,
			BindingContext context, ServerWebExchange exchange) {

		if (ctor.getParameterCount() == 0) {
			// A single default constructor -> clearly a standard JavaBeans arrangement.
			return Mono.just(BeanUtils.instantiateClass(ctor));
		}

		// A single data class constructor -> resolve constructor arguments from request parameters.
		WebExchangeDataBinder binder = context.createDataBinder(exchange, null, attributeName);
		return getValuesToBind(binder, exchange).map(bindValues -> {
			String[] paramNames = BeanUtils.getParameterNames(ctor);
			Class<?>[] paramTypes = ctor.getParameterTypes();
			Object[] args = new Object[paramTypes.length];
			String fieldDefaultPrefix = binder.getFieldDefaultPrefix();
			String fieldMarkerPrefix = binder.getFieldMarkerPrefix();
			for (int i = 0; i < paramNames.length; i++) {
				String paramName = paramNames[i];
				Class<?> paramType = paramTypes[i];
				Object value = bindValues.get(paramName);
				if (value == null) {
					if (fieldDefaultPrefix != null) {
						value = bindValues.get(fieldDefaultPrefix + paramName);
					}
					if (value == null && fieldMarkerPrefix != null) {
						if (bindValues.get(fieldMarkerPrefix + paramName) != null) {
							value = binder.getEmptyValue(paramType);
						}
					}
				}
				value = (value instanceof List ? ((List<?>) value).toArray() : value);
				MethodParameter methodParam = new MethodParameter(ctor, i);
				if (value == null && methodParam.isOptional()) {
					args[i] = (methodParam.getParameterType() == Optional.class ? Optional.empty() : null);
				}
				else {
					args[i] = binder.convertIfNecessary(value, paramTypes[i], methodParam);
				}
			}
			return BeanUtils.instantiateClass(ctor, args);
		});
	}

	/**
	 * 用于获取数据绑定值的受保护方法。默认情况下，此方法委托给 {@link WebExchangeDataBinder#getValuesToBind}。
	 * @param binder 正在使用的数据绑定器
	 * @param exchange 当前交换
	 * @return 绑定值的映射
	 * @since 5.3
	 */
	public Mono<Map<String, Object>> getValuesToBind(WebExchangeDataBinder binder, ServerWebExchange exchange) {
		return binder.getValuesToBind(exchange);
	}

	private boolean hasErrorsArgument(MethodParameter parameter) {
		int i = parameter.getParameterIndex();
		Class<?>[] paramTypes = parameter.getExecutable().getParameterTypes();
		return (paramTypes.length > i + 1 && Errors.class.isAssignableFrom(paramTypes[i + 1]));
	}

	private void validateIfApplicable(WebExchangeDataBinder binder, MethodParameter parameter) {
		for (Annotation ann : parameter.getParameterAnnotations()) {
			Object[] validationHints = ValidationAnnotationUtils.determineValidationHints(ann);
			if (validationHints != null) {
				binder.validate(validationHints);
			}
		}
	}

}
