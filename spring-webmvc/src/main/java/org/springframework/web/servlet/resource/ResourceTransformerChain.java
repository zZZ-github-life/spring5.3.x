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

package org.springframework.web.servlet.resource;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.springframework.core.io.Resource;

/**
 * 调用 {@link ResourceTransformer ResourceTransformers} 链的约定，其中每个解析器
 * 都会获得对链的引用，以便在需要时进行委托。
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public interface ResourceTransformerChain {

	/**
	 * 返回用于解析被转换 {@code Resource} 的
	 * {@code ResourceResolverChain}。解析相关资源时可能需要它，
	 * 例如指向其他资源的链接。
	 */
	ResourceResolverChain getResolverChain();

	/**
	 * 转换给定资源。
	 * @param request the current request
	 * @param resource the candidate resource to transform
	 * @return the transformed or the same resource, never {@code null}
	 * @throws IOException if transformation fails
	 */
	Resource transform(HttpServletRequest request, Resource resource) throws IOException;

}
