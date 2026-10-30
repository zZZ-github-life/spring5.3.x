/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.aop.aspectj.annotation;

import org.springframework.aop.aspectj.AspectInstanceFactory;
import org.springframework.lang.Nullable;

/**
 * {@link org.springframework.aop.aspectj.AspectInstanceFactory} 的子接口，返回与带 AspectJ 注解的
 * 类关联的 {@link AspectMetadata}。
 *
 * @author Rod Johnson
 * @since 2.0
 * @see AspectMetadata
 * @see org.aspectj.lang.reflect.AjType
 */
public interface MetadataAwareAspectInstanceFactory extends AspectInstanceFactory {

	/**
	 * 获取此工厂切面的 AspectJ AspectMetadata。
	 * @return 切面元数据
	 */
	AspectMetadata getAspectMetadata();

	/**
	 * 获取此工厂尽可能最佳的创建互斥体。
	 * @return 互斥对象（可能为 {@code null}，表示不使用互斥）
	 * @since 4.3
	 */
	@Nullable
	Object getAspectCreationMutex();

}
