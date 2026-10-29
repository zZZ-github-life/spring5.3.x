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

package org.springframework.mail.javamail;

import javax.activation.FileTypeMap;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.springframework.lang.Nullable;

/**
 * 标准 JavaMail {@link MimeMessage} 的特殊子类，携带在填充消息时
 * 使用的默认编码和用于解析附件类型的默认 Java Activation
 * {@link FileTypeMap}。
 *
 * <p>在指定了默认编码和/或默认 FileTypeMap 的情况下由
 * {@link JavaMailSenderImpl} 创建。被 {@link MimeMessageHelper} 自动检测，
 * 后者将使用所携带的编码和 FileTypeMap，除非被显式覆盖。
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see JavaMailSenderImpl#createMimeMessage()
 * @see MimeMessageHelper#getDefaultEncoding(javax.mail.internet.MimeMessage)
 * @see MimeMessageHelper#getDefaultFileTypeMap(javax.mail.internet.MimeMessage)
 */
class SmartMimeMessage extends MimeMessage {

	@Nullable
	private final String defaultEncoding;

	@Nullable
	private final FileTypeMap defaultFileTypeMap;


	/**
	 * 创建新的 SmartMimeMessage。
	 * @param session the JavaMail Session to create the message for
	 * @param defaultEncoding the default encoding, or {@code null} if none
	 * @param defaultFileTypeMap the default FileTypeMap, or {@code null} if none
	 */
	public SmartMimeMessage(
			Session session, @Nullable String defaultEncoding, @Nullable FileTypeMap defaultFileTypeMap) {

		super(session);
		this.defaultEncoding = defaultEncoding;
		this.defaultFileTypeMap = defaultFileTypeMap;
	}


	/**
	 * 返回此消息的默认编码，如果没有则返回 {@code null}。
	 */
	@Nullable
	public final String getDefaultEncoding() {
		return this.defaultEncoding;
	}

	/**
	 * 返回此消息的默认 FileTypeMap，如果没有则返回 {@code null}。
	 */
	@Nullable
	public final FileTypeMap getDefaultFileTypeMap() {
		return this.defaultFileTypeMap;
	}

}
