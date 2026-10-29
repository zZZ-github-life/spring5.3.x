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

package org.springframework.messaging.rsocket.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.rsocket.ConnectionSetupPayload;

/**
 * 用于将初始 {@link ConnectionSetupPayload} 以及后续的元数据推送映射到处理器方法的注解。
 *
 * <p>这是一个方法级注解，可以与类型级的
 * {@link org.springframework.messaging.handler.annotation.MessageMapping @MessageMapping}
 * 结合使用，形成组合路由模式。它支持与 {@code @MessageMapping} 相同的参数，
 * 但返回值必须为 {@code void}。在服务器端，处理可以是异步的（例如
 * {@code Mono<Void>}），此时仅当 {@code Mono<Void>} 完成时才接受连接。
 * 在客户端，此方法只是一个回调，不影响连接的建立。
 *
 * <p><strong>注意：</strong>{@code @ConnectMapping} 方法可以通过
 * {@link org.springframework.messaging.rsocket.RSocketRequester RSocketRequester}
 * 方法参数向远程发起请求，但必须独立于处理线程进行（例如
 * 在不同线程上订阅）。
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ConnectMapping {

	/**
	 * 此注解所表达、用于与初始 {@link ConnectionSetupPayload} 的元数据中
	 * 或后续元数据推送中的路由进行匹配的映射。
	 * <p>根据配置的
	 * {@link org.springframework.util.RouteMatcher RouteMatcher}，模式可以基于
	 * {@link org.springframework.util.AntPathMatcher AntPathMatcher} 或
	 * {@link org.springframework.web.util.pattern.PathPattern PathPattern}。
	 * <p>默认情况下这是一个空数组，此时匹配所有
	 * {@link ConnectionSetupPayload} 和元数据推送。
	 */
	String[] value() default {};

}
