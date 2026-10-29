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

package org.springframework.web.bind.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * 指示特定处理器所使用的会话属性的注解。
 *
 * <p>该注解通常会列出应透明存储在会话或某种会话式存储中的模型属性名称，
 * 这些属性用作表单支持 bean。<b>在类型级别声明</b>，应用于所注解的处理器类
 * 所操作的模型属性。
 *
 * <p><b>注意：</b>使用此注解指示的会话属性对应于特定处理器的模型属性，
 * 会被透明地存储在会话式的会话中。一旦处理器指示其会话式会话完成，
 * 这些属性将被移除。因此，请将此项功能用于那些预期在特定处理器的会话过程
 * 中<i>临时</i>存储在会话中的会话式属性。
 *
 * <p>对于永久会话属性（例如用户认证对象），请改用传统的
 * {@code session.setAttribute} 方法。或者，考虑使用通用
 * {@link org.springframework.web.context.request.WebRequest} 接口的属性管理能力。
 *
 * <p><b>注意：</b>使用控制器接口时（例如用于 AOP 代理），请确保<i>所有</i>
 * 映射注解（如 {@code @RequestMapping} 和 {@code @SessionAttributes}）
 * 一致地放在控制器<i>接口</i>上，而不是放在实现类上。
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 2.5
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface SessionAttributes {

	/**
	 * {@link #names} 的别名。
	 */
	@AliasFor("names")
	String[] value() default {};

	/**
	 * 模型中应存储在会话或某种会话式存储中的会话属性名称。
	 * <p><strong>注意</strong>：这里指示的是<em>模型属性名称</em>。
	 * <em>会话属性名称</em>可能与模型属性名称一致，也可能不一致。
	 * 因此，应用程序不应依赖会话属性名称，而应仅对模型进行操作。
	 * @since 4.2
	 */
	@AliasFor("value")
	String[] names() default {};

	/**
	 * 模型中应存储在会话或某种会话式存储中的会话属性类型。
	 * <p>这些类型的所有模型属性都将存储在会话中，与属性名称无关。
	 */
	Class<?>[] types() default {};

}
