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

package org.springframework.web.servlet;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.springframework.lang.Nullable;

/**
 * 由定义请求与处理器对象之间映射关系的对象实现的接口。
 *
 * <p>应用开发者可以实现该类，但并非必须，因为框架中已包含
 * {@link org.springframework.web.servlet.handler.BeanNameUrlHandlerMapping}
 * 和 {@link org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping}。
 * 如果应用上下文中没有注册任何 HandlerMapping bean，则前者是默认实现。
 *
 * <p>HandlerMapping 实现可以支持（但并非必须支持）映射的拦截器。
 * 处理器始终会被包装在一个 {@link HandlerExecutionChain} 实例中，
 * 并可附带一些 {@link HandlerInterceptor} 实例。
 * DispatcherServlet 会先按给定顺序调用每个 HandlerInterceptor 的
 * {@code preHandle} 方法，如果所有 {@code preHandle} 方法都返回 {@code true}，
 * 最后才会调用处理器本身。
 *
 * <p>能够对此映射进行参数化是该 MVC 框架一项强大而独特的能力。
 * 例如，可以编写基于会话状态、Cookie 状态或许多其他变量的自定义映射。
 * 似乎没有其他 MVC 框架能具备同等的灵活性。
 *
 * <p>注意：实现类可以实现 {@link org.springframework.core.Ordered} 接口，
 * 以便指定排序顺序，从而决定被 DispatcherServlet 应用的优先级。
 * 未实现 Ordered 的实例会被视为最低优先级。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.core.Ordered
 * @see org.springframework.web.servlet.handler.AbstractHandlerMapping
 * @see org.springframework.web.servlet.handler.BeanNameUrlHandlerMapping
 * @see org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
 */
public interface HandlerMapping {

	/**
	 * {@link HttpServletRequest} 属性的名称，该属性包含最佳匹配模式所映射的处理器。
	 * @since 4.3.21
	 */
	String BEST_MATCHING_HANDLER_ATTRIBUTE = HandlerMapping.class.getName() + ".bestMatchingHandler";

	/**
	 * {@link HttpServletRequest} 属性的名称，该属性包含用于查找匹配处理器的路径，
	 * 根据所配置的 {@link org.springframework.web.util.UrlPathHelper}，
	 * 它可能是完整路径或不含上下文路径的路径、已解码或未解码的路径等。
	 * @since 5.2
	 * @deprecated as of 5.3 in favor of
	 * {@link org.springframework.web.util.UrlPathHelper#PATH_ATTRIBUTE} and
	 * {@link org.springframework.web.util.ServletRequestPathUtils#PATH_ATTRIBUTE}.
	 * To access the cached path used for request mapping, use
	 * {@link org.springframework.web.util.ServletRequestPathUtils#getCachedPathValue(ServletRequest)}.
	 */
	@Deprecated
	String LOOKUP_PATH = HandlerMapping.class.getName() + ".lookupPath";

	/**
	 * {@link HttpServletRequest} 属性的名称，该属性包含处理器映射内的路径，
	 * 在模式匹配的情况下为该模式，否则为完整的相关 URI
	 * （通常在 DispatcherServlet 的映射范围内）。
	 * <p>注意：并非所有 HandlerMapping 实现都必须支持该属性。
	 * 基于 URL 的 HandlerMapping 通常会支持它，但处理器不应期望
	 * 该请求属性在所有场景下都存在。
	 */
	String PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE = HandlerMapping.class.getName() + ".pathWithinHandlerMapping";

	/**
	 * {@link HttpServletRequest} 属性的名称，该属性包含处理器映射内的最佳匹配模式。
	 * <p>注意：并非所有 HandlerMapping 实现都必须支持该属性。
	 * 基于 URL 的 HandlerMapping 通常会支持它，但处理器不应期望
	 * 该请求属性在所有场景下都存在。
	 */
	String BEST_MATCHING_PATTERN_ATTRIBUTE = HandlerMapping.class.getName() + ".bestMatchingPattern";

	/**
	 * 布尔型 {@link HttpServletRequest} 属性的名称，该属性指示是否应检查类型级映射。
	 * <p>注意：并非所有 HandlerMapping 实现都必须支持该属性。
	 */
	String INTROSPECT_TYPE_LEVEL_MAPPING = HandlerMapping.class.getName() + ".introspectTypeLevelMapping";

	/**
	 * {@link HttpServletRequest} 属性的名称，该属性包含 URI 模板映射，
	 * 即变量名称到值的映射。
	 * <p>注意：并非所有 HandlerMapping 实现都必须支持该属性。
	 * 基于 URL 的 HandlerMapping 通常会支持它，但处理器不应期望
	 * 该请求属性在所有场景下都存在。
	 */
	String URI_TEMPLATE_VARIABLES_ATTRIBUTE = HandlerMapping.class.getName() + ".uriTemplateVariables";

	/**
	 * {@link HttpServletRequest} 属性的名称，该属性包含一个映射，
	 * 其键为 URI 变量名称，值为每个变量对应的 URI 矩阵变量的 MultiValueMap。
	 * <p>注意：并非所有 HandlerMapping 实现都必须支持该属性，
	 * 并且取决于 HandlerMapping 是否配置为保留矩阵变量内容，该属性也可能不存在。
	 */
	String MATRIX_VARIABLES_ATTRIBUTE = HandlerMapping.class.getName() + ".matrixVariables";

	/**
	 * {@link HttpServletRequest} 属性的名称，该属性包含适用于所映射处理器的
	 * 可生成 MediaType 集合。
	 * <p>注意：并非所有 HandlerMapping 实现都必须支持该属性。
	 * 处理器不应期望该请求属性在所有场景下都存在。
	 */
	String PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE = HandlerMapping.class.getName() + ".producibleMediaTypes";


	/**
	 * 此 {@code HandlerMapping} 实例是否已启用解析后的
	 * {@link org.springframework.web.util.pattern.PathPattern}，
	 * 若启用，则 {@link DispatcherServlet} 会自动
	 * {@link org.springframework.web.util.ServletRequestPathUtils#parseAndCache 解析}
	 * {@code RequestPath}，使其可在 {@code HandlerMapping}、
	 * {@code HandlerInterceptor} 及其他组件中
	 * {@link org.springframework.web.util.ServletRequestPathUtils#getParsedRequestPath 访问}。
	 * @since 5.3
	 */
	default boolean usesPathPatterns() {
		return false;
	}

	/**
	 * 为此请求返回一个处理器及任意拦截器。选择可以基于请求 URL、会话状态，
	 * 或实现类所选择的任何因素。
	 * <p>返回的 HandlerExecutionChain 包含一个处理器 Object，甚至不需要标记接口，
	 * 因此处理器完全不受约束。例如，可以编写一个 HandlerAdapter
	 * 来使用其他框架的处理器对象。
	 * <p>如果未找到匹配项则返回 {@code null}。这并非错误。
	 * DispatcherServlet 会查询所有已注册的 HandlerMapping bean 以寻找匹配项，
	 * 只有所有 bean 都找不到处理器时才会认定为错误。
	 * @param request current HTTP request
	 * @return a HandlerExecutionChain instance containing handler object and
	 * any interceptors, or {@code null} if no mapping found
	 * @throws Exception if there is an internal error
	 */
	@Nullable
	HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception;

}
