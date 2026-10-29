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

package org.springframework.jms.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 聚合多个 {@link JmsListener} 注解的容器注解。
 *
 * <p>可以原生使用，声明多个嵌套的 {@link JmsListener} 注解。
 * 也可以结合 Java 8 对可重复注解的支持使用，此时只需在同一方法上
 * 多次声明 {@link JmsListener}，就会隐式生成此容器注解。
 *
 * <p>此注解可用作<em>元注解</em>，以创建自定义的<em>组合注解</em>。
 *
 * @author Stephane Nicoll
 * @since 4.2
 * @see JmsListener
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JmsListeners {

	JmsListener[] value();

}
