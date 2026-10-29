/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.stereotype;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 表示被注解的元素代表用于索引的构造型。
 *
 * <p>{@code CandidateComponentsIndex} 是类路径扫描的一种替代方案，它使用在编译时生成的元数据文件。
 * 该索引允许根据构造型检索候选组件（即全限定名）。此注解指示生成器为存在被注解元素的元素建立索引，
 * 或者为被注解元素所实现或继承的元素建立索引。构造型是被注解元素的全限定名。
 *
 * <p>考虑默认的 {@link Component} 注解，它本身带有此注解作为元注解。如果某个组件标注了
 * {@link Component}，则该组件的条目将使用 {@code org.springframework.stereotype.Component} 构造型
 * 添加到索引中。
 *
 * <p>此注解在元注解上同样有效。考虑下面这个自定义注解：
 * <pre class="code">
 * package com.example;
 *
 * &#064;Target(ElementType.TYPE)
 * &#064;Retention(RetentionPolicy.RUNTIME)
 * &#064;Documented
 * &#064;Indexed
 * &#064;Service
 * public @interface PrivilegedService { ... }
 * </pre>
 *
 * 如果上述注解出现在某个类型上，则该类型将使用两个构造型建立索引：
 * {@code org.springframework.stereotype.Component} 和 {@code com.example.PrivilegedService}。虽然
 * {@link Service} 没有直接标注 {@code Indexed}，但它带有 {@link Component} 元注解。
 *
 * <p>还可以通过在接口或类上添加 {@code @Indexed}，为某个接口的所有实现或某个类的所有子类建立索引。
 *
 * 考虑下面这个基础接口：
 * <pre class="code">
 * package com.example;
 *
 * &#064;Indexed
 * public interface AdminService { ... }
 * </pre>
 *
 * 现在，考虑某个位置存在此 {@code AdminService} 的实现：
 * <pre class="code">
 * package com.example.foo;
 *
 * import com.example.AdminService;
 *
 * public class ConfigurationAdminService implements AdminService { ... }
 * </pre>
 *
 * 由于此类实现了已建立索引的接口，因此它会自动以 {@code com.example.AdminService} 构造型被包含。如
 * 果层次结构中还有更多 {@code @Indexed} 接口和/或超类，则该类会映射到它们的全部构造型。
 *
 * @author Stephane Nicoll
 * @since 5.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Indexed {
}
