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

package org.springframework.lang;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.annotation.Nonnull;
import javax.annotation.meta.TypeQualifierDefault;

/**
 * 一个常用的 Spring 注解，用于声明对于给定包，字段默认应视为非空。
 *
 * <p>借助 JSR-305 元注解，向支持 JSR-305 的常用工具表明 Java 中的可空性，并由 Kotlin 用于推断
 * Spring API 的可空性。
 *
 * <p>应在包级别使用，并与字段级别的 {@link Nullable} 注解配合使用。
 *
 * @author Sebastien Deleuze
 * @since 5.0
 * @see NonNullApi
 * @see Nullable
 * @see NonNull
 */
@Target(ElementType.PACKAGE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Nonnull
@TypeQualifierDefault(ElementType.FIELD)
public @interface NonNullFields {
}
