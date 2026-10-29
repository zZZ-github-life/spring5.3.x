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

package org.springframework.beans.factory;

/**
 * 一个标记父接口，表示 Bean 有资格通过回调风格的方法被 Spring 容器告知某个特定的框架对象。实际的方
 * 法签名由各个子接口决定，但通常应仅包含一个接受单个参数的 void 返回方法。
 *
 * <p>请注意，仅实现 {@link Aware} 并不会提供任何默认功能。相反，必须显式进行处理，例如在
 * {@link org.springframework.beans.factory.config.BeanPostProcessor} 中。关于处理特定
 * {@code *Aware} 接口回调的示例，请参阅
 * {@link org.springframework.context.support.ApplicationContextAwareProcessor}。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 */
public interface Aware {

}
