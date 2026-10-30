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

import javax.xml.transform.Result;

/**
 * 定义对象 XML 映射（Object XML Mapping）marshaller 的契约。此接口的实现类
 * 可以将给定对象序列化为 XML 流。
 *
 * <p>尽管 {@code marshal} 方法以 {@code java.lang.Object} 作为第一个参数，
 * 但大多数 {@code Marshaller} 实现无法处理任意 {@code Object}。相反，必须将
 * 对象类注册到 marshaller，或者让其具有共同的基类。
 *
 * @author Arjen Poutsma
 * @since 3.0
 * @see Unmarshaller
 */
public interface Marshaller {

	/**
	 * 指示此 marshaller 是否能够 marshal 所提供类型的实例。
	 * @param clazz 询问此 marshaller 是否能编组的类
	 * @return 如果此 marshaller 确实能编组所提供类的实例，则返回 {@code true}；否则返回 {@code false}
	 */
	boolean supports(Class<?> clazz);

	/**
	 * 将给定根对象的对象图 marshal 到所提供的 {@link Result} 中。
	 * @param graph 要编组的对象图的根
	 * @param result 要编组到的结果
	 * @throws IOException 如果发生 I/O 错误
	 * @throws XmlMappingException 如果给定对象无法编组到结果中
	 */
	void marshal(Object graph, Result result) throws IOException, XmlMappingException;

}
