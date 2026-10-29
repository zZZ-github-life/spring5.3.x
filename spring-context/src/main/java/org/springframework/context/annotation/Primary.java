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

package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 指示当多个候选 Bean 都有资格自动注入单值依赖时，应优先选择该 Bean。如果候选中恰好存在一个“主要”
 * Bean，则它将成为自动注入的值。
 *
 * <p>此注解在语义上等同于 Spring XML 中 {@code <bean>} 元素的 {@code primary} 属性。
 *
 * <p>可用于直接或间接标注了 {@code @Component} 的任何类，或标注了 @{@link Bean} 的方法。
 *
 * <h2>示例</h2>
 * <pre class="code">
 * &#064;Component
 * public class FooService {
 *
 *     private FooRepository fooRepository;
 *
 *     &#064;Autowired
 *     public FooService(FooRepository fooRepository) {
 *         this.fooRepository = fooRepository;
 *     }
 * }
 *
 * &#064;Component
 * public class JdbcFooRepository extends FooRepository {
 *
 *     public JdbcFooRepository(DataSource dataSource) {
 *         // ...
 *     }
 * }
 *
 * &#064;Primary
 * &#064;Component
 * public class HibernateFooRepository extends FooRepository {
 *
 *     public HibernateFooRepository(SessionFactory sessionFactory) {
 *         // ...
 *     }
 * }
 * </pre>
 *
 * <p>由于 {@code HibernateFooRepository} 标注了 {@code @Primary}，假设两者都作为 Bean 存在于同一个
 * Spring 应用上下文中，它将优先于基于 jdbc 的变体被注入；在大量应用组件扫描时通常就是这种情况。
 *
 * <p>请注意，除非使用了组件扫描，否则在类级别使用 {@code @Primary} 不会生效。如果通过 XML 声明标注
 * 了 {@code @Primary} 的类，则会忽略 {@code @Primary} 注解元数据，而改为遵循
 * {@code <bean primary="true|false"/>}。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.0
 * @see Lazy
 * @see Bean
 * @see ComponentScan
 * @see org.springframework.stereotype.Component
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Primary {

}
