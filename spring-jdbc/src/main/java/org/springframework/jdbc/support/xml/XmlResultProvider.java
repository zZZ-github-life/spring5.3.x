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

package org.springframework.jdbc.support.xml;

import javax.xml.transform.Result;

/**
 * 定义提供 XML 输入所需 {@code Result} 数据相关处理的接口。
 *
 * @author Thomas Risberg
 * @since 2.5.5
 * @see javax.xml.transform.Result
 */
public interface XmlResultProvider {

	/**
	 * 实现类必须实现此方法，以为 {@code Result} 提供 XML 内容。
	 * 具体实现因所使用的 {@code Result} 实现而异。
	 * @param result the {@code Result} object being used to provide the XML input
	 */
	void provideXml(Result result);

}
