/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.oxm.mime;

import java.io.IOException;

import javax.xml.transform.Source;

import org.springframework.lang.Nullable;
import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.XmlMappingException;

/**
 * {@link org.springframework.oxm.Unmarshaller} 的子接口，可利用 MIME 附件
 * 来优化二进制数据的存储。附件可以以 MTOM、XOP 或 SwA 的形式添加。
 *
 * @author Arjen Poutsma
 * @since 3.0
 * @see <a href="https://www.w3.org/TR/2004/WD-soap12-mtom-20040608/">SOAP Message Transmission Optimization Mechanism</a>
 * @see <a href="https://www.w3.org/TR/2005/REC-xop10-20050125/">XML-binary Optimized Packaging</a>
 */
public interface MimeUnmarshaller extends Unmarshaller {

	/**
	 * 将所提供的 {@link Source} unmarshal 为对象图，
	 * 并从 {@link MimeContainer} 中读取二进制附件。
	 * @param source the source to marshal from
	 * @param mimeContainer the MIME container to read extracted binary content from
	 * @return the object graph
	 * @throws XmlMappingException if the given source cannot be mapped to an object
	 * @throws IOException if an I/O Exception occurs
	 */
	Object unmarshal(Source source, @Nullable MimeContainer mimeContainer) throws XmlMappingException, IOException;

}
