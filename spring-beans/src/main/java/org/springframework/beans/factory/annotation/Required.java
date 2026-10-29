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

package org.springframework.beans.factory.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 将某个方法（通常是 JavaBean setter 方法）标记为 'required'：也就是说，必须将该 setter 方法配置为
 * 通过依赖注入获得值。
 *
 * <p>请务必查阅 {@link RequiredAnnotationBeanPostProcessor} 类的 javadoc（默认情况下，该类会检查此
 * 注解是否存在）。
 *
 * @author Rob Harrop
 * @since 2.0
 * @see RequiredAnnotationBeanPostProcessor
 * @deprecated as of 5.1, in favor of using constructor injection for required settings
 * (or a custom {@link org.springframework.beans.factory.InitializingBean} implementation)
 */
@Deprecated
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Required {

}
