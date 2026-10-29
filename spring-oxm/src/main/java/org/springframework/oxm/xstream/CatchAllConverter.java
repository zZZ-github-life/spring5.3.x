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

package org.springframework.oxm.xstream;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * XStream {@link Converter}，支持所有类，但在 (un)marshalling 时抛出异常。
 *
 * <p>此类的主要用途是：在显式处理应受支持的领域类的转换器之外，将此转换器
 * {@linkplain com.thoughtworks.xstream.XStream#registerConverter(com.thoughtworks.xstream.converters.Converter, int) 注册}
 * 为优先级为 {@linkplain com.thoughtworks.xstream.XStream#PRIORITY_NORMAL 普通}
 * 或更高的兜底（catch-all）末位转换器。这样，优先级较低且可能存在安全漏洞的
 * 默认 XStream 转换器就不会被调用。
 *
 * <p>例如：
 * <pre class="code">
 * XStreamMarshaller unmarshaller = new XStreamMarshaller();
 * unmarshaller.getXStream().registerConverter(new MyDomainClassConverter(), XStream.PRIORITY_VERY_HIGH);
 * unmarshaller.getXStream().registerConverter(new CatchAllConverter(), XStream.PRIORITY_NORMAL);
 * MyDomainClass myObject = unmarshaller.unmarshal(source);
 * </pre>
 *
 * @author Arjen Poutsma
 * @since 3.2.5
 */
public class CatchAllConverter implements Converter {

	@Override
	@SuppressWarnings("rawtypes")
	public boolean canConvert(Class type) {
		return true;
	}

	@Override
	public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
		throw new UnsupportedOperationException("Marshalling not supported");
	}

	@Override
	public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
		throw new UnsupportedOperationException("Unmarshalling not supported");
	}

}
