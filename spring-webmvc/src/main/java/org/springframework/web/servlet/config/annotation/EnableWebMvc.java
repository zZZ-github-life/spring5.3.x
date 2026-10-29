/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.web.servlet.config.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

/**
 * 将此注解添加到 {@code @Configuration} 类上，会从
 * {@link WebMvcConfigurationSupport} 导入 Spring MVC 配置，例如：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableWebMvc
 * &#064;ComponentScan(basePackageClasses = MyConfiguration.class)
 * public class MyConfiguration {
 * }
 * </pre>
 *
 * <p>要自定义所导入的配置，请实现
 * {@link WebMvcConfigurer} 接口并重写相应的方法，例如：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableWebMvc
 * &#064;ComponentScan(basePackageClasses = MyConfiguration.class)
 * public class MyConfiguration implements WebMvcConfigurer {
 *
 *     &#064;Override
 *     public void addFormatters(FormatterRegistry formatterRegistry) {
 *         formatterRegistry.addConverter(new MyConverter());
 *     }
 *
 *     &#064;Override
 *     public void configureMessageConverters(List&lt;HttpMessageConverter&lt;?&gt;&gt; converters) {
 *         converters.add(new MyHttpMessageConverter());
 *     }
 *
 * }
 * </pre>
 *
 * <p><strong>注意：</strong>只能有一个 {@code @Configuration} 类带有
 * {@code @EnableWebMvc} 注解来导入 Spring Web MVC 配置。
 * 不过，可以有多个实现 {@code WebMvcConfigurer} 的 {@code @Configuration} 类，
 * 用于自定义所提供的配置。
 *
 * <p>如果 {@link WebMvcConfigurer} 未暴露某些需要配置的更高级设置，
 * 可以考虑去掉 {@code @EnableWebMvc} 注解，直接继承
 * {@link WebMvcConfigurationSupport} 或 {@link DelegatingWebMvcConfiguration}，例如：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;ComponentScan(basePackageClasses = { MyConfiguration.class })
 * public class MyConfiguration extends WebMvcConfigurationSupport {
 *
 *     &#064;Override
 *     public void addFormatters(FormatterRegistry formatterRegistry) {
 *         formatterRegistry.addConverter(new MyConverter());
 *     }
 *
 *     &#064;Bean
 *     public RequestMappingHandlerAdapter requestMappingHandlerAdapter() {
 *         // Create or delegate to "super" to create and
 *         // customize properties of RequestMappingHandlerAdapter
 *     }
 * }
 * </pre>
 *
 * @author Dave Syer
 * @author Rossen Stoyanchev
 * @since 3.1
 * @see org.springframework.web.servlet.config.annotation.WebMvcConfigurer
 * @see org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport
 * @see org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(DelegatingWebMvcConfiguration.class)
public @interface EnableWebMvc {
}
