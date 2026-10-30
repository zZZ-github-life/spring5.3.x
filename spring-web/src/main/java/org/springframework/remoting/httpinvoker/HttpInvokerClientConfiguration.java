/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.remoting.httpinvoker;

import org.springframework.lang.Nullable;

/**
 * 用于执行 HTTP invoker 请求的配置接口。
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see HttpInvokerRequestExecutor
 * @see HttpInvokerClientInterceptor
 * @deprecated as of 5.3 (phasing out serialization-based remoting)
 */
@Deprecated
public interface HttpInvokerClientConfiguration {

	/**
	 * 返回目标服务的 HTTP URL。
	 */
	String getServiceUrl();

	/**
	 * 返回在本地找不到类时用于下载类的 codebase URL。
	 * 可以由多个 URL 组成，以空格分隔。
	 * @return codebase URL；如果没有则为 {@code null}
	 * @see java.rmi.server.RMIClassLoader
	 */
	@Nullable
	String getCodebaseUrl();

}
