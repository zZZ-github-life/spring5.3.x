/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.bind.annotation;

/**
 * HTTP 请求方法的枚举。用于 {@link RequestMapping} 注解的
 * {@link RequestMapping#method()} 属性。
 *
 * <p>请注意，默认情况下，{@link org.springframework.web.servlet.DispatcherServlet}
 * 仅支持 GET、HEAD、POST、PUT、PATCH 和 DELETE。DispatcherServlet 将按默认的
 * HttpServlet 行为处理 TRACE 和 OPTIONS，除非明确要求也分发这些请求类型：
 * 请查看 "dispatchOptionsRequest" 和 "dispatchTraceRequest" 属性，
 * 必要时将它们设置为 "true"。
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see RequestMapping
 * @see org.springframework.web.servlet.DispatcherServlet#setDispatchOptionsRequest
 * @see org.springframework.web.servlet.DispatcherServlet#setDispatchTraceRequest
 */
public enum RequestMethod {

	GET, HEAD, POST, PUT, PATCH, DELETE, OPTIONS, TRACE

}
