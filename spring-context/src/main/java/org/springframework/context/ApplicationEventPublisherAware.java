/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.context;

import org.springframework.beans.factory.Aware;

/**
 * 由任何希望获知其运行所在 ApplicationEventPublisher（通常是 ApplicationContext）的对象实现的接口。
 *
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 1.1.1
 * @see ApplicationContextAware
 */
public interface ApplicationEventPublisherAware extends Aware {

	/**
	 * 设置此对象运行所在的 ApplicationEventPublisher。
	 *
	 * <p>在填充常规 Bean 属性之后、但在诸如 InitializingBean 的 afterPropertiesSet 之类的初始化回调或
	 * 自定义 init-method 之前调用。在 ApplicationContextAware 的 setApplicationContext 之前调用。
	 * @param applicationEventPublisher event publisher to be used by this object
	 */
	void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher);

}
