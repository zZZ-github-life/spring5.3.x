/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.servlet.handler;

import org.springframework.web.method.HandlerMethod;

/**
 * 为处理器方法的映射分配名称的策略。
 *
 * <p>该策略可以在
 * {@link org.springframework.web.servlet.handler.AbstractHandlerMethodMapping
 * AbstractHandlerMethodMapping} 上配置。它用于为
 * 每个已注册的处理器方法的映射分配名称。之后可以通过
 * {@link org.springframework.web.servlet.handler.AbstractHandlerMethodMapping#getHandlerMethodsForMappingName(String)
 * AbstractHandlerMethodMapping#getHandlerMethodsForMappingName} 查询这些名称。
 *
 * <p>应用程序可以借助静态方法
 * {@link org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder#fromMappingName(String)
 * MvcUriComponentsBuilder#fromMappingName} 按名称构建指向控制器方法的 URL，
 * 也可以在 JSP 中通过 Spring 标签库注册的 "mvcUrl"
 * 函数来实现。
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 * @param <T> 映射类型
 */
@FunctionalInterface
public interface HandlerMethodMappingNamingStrategy<T> {

	/**
	 * 确定给定 HandlerMethod 和映射的名称。
	 * @param handlerMethod 处理器方法
	 * @param mapping 映射
	 * @return 名称
	 */
	String getName(HandlerMethod handlerMethod, T mapping);

}
