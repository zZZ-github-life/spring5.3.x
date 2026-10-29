/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.messaging.simp.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于根据订阅的目标将订阅消息映射到特定处理器方法的注解。
 * 仅支持基于 WebSocket 的 STOMP（例如 STOMP SUBSCRIBE 帧）。
 *
 * <p>这是一个方法级注解，可以与类型级
 * {@link org.springframework.messaging.handler.annotation.MessageMapping @MessageMapping}
 * 组合使用。
 *
 * <p>支持与 {@code @MessageMapping} 相同的方法参数；不过订阅消息通常没有正文。
 *
 * <p>返回值的处理也遵循与 {@code @MessageMapping} 相同的规则，
 * 但如果方法没有使用
 * {@link org.springframework.messaging.handler.annotation.SendTo SendTo} 或
 * {@link SendToUser} 注解，则消息会直接发送回连接的用户，
 * 不经过消息代理。这对于实现请求-应答模式很有用。
 *
 * <p><b>注意：</b>使用控制器接口时（例如用于 AOP 代理），
 * 请确保将<i>所有</i>映射注解（例如 {@code @MessageMapping} 和 {@code @SubscribeMapping}）
 * 一致地放在控制器<i>接口</i>上，而不是实现类上。
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 * @see org.springframework.messaging.handler.annotation.MessageMapping
 * @see org.springframework.messaging.handler.annotation.SendTo
 * @see org.springframework.messaging.simp.annotation.SendToUser
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SubscribeMapping {

	/**
	 * 此注解表示的目标映射。
	 * <p>这是 STOMP 消息的目标（例如 {@code "/positions"}）。
	 * 还支持 Ant 风格路径模式（例如 {@code "/price.stock.*"}）
	 * 和路径模板变量（例如 <code>"/price.stock.{ticker}"</code>）。
	 */
	String[] value() default {};

}
