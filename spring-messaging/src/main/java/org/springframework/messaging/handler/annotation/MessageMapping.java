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

package org.springframework.messaging.handler.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.messaging.Message;

/**
 * 通过将声明的 {@link #value() 模式}与从消息中提取的目标进行匹配，
 * 将 {@link Message} 映射到消息处理方法的注解。该注解也支持在类型级别使用，
 * 以便在所有类方法上声明模式前缀。
 *
 * <p>{@code @MessageMapping} 方法支持以下参数：
 * <ul>
 * <li>{@link Payload @Payload} 方法参数，用于提取消息的载荷并将其反序列化为
 * 声明的目标类型。{@code @Payload} 参数还可以使用诸如
 * {@link org.springframework.validation.annotation.Validated @Validated}
 * 的校验注解进行标注，此时将应用 JSR-303 校验。请注意，该注解并非必须存在，
 * 因为对于未以其他方式处理的参数，默认会假定使用它。</li>
 * <li>{@link DestinationVariable @DestinationVariable} 方法参数，用于访问从消息目标
 * 中提取的模板变量值，例如 {@code /hotels/{hotel}}。如有必要，变量值还可以从 String
 * 转换为声明的方法参数类型。</li>
 * <li>{@link Header @Header} 方法参数，用于提取特定的消息头值，并对其应用
 * {@link org.springframework.core.convert.converter.Converter Converter}
 * 将值转换为声明的目标类型。</li>
 * <li>{@link Headers @Headers} 方法参数，该参数还可以赋值给 {@link java.util.Map}，
 * 用于访问所有头。</li>
 * <li>{@link org.springframework.messaging.MessageHeaders MessageHeaders}
 * 方法参数，用于访问所有头。</li>
 * <li>{@link org.springframework.messaging.support.MessageHeaderAccessor
 * MessageHeaderAccessor} 方法参数，用于访问所有头。在某些处理场景中，
 * 例如基于 WebSocket 的 STOMP，它也可能是诸如
 * {@link org.springframework.messaging.simp.SimpMessageHeaderAccessor
 * SimpMessageHeaderAccessor} 的特化类型。</li>
 * <li>{@link Message Message&lt;T&gt;}，用于访问正文和头，如有必要会将正文
 * 反序列化以匹配声明类型。</li>
 * <li>在某些处理场景（例如基于 WebSocket 的 STOMP）中支持
 * {@link java.security.Principal} 方法参数。它反映已认证的用户。</li>
 * </ul>
 *
 * <p>返回值的处理取决于处理场景：
 * <ul>
 * <li>基于 WebSocket 的 STOMP —— 值会被转换为消息，并发送到默认响应目标
 * 或通过 {@link SendTo @SendTo} 或
 * {@link org.springframework.messaging.simp.annotation.SendToUser @SendToUser}
 * 注解指定的自定义目标。
 * <li>RSocket —— 响应用于回复流请求。
 * </ul>
 *
 * <p>此注解的特化形式包括
 * {@link org.springframework.messaging.simp.annotation.SubscribeMapping @SubscribeMapping}
 * （例如 STOMP 订阅）和
 * {@link org.springframework.messaging.rsocket.annotation.ConnectMapping @ConnectMapping}
 * （例如 RSocket 连接）。两者都会进一步缩小主映射的范围，并且还会匹配消息类型。
 * 两者都可以与声明公共模式前缀的类型级 {@code @MessageMapping} 组合使用。
 *
 * <p>有关此注解在不同上下文中使用的更多详细信息，
 * 请参阅 Spring Framework 参考文档的以下章节：
 * <ul>
 * <li>基于 WebSocket 的 STOMP
 * <a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/web.html#websocket-stomp-handle-annotations">
 * “带注解的控制器”</a>。
 * <li>RSocket
 * <a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/web-reactive.html#rsocket-annot-responders">
 * “带注解的响应器”</a>。
 * </ul>
 *
 * <p><b>注意：</b>使用控制器接口时（例如用于 AOP 代理），
 * 请确保将<i>所有</i>映射注解（例如 {@code @MessageMapping} 和 {@code @SubscribeMapping}）
 * 一致地放在控制器<i>接口</i>上，而不是实现类上。
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 * @see org.springframework.messaging.simp.annotation.support.SimpAnnotationMethodMessageHandler
 * @see org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MessageMapping {

	/**
	 * 此注解表示的目标映射。
	 * <p>对于基于 WebSocket 的 STOMP 消息，这是
	 * {@link org.springframework.util.AntPathMatcher AntPathMatcher} 风格的模式，
	 * 与消息的 STOMP 目标进行匹配。
	 * <p>对于 RSocket，这可以是基于
	 * {@link org.springframework.util.AntPathMatcher AntPathMatcher} 或
	 * {@link org.springframework.web.util.pattern.PathPattern PathPattern}
	 * 的模式（取决于配置），与流请求的路由进行匹配。
	 * <p>如果未配置模式，则映射匹配所有目标。
	 */
	String[] value() default {};

}
