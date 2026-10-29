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

package org.springframework.context;

import org.springframework.lang.Nullable;

/**
 * MessageSource 的子接口，由能够以层次结构解析消息的对象实现。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public interface HierarchicalMessageSource extends MessageSource {

	/**
	 * 设置父级，当此对象无法解析消息时，将尝试用该父级来解析。
	 * @param parent the parent MessageSource that will be used to
	 * resolve messages that this object can't resolve.
	 * May be {@code null}, in which case no further resolution is possible.
	 */
	void setParentMessageSource(@Nullable MessageSource parent);

	/**
	 * 返回此 MessageSource 的父级；如果没有，则返回 {@code null}。
	 */
	@Nullable
	MessageSource getParentMessageSource();

}
