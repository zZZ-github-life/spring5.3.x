/*
 * Copyright 2002-2023 the original author or authors.
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
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.activation.FileTypeMap;
import javax.mail.Address;
import javax.mail.AuthenticationFailedException;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;

import org.springframework.lang.Nullable;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.util.Assert;

/**
 * {@link JavaMailSender} 接口的生产实现，
 * 同时支持 JavaMail {@link MimeMessage MimeMessages} 和 Spring
 * {@link SimpleMailMessage SimpleMailMessages}。也可以作为普通的
 * {@link org.springframework.mail.MailSender} 实现使用。
 *
 * <p>允许将所有设置定义为 bean 属性。或者，可以指定一个预配置的
 * JavaMail {@link javax.mail.Session}，可能从应用服务器的 JNDI 环境中获取。
 *
 * <p>此对象中的非默认属性将始终覆盖 JavaMail {@code Session} 中的设置。
 * 请注意，如果在本地覆盖所有值，则设置预配置的 {@code Session} 没有额外意义。
 *
 * @author Dmitriy Kopylenko
 * @author Juergen Hoeller
 * @since 10.09.2003
 * @see javax.mail.internet.MimeMessage
 * @see javax.mail.Session
 * @see #setSession
 * @see #setJavaMailProperties
 * @see #setHost
 * @see #setPort
 * @see #setUsername
 * @see #setPassword
 */
public class JavaMailSenderImpl implements JavaMailSender {

	/** 默认协议：'smtp'。 */
	public static final String DEFAULT_PROTOCOL = "smtp";

	/** 默认端口：-1。 */
	public static final int DEFAULT_PORT = -1;

	private static final String HEADER_MESSAGE_ID = "Message-ID";


	private Properties javaMailProperties = new Properties();

	@Nullable
	private Session session;

	@Nullable
	private String protocol;

	@Nullable
	private String host;

	private int port = DEFAULT_PORT;

	@Nullable
	private String username;

	@Nullable
	private String password;

	@Nullable
	private String defaultEncoding;

	@Nullable
	private FileTypeMap defaultFileTypeMap;


	/**
	 * 创建 {@code JavaMailSenderImpl} 类的新实例。
	 * <p>使用默认的 {@link ConfigurableMimeFileTypeMap} 初始化
	 * {@link #setDefaultFileTypeMap "defaultFileTypeMap"} 属性。
	 */
	public JavaMailSenderImpl() {
		ConfigurableMimeFileTypeMap fileTypeMap = new ConfigurableMimeFileTypeMap();
		fileTypeMap.afterPropertiesSet();
		this.defaultFileTypeMap = fileTypeMap;
	}


	/**
	 * 为 {@code Session} 设置 JavaMail 属性。
	 * <p>将使用这些属性创建一个新的 {@code Session}。
	 * 使用此方法或 {@link #setSession}，但不要同时使用。
	 * <p>此实例中的非默认属性将覆盖给定的 JavaMail 属性。
	 */
	public void setJavaMailProperties(Properties javaMailProperties) {
		this.javaMailProperties = javaMailProperties;
		synchronized (this) {
			this.session = null;
		}
	}

	/**
	 * 允许以 {@code Map} 方式访问此发送器的 JavaMail 属性，
	 * 可以添加或覆盖特定条目。
	 * <p>适用于直接指定条目，例如通过
	 * {@code javaMailProperties[mail.smtp.auth]}。
	 */
	public Properties getJavaMailProperties() {
		return this.javaMailProperties;
	}

	/**
	 * 设置 JavaMail {@code Session}，可能从 JNDI 中获取。
	 * <p>默认创建一个新的不带默认值的 {@code Session}，
	 * 完全通过此实例的属性进行配置。
	 * <p>如果使用预配置的 {@code Session}，此实例中的非默认属性
	 * 将覆盖 {@code Session} 中的设置。
	 * @see #setJavaMailProperties
	 */
	public synchronized void setSession(Session session) {
		Assert.notNull(session, "Session must not be null");
		this.session = session;
	}

	/**
	 * 返回 JavaMail {@code Session}，
	 * 如果尚未显式指定则进行延迟初始化。
	 */
	public synchronized Session getSession() {
		if (this.session == null) {
			this.session = Session.getInstance(this.javaMailProperties);
		}
		return this.session;
	}

	/**
	 * 设置邮件协议。默认为 "smtp"。
	 */
	public void setProtocol(@Nullable String protocol) {
		this.protocol = protocol;
	}

	/**
	 * 返回邮件协议。
	 */
	@Nullable
	public String getProtocol() {
		return this.protocol;
	}

	/**
	 * 设置邮件服务器主机，通常为 SMTP 主机。
	 * <p>默认为底层 JavaMail Session 的默认主机。
	 */
	public void setHost(@Nullable String host) {
		this.host = host;
	}

	/**
	 * 返回邮件服务器主机。
	 */
	@Nullable
	public String getHost() {
		return this.host;
	}

	/**
	 * 设置邮件服务器端口。
	 * <p>默认为 {@link #DEFAULT_PORT}，让 JavaMail 使用默认的
	 * SMTP 端口（25）。
	*/
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * 返回邮件服务器端口。
	 */
	public int getPort() {
		return this.port;
	}

	/**
	 * 设置邮件主机上账户的用户名（如果有）。
	 * <p>请注意，底层的 JavaMail {@code Session} 需要配置属性
	 * {@code "mail.smtp.auth"} 设置为 {@code true}，否则指定的用户名
	 * 不会被 JavaMail 运行时发送到邮件服务器。如果没有显式传入
	 * 要使用的 {@code Session}，只需通过
	 * {@link #setJavaMailProperties} 指定此设置。
	 * @see #setSession
	 * @see #setPassword
	 */
	public void setUsername(@Nullable String username) {
		this.username = username;
	}

	/**
	 * 返回邮件主机上账户的用户名。
	 */
	@Nullable
	public String getUsername() {
		return this.username;
	}

	/**
	 * 设置邮件主机上账户的密码（如果有）。
	 * <p>请注意，底层的 JavaMail {@code Session} 需要配置属性
	 * {@code "mail.smtp.auth"} 设置为 {@code true}，否则指定的密码
	 * 不会被 JavaMail 运行时发送到邮件服务器。如果没有显式传入
	 * 要使用的 {@code Session}，只需通过
	 * {@link #setJavaMailProperties} 指定此设置。
	 * @see #setSession
	 * @see #setUsername
	 */
	public void setPassword(@Nullable String password) {
		this.password = password;
	}

	/**
	 * 返回邮件主机上账户的密码。
	 */
	@Nullable
	public String getPassword() {
		return this.password;
	}

	/**
	 * 设置由此实例创建的 {@link MimeMessage MimeMessages} 使用的默认编码。
	 * <p>此类编码将被 {@link MimeMessageHelper} 自动检测。
	 */
	public void setDefaultEncoding(@Nullable String defaultEncoding) {
		this.defaultEncoding = defaultEncoding;
	}

	/**
	 * 返回 {@link MimeMessage MimeMessages} 的默认编码，
	 * 如果没有则返回 {@code null}。
	 */
	@Nullable
	public String getDefaultEncoding() {
		return this.defaultEncoding;
	}

	/**
	 * 设置由此实例创建的 {@link MimeMessage MimeMessages} 使用的
	 * 默认 Java Activation {@link FileTypeMap}。
	 * <p>此处指定的 {@code FileTypeMap} 将被 {@link MimeMessageHelper}
	 * 自动检测，避免需要为每个 {@code MimeMessageHelper} 实例指定
	 * {@code FileTypeMap}。
	 * <p>例如，可以在此指定 Spring 的 {@link ConfigurableMimeFileTypeMap}
	 * 的自定义实例。如果未显式指定，将使用默认的
	 * {@code ConfigurableMimeFileTypeMap}，其中包含扩展的 MIME 类型映射集合
	 *（由 Spring jar 中包含的 {@code mime.types} 文件定义）。
	 * @see MimeMessageHelper#setFileTypeMap
	 */
	public void setDefaultFileTypeMap(@Nullable FileTypeMap defaultFileTypeMap) {
		this.defaultFileTypeMap = defaultFileTypeMap;
	}

	/**
	 * 返回 {@link MimeMessage MimeMessages} 的默认 Java Activation
	 * {@link FileTypeMap}，如果没有则返回 {@code null}。
	 */
	@Nullable
	public FileTypeMap getDefaultFileTypeMap() {
		return this.defaultFileTypeMap;
	}


	//---------------------------------------------------------------------
	// Implementation of MailSender
	//---------------------------------------------------------------------

	@Override
	public void send(SimpleMailMessage simpleMessage) throws MailException {
		send(new SimpleMailMessage[] {simpleMessage});
	}

	@Override
	public void send(SimpleMailMessage... simpleMessages) throws MailException {
		List<MimeMessage> mimeMessages = new ArrayList<>(simpleMessages.length);
		for (SimpleMailMessage simpleMessage : simpleMessages) {
			MimeMailMessage message = new MimeMailMessage(createMimeMessage());
			simpleMessage.copyTo(message);
			mimeMessages.add(message.getMimeMessage());
		}
		doSend(mimeMessages.toArray(new MimeMessage[0]), simpleMessages);
	}


	//---------------------------------------------------------------------
	// Implementation of JavaMailSender
	//---------------------------------------------------------------------

	/**
	 * 此实现创建一个 SmartMimeMessage，持有指定的默认编码和默认 FileTypeMap。
	 * 这个特殊的携带默认值的消息将被 {@link MimeMessageHelper} 自动检测，
	 * 后者将使用所携带的编码和 FileTypeMap，除非被显式覆盖。
	 * @see #setDefaultEncoding
	 * @see #setDefaultFileTypeMap
	 */
	@Override
	public MimeMessage createMimeMessage() {
		return new SmartMimeMessage(getSession(), getDefaultEncoding(), getDefaultFileTypeMap());
	}

	@Override
	public MimeMessage createMimeMessage(InputStream contentStream) throws MailException {
		try {
			return new MimeMessage(getSession(), contentStream);
		}
		catch (Exception ex) {
			throw new MailParseException("Could not parse raw MIME content", ex);
		}
	}

	@Override
	public void send(MimeMessage mimeMessage) throws MailException {
		send(new MimeMessage[] {mimeMessage});
	}

	@Override
	public void send(MimeMessage... mimeMessages) throws MailException {
		doSend(mimeMessages, null);
	}

	@Override
	public void send(MimeMessagePreparator mimeMessagePreparator) throws MailException {
		send(new MimeMessagePreparator[] {mimeMessagePreparator});
	}

	@Override
	public void send(MimeMessagePreparator... mimeMessagePreparators) throws MailException {
		try {
			List<MimeMessage> mimeMessages = new ArrayList<>(mimeMessagePreparators.length);
			for (MimeMessagePreparator preparator : mimeMessagePreparators) {
				MimeMessage mimeMessage = createMimeMessage();
				preparator.prepare(mimeMessage);
				mimeMessages.add(mimeMessage);
			}
			send(mimeMessages.toArray(new MimeMessage[0]));
		}
		catch (MailException ex) {
			throw ex;
		}
		catch (MessagingException ex) {
			throw new MailParseException(ex);
		}
		catch (Exception ex) {
			throw new MailPreparationException(ex);
		}
	}

	/**
	 * 验证实例可以连接到为其配置的服务器。
	 * 如果连接尝试失败则抛出 {@link MessagingException}。
	 */
	public void testConnection() throws MessagingException {
		Transport transport = null;
		try {
			transport = connectTransport();
		}
		finally {
			if (transport != null) {
				transport.close();
			}
		}
	}

	/**
	 * 通过 JavaMail 实际发送给定的 MimeMessage 数组。
	 * @param mimeMessages the MimeMessage objects to send
	 * @param originalMessages corresponding original message objects
	 * that the MimeMessages have been created from (with same array
	 * length and indices as the "mimeMessages" array), if any
	 * @throws org.springframework.mail.MailAuthenticationException
	 * in case of authentication failure
	 * @throws org.springframework.mail.MailSendException
	 * in case of failure when sending a message
	 */
	protected void doSend(MimeMessage[] mimeMessages, @Nullable Object[] originalMessages) throws MailException {
		Map<Object, Exception> failedMessages = new LinkedHashMap<>();
		Transport transport = null;

		try {
			for (int i = 0; i < mimeMessages.length; i++) {

				// Check transport connection first...
				if (transport == null || !transport.isConnected()) {
					if (transport != null) {
						try {
							transport.close();
						}
						catch (Exception ex) {
							// Ignore - we're reconnecting anyway
						}
						transport = null;
					}
					try {
						transport = connectTransport();
					}
					catch (AuthenticationFailedException ex) {
						throw new MailAuthenticationException(ex);
					}
					catch (Exception ex) {
						// Effectively, all remaining messages failed...
						for (int j = i; j < mimeMessages.length; j++) {
							Object original = (originalMessages != null ? originalMessages[j] : mimeMessages[j]);
							failedMessages.put(original, ex);
						}
						throw new MailSendException("Mail server connection failed", ex, failedMessages);
					}
				}

				// Send message via current transport...
				MimeMessage mimeMessage = mimeMessages[i];
				try {
					if (mimeMessage.getSentDate() == null) {
						mimeMessage.setSentDate(new Date());
					}
					String messageId = mimeMessage.getMessageID();
					mimeMessage.saveChanges();
					if (messageId != null) {
						// Preserve explicitly specified message id...
						mimeMessage.setHeader(HEADER_MESSAGE_ID, messageId);
					}
					Address[] addresses = mimeMessage.getAllRecipients();
					transport.sendMessage(mimeMessage, (addresses != null ? addresses : new Address[0]));
				}
				catch (Exception ex) {
					Object original = (originalMessages != null ? originalMessages[i] : mimeMessage);
					failedMessages.put(original, ex);
				}
			}
		}
		finally {
			try {
				if (transport != null) {
					transport.close();
				}
			}
			catch (Exception ex) {
				if (!failedMessages.isEmpty()) {
					throw new MailSendException("Failed to close server connection after message failures", ex,
							failedMessages);
				}
				else {
					throw new MailSendException("Failed to close server connection after message sending", ex);
				}
			}
		}

		if (!failedMessages.isEmpty()) {
			throw new MailSendException(failedMessages);
		}
	}

	/**
	 * 从底层 JavaMail Session 获取并连接 Transport，
	 * 传入指定的主机、端口、用户名和密码。
	 * @return the connected Transport object
	 * @throws MessagingException if the connect attempt failed
	 * @since 4.1.2
	 * @see #getTransport
	 * @see #getHost()
	 * @see #getPort()
	 * @see #getUsername()
	 * @see #getPassword()
	 */
	protected Transport connectTransport() throws MessagingException {
		String username = getUsername();
		String password = getPassword();
		if ("".equals(username)) {  // probably from a placeholder
			username = null;
			if ("".equals(password)) {  // in conjunction with "" username, this means no password to use
				password = null;
			}
		}

		Transport transport = getTransport(getSession());
		transport.connect(getHost(), getPort(), username, password);
		return transport;
	}

	/**
	 * 从给定的 JavaMail Session 获取 Transport 对象，
	 * 使用配置的协议。
	 * <p>可以在子类中覆盖，例如返回模拟的 Transport 对象。
	 * @see javax.mail.Session#getTransport(String)
	 * @see #getSession()
	 * @see #getProtocol()
	 */
	protected Transport getTransport(Session session) throws NoSuchProviderException {
		String protocol	= getProtocol();
		if (protocol == null) {
			protocol = session.getProperty("mail.transport.protocol");
			if (protocol == null) {
				protocol = DEFAULT_PROTOCOL;
			}
		}
		return session.getTransport(protocol);
	}

}
