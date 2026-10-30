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

package org.springframework.oxm.mime;

import javax.activation.DataHandler;

import org.springframework.lang.Nullable;

/**
 * 表示 MIME 附件的容器。
 * 具体实现可以适配 SOAPMessage 或电子邮件消息。
 *
 * @author Arjen Poutsma
 * @since 3.0
 * @see <a href="https://www.w3.org/TR/2005/REC-xop10-20050125/">XML-binary Optimized Packaging</a>
 */
public interface MimeContainer {

	/**
	 * 指示此容器是否为 XOP 包。
	 * @return 当满足 <a href="https://www.w3.org/TR/2005/REC-xop10-20050125/#identifying_xop_documents">
	 * Identifying XOP Documents</a> 中指定的约束时返回 {@code true}
	 * @see <a href="https://www.w3.org/TR/2005/REC-xop10-20050125/#xop_packages">XOP Packages</a>
	 */
	boolean isXopPackage();

	/**
	 * 将此消息转换为 XOP 包。
	 * @return 当消息实际上是 XOP 包时返回 {@code true}
	 * @see <a href="https://www.w3.org/TR/2005/REC-xop10-20050125/#xop_packages">XOP Packages</a>
	 */
	boolean convertToXopPackage();

	/**
	 * 将给定的数据处理器作为附件添加到此容器中。
	 * @param contentId 附件的 content id
	 * @param dataHandler 包含附件数据的 data handler
	 */
	void addAttachment(String contentId, DataHandler dataHandler);

	/**
	 * 返回具有给定内容 ID 的附件，如果未找到则返回 {@code null}。
	 * @param contentId 内容的 id
	 * @return 附件，作为 data handler
	 */
	@Nullable
	DataHandler getAttachment(String contentId);

}
