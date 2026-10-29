/*
 * Copyright 2002-2020 the original author or authors.
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Date;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.activation.FileTypeMap;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimePart;
import javax.mail.internet.MimeUtility;

import org.springframework.core.io.InputStreamSource;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 用于填充 {@link javax.mail.internet.MimeMessage} 的帮助类。
 *
 * <p>镜像 {@link org.springframework.mail.SimpleMailMessage} 的简单设置方法，
 * 直接将值应用于底层的 MimeMessage。允许为整个消息定义字符编码，
 * 由此帮助类的所有方法自动应用。
 *
 * <p>支持 HTML 文本内容、内联元素（如图片）以及典型的邮件附件。
 * 还支持随邮件地址附带的个人名称。请注意，高级设置仍然可以直接
 * 应用于底层的 MimeMessage 对象！
 *
 * <p>通常在 {@link MimeMessagePreparator} 实现或
 * {@link JavaMailSender} 客户端代码中使用：简单地将其作为 MimeMessage 的
 * 包装器实例化，在包装器上调用设置方法，使用底层 MimeMessage 进行邮件发送。
 * 也在 {@link JavaMailSenderImpl} 内部使用。
 *
 * <p>包含内联图片和 PDF 附件的 HTML 邮件示例代码：
 *
 * <pre class="code">
 * mailSender.send(new MimeMessagePreparator() {
 *   public void prepare(MimeMessage mimeMessage) throws MessagingException {
 *     MimeMessageHelper message = new MimeMessageHelper(mimeMessage, true, "UTF-8");
 *     message.setFrom("me@mail.com");
 *     message.setTo("you@mail.com");
 *     message.setSubject("my subject");
 *     message.setText("my text &lt;img src='cid:myLogo'&gt;", true);
 *     message.addInline("myLogo", new ClassPathResource("img/mylogo.gif"));
 *     message.addAttachment("myDocument.pdf", new ClassPathResource("doc/myDocument.pdf"));
 *   }
 * });</pre>
 *
 * 考虑在此帮助类之上使用 {@link MimeMailMessage}（它实现了通用的
 * {@link org.springframework.mail.MailMessage} 接口，就像
 * {@link org.springframework.mail.SimpleMailMessage} 一样），
 * 使消息填充代码可以通过统一接口操作简单消息或 MIME 消息。
 *
 * <p><b>关于多部分邮件的警告：</b>仅包含 HTML 文本而没有内联元素或附件的
 * 简单 MIME 消息可以在几乎任何支持 HTML 渲染的邮件客户端上正常工作。
 * 然而，内联元素和附件在邮件客户端之间仍然存在重大的兼容性问题：
 * 要让内联元素和附件在 Microsoft Outlook、Lotus Notes 和 Mac Mail 之间
 * 都能正常工作几乎是不可能的。请根据需求选择特定的多部分模式：
 * MULTIPART_MODE 常量的 javadoc 中包含更详细的信息。
 *
 * @author Juergen Hoeller
 * @since 19.01.2004
 * @see #setText(String, boolean)
 * @see #setText(String, String)
 * @see #addInline(String, org.springframework.core.io.Resource)
 * @see #addAttachment(String, org.springframework.core.io.InputStreamSource)
 * @see #MULTIPART_MODE_MIXED_RELATED
 * @see #MULTIPART_MODE_RELATED
 * @see #getMimeMessage()
 * @see JavaMailSender
 */
public class MimeMessageHelper {

	/**
	 * 表示非多部分消息的常量。
	 */
	public static final int MULTIPART_MODE_NO = 0;

	/**
	 * 表示多部分消息的常量，具有单个类型为 "mixed" 的根多部分元素。
	 * 文本、内联元素和附件都将被添加到该根元素中。
	 * <p>这是 Spring 1.0 的默认行为。已知在 Outlook 上可以正常工作。
	 * 然而，其他邮件客户端倾向于将内联元素误解为附件，和/或也会
	 * 以内联方式显示附件。
	 */
	public static final int MULTIPART_MODE_MIXED = 1;

	/**
	 * 表示多部分消息的常量，具有单个类型为 "related" 的根多部分元素。
	 * 文本、内联元素和附件都将被添加到该根元素中。
	 * <p>这是 Spring 1.1 到 1.2 final 的默认行为。
	 * 这是 "Microsoft 多部分模式"，由 Outlook 原生发送。
	 * 已知在 Outlook、Outlook Express、Yahoo Mail 上可以正常工作，
	 * 在 Mac Mail 上也能在很大程度上正常工作（内联元素会额外列为附件，
	 * 尽管内联元素也已内联显示）。在 Lotus Notes 上不能正常工作
	 *（附件不会在那里显示）。
	 */
	public static final int MULTIPART_MODE_RELATED = 2;

	/**
	 * 表示多部分消息的常量，具有 "mixed" 类型的根多部分元素
	 * 加上 "related" 类型的嵌套多部分元素。文本和内联元素将被添加到
	 * 嵌套的 "related" 元素中，而附件将被添加到 "mixed" 根元素中。
	 * <p>这是自 Spring 1.2.1 以来的默认值。根据 MIME 规范，这可以说是
	 * 最正确的 MIME 结构：已知在 Outlook、Outlook Express、Yahoo Mail
	 * 和 Lotus Notes 上可以正常工作。在 Mac Mail 上不能正常工作。
	 * 如果目标是 Mac Mail或在 Outlook 上遇到特定邮件的问题，
	 * 请考虑使用 {@code MULTIPART_MODE_RELATED}。
	 */
	public static final int MULTIPART_MODE_MIXED_RELATED = 3;


	private static final String MULTIPART_SUBTYPE_MIXED = "mixed";

	private static final String MULTIPART_SUBTYPE_RELATED = "related";

	private static final String MULTIPART_SUBTYPE_ALTERNATIVE = "alternative";

	private static final String CONTENT_TYPE_ALTERNATIVE = "text/alternative";

	private static final String CONTENT_TYPE_HTML = "text/html";

	private static final String CONTENT_TYPE_CHARSET_SUFFIX = ";charset=";

	private static final String HEADER_PRIORITY = "X-Priority";


	private final MimeMessage mimeMessage;

	@Nullable
	private MimeMultipart rootMimeMultipart;

	@Nullable
	private MimeMultipart mimeMultipart;

	@Nullable
	private final String encoding;

	private FileTypeMap fileTypeMap;

	private boolean encodeFilenames = false;

	private boolean validateAddresses = false;


	/**
	 * 为给定的 MimeMessage 创建新的 MimeMessageHelper，
	 * 假设为简单文本消息（无多部分内容，
	 * 即无替代文本、无内联元素或附件）。
	 * <p>消息的字符编码将从传入的 MimeMessage 对象中获取（如果有的话）。
	 * 否则将使用 JavaMail 的默认编码。
	 * @param mimeMessage the mime message to work on
	 * @see #MimeMessageHelper(javax.mail.internet.MimeMessage, boolean)
	 * @see #getDefaultEncoding(javax.mail.internet.MimeMessage)
	 * @see JavaMailSenderImpl#setDefaultEncoding
	 */
	public MimeMessageHelper(MimeMessage mimeMessage) {
		this(mimeMessage, null);
	}

	/**
	 * 为给定的 MimeMessage 创建新的 MimeMessageHelper，
	 * 假设为简单文本消息（无多部分内容，
	 * 即无替代文本、无内联元素或附件）。
	 * @param mimeMessage the mime message to work on
	 * @param encoding the character encoding to use for the message
	 * @see #MimeMessageHelper(javax.mail.internet.MimeMessage, boolean)
	 */
	public MimeMessageHelper(MimeMessage mimeMessage, @Nullable String encoding) {
		this.mimeMessage = mimeMessage;
		this.encoding = (encoding != null ? encoding : getDefaultEncoding(mimeMessage));
		this.fileTypeMap = getDefaultFileTypeMap(mimeMessage);
	}

	/**
	 * 为给定的 MimeMessage 创建新的 MimeMessageHelper，
	 * 如果请求则以多部分模式（支持替代文本、内联元素和附件）。
	 * <p>考虑使用接受 multipartMode 参数的 MimeMessageHelper 构造方法
	 * 来选择 MULTIPART_MODE_MIXED_RELATED 之外的特定多部分模式。
	 * <p>消息的字符编码将从传入的 MimeMessage 对象中获取（如果有的话）。
	 * 否则将使用 JavaMail 的默认编码。
	 * @param mimeMessage the mime message to work on
	 * @param multipart whether to create a multipart message that
	 * supports alternative texts, inline elements and attachments
	 * (corresponds to MULTIPART_MODE_MIXED_RELATED)
	 * @throws MessagingException if multipart creation failed
	 * @see #MimeMessageHelper(javax.mail.internet.MimeMessage, int)
	 * @see #getDefaultEncoding(javax.mail.internet.MimeMessage)
	 * @see JavaMailSenderImpl#setDefaultEncoding
	 */
	public MimeMessageHelper(MimeMessage mimeMessage, boolean multipart) throws MessagingException {
		this(mimeMessage, multipart, null);
	}

	/**
	 * 为给定的 MimeMessage 创建新的 MimeMessageHelper，
	 * 如果请求则以多部分模式（支持替代文本、内联元素和附件）。
	 * <p>考虑使用接受 multipartMode 参数的 MimeMessageHelper 构造方法
	 * 来选择 MULTIPART_MODE_MIXED_RELATED 之外的特定多部分模式。
	 * @param mimeMessage the mime message to work on
	 * @param multipart whether to create a multipart message that
	 * supports alternative texts, inline elements and attachments
	 * (corresponds to MULTIPART_MODE_MIXED_RELATED)
	 * @param encoding the character encoding to use for the message
	 * @throws MessagingException if multipart creation failed
	 * @see #MimeMessageHelper(javax.mail.internet.MimeMessage, int, String)
	 */
	public MimeMessageHelper(MimeMessage mimeMessage, boolean multipart, @Nullable String encoding)
			throws MessagingException {

		this(mimeMessage, (multipart ? MULTIPART_MODE_MIXED_RELATED : MULTIPART_MODE_NO), encoding);
	}

	/**
	 * 为给定的 MimeMessage 创建新的 MimeMessageHelper，
	 * 如果请求则以多部分模式（支持替代文本、内联元素和附件）。
	 * <p>消息的字符编码将从传入的 MimeMessage 对象中获取（如果有的话）。
	 * 否则将使用 JavaMail 的默认编码。
	 * @param mimeMessage the mime message to work on
	 * @param multipartMode which kind of multipart message to create
	 * (MIXED, RELATED, MIXED_RELATED, or NO)
	 * @throws MessagingException if multipart creation failed
	 * @see #MULTIPART_MODE_NO
	 * @see #MULTIPART_MODE_MIXED
	 * @see #MULTIPART_MODE_RELATED
	 * @see #MULTIPART_MODE_MIXED_RELATED
	 * @see #getDefaultEncoding(javax.mail.internet.MimeMessage)
	 * @see JavaMailSenderImpl#setDefaultEncoding
	 */
	public MimeMessageHelper(MimeMessage mimeMessage, int multipartMode) throws MessagingException {
		this(mimeMessage, multipartMode, null);
	}

	/**
	 * 为给定的 MimeMessage 创建新的 MimeMessageHelper，
	 * 如果请求则以多部分模式（支持替代文本、内联元素和附件）。
	 * @param mimeMessage the mime message to work on
	 * @param multipartMode which kind of multipart message to create
	 * (MIXED, RELATED, MIXED_RELATED, or NO)
	 * @param encoding the character encoding to use for the message
	 * @throws MessagingException if multipart creation failed
	 * @see #MULTIPART_MODE_NO
	 * @see #MULTIPART_MODE_MIXED
	 * @see #MULTIPART_MODE_RELATED
	 * @see #MULTIPART_MODE_MIXED_RELATED
	 */
	public MimeMessageHelper(MimeMessage mimeMessage, int multipartMode, @Nullable String encoding)
			throws MessagingException {

		this.mimeMessage = mimeMessage;
		createMimeMultiparts(mimeMessage, multipartMode);
		this.encoding = (encoding != null ? encoding : getDefaultEncoding(mimeMessage));
		this.fileTypeMap = getDefaultFileTypeMap(mimeMessage);
	}


	/**
	 * 返回底层的 MimeMessage 对象。
	 */
	public final MimeMessage getMimeMessage() {
		return this.mimeMessage;
	}


	/**
	 * 确定要使用的 MimeMultipart 对象，一方面用于存储附件，
	 * 另一方面用于存储文本和内联元素。
	 * <p>文本和内联元素可以存储在根元素本身中
	 *（MULTIPART_MODE_MIXED、MULTIPART_MODE_RELATED），
	 * 或者存储在嵌套元素中而非直接存储在根元素中
	 *（MULTIPART_MODE_MIXED_RELATED）。
	 * <p>默认情况下，根 MimeMultipart 元素的类型为 "mixed"
	 *（MULTIPART_MODE_MIXED）或 "related"（MULTIPART_MODE_RELATED）。
	 * 主要的多部分元素将作为 "related" 类型的嵌套元素添加
	 *（MULTIPART_MODE_MIXED_RELATED），或者与根元素本身相同
	 *（MULTIPART_MODE_MIXED、MULTIPART_MODE_RELATED）。
	 * @param mimeMessage the MimeMessage object to add the root MimeMultipart
	 * object to
	 * @param multipartMode the multipart mode, as passed into the constructor
	 * (MIXED, RELATED, MIXED_RELATED, or NO)
	 * @throws MessagingException if multipart creation failed
	 * @see #setMimeMultiparts
	 * @see #MULTIPART_MODE_NO
	 * @see #MULTIPART_MODE_MIXED
	 * @see #MULTIPART_MODE_RELATED
	 * @see #MULTIPART_MODE_MIXED_RELATED
	 */
	protected void createMimeMultiparts(MimeMessage mimeMessage, int multipartMode) throws MessagingException {
		switch (multipartMode) {
			case MULTIPART_MODE_NO:
				setMimeMultiparts(null, null);
				break;
			case MULTIPART_MODE_MIXED:
				MimeMultipart mixedMultipart = new MimeMultipart(MULTIPART_SUBTYPE_MIXED);
				mimeMessage.setContent(mixedMultipart);
				setMimeMultiparts(mixedMultipart, mixedMultipart);
				break;
			case MULTIPART_MODE_RELATED:
				MimeMultipart relatedMultipart = new MimeMultipart(MULTIPART_SUBTYPE_RELATED);
				mimeMessage.setContent(relatedMultipart);
				setMimeMultiparts(relatedMultipart, relatedMultipart);
				break;
			case MULTIPART_MODE_MIXED_RELATED:
				MimeMultipart rootMixedMultipart = new MimeMultipart(MULTIPART_SUBTYPE_MIXED);
				mimeMessage.setContent(rootMixedMultipart);
				MimeMultipart nestedRelatedMultipart = new MimeMultipart(MULTIPART_SUBTYPE_RELATED);
				MimeBodyPart relatedBodyPart = new MimeBodyPart();
				relatedBodyPart.setContent(nestedRelatedMultipart);
				rootMixedMultipart.addBodyPart(relatedBodyPart);
				setMimeMultiparts(rootMixedMultipart, nestedRelatedMultipart);
				break;
			default:
				throw new IllegalArgumentException("Only multipart modes MIXED_RELATED, RELATED and NO supported");
		}
	}

	/**
	 * 设置供此 MimeMessageHelper 使用的给定 MimeMultipart 对象。
	 * @param root the root MimeMultipart object, which attachments will be added to;
	 * or {@code null} to indicate no multipart at all
	 * @param main the main MimeMultipart object, which text(s) and inline elements
	 * will be added to (can be the same as the root multipart object, or an element
	 * nested underneath the root multipart element)
	 */
	protected final void setMimeMultiparts(@Nullable MimeMultipart root, @Nullable MimeMultipart main) {
		this.rootMimeMultipart = root;
		this.mimeMultipart = main;
	}

	/**
	 * 返回此帮助类是否处于多部分模式，
	 * 即是否持有多部分消息。
	 * @see #MimeMessageHelper(MimeMessage, boolean)
	 */
	public final boolean isMultipart() {
		return (this.rootMimeMultipart != null);
	}

	/**
	 * 返回根 MIME "multipart/mixed" 对象（如果有）。
	 * 可用于手动添加附件。
	 * <p>在多部分邮件的情况下，这将是 MimeMessage 的直接内容。
	 * @throws IllegalStateException if this helper is not in multipart mode
	 * @see #isMultipart
	 * @see #getMimeMessage
	 * @see javax.mail.internet.MimeMultipart#addBodyPart
	 */
	public final MimeMultipart getRootMimeMultipart() throws IllegalStateException {
		if (this.rootMimeMultipart == null) {
			throw new IllegalStateException("Not in multipart mode - " +
					"create an appropriate MimeMessageHelper via a constructor that takes a 'multipart' flag " +
					"if you need to set alternative texts or add inline elements or attachments.");
		}
		return this.rootMimeMultipart;
	}

	/**
	 * 返回底层的 MIME "multipart/related" 对象（如果有）。
	 * 可用于手动添加正文部分、内联元素等。
	 * <p>在多部分邮件的情况下，这将嵌套在根 MimeMultipart 中。
	 * @throws IllegalStateException if this helper is not in multipart mode
	 * @see #isMultipart
	 * @see #getRootMimeMultipart
	 * @see javax.mail.internet.MimeMultipart#addBodyPart
	 */
	public final MimeMultipart getMimeMultipart() throws IllegalStateException {
		if (this.mimeMultipart == null) {
			throw new IllegalStateException("Not in multipart mode - " +
					"create an appropriate MimeMessageHelper via a constructor that takes a 'multipart' flag " +
					"if you need to set alternative texts or add inline elements or attachments.");
		}
		return this.mimeMultipart;
	}


	/**
	 * 确定给定 MimeMessage 的默认编码。
	 * @param mimeMessage the passed-in MimeMessage
	 * @return the default encoding associated with the MimeMessage,
	 * or {@code null} if none found
	 */
	@Nullable
	protected String getDefaultEncoding(MimeMessage mimeMessage) {
		if (mimeMessage instanceof SmartMimeMessage) {
			return ((SmartMimeMessage) mimeMessage).getDefaultEncoding();
		}
		return null;
	}

	/**
	 * 返回此消息使用的特定字符编码（如果有）。
	 */
	@Nullable
	public String getEncoding() {
		return this.encoding;
	}

	/**
	 * 确定给定 MimeMessage 的默认 Java Activation FileTypeMap。
	 * @param mimeMessage the passed-in MimeMessage
	 * @return the default FileTypeMap associated with the MimeMessage,
	 * or a default ConfigurableMimeFileTypeMap if none found for the message
	 * @see ConfigurableMimeFileTypeMap
	 */
	protected FileTypeMap getDefaultFileTypeMap(MimeMessage mimeMessage) {
		if (mimeMessage instanceof SmartMimeMessage) {
			FileTypeMap fileTypeMap = ((SmartMimeMessage) mimeMessage).getDefaultFileTypeMap();
			if (fileTypeMap != null) {
				return fileTypeMap;
			}
		}
		ConfigurableMimeFileTypeMap fileTypeMap = new ConfigurableMimeFileTypeMap();
		fileTypeMap.afterPropertiesSet();
		return fileTypeMap;
	}

	/**
	 * 设置用于确定添加到消息中的内联内容和附件的内容类型的
	 * Java Activation Framework {@code FileTypeMap}。
	 * <p>默认为底层 MimeMessage 所携带的 {@code FileTypeMap}（如果有），
	 * 否则为 Activation Framework 的默认 {@code FileTypeMap} 实例。
	 * @see #addInline
	 * @see #addAttachment
	 * @see #getDefaultFileTypeMap(javax.mail.internet.MimeMessage)
	 * @see JavaMailSenderImpl#setDefaultFileTypeMap
	 * @see javax.activation.FileTypeMap#getDefaultFileTypeMap
	 * @see ConfigurableMimeFileTypeMap
	 */
	public void setFileTypeMap(@Nullable FileTypeMap fileTypeMap) {
		this.fileTypeMap = (fileTypeMap != null ? fileTypeMap : getDefaultFileTypeMap(getMimeMessage()));
	}

	/**
	 * 返回此 MimeMessageHelper 使用的 {@code FileTypeMap}。
	 * @see #setFileTypeMap
	 */
	public FileTypeMap getFileTypeMap() {
		return this.fileTypeMap;
	}


	/**
	 * 设置是否编码传递给此帮助类的 {@code #addAttachment} 方法的附件文件名。
	 * <p>默认为 {@code false}，遵循标准 MIME 行为；将其设为 {@code true}
	 * 以兼容旧版邮件客户端。与此相关的是，请查看 JavaMail 的
	 * {@code mail.mime.encodefilename} 系统属性。
	 * <p><b>注意：</b>在 5.3 中默认值改为 {@code false}，
	 * 以支持 JavaMail 标准的 {@code mail.mime.encodefilename} 系统属性。
	 * @since 5.2.9
	 * @see #addAttachment(String, DataSource)
	 * @see MimeBodyPart#setFileName(String)
	 */
	public void setEncodeFilenames(boolean encodeFilenames) {
		this.encodeFilenames = encodeFilenames;
	}

	/**
	 * 返回是否编码传递给此帮助类的 {@code #addAttachment} 方法的附件文件名。
	 * @since 5.2.9
	 * @see #setEncodeFilenames
	 */
	public boolean isEncodeFilenames() {
		return this.encodeFilenames;
	}

	/**
	 * 设置是否验证传递给此帮助类的所有地址。
	 * <p>默认为 {@code false}。
	 * @see #validateAddress
	 */
	public void setValidateAddresses(boolean validateAddresses) {
		this.validateAddresses = validateAddresses;
	}

	/**
	 * 返回此帮助类是否将验证传递给它的的所有地址。
	 * @see #setValidateAddresses
	 */
	public boolean isValidateAddresses() {
		return this.validateAddresses;
	}

	/**
	 * 验证给定的邮件地址。
	 * 由 MimeMessageHelper 的所有地址设置方法和添加方法调用。
	 * <p>默认实现在地址验证已为此帮助类实例激活的情况下，
	 * 调用 {@link InternetAddress#validate()}。
	 * @param address the address to validate
	 * @throws AddressException if validation failed
	 * @see #isValidateAddresses()
	 * @see javax.mail.internet.InternetAddress#validate()
	 */
	protected void validateAddress(InternetAddress address) throws AddressException {
		if (isValidateAddresses()) {
			address.validate();
		}
	}

	/**
	 * 验证所有给定的邮件地址。
	 * <p>默认实现简单地委托给 {@link #validateAddress} 处理每个地址。
	 * @param addresses the addresses to validate
	 * @throws AddressException if validation failed
	 * @see #validateAddress(InternetAddress)
	 */
	protected void validateAddresses(InternetAddress[] addresses) throws AddressException {
		for (InternetAddress address : addresses) {
			validateAddress(address);
		}
	}


	public void setFrom(InternetAddress from) throws MessagingException {
		Assert.notNull(from, "From address must not be null");
		validateAddress(from);
		this.mimeMessage.setFrom(from);
	}

	public void setFrom(String from) throws MessagingException {
		Assert.notNull(from, "From address must not be null");
		setFrom(parseAddress(from));
	}

	public void setFrom(String from, String personal) throws MessagingException, UnsupportedEncodingException {
		Assert.notNull(from, "From address must not be null");
		setFrom(getEncoding() != null ?
			new InternetAddress(from, personal, getEncoding()) : new InternetAddress(from, personal));
	}

	public void setReplyTo(InternetAddress replyTo) throws MessagingException {
		Assert.notNull(replyTo, "Reply-to address must not be null");
		validateAddress(replyTo);
		this.mimeMessage.setReplyTo(new InternetAddress[] {replyTo});
	}

	public void setReplyTo(String replyTo) throws MessagingException {
		Assert.notNull(replyTo, "Reply-to address must not be null");
		setReplyTo(parseAddress(replyTo));
	}

	public void setReplyTo(String replyTo, String personal) throws MessagingException, UnsupportedEncodingException {
		Assert.notNull(replyTo, "Reply-to address must not be null");
		InternetAddress replyToAddress = (getEncoding() != null) ?
				new InternetAddress(replyTo, personal, getEncoding()) : new InternetAddress(replyTo, personal);
		setReplyTo(replyToAddress);
	}


	public void setTo(InternetAddress to) throws MessagingException {
		Assert.notNull(to, "To address must not be null");
		validateAddress(to);
		this.mimeMessage.setRecipient(Message.RecipientType.TO, to);
	}

	public void setTo(InternetAddress[] to) throws MessagingException {
		Assert.notNull(to, "To address array must not be null");
		validateAddresses(to);
		this.mimeMessage.setRecipients(Message.RecipientType.TO, to);
	}

	public void setTo(String to) throws MessagingException {
		Assert.notNull(to, "To address must not be null");
		setTo(parseAddress(to));
	}

	public void setTo(String[] to) throws MessagingException {
		Assert.notNull(to, "To address array must not be null");
		InternetAddress[] addresses = new InternetAddress[to.length];
		for (int i = 0; i < to.length; i++) {
			addresses[i] = parseAddress(to[i]);
		}
		setTo(addresses);
	}

	public void addTo(InternetAddress to) throws MessagingException {
		Assert.notNull(to, "To address must not be null");
		validateAddress(to);
		this.mimeMessage.addRecipient(Message.RecipientType.TO, to);
	}

	public void addTo(String to) throws MessagingException {
		Assert.notNull(to, "To address must not be null");
		addTo(parseAddress(to));
	}

	public void addTo(String to, String personal) throws MessagingException, UnsupportedEncodingException {
		Assert.notNull(to, "To address must not be null");
		addTo(getEncoding() != null ?
			new InternetAddress(to, personal, getEncoding()) :
			new InternetAddress(to, personal));
	}


	public void setCc(InternetAddress cc) throws MessagingException {
		Assert.notNull(cc, "Cc address must not be null");
		validateAddress(cc);
		this.mimeMessage.setRecipient(Message.RecipientType.CC, cc);
	}

	public void setCc(InternetAddress[] cc) throws MessagingException {
		Assert.notNull(cc, "Cc address array must not be null");
		validateAddresses(cc);
		this.mimeMessage.setRecipients(Message.RecipientType.CC, cc);
	}

	public void setCc(String cc) throws MessagingException {
		Assert.notNull(cc, "Cc address must not be null");
		setCc(parseAddress(cc));
	}

	public void setCc(String[] cc) throws MessagingException {
		Assert.notNull(cc, "Cc address array must not be null");
		InternetAddress[] addresses = new InternetAddress[cc.length];
		for (int i = 0; i < cc.length; i++) {
			addresses[i] = parseAddress(cc[i]);
		}
		setCc(addresses);
	}

	public void addCc(InternetAddress cc) throws MessagingException {
		Assert.notNull(cc, "Cc address must not be null");
		validateAddress(cc);
		this.mimeMessage.addRecipient(Message.RecipientType.CC, cc);
	}

	public void addCc(String cc) throws MessagingException {
		Assert.notNull(cc, "Cc address must not be null");
		addCc(parseAddress(cc));
	}

	public void addCc(String cc, String personal) throws MessagingException, UnsupportedEncodingException {
		Assert.notNull(cc, "Cc address must not be null");
		addCc(getEncoding() != null ?
			new InternetAddress(cc, personal, getEncoding()) :
			new InternetAddress(cc, personal));
	}


	public void setBcc(InternetAddress bcc) throws MessagingException {
		Assert.notNull(bcc, "Bcc address must not be null");
		validateAddress(bcc);
		this.mimeMessage.setRecipient(Message.RecipientType.BCC, bcc);
	}

	public void setBcc(InternetAddress[] bcc) throws MessagingException {
		Assert.notNull(bcc, "Bcc address array must not be null");
		validateAddresses(bcc);
		this.mimeMessage.setRecipients(Message.RecipientType.BCC, bcc);
	}

	public void setBcc(String bcc) throws MessagingException {
		Assert.notNull(bcc, "Bcc address must not be null");
		setBcc(parseAddress(bcc));
	}

	public void setBcc(String[] bcc) throws MessagingException {
		Assert.notNull(bcc, "Bcc address array must not be null");
		InternetAddress[] addresses = new InternetAddress[bcc.length];
		for (int i = 0; i < bcc.length; i++) {
			addresses[i] = parseAddress(bcc[i]);
		}
		setBcc(addresses);
	}

	public void addBcc(InternetAddress bcc) throws MessagingException {
		Assert.notNull(bcc, "Bcc address must not be null");
		validateAddress(bcc);
		this.mimeMessage.addRecipient(Message.RecipientType.BCC, bcc);
	}

	public void addBcc(String bcc) throws MessagingException {
		Assert.notNull(bcc, "Bcc address must not be null");
		addBcc(parseAddress(bcc));
	}

	public void addBcc(String bcc, String personal) throws MessagingException, UnsupportedEncodingException {
		Assert.notNull(bcc, "Bcc address must not be null");
		addBcc(getEncoding() != null ?
			new InternetAddress(bcc, personal, getEncoding()) :
			new InternetAddress(bcc, personal));
	}

	private InternetAddress parseAddress(String address) throws MessagingException {
		InternetAddress[] parsed = InternetAddress.parse(address);
		if (parsed.length != 1) {
			throw new AddressException("Illegal address", address);
		}
		InternetAddress raw = parsed[0];
		try {
			return (getEncoding() != null ?
					new InternetAddress(raw.getAddress(), raw.getPersonal(), getEncoding()) : raw);
		}
		catch (UnsupportedEncodingException ex) {
			throw new MessagingException("Failed to parse embedded personal name to correct encoding", ex);
		}
	}


	/**
	 * 设置消息的优先级（"X-Priority" 头）。
	 * @param priority the priority value;
	 * typically between 1 (highest) and 5 (lowest)
	 * @throws MessagingException in case of errors
	 */
	public void setPriority(int priority) throws MessagingException {
		this.mimeMessage.setHeader(HEADER_PRIORITY, Integer.toString(priority));
	}

	/**
	 * 设置消息的发送日期。
	 * @param sentDate the date to set (never {@code null})
	 * @throws MessagingException in case of errors
	 */
	public void setSentDate(Date sentDate) throws MessagingException {
		Assert.notNull(sentDate, "Sent date must not be null");
		this.mimeMessage.setSentDate(sentDate);
	}

	/**
	 * 使用正确的编码设置消息的主题。
	 * @param subject the subject text
	 * @throws MessagingException in case of errors
	 */
	public void setSubject(String subject) throws MessagingException {
		Assert.notNull(subject, "Subject must not be null");
		if (getEncoding() != null) {
			this.mimeMessage.setSubject(subject, getEncoding());
		}
		else {
			this.mimeMessage.setSubject(subject);
		}
	}


	/**
	 * 在非多部分模式下将给定文本直接设置为内容，
	 * 或在多部分模式下设置为默认正文部分。
	 * 始终应用默认的内容类型 "text/plain"。
	 * <p><b>注意：</b>请在 {@code setText} <i>之后</i>调用 {@link #addInline}；
	 * 否则邮件阅读器可能无法正确解析内联引用。
	 * @param text the text for the message
	 * @throws MessagingException in case of errors
	 */
	public void setText(String text) throws MessagingException {
		setText(text, false);
	}

	/**
	 * 在非多部分模式下将给定文本直接设置为内容，
	 * 或在多部分模式下设置为默认正文部分。
	 * "html" 标志决定要应用的内容类型。
	 * <p><b>注意：</b>请在 {@code setText} <i>之后</i>调用 {@link #addInline}；
	 * 否则邮件阅读器可能无法正确解析内联引用。
	 * @param text the text for the message
	 * @param html whether to apply content type "text/html" for an
	 * HTML mail, using default content type ("text/plain") else
	 * @throws MessagingException in case of errors
	 */
	public void setText(String text, boolean html) throws MessagingException {
		Assert.notNull(text, "Text must not be null");
		MimePart partToUse;
		if (isMultipart()) {
			partToUse = getMainPart();
		}
		else {
			partToUse = this.mimeMessage;
		}
		if (html) {
			setHtmlTextToMimePart(partToUse, text);
		}
		else {
			setPlainTextToMimePart(partToUse, text);
		}
	}

	/**
	 * 将给定的纯文本和 HTML 文本设置为替代内容，
	 * 向邮件客户端提供两种选项。需要多部分模式。
	 * <p><b>注意：</b>请在 {@code setText} <i>之后</i>调用 {@link #addInline}；
	 * 否则邮件阅读器可能无法正确解析内联引用。
	 * @param plainText the plain text for the message
	 * @param htmlText the HTML text for the message
	 * @throws MessagingException in case of errors
	 */
	public void setText(String plainText, String htmlText) throws MessagingException {
		Assert.notNull(plainText, "Plain text must not be null");
		Assert.notNull(htmlText, "HTML text must not be null");

		MimeMultipart messageBody = new MimeMultipart(MULTIPART_SUBTYPE_ALTERNATIVE);
		getMainPart().setContent(messageBody, CONTENT_TYPE_ALTERNATIVE);

		// Create the plain text part of the message.
		MimeBodyPart plainTextPart = new MimeBodyPart();
		setPlainTextToMimePart(plainTextPart, plainText);
		messageBody.addBodyPart(plainTextPart);

		// Create the HTML text part of the message.
		MimeBodyPart htmlTextPart = new MimeBodyPart();
		setHtmlTextToMimePart(htmlTextPart, htmlText);
		messageBody.addBodyPart(htmlTextPart);
	}

	private MimeBodyPart getMainPart() throws MessagingException {
		MimeMultipart mimeMultipart = getMimeMultipart();
		MimeBodyPart bodyPart = null;
		for (int i = 0; i < mimeMultipart.getCount(); i++) {
			BodyPart bp = mimeMultipart.getBodyPart(i);
			if (bp.getFileName() == null) {
				bodyPart = (MimeBodyPart) bp;
			}
		}
		if (bodyPart == null) {
			MimeBodyPart mimeBodyPart = new MimeBodyPart();
			mimeMultipart.addBodyPart(mimeBodyPart);
			bodyPart = mimeBodyPart;
		}
		return bodyPart;
	}

	private void setPlainTextToMimePart(MimePart mimePart, String text) throws MessagingException {
		if (getEncoding() != null) {
			mimePart.setText(text, getEncoding());
		}
		else {
			mimePart.setText(text);
		}
	}

	private void setHtmlTextToMimePart(MimePart mimePart, String text) throws MessagingException {
		if (getEncoding() != null) {
			mimePart.setContent(text, CONTENT_TYPE_HTML + CONTENT_TYPE_CHARSET_SUFFIX + getEncoding());
		}
		else {
			mimePart.setContent(text, CONTENT_TYPE_HTML);
		}
	}


	/**
	 * 向 MimeMessage 添加内联元素，内容取自
	 * {@code javax.activation.DataSource}。
	 * <p>请注意，DataSource 实现返回的 InputStream
	 * 需要是<i>每次调用时的新流</i>，因为 JavaMail 会多次调用
	 * {@code getInputStream()}。
	 * <p><b>注意：</b>请在 {@link #setText} <i>之后</i>调用 {@code addInline}；
	 * 否则邮件阅读器可能无法正确解析内联引用。
	 * @param contentId the content ID to use. Will end up as "Content-ID" header
	 * in the body part, surrounded by angle brackets: e.g. "myId" &rarr; "&lt;myId&gt;".
	 * Can be referenced in HTML source via src="cid:myId" expressions.
	 * @param dataSource the {@code javax.activation.DataSource} to take
	 * the content from, determining the InputStream and the content type
	 * @throws MessagingException in case of errors
	 * @see #addInline(String, java.io.File)
	 * @see #addInline(String, org.springframework.core.io.Resource)
	 */
	public void addInline(String contentId, DataSource dataSource) throws MessagingException {
		Assert.notNull(contentId, "Content ID must not be null");
		Assert.notNull(dataSource, "DataSource must not be null");
		MimeBodyPart mimeBodyPart = new MimeBodyPart();
		mimeBodyPart.setDisposition(MimeBodyPart.INLINE);
		mimeBodyPart.setContentID("<" + contentId + ">");
		mimeBodyPart.setDataHandler(new DataHandler(dataSource));
		getMimeMultipart().addBodyPart(mimeBodyPart);
	}

	/**
	 * 向 MimeMessage 添加内联元素，内容取自 {@code java.io.File}。
	 * <p>内容类型将根据给定内容文件的名称确定。
	 * 不要将此用于具有任意文件名的临时文件（可能以 ".tmp" 结尾等）！
	 * <p><b>注意：</b>请在 {@link #setText} <i>之后</i>调用 {@code addInline}；
	 * 否则邮件阅读器可能无法正确解析内联引用。
	 * @param contentId the content ID to use. Will end up as "Content-ID" header
	 * in the body part, surrounded by angle brackets: e.g. "myId" &rarr; "&lt;myId&gt;".
	 * Can be referenced in HTML source via src="cid:myId" expressions.
	 * @param file the File resource to take the content from
	 * @throws MessagingException in case of errors
	 * @see #setText
	 * @see #addInline(String, org.springframework.core.io.Resource)
	 * @see #addInline(String, javax.activation.DataSource)
	 */
	public void addInline(String contentId, File file) throws MessagingException {
		Assert.notNull(file, "File must not be null");
		FileDataSource dataSource = new FileDataSource(file);
		dataSource.setFileTypeMap(getFileTypeMap());
		addInline(contentId, dataSource);
	}

	/**
	 * 向 MimeMessage 添加内联元素，内容取自
	 * {@code org.springframework.core.io.Resource}。
	 * <p>内容类型将根据给定内容文件的名称确定。
	 * 不要将此用于具有任意文件名的临时文件（可能以 ".tmp" 结尾等）！
	 * <p>请注意，Resource 实现返回的 InputStream
	 * 需要是<i>每次调用时的新流</i>，因为 JavaMail 会多次调用
	 * {@code getInputStream()}。
	 * <p><b>注意：</b>请在 {@link #setText} <i>之后</i>调用 {@code addInline}；
	 * 否则邮件阅读器可能无法正确解析内联引用。
	 * @param contentId the content ID to use. Will end up as "Content-ID" header
	 * in the body part, surrounded by angle brackets: e.g. "myId" &rarr; "&lt;myId&gt;".
	 * Can be referenced in HTML source via src="cid:myId" expressions.
	 * @param resource the resource to take the content from
	 * @throws MessagingException in case of errors
	 * @see #setText
	 * @see #addInline(String, java.io.File)
	 * @see #addInline(String, javax.activation.DataSource)
	 */
	public void addInline(String contentId, Resource resource) throws MessagingException {
		Assert.notNull(resource, "Resource must not be null");
		String contentType = getFileTypeMap().getContentType(resource.getFilename());
		addInline(contentId, resource, contentType);
	}

	/**
	 * 向 MimeMessage 添加内联元素，内容取自
	 * {@code org.springframework.core.InputStreamResource}，
	 * 并显式指定内容类型。
	 * <p>可以通过 Java Activation Framework 的 FileTypeMap
	 *（例如由此帮助类持有的）确定任何给定文件名的内容类型。
	 * <p>请注意，InputStreamSource 实现返回的 InputStream
	 * 需要是<i>每次调用时的新流</i>，因为 JavaMail 会多次调用
	 * {@code getInputStream()}。
	 * <p><b>注意：</b>请在 {@code setText} <i>之后</i>调用 {@code addInline}；
	 * 否则邮件阅读器可能无法正确解析内联引用。
	 * @param contentId the content ID to use. Will end up as "Content-ID" header
	 * in the body part, surrounded by angle brackets: e.g. "myId" &rarr; "&lt;myId&gt;".
	 * Can be referenced in HTML source via src="cid:myId" expressions.
	 * @param inputStreamSource the resource to take the content from
	 * @param contentType the content type to use for the element
	 * @throws MessagingException in case of errors
	 * @see #setText
	 * @see #getFileTypeMap
	 * @see #addInline(String, org.springframework.core.io.Resource)
	 * @see #addInline(String, javax.activation.DataSource)
	 */
	public void addInline(String contentId, InputStreamSource inputStreamSource, String contentType)
			throws MessagingException {

		Assert.notNull(inputStreamSource, "InputStreamSource must not be null");
		if (inputStreamSource instanceof Resource && ((Resource) inputStreamSource).isOpen()) {
			throw new IllegalArgumentException(
					"Passed-in Resource contains an open stream: invalid argument. " +
					"JavaMail requires an InputStreamSource that creates a fresh stream for every call.");
		}
		DataSource dataSource = createDataSource(inputStreamSource, contentType, "inline");
		addInline(contentId, dataSource);
	}

	/**
	 * 向 MimeMessage 添加附件，内容取自
	 * {@code javax.activation.DataSource}。
	 * <p>请注意，DataSource 实现返回的 InputStream
	 * 需要是<i>每次调用时的新流</i>，因为 JavaMail 会多次调用
	 * {@code getInputStream()}。
	 * @param attachmentFilename the name of the attachment as it will
	 * appear in the mail (the content type will be determined by this)
	 * @param dataSource the {@code javax.activation.DataSource} to take
	 * the content from, determining the InputStream and the content type
	 * @throws MessagingException in case of errors
	 * @see #addAttachment(String, org.springframework.core.io.InputStreamSource)
	 * @see #addAttachment(String, java.io.File)
	 */
	public void addAttachment(String attachmentFilename, DataSource dataSource) throws MessagingException {
		Assert.notNull(attachmentFilename, "Attachment filename must not be null");
		Assert.notNull(dataSource, "DataSource must not be null");
		try {
			MimeBodyPart mimeBodyPart = new MimeBodyPart();
			mimeBodyPart.setDisposition(MimeBodyPart.ATTACHMENT);
			mimeBodyPart.setFileName(isEncodeFilenames() ?
					MimeUtility.encodeText(attachmentFilename) : attachmentFilename);
			mimeBodyPart.setDataHandler(new DataHandler(dataSource));
			getRootMimeMultipart().addBodyPart(mimeBodyPart);
		}
		catch (UnsupportedEncodingException ex) {
			throw new MessagingException("Failed to encode attachment filename", ex);
		}
	}

	/**
	 * 向 MimeMessage 添加附件，内容取自 {@code java.io.File}。
	 * <p>内容类型将根据给定内容文件的名称确定。
	 * 不要将此用于具有任意文件名的临时文件（可能以 ".tmp" 结尾等）！
	 * @param attachmentFilename the name of the attachment as it will
	 * appear in the mail
	 * @param file the File resource to take the content from
	 * @throws MessagingException in case of errors
	 * @see #addAttachment(String, org.springframework.core.io.InputStreamSource)
	 * @see #addAttachment(String, javax.activation.DataSource)
	 */
	public void addAttachment(String attachmentFilename, File file) throws MessagingException {
		Assert.notNull(file, "File must not be null");
		FileDataSource dataSource = new FileDataSource(file);
		dataSource.setFileTypeMap(getFileTypeMap());
		addAttachment(attachmentFilename, dataSource);
	}

	/**
	 * 向 MimeMessage 添加附件，内容取自
	 * {@code org.springframework.core.io.InputStreamResource}。
	 * <p>内容类型将根据给定的附件文件名确定。因此，任何内容源都可以，
	 * 包括具有任意文件名的临时文件。
	 * <p>请注意，InputStreamSource 实现返回的 InputStream
	 * 需要是<i>每次调用时的新流</i>，因为 JavaMail 会多次调用
	 * {@code getInputStream()}。
	 * @param attachmentFilename the name of the attachment as it will
	 * appear in the mail
	 * @param inputStreamSource the resource to take the content from
	 * (all of Spring's Resource implementations can be passed in here)
	 * @throws MessagingException in case of errors
	 * @see #addAttachment(String, java.io.File)
	 * @see #addAttachment(String, javax.activation.DataSource)
	 * @see org.springframework.core.io.Resource
	 */
	public void addAttachment(String attachmentFilename, InputStreamSource inputStreamSource)
			throws MessagingException {

		String contentType = getFileTypeMap().getContentType(attachmentFilename);
		addAttachment(attachmentFilename, inputStreamSource, contentType);
	}

	/**
	 * 向 MimeMessage 添加附件，内容取自
	 * {@code org.springframework.core.io.InputStreamResource}。
	 * <p>请注意，InputStreamSource 实现返回的 InputStream
	 * 需要是<i>每次调用时的新流</i>，因为 JavaMail 会多次调用
	 * {@code getInputStream()}。
	 * @param attachmentFilename the name of the attachment as it will
	 * appear in the mail
	 * @param inputStreamSource the resource to take the content from
	 * (all of Spring's Resource implementations can be passed in here)
	 * @param contentType the content type to use for the element
	 * @throws MessagingException in case of errors
	 * @see #addAttachment(String, java.io.File)
	 * @see #addAttachment(String, javax.activation.DataSource)
	 * @see org.springframework.core.io.Resource
	 */
	public void addAttachment(
			String attachmentFilename, InputStreamSource inputStreamSource, String contentType)
			throws MessagingException {

		Assert.notNull(inputStreamSource, "InputStreamSource must not be null");
		if (inputStreamSource instanceof Resource && ((Resource) inputStreamSource).isOpen()) {
			throw new IllegalArgumentException(
					"Passed-in Resource contains an open stream: invalid argument. " +
					"JavaMail requires an InputStreamSource that creates a fresh stream for every call.");
		}
		DataSource dataSource = createDataSource(inputStreamSource, contentType, attachmentFilename);
		addAttachment(attachmentFilename, dataSource);
	}

	/**
	 * 为给定的 InputStreamSource 创建 Activation Framework DataSource。
	 * @param inputStreamSource the InputStreamSource (typically a Spring Resource)
	 * @param contentType the content type
	 * @param name the name of the DataSource
	 * @return the Activation Framework DataSource
	 */
	protected DataSource createDataSource(
		final InputStreamSource inputStreamSource, final String contentType, final String name) {

		return new DataSource() {
			@Override
			public InputStream getInputStream() throws IOException {
				return inputStreamSource.getInputStream();
			}
			@Override
			public OutputStream getOutputStream() {
				throw new UnsupportedOperationException("Read-only javax.activation.DataSource");
			}
			@Override
			public String getContentType() {
				return contentType;
			}
			@Override
			public String getName() {
				return name;
			}
		};
	}

}
