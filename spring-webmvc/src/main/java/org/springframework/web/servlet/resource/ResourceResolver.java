/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.servlet.resource;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;

/**
 * 将请求解析为服务器端资源的策略。
 *
 * <p>提供将传入请求解析为实际
 * {@link org.springframework.core.io.Resource} 的机制，以及获取客户端在请求该资源时
 * 应使用的公共 URL 路径的机制。
 * @author Jeremy Grelle
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 4.1
 * @see org.springframework.web.servlet.resource.ResourceResolverChain
 */
public interface ResourceResolver {

	/**
	 * 将所提供的请求和请求路径解析为存在于
	 * 给定资源位置之一的 {@link Resource}。
	 * @param request 当前请求（在某些调用中可能不存在）
	 * @param requestPath 要使用的请求路径部分
	 * @param locations 查找资源时搜索的位置
	 * @param chain 要委托的其余解析器链
	 * @return 解析出的资源；如果无法解析，则为 {@code null}
	 */
	@Nullable
	Resource resolveResource(@Nullable HttpServletRequest request, String requestPath,
			List<? extends Resource> locations, ResourceResolverChain chain);

	/**
	 * 为客户端解析面向外部的<em>公共</em> URL 路径，以便访问位于
	 * 给定<em>内部</em>资源路径的资源。
	 * <p>这在向客户端渲染 URL 链接时很有用。
	 * @param resourcePath 内部资源路径
	 * @param locations 查找资源时搜索的位置
	 * @param chain 要委托的解析器链
	 * @return 解析出的公共 URL 路径；如果无法解析，则为 {@code null}
	 */
	@Nullable
	String resolveUrlPath(String resourcePath, List<? extends Resource> locations, ResourceResolverChain chain);

}
