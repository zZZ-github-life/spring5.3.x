/*
 * Copyright 2002-2020 the original author or authors.
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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * 用于将 Web 请求映射到具有灵活方法签名的请求处理类中的方法的注解。
 *
 * <p>Spring MVC 和 Spring WebFlux 都通过各自模块和包结构中的
 * {@code RequestMappingHandlerMapping} 和 {@code RequestMappingHandlerAdapter}
 * 支持此注解。关于各自支持的处理器方法参数和返回类型的完整列表，
 * 请使用下面的参考文档链接：
 * <ul>
 * <li>Spring MVC
 * <a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/web.html#mvc-ann-arguments">方法参数</a>
 * 和
 * <a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/web.html#mvc-ann-return-types">返回值</a>
 * </li>
 * <li>Spring WebFlux
 * <a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/web-reactive.html#webflux-ann-arguments">方法参数</a>
 * 和
 * <a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/web-reactive.html#webflux-ann-return-types">返回值</a>
 * </li>
 * </ul>
 *
 * <p><strong>注意：</strong>此注解既可以在类级别使用，也可以在方法级别使用。
 * 在大多数情况下，在方法级别应用程序会更倾向于使用某个特定于 HTTP 方法的变体：
 * {@link GetMapping @GetMapping}、{@link PostMapping @PostMapping}、
 * {@link PutMapping @PutMapping}、{@link DeleteMapping @DeleteMapping} 或
 * {@link PatchMapping @PatchMapping}。</p>
 *
 * <p><b>注意：</b>使用控制器接口时（例如用于 AOP 代理），请确保<i>所有</i>
 * 映射注解（如 {@code @RequestMapping} 和 {@code @SessionAttributes}）
 * 一致地放在控制器<i>接口</i>上，而不是放在实现类上。
 *
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @author Sam Brannen
 * @since 2.5
 * @see GetMapping
 * @see PostMapping
 * @see PutMapping
 * @see DeleteMapping
 * @see PatchMapping
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Mapping
public @interface RequestMapping {

	/**
	 * 为此映射分配一个名称。
	 * <p><b>类型级别和方法级别均支持！</b>
	 * 当在两个级别上使用时，组合名称会以 "#" 作为分隔符拼接得出。
	 * @see org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder
	 * @see org.springframework.web.servlet.handler.HandlerMethodMappingNamingStrategy
	 */
	String name() default "";

	/**
	 * 此注解所表达的主要映射。
	 * <p>这是 {@link #path} 的别名。例如，
	 * {@code @RequestMapping("/foo")} 等价于
	 * {@code @RequestMapping(path="/foo")}。
	 * <p><b>类型级别和方法级别均支持！</b>
	 * 在类型级别使用时，所有方法级别的映射都会继承此主要映射，
	 * 并为特定的处理器方法缩小范围。
	 * <p><strong>注意</strong>：未显式映射到任何路径的处理器方法实际上被映射到空路径。
	 */
	@AliasFor("path")
	String[] value() default {};

	/**
	 * 路径映射 URI（例如 {@code "/profile"}）。
	 * <p>也支持 Ant 风格的路径模式（例如 {@code "/profile/**"}）。
	 * 在方法级别，支持在类型级别表达的主要映射内使用相对路径（例如 {@code "edit"}）。
	 * 路径映射 URI 可以包含占位符（例如 <code>"/${profile_path}"</code>）。
	 * <p><b>类型级别和方法级别均支持！</b>
	 * 在类型级别使用时，所有方法级别的映射都会继承此主要映射，
	 * 并为特定的处理器方法缩小范围。
	 * <p><strong>注意</strong>：未显式映射到任何路径的处理器方法实际上被映射到空路径。
	 * @since 4.2
	 */
	@AliasFor("value")
	String[] path() default {};

	/**
	 * 要映射到的 HTTP 请求方法，用于缩小主要映射：
	 * GET、POST、HEAD、OPTIONS、PUT、PATCH、DELETE、TRACE。
	 * <p><b>类型级别和方法级别均支持！</b>
	 * 在类型级别使用时，所有方法级别的映射都会继承此 HTTP 方法限制。
	 */
	RequestMethod[] method() default {};

	/**
	 * 所映射请求的参数，用于缩小主要映射。
	 * <p>适用于任何环境的相同格式：一系列 "myParam=myValue" 风格的表达式，
	 * 只有当每个这样的参数都被发现具有给定值时，请求才会被映射。
	 * 可以使用 "!=" 运算符对表达式取反，如 "myParam!=myValue"。
	 * 也支持 "myParam" 风格的表达式，此时这些参数必须存在于请求中
	 * （允许具有任意值）。最后，"!myParam" 风格的表达式表示指定参数
	 * <i>不应</i>存在于请求中。
	 * <p><b>类型级别和方法级别均支持！</b>
	 * 在类型级别使用时，所有方法级别的映射都会继承此参数限制。
	 */
	String[] params() default {};

	/**
	 * 所映射请求的头，用于缩小主要映射。
	 * <p>适用于任何环境的相同格式：一系列 "My-Header=myValue" 风格的表达式，
	 * 只有当每个这样的头都被发现具有给定值时，请求才会被映射。
	 * 可以使用 "!=" 运算符对表达式取反，如 "My-Header!=myValue"。
	 * 也支持 "My-Header" 风格的表达式，此时这些头必须存在于请求中
	 * （允许具有任意值）。最后，"!My-Header" 风格的表达式表示指定头
	 * <i>不应</i>存在于请求中。
	 * <p>对于诸如 Accept 和 Content-Type 之类的头，还支持媒体类型通配符（*）。
	 * 例如，
	 * <pre class="code">
	 * &#064;RequestMapping(value = "/something", headers = "content-type=text/*")
	 * </pre>
	 * 将匹配 Content-Type 为 "text/html"、"text/plain" 等的请求。
	 * <p><b>类型级别和方法级别均支持！</b>
	 * 在类型级别使用时，所有方法级别的映射都会继承此头限制。
	 * @see org.springframework.http.MediaType
	 */
	String[] headers() default {};

	/**
	 * 通过所映射处理器可以消费的媒体类型来缩小主要映射。
	 * 由一个或多个媒体类型组成，其中必须有一个与请求的 {@code Content-Type}
	 * 头匹配。示例：
	 * <pre class="code">
	 * consumes = "text/plain"
	 * consumes = {"text/plain", "application/*"}
	 * consumes = MediaType.TEXT_PLAIN_VALUE
	 * </pre>
	 * 可以使用 "!" 运算符对表达式取反，如 "!text/plain"，
	 * 它匹配 {@code Content-Type} 不是 "text/plain" 的所有请求。
	 * <p><b>类型级别和方法级别均支持！</b>
	 * 如果在两个级别都指定，方法级别的 consumes 条件会覆盖类型级别的条件。
	 * @see org.springframework.http.MediaType
	 * @see javax.servlet.http.HttpServletRequest#getContentType()
	 */
	String[] consumes() default {};

	/**
	 * 通过所映射处理器可以产生的媒体类型来缩小主要映射。
	 * 由一个或多个媒体类型组成，其中必须有一个通过内容协商针对请求的
	 * "可接受" 媒体类型被选中。通常这些媒体类型从 {@code "Accept"} 头中提取，
	 * 但也可能来自查询参数或其他来源。示例：
	 * <pre class="code">
	 * produces = "text/plain"
	 * produces = {"text/plain", "application/*"}
	 * produces = MediaType.TEXT_PLAIN_VALUE
	 * produces = "text/plain;charset=UTF-8"
	 * </pre>
	 * <p>如果声明的媒体类型包含参数（例如 "charset=UTF-8"、"type=feed"、
	 * "type=entry"），并且来自请求的兼容媒体类型也具有该参数，
	 * 则参数值必须匹配。否则，如果来自请求的媒体类型不包含该参数，
	 * 则假定客户端接受任何值。
	 * <p>可以使用 "!" 运算符对表达式取反，如 "!text/plain"，
	 * 它匹配 {@code Accept} 不是 "text/plain" 的所有请求。
	 * <p><b>类型级别和方法级别均支持！</b>
	 * 如果在两个级别都指定，方法级别的 produces 条件会覆盖类型级别的条件。
	 * @see org.springframework.http.MediaType
	 */
	String[] produces() default {};

}
