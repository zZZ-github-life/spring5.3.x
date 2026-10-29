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

package org.springframework.web.socket.config.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

/**
 * 将此注解添加到 {@code @Configuration} 类，以通过更高级别的消息传递子协议
 * 启用基于代理的 WebSocket 消息传递。
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableWebSocketMessageBroker
 * public class MyWebSocketConfig {
 *
 * }
 * </pre>
 *
 * <p>通过实现
 * {@link WebSocketMessageBrokerConfigurer} 接口来定制导入的配置：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableWebSocketMessageBroker
 * public class MyConfiguration implements WebSocketMessageBrokerConfigurer {
 *
 *     &#064;Override
 *     public void registerStompEndpoints(StompEndpointRegistry registry) {
 *         registry.addEndpoint("/portfolio").withSockJS();
 *     }
 *
 *     &#064;Override
 *     public void configureMessageBroker(MessageBrokerRegistry registry) {
 *         registry.enableStompBrokerRelay("/queue/", "/topic/");
 *         registry.setApplicationDestinationPrefixes("/app/");
 *     }
 * }
 * </pre>
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(DelegatingWebSocketMessageBrokerConfiguration.class)
public @interface EnableWebSocketMessageBroker {

}
