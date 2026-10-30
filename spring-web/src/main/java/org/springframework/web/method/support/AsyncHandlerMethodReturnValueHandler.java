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

package org.springframework.web.method.support;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;

/**
 * 支持异步类型的返回值处理器。此类返回值类型需要优先处理，
 * 以便能够“解包”异步值。
 *
 * <p><strong>注意：</strong>实现此契约并非必需，但当处理器需要优先于其他处理器时，
 * 应当实现此契约。例如，自定义（异步）处理器默认排在内置处理器之后，
 * 它们应优先于 {@code @ResponseBody} 或 {@code @ModelAttribute} 处理，
 * 后者应等到异步值就绪后再执行。相比之下，内置（异步）处理器
 * 已经排在同步处理器之前。
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
public interface AsyncHandlerMethodReturnValueHandler extends HandlerMethodReturnValueHandler {

	/**
	 * 给定返回值是否表示异步计算。
	 * @param returnValue 处理器方法返回的值
	 * @param returnType 返回类型
	 * @return 如果返回值类型表示异步值则返回 {@code true}
	 */
	boolean isAsyncReturnValue(@Nullable Object returnValue, MethodParameter returnType);

}
