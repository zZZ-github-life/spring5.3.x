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

/**
 * 指示方法参数应绑定到目标模板字符串中的模板变量的注解。
 * 支持在诸如 {@link MessageMapping @MessageMapping} 的消息处理方法上使用。
 *
 * <p>{@code @DestinationVariable} 模板变量始终是必需的。
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @since 4.0
 * @see org.springframework.messaging.handler.annotation.MessageMapping
 * @see org.springframework.messaging.simp.annotation.support.SimpAnnotationMethodMessageHandler
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DestinationVariable {

	/**
	 * 要绑定的目标模板变量名称。
	 */
	String value() default "";

}
