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

package org.springframework.web.method.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.SmartValidator;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.ValidationAnnotationUtils;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.bind.support.WebRequestDataBinder;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartRequest;
import org.springframework.web.multipart.support.StandardServletPartUtils;

/**
 * 解析标注了 {@code @ModelAttribute} 的方法参数，并处理标注了
 * {@code @ModelAttribute} 的方法的返回值。
 *
 * <p>模型属性从模型中获取，或使用默认构造函数创建（然后添加到模型中）。
 * 创建后，通过数据绑定使用 Servlet 请求参数填充该属性。如果参数标注了
 * {@code @javax.validation.Valid} 或 Spring 自身的
 * {@code @org.springframework.validation.annotation.Validated}，
 * 则可能应用校验。
 *
 * <p>当以 {@code annotationNotRequired=true} 创建此处理器时，
 * 任何非简单类型的参数和返回值都被视为模型属性，
 * 无论是否带有 {@code @ModelAttribute}。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @author Vladislav Kisel
 * @since 3.1
 */
public class ModelAttributeMethodProcessor implements HandlerMethodArgumentResolver, HandlerMethodReturnValueHandler {

	protected final Log logger = LogFactory.getLog(getClass());

	private final boolean annotationNotRequired;


	/**
	 * 类构造函数。
	 * @param annotationNotRequired 如果为 "true"，非简单方法参数和返回值无论
	 * 是否带有 {@code @ModelAttribute} 注解都被视为模型属性
	 */
	public ModelAttributeMethodProcessor(boolean annotationNotRequired) {
		this.annotationNotRequired = annotationNotRequired;
	}


	/**
	 * 如果参数标注了 {@link ModelAttribute}，或者在默认解析模式下
	 * 对于任何非简单类型的方法参数，则返回 {@code true}。
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return (parameter.hasParameterAnnotation(ModelAttribute.class) ||
				(this.annotationNotRequired && !BeanUtils.isSimpleProperty(parameter.getParameterType())));
	}

	/**
	 * 从模型中解析参数；如果未找到，则在可用时使用默认值实例化它。
	 * 随后通过数据绑定用请求值填充模型属性，并在参数上存在
	 * {@code @java.validation.Valid} 时可选地进行校验。
	 * @throws BindException 如果数据绑定和校验产生错误，
	 * 且下一个方法参数不是 {@link Errors} 类型
	 * @throws Exception 如果 WebDataBinder 初始化失败
	 */
	@Override
	@Nullable
	public final Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) throws Exception {

		Assert.state(mavContainer != null, "ModelAttributeMethodProcessor requires ModelAndViewContainer");
		Assert.state(binderFactory != null, "ModelAttributeMethodProcessor requires WebDataBinderFactory");

		String name = ModelFactory.getNameForParameter(parameter);
		ModelAttribute ann = parameter.getParameterAnnotation(ModelAttribute.class);
		if (ann != null) {
			mavContainer.setBinding(name, ann.binding());
		}

		Object attribute = null;
		BindingResult bindingResult = null;

		if (mavContainer.containsAttribute(name)) {
			attribute = mavContainer.getModel().get(name);
		}
		else {
			// Create attribute instance
			try {
				attribute = createAttribute(name, parameter, binderFactory, webRequest);
			}
			catch (BindException ex) {
				if (isBindExceptionRequired(parameter)) {
					// No BindingResult parameter -> fail with BindException
					throw ex;
				}
				// Otherwise, expose null/empty value and associated BindingResult
				if (parameter.getParameterType() == Optional.class) {
					attribute = Optional.empty();
				}
				else {
					attribute = ex.getTarget();
				}
				bindingResult = ex.getBindingResult();
			}
		}

		if (bindingResult == null) {
			// Bean property binding and validation;
			// skipped in case of binding failure on construction.
			WebDataBinder binder = binderFactory.createBinder(webRequest, attribute, name);
			if (binder.getTarget() != null) {
				if (!mavContainer.isBindingDisabled(name)) {
					bindRequestParameters(binder, webRequest);
				}
				validateIfApplicable(binder, parameter);
				if (binder.getBindingResult().hasErrors() && isBindExceptionRequired(binder, parameter)) {
					throw new BindException(binder.getBindingResult());
				}
			}
			// Value type adaptation, also covering java.util.Optional
			if (!parameter.getParameterType().isInstance(attribute)) {
				attribute = binder.convertIfNecessary(binder.getTarget(), parameter.getParameterType(), parameter);
			}
			bindingResult = binder.getBindingResult();
		}

		// Add resolved attribute and BindingResult at the end of the model
		Map<String, Object> bindingResultModel = bindingResult.getModel();
		mavContainer.removeAttributes(bindingResultModel);
		mavContainer.addAllAttributes(bindingResultModel);

		return attribute;
	}

	/**
	 * 扩展点：如果模型中未找到模型属性则创建它，
	 * 随后（除非被禁止）通过 Bean 属性进行参数绑定。
	 * <p>默认实现通常使用唯一可用的公共无参构造函数（如果存在），
	 * 但也支持针对数据类的“主构造函数”方式：
	 * 它既理解 JavaBeans 的 {@code ConstructorProperties} 注解，
	 * 也理解字节码中保留的运行时参数名，从而按名称将请求参数与构造函数参数关联起来。
	 * 如果找不到这样的构造函数，则使用默认构造函数（即使不是公共的），
	 * 假定随后通过 setter 方法进行 Bean 属性绑定。
	 * @param attributeName 属性的名称（永不为 {@code null}）
	 * @param parameter 方法参数声明
	 * @param binderFactory 用于创建 WebDataBinder 实例
	 * @param webRequest 当前请求
	 * @return 创建的模型属性（永不为 {@code null}）
	 * @throws BindException 如果构造器参数绑定失败
	 * @throws Exception 如果构造器调用失败
	 * @see #constructAttribute(Constructor, String, MethodParameter, WebDataBinderFactory, NativeWebRequest)
	 * @see BeanUtils#findPrimaryConstructor(Class)
	 */
	protected Object createAttribute(String attributeName, MethodParameter parameter,
			WebDataBinderFactory binderFactory, NativeWebRequest webRequest) throws Exception {

		MethodParameter nestedParameter = parameter.nestedIfOptional();
		Class<?> clazz = nestedParameter.getNestedParameterType();

		Constructor<?> ctor = BeanUtils.getResolvableConstructor(clazz);
		Object attribute = constructAttribute(ctor, attributeName, parameter, binderFactory, webRequest);
		if (parameter != nestedParameter) {
			attribute = Optional.of(attribute);
		}
		return attribute;
	}

	/**
	 * 使用给定的构造函数构造新的属性实例。
	 * <p>在构造函数解析之后由
	 * {@link #createAttribute(String, MethodParameter, WebDataBinderFactory, NativeWebRequest)}
	 * 调用。
	 * @param ctor 要使用的构造器
	 * @param attributeName 属性的名称（永不为 {@code null}）
	 * @param binderFactory 用于创建 WebDataBinder 实例
	 * @param webRequest 当前请求
	 * @return 创建的模型属性（永不为 {@code null}）
	 * @throws BindException 如果构造器参数绑定失败
	 * @throws Exception 如果构造器调用失败
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	protected Object constructAttribute(Constructor<?> ctor, String attributeName, MethodParameter parameter,
			WebDataBinderFactory binderFactory, NativeWebRequest webRequest) throws Exception {

		if (ctor.getParameterCount() == 0) {
			// A single default constructor -> clearly a standard JavaBeans arrangement.
			return BeanUtils.instantiateClass(ctor);
		}

		// A single data class constructor -> resolve constructor arguments from request parameters.
		String[] paramNames = BeanUtils.getParameterNames(ctor);
		Class<?>[] paramTypes = ctor.getParameterTypes();
		Object[] args = new Object[paramTypes.length];
		WebDataBinder binder = binderFactory.createBinder(webRequest, null, attributeName);
		String fieldDefaultPrefix = binder.getFieldDefaultPrefix();
		String fieldMarkerPrefix = binder.getFieldMarkerPrefix();
		boolean bindingFailure = false;
		Set<String> failedParams = new HashSet<>(4);

		for (int i = 0; i < paramNames.length; i++) {
			String paramName = paramNames[i];
			Class<?> paramType = paramTypes[i];
			Object value = webRequest.getParameterValues(paramName);

			// Since WebRequest#getParameter exposes a single-value parameter as an array
			// with a single element, we unwrap the single value in such cases, analogous
			// to WebExchangeDataBinder.addBindValue(Map<String, Object>, String, List<?>).
			if (ObjectUtils.isArray(value) && Array.getLength(value) == 1) {
				value = Array.get(value, 0);
			}

			if (value == null) {
				if (fieldDefaultPrefix != null) {
					value = webRequest.getParameter(fieldDefaultPrefix + paramName);
				}
				if (value == null) {
					if (fieldMarkerPrefix != null && webRequest.getParameter(fieldMarkerPrefix + paramName) != null) {
						value = binder.getEmptyValue(paramType);
					}
					else {
						value = resolveConstructorArgument(paramName, paramType, webRequest);
					}
				}
			}

			try {
				MethodParameter methodParam = new FieldAwareConstructorParameter(ctor, i, paramName);
				if (value == null && methodParam.isOptional()) {
					args[i] = (methodParam.getParameterType() == Optional.class ? Optional.empty() : null);
				}
				else {
					args[i] = binder.convertIfNecessary(value, paramType, methodParam);
				}
			}
			catch (TypeMismatchException ex) {
				ex.initPropertyName(paramName);
				args[i] = null;
				failedParams.add(paramName);
				binder.getBindingResult().recordFieldValue(paramName, paramType, value);
				binder.getBindingErrorProcessor().processPropertyAccessException(ex, binder.getBindingResult());
				bindingFailure = true;
			}
		}

		if (bindingFailure) {
			BindingResult result = binder.getBindingResult();
			for (int i = 0; i < paramNames.length; i++) {
				String paramName = paramNames[i];
				if (!failedParams.contains(paramName)) {
					Object value = args[i];
					result.recordFieldValue(paramName, paramTypes[i], value);
					validateValueIfApplicable(binder, parameter, ctor.getDeclaringClass(), paramName, value);
				}
			}
			if (!parameter.isOptional()) {
				try {
					Object target = BeanUtils.instantiateClass(ctor, args);
					throw new BindException(result) {
						@Override
						public Object getTarget() {
							return target;
						}
					};
				}
				catch (BeanInstantiationException ex) {
					// swallow and proceed without target instance
				}
			}
			throw new BindException(result);
		}

		return BeanUtils.instantiateClass(ctor, args);
	}

	/**
	 * 扩展点：将请求绑定到目标对象。
	 * @param binder 用于绑定的数据绑定器实例
	 * @param request 当前请求
	 */
	protected void bindRequestParameters(WebDataBinder binder, NativeWebRequest request) {
		((WebRequestDataBinder) binder).bind(request);
	}

	@Nullable
	public Object resolveConstructorArgument(String paramName, Class<?> paramType, NativeWebRequest request)
			throws Exception {

		MultipartRequest multipartRequest = request.getNativeRequest(MultipartRequest.class);
		if (multipartRequest != null) {
			List<MultipartFile> files = multipartRequest.getFiles(paramName);
			if (!files.isEmpty()) {
				return (files.size() == 1 ? files.get(0) : files);
			}
		}
		else if (StringUtils.startsWithIgnoreCase(
				request.getHeader(HttpHeaders.CONTENT_TYPE), MediaType.MULTIPART_FORM_DATA_VALUE)) {
			HttpServletRequest servletRequest = request.getNativeRequest(HttpServletRequest.class);
			if (servletRequest != null && HttpMethod.POST.matches(servletRequest.getMethod())) {
				List<Part> parts = StandardServletPartUtils.getParts(servletRequest, paramName);
				if (!parts.isEmpty()) {
					return (parts.size() == 1 ? parts.get(0) : parts);
				}
			}
		}
		return null;
	}

	/**
	 * 在适用时校验模型属性。
	 * <p>默认实现会检查 {@code @javax.validation.Valid}、
	 * Spring 的 {@link org.springframework.validation.annotation.Validated}，
	 * 以及名称以 "Valid" 开头的自定义注解。
	 * @param binder 要使用的 DataBinder
	 * @param parameter 方法参数声明
	 * @see WebDataBinder#validate(Object...)
	 * @see SmartValidator#validate(Object, Errors, Object...)
	 */
	protected void validateIfApplicable(WebDataBinder binder, MethodParameter parameter) {
		for (Annotation ann : parameter.getParameterAnnotations()) {
			Object[] validationHints = ValidationAnnotationUtils.determineValidationHints(ann);
			if (validationHints != null) {
				binder.validate(validationHints);
				break;
			}
		}
	}

	/**
	 * 在适用时校验指定的候选值。
	 * <p>默认实现会检查 {@code @javax.validation.Valid}、
	 * Spring 的 {@link org.springframework.validation.annotation.Validated}，
	 * 以及名称以 "Valid" 开头的自定义注解。
	 * @param binder 要使用的 DataBinder
	 * @param parameter 方法参数声明
	 * @param targetType 目标类型
	 * @param fieldName 字段的名称
	 * @param value 候选值
	 * @since 5.1
	 * @see #validateIfApplicable(WebDataBinder, MethodParameter)
	 * @see SmartValidator#validateValue(Class, String, Object, Errors, Object...)
	 */
	protected void validateValueIfApplicable(WebDataBinder binder, MethodParameter parameter,
			Class<?> targetType, String fieldName, @Nullable Object value) {

		for (Annotation ann : parameter.getParameterAnnotations()) {
			Object[] validationHints = ValidationAnnotationUtils.determineValidationHints(ann);
			if (validationHints != null) {
				for (Validator validator : binder.getValidators()) {
					if (validator instanceof SmartValidator) {
						try {
							((SmartValidator) validator).validateValue(targetType, fieldName, value,
									binder.getBindingResult(), validationHints);
						}
						catch (IllegalArgumentException ex) {
							// No corresponding field on the target class...
						}
					}
				}
				break;
			}
		}
	}

	/**
	 * 是否在校验出错时抛出致命的数据绑定异常。
	 * <p>默认实现委托给 {@link #isBindExceptionRequired(MethodParameter)}。
	 * @param binder 用于执行数据绑定的数据绑定器
	 * @param parameter 方法参数声明
	 * @return 如果下一个方法参数不是 {@link Errors} 类型则返回 {@code true}
	 * @see #isBindExceptionRequired(MethodParameter)
	 */
	protected boolean isBindExceptionRequired(WebDataBinder binder, MethodParameter parameter) {
		return isBindExceptionRequired(parameter);
	}

	/**
	 * 是否在校验出错时抛出致命的数据绑定异常。
	 * @param parameter 方法参数声明
	 * @return 如果下一个方法参数不是 {@link Errors} 类型则返回 {@code true}
	 * @since 5.0
	 */
	protected boolean isBindExceptionRequired(MethodParameter parameter) {
		int i = parameter.getParameterIndex();
		Class<?>[] paramTypes = parameter.getExecutable().getParameterTypes();
		boolean hasBindingResult = (paramTypes.length > (i + 1) && Errors.class.isAssignableFrom(paramTypes[i + 1]));
		return !hasBindingResult;
	}

	/**
	 * 如果存在方法级 {@code @ModelAttribute}，或者在默认解析模式下
	 * 对于任何非简单类型的返回值，则返回 {@code true}。
	 */
	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		return (returnType.hasMethodAnnotation(ModelAttribute.class) ||
				(this.annotationNotRequired && !BeanUtils.isSimpleProperty(returnType.getParameterType())));
	}

	/**
	 * 将非空返回值添加到 {@link ModelAndViewContainer}。
	 */
	@Override
	public void handleReturnValue(@Nullable Object returnValue, MethodParameter returnType,
			ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {

		if (returnValue != null) {
			String name = ModelFactory.getNameForReturnValue(returnValue, returnType);
			mavContainer.addAttribute(name, returnValue);
		}
	}


	/**
	 * {@link MethodParameter} 的子类，同时还能检测字段注解。
	 * @since 5.1
	 */
	private static class FieldAwareConstructorParameter extends MethodParameter {

		private final String parameterName;

		@Nullable
		private volatile Annotation[] combinedAnnotations;

		public FieldAwareConstructorParameter(Constructor<?> constructor, int parameterIndex, String parameterName) {
			super(constructor, parameterIndex);
			this.parameterName = parameterName;
		}

		@Override
		public Annotation[] getParameterAnnotations() {
			Annotation[] anns = this.combinedAnnotations;
			if (anns == null) {
				anns = super.getParameterAnnotations();
				try {
					Field field = getDeclaringClass().getDeclaredField(this.parameterName);
					Annotation[] fieldAnns = field.getAnnotations();
					if (fieldAnns.length > 0) {
						List<Annotation> merged = new ArrayList<>(anns.length + fieldAnns.length);
						merged.addAll(Arrays.asList(anns));
						for (Annotation fieldAnn : fieldAnns) {
							boolean existingType = false;
							for (Annotation ann : anns) {
								if (ann.annotationType() == fieldAnn.annotationType()) {
									existingType = true;
									break;
								}
							}
							if (!existingType) {
								merged.add(fieldAnn);
							}
						}
						anns = merged.toArray(new Annotation[0]);
					}
				}
				catch (NoSuchFieldException | SecurityException ex) {
					// ignore
				}
				this.combinedAnnotations = anns;
			}
			return anns;
		}

		@Override
		public String getParameterName() {
			return this.parameterName;
		}
	}

}
