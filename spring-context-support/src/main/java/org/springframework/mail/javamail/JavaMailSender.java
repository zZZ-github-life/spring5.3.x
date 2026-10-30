/*
 * Copyright 2002-2014 the original author or authors.
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

import java.io.InputStream;

import javax.mail.internet.MimeMessage;

import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;

/**
 * 扩展的 {@link org.springframework.mail.MailSender} 接口，用于 JavaMail，
 * 支持 MIME 消息作为直接参数和通过准备回调。通常与 {@link MimeMessageHelper}
 * 类配合使用，以便便捷地创建 JavaMail {@link MimeMessage MimeMessages}，
 * 包括附件等。
 *
 * <p>如果客户端需要 {@link org.springframework.mail.SimpleMailMessage} 之外的
 * 邮件功能，应通过此接口与邮件发送器交互。生产实现为 {@link JavaMailSenderImpl}；
 * 测试时可以基于此接口创建模拟对象。客户端通常通过依赖注入接收
 * JavaMailSender 引用。
 *
 * <p>推荐使用此接口的方式是使用 {@link MimeMessagePreparator} 机制，
 * 可能配合 {@link MimeMessageHelper} 来填充消息。
 * 请参阅 {@link MimeMessageHelper MimeMessageHelper 的 javadoc} 中的示例。
 *
 * <p>整个 JavaMail {@link javax.mail.Session} 的管理由 JavaMailSender 抽象。
 * 客户端代码不应以任何方式处理 Session，而应将整个 JavaMail 配置和资源处理
 * 留给 JavaMailSender 实现。这也提高了可测试性。
 *
 * <p>JavaMailSender 客户端不像普通的 {@link org.springframework.mail.MailSender}
 * 客户端那样容易测试，但与传统 JavaMail 代码相比仍然简单直接：只需让
 * {@link #createMimeMessage()} 返回通过 {@code Session.getInstance(new Properties())}
 * 调用创建的普通 {@link MimeMessage}，并在各种 {@code send} 方法的模拟实现中
 * 检查传入的消息即可。
 *
 * @author Juergen Hoeller
 * @since 07.10.2003
 * @see javax.mail.internet.MimeMessage
 * @see javax.mail.Session
 * @see JavaMailSenderImpl
 * @see MimeMessagePreparator
 * @see MimeMessageHelper
 */
public interface JavaMailSender extends MailSender {

	/**
	 * 为此发送器的底层 JavaMail Session 创建一个新的 JavaMail MimeMessage。
	 * 需要调用此方法来创建可由客户端准备并传递给 send(MimeMessage) 的
	 * MimeMessage 实例。
	 * @return 新的 MimeMessage 实例
	 * @see #send(MimeMessage)
	 * @see #send(MimeMessage[])
	 */
	MimeMessage createMimeMessage();

	/**
	 * 为此发送器的底层 JavaMail Session 创建一个新的 JavaMail MimeMessage，
	 * 使用给定的输入流作为消息源。
	 * @param contentStream 消息的原始 MIME 输入流
	 * @return 新的 MimeMessage 实例
	 * @throws org.springframework.mail.MailParseException 消息创建失败时
	*/
	MimeMessage createMimeMessage(InputStream contentStream) throws MailException;

	/**
	 * 发送给定的 JavaMail MIME 消息。
	 * 该消息需要通过 {@link #createMimeMessage()} 创建。
	 * @param mimeMessage 要发送的消息
	 * @throws org.springframework.mail.MailAuthenticationException 认证失败时
	 * @throws org.springframework.mail.MailSendException 发送消息失败时
	 * @see #createMimeMessage
	 */
	void send(MimeMessage mimeMessage) throws MailException;

	/**
	 * 批量发送给定的 JavaMail MIME 消息数组。
	 * 这些消息需要通过 {@link #createMimeMessage()} 创建。
	 * @param mimeMessages 要发送的消息
	 * @throws org.springframework.mail.MailAuthenticationException 认证失败时
	 * @throws org.springframework.mail.MailSendException 发送消息失败时
	 * @see #createMimeMessage
	 */
	void send(MimeMessage... mimeMessages) throws MailException;

	/**
	 * 发送由给定 MimeMessagePreparator 准备的 JavaMail MIME 消息。
	 * <p>替代 {@link #createMimeMessage()} 和 {@link #send(MimeMessage)} 调用
	 * 的另一种方式来准备 MimeMessage 实例。负责进行正确的异常转换。
	 * @param mimeMessagePreparator 要使用的准备器
	 * @throws org.springframework.mail.MailPreparationException 准备消息失败时
	 * @throws org.springframework.mail.MailParseException 解析消息失败时
	 * @throws org.springframework.mail.MailAuthenticationException 认证失败时
	 * @throws org.springframework.mail.MailSendException 发送消息失败时
	 */
	void send(MimeMessagePreparator mimeMessagePreparator) throws MailException;

	/**
	 * 发送由给定 MimeMessagePreparators 准备的 JavaMail MIME 消息。
	 * <p>替代 {@link #createMimeMessage()} 和 {@link #send(MimeMessage[])} 调用
	 * 的另一种方式来准备 MimeMessage 实例。负责进行正确的异常转换。
	 * @param mimeMessagePreparators 要使用的准备器
	 * @throws org.springframework.mail.MailPreparationException 准备消息失败时
	 * @throws org.springframework.mail.MailParseException 解析消息失败时
	 * @throws org.springframework.mail.MailAuthenticationException 认证失败时
	 * @throws org.springframework.mail.MailSendException 发送消息失败时
	 */
	void send(MimeMessagePreparator... mimeMessagePreparators) throws MailException;

}
