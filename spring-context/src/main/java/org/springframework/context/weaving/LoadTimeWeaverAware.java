/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.context.weaving;

import org.springframework.beans.factory.Aware;
import org.springframework.instrument.classloading.LoadTimeWeaver;

/**
 * 任何希望获知应用上下文默认 {@link LoadTimeWeaver} 的对象都应实现的接口。
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 2.5
 * @see org.springframework.context.ConfigurableApplicationContext#LOAD_TIME_WEAVER_BEAN_NAME
 */
public interface LoadTimeWeaverAware extends Aware {

	/**
	 * 设置此对象所在 {@link org.springframework.context.ApplicationContext ApplicationContext} 的
	 * {@link LoadTimeWeaver}。
	 * <p>在普通 Bean 属性填充之后、但在诸如
	 * {@link org.springframework.beans.factory.InitializingBean InitializingBean's}
	 * {@link org.springframework.beans.factory.InitializingBean#afterPropertiesSet() afterPropertiesSet()}
	 * 或自定义 init-method 之类的初始化回调之前调用。在
	 * {@link org.springframework.context.ApplicationContextAware ApplicationContextAware's}
	 * {@link org.springframework.context.ApplicationContextAware#setApplicationContext setApplicationContext(..)}
	 * 之后调用。
	 * <p><b>NOTE:</b> 只有在应用上下文中确实存在可用的 {@code LoadTimeWeaver} 时才会调用此方法。如果不
	 * 存在，则根本不会调用该方法，此时假定实现对象能够相应地激活其织入依赖。
	 * @param loadTimeWeaver {@code LoadTimeWeaver} 实例（永不为 {@code null}）
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet
	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext
	 */
	void setLoadTimeWeaver(LoadTimeWeaver loadTimeWeaver);

}
