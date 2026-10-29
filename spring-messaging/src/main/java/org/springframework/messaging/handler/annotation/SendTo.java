/*
 * Copyright 2002-2016 the original author or authors.
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
 * 指示方法的返回值应在必要时转换为 {@link Message}，并发送到指定目标的注解。
 *
 * <p>在典型的请求/应答场景中，传入的 {@link Message} 可能会约定用于应答的目标。
 * 在这种情况下，该目标应优先。
 *
* <p>此注解可以放在类级别，此时会由该类的方法继承。
 *
 * @author Rossen Stoyanchev
 * @author Stephane Nicoll
 * @since 4.0
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SendTo {

	/**
	 * 根据方法返回值创建的消息的目标。
	 */
	String[] value() default {};

}
