/*
 * Copyright 2002-2018 the original author or authors.
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

import org.springframework.core.annotation.AliasFor;

/**
 * 指示消息处理方法的返回值应作为 {@link org.springframework.messaging.Message}
 * 发送到指定目标，并在前面加上 <code>"/user/{username}"</code>，
 * 其中用户名从正在处理的输入消息头中提取。
 *
 * <p>{@code @SendTo} 和 {@code @SendToUser} 可以用于同一方法，
 * 此时消息会发送到两个注解指定的目标。
 *
 * <p>此注解可以放在类级别，此时会由该类的方法继承。
 * 同时，方法级的 {@code @SendTo} 或 {@code @SendToUser} 注解
 * 会覆盖类级别上的相应注解。

 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 4.0
 * @see org.springframework.messaging.simp.annotation.support.SendToMethodReturnValueHandler
 * @see org.springframework.messaging.simp.user.UserDestinationMessageHandler
 * @see org.springframework.messaging.simp.SimpMessageHeaderAccessor#getUser()
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SendToUser {

	/**
	 * {@link #destinations} 的别名。
	 * @see #destinations
	 */
	@AliasFor("destinations")
	String[] value() default {};

	/**
	 * 一个或多个要发送消息的目标。
	 * <p>如果未指定，则根据正在处理的输入消息的目标选择默认目标。
	 * @since 4.2
	 * @see #value
	 * @see org.springframework.messaging.simp.annotation.support.SendToMethodReturnValueHandler
	 */
	@AliasFor("value")
	String[] destinations() default {};

	/**
	 * 消息应发送到与该用户关联的所有会话，还是仅发送到正在处理的输入消息的会话。
	 * <p>默认值为 {@code true}，此时消息会广播到所有会话。
	 */
	boolean broadcast() default true;

}
