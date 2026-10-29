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

package org.springframework.beans.factory.config;

/**
 * 适配器，将 {@link SmartInstantiationAwareBeanPostProcessor} 上的所有方法实现为空操作，不会改变容
 * 器实例化的每个 Bean 的正常处理。子类可以仅重写它们真正关心的方法。
 *
 * <p>请注意，只有当确实需要 {@link InstantiationAwareBeanPostProcessor} 功能时才推荐使用此基类。如
 * 果只需要普通的 {@link BeanPostProcessor} 功能，建议直接实现那个（更简单的）接口。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 * @deprecated as of 5.3 in favor of implementing {@link InstantiationAwareBeanPostProcessor}
 * or {@link SmartInstantiationAwareBeanPostProcessor} directly.
 */
@Deprecated
public abstract class InstantiationAwareBeanPostProcessorAdapter implements SmartInstantiationAwareBeanPostProcessor {

}
