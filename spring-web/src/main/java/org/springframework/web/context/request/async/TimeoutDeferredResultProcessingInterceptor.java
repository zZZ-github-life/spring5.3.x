/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.context.request.async;

import org.springframework.web.context.request.NativeWebRequest;

/**
 * 如果响应尚未提交，则在超时时发送 503（SERVICE_UNAVAILABLE）。自 4.2.8 起，这是通过返回
 * {@link AsyncRequestTimeoutException} 作为处理结果来间接实现的，之后由 Spring MVC 的默认异常处
 * 理将其作为 503 错误处理。
 *
 * <p>注册在最后，位于所有其他拦截器之后，因此只有在没有其他拦截器处理超时时才会被调用。
 *
 * <p>注意，根据 RFC 7231，不带 'Retry-After' 头的 503 会被解释为 500 错误，客户端不应重试。应用
 * 程序可以安装自己的拦截器来处理超时，并在需要时添加 'Retry-After' 头。
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class TimeoutDeferredResultProcessingInterceptor implements DeferredResultProcessingInterceptor {

	@Override
	public <T> boolean handleTimeout(NativeWebRequest request, DeferredResult<T> result) throws Exception {
		result.setErrorResult(new AsyncRequestTimeoutException());
		return false;
	}

}
