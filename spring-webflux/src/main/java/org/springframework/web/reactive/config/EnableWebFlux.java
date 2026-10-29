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

package org.springframework.web.reactive.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

/**
 * 将此注解添加到 {@code @Configuration} 类会从 {@link WebFluxConfigurationSupport} 导入 Spring WebFlux 配置，从而启用注解式控制器和函数式端点。
 *
 * <p>例如：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableWebFlux
 * &#064;ComponentScan
 * public class MyConfiguration {
 * }
 * </pre>
 *
 * <p>要自定义导入的配置，请实现 {@link WebFluxConfigurer} 及其一个或多个方法：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableWebFlux
 * &#064;ComponentScan
 * public class MyConfiguration implements WebFluxConfigurer {
 *
 *     &#064;Autowired
 *     private ObjectMapper objectMapper;
 *
 *     &#064;Override
 *     public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
 *         configurer.defaultCodecs().jackson2JsonEncoder(
 *             new Jackson2JsonEncoder(objectMapper)
 *         );
 *         configurer.defaultCodecs().jackson2JsonDecoder(
 *             new Jackson2JsonDecoder(objectMapper)
 *         );
 *     }
 *
 *     // ...
 * }
 * </pre>
 *
 * <p>只应有一个 {@code @Configuration} 类带有 {@code @EnableWebFlux} 注解，以导入 Spring WebFlux 配置。不过，可以有多个实现 {@code WebFluxConfigurer} 的 {@code @Configuration} 类来自定义所提供的配置。
 *
 * <p>如果 {@code WebFluxConfigurer} 未暴露某些需要配置的设置，可考虑切换到高级模式：移除 {@code @EnableWebFlux} 注解，直接继承 {@link WebFluxConfigurationSupport} 或 {@link DelegatingWebFluxConfiguration}，后者允许检测并委托给一个或多个 {@code WebFluxConfigurer} 配置类。
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see WebFluxConfigurer
 * @see WebFluxConfigurationSupport
 * @see DelegatingWebFluxConfiguration
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(DelegatingWebFluxConfiguration.class)
public @interface EnableWebFlux {
}
