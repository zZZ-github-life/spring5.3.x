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

package org.springframework.oxm;

import java.io.IOException;

import javax.xml.transform.Source;

/**
 * 定义对象 XML 映射（Object XML Mapping）unmarshaller 的契约。此接口的实现类
 * 可以将给定 XML 流反序列化为对象图。
 *
 * @author Arjen Poutsma
 * @since 3.0
 * @see Marshaller
 */
public interface Unmarshaller {

	/**
	 * 指示此 unmarshaller 是否能够 unmarshal 所提供类型的实例。
	 * @param clazz 询问此 unmarshaller 是否能编组的类
	 * @return 如果此 unmarshaller 确实能解组为所提供的类，则返回 {@code true}；否则返回 {@code false}
	 */
	boolean supports(Class<?> clazz);

	/**
	 * 将给定的 {@link Source} unmarshal 为对象图。
	 * @param source 要从中解组的源
	 * @return 对象图
	 * @throws IOException 如果发生 I/O 错误
	 * @throws XmlMappingException 如果给定源无法映射到对象
	 */
	Object unmarshal(Source source) throws IOException, XmlMappingException;

}
