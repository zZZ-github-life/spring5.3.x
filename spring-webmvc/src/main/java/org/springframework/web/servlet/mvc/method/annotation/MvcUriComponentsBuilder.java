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

package org.springframework.web.servlet.mvc.method.annotation;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.cglib.core.SpringNamingPolicy;
import org.springframework.cglib.proxy.Callback;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.Factory;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.objenesis.ObjenesisException;
import org.springframework.objenesis.SpringObjenesis;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.PathMatcher;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.MethodFilter;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.annotation.RequestParamMethodArgumentResolver;
import org.springframework.web.method.support.CompositeUriComponentsContributor;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 通过指向 Spring MVC 控制器上的 {@code @RequestMapping} 方法，
 * 创建 {@link org.springframework.web.util.UriComponentsBuilder} 实例。
 *
 * <p>方法分为以下几组：
 * <ul>
 * <li>静态的 {@code fromXxx(...)} 方法，使用由
 * {@link org.springframework.web.servlet.support.ServletUriComponentsBuilder#fromCurrentServletMapping()}
 * 调用所确定的当前请求信息来准备链接。
 * <li>静态的 {@code fromXxx(UriComponentsBuilder,...)} 方法，在请求上下文之外
 * 操作时可以为其提供 baseUrl。
 * <li>基于实例的 {@code withXxx(...)} 方法，其中通过
 * {@link #relativeTo(org.springframework.web.util.UriComponentsBuilder)} 使用 baseUrl
 * 创建 MvcUriComponentsBuilder 实例。
 * </ul>
 *
 * <p><strong>注意：</strong>此类会使用 "Forwarded"
 * (<a href="https://tools.ietf.org/html/rfc7239">RFC 7239</a>)、
 * "X-Forwarded-Host"、"X-Forwarded-Port" 和 "X-Forwarded-Proto" 头（如果存在）中的值，
 * 以反映客户端发起的协议和地址。
 * 可考虑使用 {@code ForwardedHeaderFilter}，以便在一个集中位置
 * 选择是提取并使用这些头，还是丢弃它们。
 * 有关此过滤器的更多信息，请参见 Spring Framework 参考文档。
 * @author Oliver Gierke
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 4.0
 */
public class MvcUriComponentsBuilder {

	/**
	 * {@link CompositeUriComponentsContributor} 对象在 bean 工厂中的众所周知名称。
	 */
	public static final String MVC_URI_COMPONENTS_CONTRIBUTOR_BEAN_NAME = "mvcUriComponentsContributor";


	private static final Log logger = LogFactory.getLog(MvcUriComponentsBuilder.class);

	private static final SpringObjenesis objenesis = new SpringObjenesis();

	private static final PathMatcher pathMatcher = new AntPathMatcher();

	private static final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

	private static final CompositeUriComponentsContributor defaultUriComponentsContributor;

	static {
		defaultUriComponentsContributor = new CompositeUriComponentsContributor(
				new PathVariableMethodArgumentResolver(), new RequestParamMethodArgumentResolver(false));
	}

	private final UriComponentsBuilder baseUrl;


	/**
	 * 默认构造函数。设为受保护以防止直接实例化。
	 * @see #fromController(Class)
	 * @see #fromMethodName(Class, String, Object...)
	 * @see #fromMethodCall(Object)
	 * @see #fromMappingName(String)
	 * @see #fromMethod(Class, Method, Object...)
	 */
	protected MvcUriComponentsBuilder(UriComponentsBuilder baseUrl) {
		Assert.notNull(baseUrl, "'baseUrl' is required");
		this.baseUrl = baseUrl;
	}


	/**
	 * 使用基础 URL 创建此类的实例。此后调用任意
	 * 基于实例的 {@code withXxx(...}} 方法都将创建相对于
	 * 给定基础 URL 的 URL。
	 */
	public static MvcUriComponentsBuilder relativeTo(UriComponentsBuilder baseUrl) {
		return new MvcUriComponentsBuilder(baseUrl);
	}


	/**
	 * 根据控制器类的映射和当前请求信息（包括 Servlet 映射）
	 * 创建 {@link UriComponentsBuilder}。如果控制器
	 * 包含多个映射，则仅使用第一个。
	 * <p><strong>注意：</strong>如果找到 "Forwarded" 和
	 * "X-Forwarded-*" 头，此方法会从中提取值。请参见类级文档。
	 * @param controllerType 要为其构建 URI 的控制器
	 * @return UriComponentsBuilder 实例（永不为 {@code null}）
	 */
	public static UriComponentsBuilder fromController(Class<?> controllerType) {
		return fromController(null, controllerType);
	}

	/**
	 * {@link #fromController(Class)} 的替代方案，接受一个
	 * 表示基础 URL 的 {@code UriComponentsBuilder}。当在
	 * 处理请求的上下文之外使用 MvcUriComponentsBuilder，或希望应用
	 * 与当前请求不匹配的自定义 baseUrl 时，这很有用。
	 * <p><strong>注意：</strong>如果找到 "Forwarded" 和
	 * "X-Forwarded-*" 头，此方法会从中提取值。请参见类级文档。
	 * @param builder 基础 URL 的构建器；该构建器将被克隆，
	 * 因此不会被修改，可复用于后续调用。
	 * @param controllerType 要为其构建 URI 的控制器
	 * @return UriComponentsBuilder 实例（永不为 {@code null}）
	 */
	public static UriComponentsBuilder fromController(@Nullable UriComponentsBuilder builder,
			Class<?> controllerType) {

		builder = getBaseUrlToUse(builder);

		// Externally configured prefix via PathConfigurer..
		String prefix = getPathPrefix(controllerType);
		builder.path(prefix);

		String mapping = getClassMapping(controllerType);
		builder.path(mapping);

		return builder;
	}

	/**
	 * 根据控制器方法的映射和方法参数值数组
	 * 创建 {@link UriComponentsBuilder}。此方法委托
	 * 给 {@link #fromMethod(Class, Method, Object...)}。
	 * <p><strong>注意：</strong>如果找到 "Forwarded" 和
	 * "X-Forwarded-*" 头，此方法会从中提取值。请参见类级文档。
	 * @param controllerType 控制器
	 * @param methodName 方法名称
	 * @param args 参数值
	 * @return UriComponentsBuilder 实例，永不为 {@code null}
	 * @throws IllegalArgumentException 如果没有匹配的方法或有多个匹配的方法
	 */
	public static UriComponentsBuilder fromMethodName(Class<?> controllerType,
			String methodName, Object... args) {

		Method method = getMethod(controllerType, methodName, args);
		return fromMethodInternal(null, controllerType, method, args);
	}

	/**
	 * {@link #fromMethodName(Class, String, Object...)} 的替代方案，接受
	 * 一个表示基础 URL 的 {@code UriComponentsBuilder}。当在
	 * 处理请求的上下文之外使用 MvcUriComponentsBuilder，或希望应用
	 * 与当前请求不匹配的自定义 baseUrl 时，这很有用。
	 * <p><strong>注意：</strong>如果找到 "Forwarded" 和
	 * "X-Forwarded-*" 头，此方法会从中提取值。请参见类级文档。
	 * @param builder 基础 URL 的构建器；该构建器将被克隆，
	 * 因此不会被修改，可复用于后续调用。
	 * @param controllerType 控制器
	 * @param methodName 方法名称
	 * @param args 参数值
	 * @return UriComponentsBuilder 实例，永不为 {@code null}
	 * @throws IllegalArgumentException 如果没有匹配的方法或有多个匹配的方法
	 */
	public static UriComponentsBuilder fromMethodName(UriComponentsBuilder builder,
			Class<?> controllerType, String methodName, Object... args) {

		Method method = getMethod(controllerType, methodName, args);
		return fromMethodInternal(builder, controllerType, method, args);
	}

	/**
	 * 根据控制器方法的映射和方法参数值数组创建 {@link UriComponentsBuilder}。
	 * 值数组必须与控制器方法的签名匹配。{@code @RequestParam} 和
	 * {@code @PathVariable} 的值用于构建 URI（通过
	 * {@link org.springframework.web.method.support.UriComponentsContributor
	 * UriComponentsContributor} 的实现），其余参数值会被忽略，
	 * 可以为 {@code null}。
	 * <p><strong>注意：</strong>如果找到 "Forwarded" 和
	 * "X-Forwarded-*" 头，此方法会从中提取值。请参见类级文档。
	 * @param controllerType 控制器类型
	 * @param method 控制器方法
	 * @param args 控制器方法的参数值
	 * @return UriComponentsBuilder 实例，永不为 {@code null}
	 * @since 4.2
	 */
	public static UriComponentsBuilder fromMethod(Class<?> controllerType, Method method, Object... args) {
		return fromMethodInternal(null, controllerType, method, args);
	}

	/**
	 * {@link #fromMethod(Class, Method, Object...)} 的替代方案，
	 * 接受一个表示基础 URL 的 {@code UriComponentsBuilder}。
	 * 当在处理请求的上下文之外使用 MvcUriComponentsBuilder，
	 * 或希望应用与当前请求不匹配的
	 * 自定义 baseUrl 时，这很有用。
	 * <p><strong>注意：</strong>如果找到 "Forwarded" 和
	 * "X-Forwarded-*" 头，此方法会从中提取值。请参见类级文档。
	 * @param baseUrl 基础 URL 的构建器；该构建器将被克隆，
	 * 因此不会被修改，可复用于后续调用。
	 * @param controllerType 控制器类型
	 * @param method 控制器方法
	 * @param args 控制器方法的参数值
	 * @return UriComponentsBuilder 实例（永不为 {@code null}）
	 * @since 4.2
	 */
	public static UriComponentsBuilder fromMethod(UriComponentsBuilder baseUrl,
			@Nullable Class<?> controllerType, Method method, Object... args) {

		return fromMethodInternal(baseUrl,
				(controllerType != null ? controllerType : method.getDeclaringClass()), method, args);
	}

	/**
	 * 通过调用“模拟”控制器方法来创建 {@link UriComponentsBuilder}。
	 * 随后使用该控制器方法和所提供的参数值
	 * 委托给 {@link #fromMethod(Class, Method, Object...)}。
	 * <p>例如，对于以下控制器：
	 * <pre class="code">
	 * &#064;RequestMapping("/people/{id}/addresses")
	 * class AddressController {
	 *
	 *   &#064;RequestMapping("/{country}")
	 *   public HttpEntity&lt;Void&gt; getAddressesForCountry(&#064;PathVariable String country) { ... }
	 *
	 *   &#064;RequestMapping(value="/", method=RequestMethod.POST)
	 *   public void addAddress(Address address) { ... }
	 * }
	 * </pre>
	 * 可以这样创建 UriComponentsBuilder：
	 * <pre class="code">
	 * // Inline style with static import of "MvcUriComponentsBuilder.on"
	 *
	 * MvcUriComponentsBuilder.fromMethodCall(
	 * 		on(AddressController.class).getAddressesForCountry("US")).buildAndExpand(1);
	 *
	 * // Longer form useful for repeated invocation (and void controller methods)
	 *
	 * AddressController controller = MvcUriComponentsBuilder.on(AddressController.class);
	 * controller.addAddress(null);
	 * builder = MvcUriComponentsBuilder.fromMethodCall(controller);
	 * controller.getAddressesForCountry("US")
	 * builder = MvcUriComponentsBuilder.fromMethodCall(controller);
	 * </pre>
	 * <p><strong>注意：</strong>如果找到 "Forwarded" 和
	 * "X-Forwarded-*" 头，此方法会从中提取值。请参见类级文档。
	 * @param info 要么是从 "mock" 控制器调用返回的值，
	 * 要么是调用后的 "mock" 控制器本身
	 * @return UriComponents 实例
	 * @see #on(Class)
	 * @see #controller(Class)
	 */
	public static UriComponentsBuilder fromMethodCall(Object info) {
		Assert.isInstanceOf(MethodInvocationInfo.class, info, "MethodInvocationInfo required");
		MethodInvocationInfo invocationInfo = (MethodInvocationInfo) info;
		Class<?> controllerType = invocationInfo.getControllerType();
		Method method = invocationInfo.getControllerMethod();
		Object[] arguments = invocationInfo.getArgumentValues();
		return fromMethodInternal(null, controllerType, method, arguments);
	}

	/**
	 * {@link #fromMethodCall(Object)} 的替代方案，接受一个
	 * 表示基础 URL 的 {@code UriComponentsBuilder}。当在
	 * 处理请求的上下文之外使用 MvcUriComponentsBuilder，或希望应用
	 * 与当前请求不匹配的自定义 baseUrl 时，这很有用。
	 * <p><strong>注意：</strong>如果找到 "Forwarded" 和
	 * "X-Forwarded-*" 头，此方法会从中提取值。请参见类级文档。
	 * @param builder 基础 URL 的构建器；该构建器将被克隆，
	 * 因此不会被修改，可复用于后续调用。
	 * @param info 要么是从 "mock" 控制器调用返回的值，
	 * 要么是调用后的 "mock" 控制器本身
	 * @return UriComponents 实例
	 */
	public static UriComponentsBuilder fromMethodCall(UriComponentsBuilder builder, Object info) {
		Assert.isInstanceOf(MethodInvocationInfo.class, info, "MethodInvocationInfo required");
		MethodInvocationInfo invocationInfo = (MethodInvocationInfo) info;
		Class<?> controllerType = invocationInfo.getControllerType();
		Method method = invocationInfo.getControllerMethod();
		Object[] arguments = invocationInfo.getArgumentValues();
		return fromMethodInternal(builder, controllerType, method, arguments);
	}

	/**
	 * 返回一个“模拟”控制器实例。当调用控制器上的某个 {@code @RequestMapping}
	 * 方法时，所提供的参数值会被记录下来，
	 * 随后结果可用于通过 {@link #fromMethodCall(Object)} 创建
	 * {@code UriComponentsBuilder}。
	 * <p>请注意，这是 {@link #controller(Class)} 的简写版本，
	 * 用于内联使用（配合静态导入），例如：
	 * <pre class="code">
	 * MvcUriComponentsBuilder.fromMethodCall(on(FooController.class).getFoo(1)).build();
	 * </pre>
	 * <p><strong>注意：</strong>如果找到 "Forwarded" 和
	 * "X-Forwarded-*" 头，此方法会从中提取值。请参见类级文档。
	 * @param controllerType 目标控制器
	 */
	public static <T> T on(Class<T> controllerType) {
		return controller(controllerType);
	}

	/**
	 * 返回一个“模拟”控制器实例。当调用控制器上的某个 {@code @RequestMapping}
	 * 方法时，所提供的参数值会被记录下来，
	 * 随后结果可通过 {@link #fromMethodCall(Object)} 用于创建
	 * {@code UriComponentsBuilder}。
	 * <p>这是 {@link #on(Class)} 的较长版本。对于返回 void 的控制器方法
	 * 以及需要重复调用的情况，也需要使用它。
	 * <pre class="code">
	 * FooController fooController = controller(FooController.class);
	 *
	 * fooController.saveFoo(1, null);
	 * builder = MvcUriComponentsBuilder.fromMethodCall(fooController);
	 *
	 * fooController.saveFoo(2, null);
	 * builder = MvcUriComponentsBuilder.fromMethodCall(fooController);
	 * </pre>
	 * <p><strong>注意：</strong>如果找到 "Forwarded" 和
	 * "X-Forwarded-*" 头，此方法会从中提取值。请参见类级文档。
	 * @param controllerType 目标控制器
	 */
	public static <T> T controller(Class<T> controllerType) {
		Assert.notNull(controllerType, "'controllerType' must not be null");
		return ControllerMethodInvocationInterceptor.initProxy(controllerType, null);
	}

	/**
	 * 根据 Spring MVC 控制器方法请求映射的名称创建 URL。
	 * <p>已配置的
	 * {@link org.springframework.web.servlet.handler.HandlerMethodMappingNamingStrategy
	 * HandlerMethodMappingNamingStrategy} 会在启动时确定控制器
	 * 方法请求映射的名称。默认情况下，所有映射都会根据
	 * 类名中的大写字母命名，后跟作为分隔符的 "#"，
	 * 然后是方法名。例如，对于名为 PersonController
	 * 且方法为 getPerson 的类，名称为 "PC#getPerson"。如果
	 * 命名约定无法产生唯一结果，可以通过 {@code @RequestMapping}
	 * 注解的 name 属性分配显式名称。
	 * <p>这主要面向视图渲染技术和 EL
	 * 表达式使用。Spring URL 标签库将此方法注册为名为
	 * "mvcUrl" 的函数。
	 * <p>例如，对于以下控制器：
	 * <pre class="code">
	 * &#064;RequestMapping("/people")
	 * class PersonController {
	 *
	 *   &#064;RequestMapping("/{id}")
	 *   public HttpEntity&lt;Void&gt; getPerson(&#064;PathVariable String id) { ... }
	 *
	 * }
	 * </pre>
	 *
	 * JSP 可以按如下方式准备指向该控制器方法的 URL：
	 *
	 * <pre class="code">
	 * &lt;%@ taglib uri="http://www.springframework.org/tags" prefix="s" %&gt;
	 *
	 * &lt;a href="${s:mvcUrl('PC#getPerson').arg(0,"123").build()}"&gt;Get Person&lt;/a&gt;
	 * </pre>
	 * <p>请注意，无需指定所有参数。只需指定准备 URL
	 * 所必需的参数，主要是 {@code @RequestParam} 和 {@code @PathVariable}。
	 *
	 * <p><strong>注意：</strong>如果找到 "Forwarded" 和
	 * "X-Forwarded-*" 头，此方法会从中提取值。请参见类级文档。
	 * @param mappingName 映射名称
	 * @return 用于准备 URI 字符串的构建器
	 * @throws IllegalArgumentException 如果未找到该映射名称或没有唯一匹配
	 * @since 4.1
	 */
	public static MethodArgumentBuilder fromMappingName(String mappingName) {
		return fromMappingName(null, mappingName);
	}

	/**
	 * {@link #fromMappingName(String)} 的替代方案，接受一个
	 * 表示基础 URL 的 {@code UriComponentsBuilder}。当在
	 * 处理请求的上下文之外使用 MvcUriComponentsBuilder，或希望应用
	 * 与当前请求不匹配的自定义 baseUrl 时，这很有用。
	 * <p><strong>注意：</strong>如果找到 "Forwarded" 和
	 * "X-Forwarded-*" 头，此方法会从中提取值。请参见类级文档。
	 * @param builder 基础 URL 的构建器；该构建器将被克隆，
	 * 因此不会被修改，可复用于后续调用。
	 * @param name 映射名称
	 * @return 用于准备 URI 字符串的构建器
	 * @throws IllegalArgumentException 如果未找到该映射名称或没有唯一匹配
	 * @since 4.2
	 */
	public static MethodArgumentBuilder fromMappingName(@Nullable UriComponentsBuilder builder, String name) {
		WebApplicationContext wac = getWebApplicationContext();
		Assert.notNull(wac, "No WebApplicationContext");
		Map<String, RequestMappingInfoHandlerMapping> map = wac.getBeansOfType(RequestMappingInfoHandlerMapping.class);
		List<HandlerMethod> handlerMethods = null;
		for (RequestMappingInfoHandlerMapping mapping : map.values()) {
			handlerMethods = mapping.getHandlerMethodsForMappingName(name);
			if (handlerMethods != null) {
				break;
			}
		}
		if (handlerMethods == null) {
			throw new IllegalArgumentException("Mapping not found: " + name);
		}
		else if (handlerMethods.size() != 1) {
			throw new IllegalArgumentException("No unique match for mapping " + name + ": " + handlerMethods);
		}
		else {
			HandlerMethod handlerMethod = handlerMethods.get(0);
			Class<?> controllerType = handlerMethod.getBeanType();
			Method method = handlerMethod.getMethod();
			return new MethodArgumentBuilder(builder, controllerType, method);
		}
	}


	// Instance methods, relative to a base UriComponentsBuilder...

	/**
	 * {@link #fromController(Class)} 的替代方案，用于通过调用 {@link #relativeTo}
	 * 创建的此类实例。
	 * <p><strong>注意：</strong>如果找到 "Forwarded" 和
	 * "X-Forwarded-*" 头，此方法会从中提取值。请参见类级文档。
	 * @since 4.2
	 */
	public UriComponentsBuilder withController(Class<?> controllerType) {
		return fromController(this.baseUrl, controllerType);
	}

	/**
	 * {@link #fromMethodName(Class, String, Object...)}} 的替代方案，
	 * 用于通过 {@link #relativeTo} 创建的此类实例。
	 * <p><strong>注意：</strong>如果找到 "Forwarded" 和
	 * "X-Forwarded-*" 头，此方法会从中提取值。请参见类级文档。
	 * @since 4.2
	 */
	public UriComponentsBuilder withMethodName(Class<?> controllerType, String methodName, Object... args) {
		return fromMethodName(this.baseUrl, controllerType, methodName, args);
	}

	/**
	 * {@link #fromMethodCall(Object)} 的替代方案，用于通过
	 * {@link #relativeTo} 创建的此类实例。
	 * <p><strong>注意：</strong>如果找到 "Forwarded" 和
	 * "X-Forwarded-*" 头，此方法会从中提取值。请参见类级文档。
	 * @since 4.2
	 */
	public UriComponentsBuilder withMethodCall(Object invocationInfo) {
		return fromMethodCall(this.baseUrl, invocationInfo);
	}

	/**
	 * {@link #fromMappingName(String)} 的替代方案，用于通过
	 * {@link #relativeTo} 创建的此类实例。
	 * <p><strong>注意：</strong>如果找到 "Forwarded" 和
	 * "X-Forwarded-*" 头，此方法会从中提取值。请参见类级文档。
	 * @since 4.2
	 */
	public MethodArgumentBuilder withMappingName(String mappingName) {
		return fromMappingName(this.baseUrl, mappingName);
	}

	/**
	 * {@link #fromMethod(Class, Method, Object...)} 的替代方案，
	 * 用于通过 {@link #relativeTo} 创建的此类实例。
	 * <p><strong>注意：</strong>如果找到 "Forwarded" 和
	 * "X-Forwarded-*" 头，此方法会从中提取值。请参见类级文档。
	 * @since 4.2
	 */
	public UriComponentsBuilder withMethod(Class<?> controllerType, Method method, Object... args) {
		return fromMethod(this.baseUrl, controllerType, method, args);
	}


	private static UriComponentsBuilder fromMethodInternal(@Nullable UriComponentsBuilder builder,
			Class<?> controllerType, Method method, Object... args) {

		builder = getBaseUrlToUse(builder);

		// Externally configured prefix via PathConfigurer..
		String prefix = getPathPrefix(controllerType);
		builder.path(prefix);

		String typePath = getClassMapping(controllerType);
		String methodPath = getMethodMapping(method);
		String path = pathMatcher.combine(typePath, methodPath);
		if (StringUtils.hasLength(path) && !path.startsWith("/")) {
			path = "/" + path;
		}
		builder.path(path);

		return applyContributors(builder, method, args);
	}

	private static UriComponentsBuilder getBaseUrlToUse(@Nullable UriComponentsBuilder baseUrl) {
		return baseUrl == null ?
				ServletUriComponentsBuilder.fromCurrentServletMapping() :
				baseUrl.cloneBuilder();
	}

	private static String getPathPrefix(Class<?> controllerType) {
		WebApplicationContext wac = getWebApplicationContext();
		if (wac != null) {
			Map<String, RequestMappingHandlerMapping> map = wac.getBeansOfType(RequestMappingHandlerMapping.class);
			for (RequestMappingHandlerMapping mapping : map.values()) {
				if (mapping.isHandler(controllerType)) {
					String prefix = mapping.getPathPrefix(controllerType);
					if (prefix != null) {
						return prefix;
					}
				}
			}
		}
		return "";
	}

	private static String getClassMapping(Class<?> controllerType) {
		Assert.notNull(controllerType, "'controllerType' must not be null");
		RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(controllerType, RequestMapping.class);
		if (mapping == null) {
			return "/";
		}
		String[] paths = mapping.path();
		if (ObjectUtils.isEmpty(paths) || !StringUtils.hasLength(paths[0])) {
			return "/";
		}
		if (paths.length > 1 && logger.isTraceEnabled()) {
			logger.trace("Using first of multiple paths on " + controllerType.getName());
		}
		return paths[0];
	}

	private static String getMethodMapping(Method method) {
		Assert.notNull(method, "'method' must not be null");
		RequestMapping requestMapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
		if (requestMapping == null) {
			throw new IllegalArgumentException("No @RequestMapping on: " + method.toGenericString());
		}
		String[] paths = requestMapping.path();
		if (ObjectUtils.isEmpty(paths) || !StringUtils.hasLength(paths[0])) {
			return "/";
		}
		if (paths.length > 1 && logger.isTraceEnabled()) {
			logger.trace("Using first of multiple paths on " + method.toGenericString());
		}
		return paths[0];
	}

	private static Method getMethod(Class<?> controllerType, final String methodName, final Object... args) {
		MethodFilter selector = method -> {
			String name = method.getName();
			int argLength = method.getParameterCount();
			return (name.equals(methodName) && argLength == args.length);
		};
		Set<Method> methods = MethodIntrospector.selectMethods(controllerType, selector);
		if (methods.size() == 1) {
			return methods.iterator().next();
		}
		else if (methods.size() > 1) {
			throw new IllegalArgumentException(String.format(
					"Found two methods named '%s' accepting arguments %s in controller %s: [%s]",
					methodName, Arrays.asList(args), controllerType.getName(), methods));
		}
		else {
			throw new IllegalArgumentException("No method named '" + methodName + "' with " + args.length +
					" arguments found in controller " + controllerType.getName());
		}
	}

	private static UriComponentsBuilder applyContributors(UriComponentsBuilder builder, Method method, Object... args) {
		CompositeUriComponentsContributor contributor = getUriComponentsContributor();

		int paramCount = method.getParameterCount();
		int argCount = args.length;
		if (paramCount != argCount) {
			throw new IllegalArgumentException("Number of method parameters " + paramCount +
					" does not match number of argument values " + argCount);
		}

		final Map<String, Object> uriVars = new HashMap<>();
		for (int i = 0; i < paramCount; i++) {
			MethodParameter param = new SynthesizingMethodParameter(method, i);
			param.initParameterNameDiscovery(parameterNameDiscoverer);
			contributor.contributeMethodArgument(param, args[i], builder, uriVars);
		}

		// This may not be all the URI variables, supply what we have so far.
		return builder.uriVariables(uriVars);
	}

	private static CompositeUriComponentsContributor getUriComponentsContributor() {
		WebApplicationContext wac = getWebApplicationContext();
		if (wac != null) {
			try {
				return wac.getBean(MVC_URI_COMPONENTS_CONTRIBUTOR_BEAN_NAME, CompositeUriComponentsContributor.class);
			}
			catch (NoSuchBeanDefinitionException ex) {
				// Ignore
			}
		}
		return defaultUriComponentsContributor;
	}

	@Nullable
	private static WebApplicationContext getWebApplicationContext() {
		RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
		if (requestAttributes == null) {
			return null;
		}
		HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
		String attributeName = DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE;
		WebApplicationContext wac = (WebApplicationContext) request.getAttribute(attributeName);
		if (wac == null) {
			return null;
		}
		return wac;
	}



	/**
	 * 方法调用信息。
	 */
	public interface MethodInvocationInfo {

		/**
		 * 返回控制器类型。
		 */
		Class<?> getControllerType();

		/**
		 * 返回控制器方法。
		 */
		Method getControllerMethod();

		/**
		 * 返回参数值。
		 */
		Object[] getArgumentValues();
	}


	private static class ControllerMethodInvocationInterceptor
			implements MethodInterceptor, InvocationHandler, MethodInvocationInfo {

		private final Class<?> controllerType;

		@Nullable
		private Method controllerMethod;

		@Nullable
		private Object[] argumentValues;

		ControllerMethodInvocationInterceptor(Class<?> controllerType) {
			this.controllerType = controllerType;
		}

		@Override
		@Nullable
		public Object intercept(@Nullable Object obj, Method method, Object[] args, @Nullable MethodProxy proxy) {
			switch (method.getName()) {
				case "getControllerType": return this.controllerType;
				case "getControllerMethod": return this.controllerMethod;
				case "getArgumentValues": return this.argumentValues;
			}
			if (ReflectionUtils.isObjectMethod(method)) {
				return ReflectionUtils.invokeMethod(method, obj, args);
			}
			else {
				this.controllerMethod = method;
				this.argumentValues = args;
				Class<?> returnType = method.getReturnType();
				try {
					return (returnType == void.class ? null : returnType.cast(initProxy(returnType, this)));
				}
				catch (Throwable ex) {
					throw new IllegalStateException(
							"Failed to create proxy for controller method return type: " + method, ex);
				}
			}
		}

		@Override
		@Nullable
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			return intercept(proxy, method, args, null);
		}

		@Override
		public Class<?> getControllerType() {
			return this.controllerType;
		}

		@Override
		public Method getControllerMethod() {
			Assert.state(this.controllerMethod != null, "Not initialized yet");
			return this.controllerMethod;
		}

		@Override
		public Object[] getArgumentValues() {
			Assert.state(this.argumentValues != null, "Not initialized yet");
			return this.argumentValues;
		}


		@SuppressWarnings("unchecked")
		private static <T> T initProxy(
				Class<?> controllerType, @Nullable ControllerMethodInvocationInterceptor interceptor) {

			interceptor = (interceptor != null ?
					interceptor : new ControllerMethodInvocationInterceptor(controllerType));

			if (controllerType == Object.class) {
				return (T) interceptor;
			}

			else if (controllerType.isInterface()) {
				ClassLoader classLoader = controllerType.getClassLoader();
				if (classLoader == null || classLoader.getParent() == null) {
					// JDK interface type from bootstrap loader or platform loader ->
					// use higher-level loader which can see Spring infrastructure classes
					classLoader = MethodInvocationInfo.class.getClassLoader();
				}
				Class<?>[] ifcs = new Class<?>[] {controllerType, MethodInvocationInfo.class};
				return (T) Proxy.newProxyInstance(classLoader, ifcs, interceptor);
			}

			else {
				Enhancer enhancer = new Enhancer();
				enhancer.setSuperclass(controllerType);
				enhancer.setInterfaces(new Class<?>[] {MethodInvocationInfo.class});
				enhancer.setNamingPolicy(SpringNamingPolicy.INSTANCE);
				enhancer.setCallbackType(MethodInterceptor.class);

				Class<?> proxyClass = enhancer.createClass();
				Object proxy = null;

				if (objenesis.isWorthTrying()) {
					try {
						proxy = objenesis.newInstance(proxyClass, enhancer.getUseCache());
					}
					catch (ObjenesisException ex) {
						logger.debug("Failed to create controller proxy, falling back on default constructor", ex);
					}
				}

				if (proxy == null) {
					try {
						proxy = ReflectionUtils.accessibleConstructor(proxyClass).newInstance();
					}
					catch (Throwable ex) {
						throw new IllegalStateException(
								"Failed to create controller proxy or use default constructor", ex);
					}
				}

				((Factory) proxy).setCallbacks(new Callback[] {interceptor});
				return (T) proxy;
			}
		}
	}


	/**
	 * 用于为方法参数创建 URL 的构建器类。
	 */
	public static class MethodArgumentBuilder {

		private final Class<?> controllerType;

		private final Method method;

		private final Object[] argumentValues;

		private final UriComponentsBuilder baseUrl;

		/**
		 * 创建一个新的 {@link MethodArgumentBuilder} 实例。
		 * @since 4.2
		 */
		public MethodArgumentBuilder(Class<?> controllerType, Method method) {
			this(null, controllerType, method);
		}

		/**
		 * 创建一个新的 {@link MethodArgumentBuilder} 实例。
		 * @since 4.2
		 */
		public MethodArgumentBuilder(@Nullable UriComponentsBuilder baseUrl, Class<?> controllerType, Method method) {
			Assert.notNull(controllerType, "'controllerType' is required");
			Assert.notNull(method, "'method' is required");
			this.baseUrl = baseUrl != null ? baseUrl : UriComponentsBuilder.fromPath(getPath());
			this.controllerType = controllerType;
			this.method = method;
			this.argumentValues = new Object[method.getParameterCount()];
		}

		private static String getPath() {
			UriComponentsBuilder builder = ServletUriComponentsBuilder.fromCurrentServletMapping();
			String path = builder.build().getPath();
			return path != null ? path : "";
		}

		public MethodArgumentBuilder arg(int index, Object value) {
			this.argumentValues[index] = value;
			return this;
		}

		/**
		 * 仅当你需要对展开的 URI 变量应用强编码（即对具有保留含义的所有字符
		 * 加引号转义）时才使用此方法。
		 * @since 5.0.8
		 */
		public MethodArgumentBuilder encode() {
			this.baseUrl.encode();
			return this;
		}

		public String build() {
			return fromMethodInternal(this.baseUrl, this.controllerType, this.method, this.argumentValues)
					.build().encode().toUriString();
		}

		public String buildAndExpand(Object... uriVars) {
			return fromMethodInternal(this.baseUrl, this.controllerType, this.method, this.argumentValues)
					.buildAndExpand(uriVars).encode().toString();
		}
	}

}
