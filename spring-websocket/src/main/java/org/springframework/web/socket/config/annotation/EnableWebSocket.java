/*
 * Copyright 2002-2023 the original author or authors.
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
 * 将此注解添加到 {@code @Configuration} 类以配置 WebSocket 请求的处理。
 * 典型的配置如下所示：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableWebSocket
 * public class MyWebSocketConfig {
 *
 * }
 * </pre>
 *
 * <p>通过实现
 * {@link WebSocketConfigurer} 接口来定制导入的配置：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableWebSocket
 * public class MyConfiguration implements WebSocketConfigurer {
 *
 * 	   &#064;Override
 * 	   public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
 *         registry.addHandler(echoWebSocketHandler(), "/echo").withSockJS();
 * 	   }
 *
 *	   &#064;Bean
 *	   public WebSocketHandler echoWebSocketHandler() {
 *         return new EchoWebSocketHandler();
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
@Import(DelegatingWebSocketConfiguration.class)
public @interface EnableWebSocket {
}
