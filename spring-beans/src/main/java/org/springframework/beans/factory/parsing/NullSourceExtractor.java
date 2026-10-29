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

package org.springframework.beans.factory.parsing;

import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;

/**
 * 返回 {@code null} 作为源元数据的 {@link SourceExtractor} 简单实现。
 *
 * <p>这是默认实现，可防止在正常的（非工具化）运行时使用过程中在内存中保留过多元数据。
 *
 * @author Rob Harrop
 * @since 2.0
 */
public class NullSourceExtractor implements SourceExtractor {

	/**
	 * 此实现对任何输入都只返回 {@code null}。
	 */
	@Override
	@Nullable
	public Object extractSource(Object sourceCandidate, @Nullable Resource definitionResource) {
		return null;
	}

}
