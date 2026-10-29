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

package org.springframework.cache.interceptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.springframework.cache.CacheManager;
import org.springframework.lang.Nullable;

/**
 * 一种 {@link CacheResolver}，它强制将解析限定为
 * 针对给定 {@link CacheManager} 的可配置名称集合。
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
public class NamedCacheResolver extends AbstractCacheResolver {

	@Nullable
	private Collection<String> cacheNames;


	public NamedCacheResolver() {
	}

	public NamedCacheResolver(CacheManager cacheManager, String... cacheNames) {
		super(cacheManager);
		this.cacheNames = new ArrayList<>(Arrays.asList(cacheNames));
	}


	/**
	 * 设置此解析器应使用的缓存名称。
	 */
	public void setCacheNames(Collection<String> cacheNames) {
		this.cacheNames = cacheNames;
	}

	@Override
	protected Collection<String> getCacheNames(CacheOperationInvocationContext<?> context) {
		return this.cacheNames;
	}

}
