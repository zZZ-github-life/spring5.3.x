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

package org.springframework.jms.listener.adapter;

import java.lang.reflect.InvocationTargetException;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Session;

import org.springframework.jms.listener.SessionAwareMessageListener;
import org.springframework.jms.listener.SubscriptionNameProvider;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.SimpleMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MethodInvoker;
import org.springframework.util.ObjectUtils;

/**
 * 消息监听器适配器，通过反射将消息处理委托给目标监听器方法，
 * 并支持灵活的消息类型转换。使监听器方法可以完全独立于 JMS API，
 * 直接在消息内容类型上操作。
 *
 * <p>默认情况下，传入的 JMS 消息内容会先被提取出来，再传给目标监听器方法，
 * 以便目标方法处理 String 或字节数组等消息内容类型，而不是原始的 {@link Message}。
 * 消息类型转换委托给 Spring JMS {@link MessageConverter}。默认将使用
 * {@link SimpleMessageConverter}。（如果不想进行这种自动消息转换，
 * 请务必将 {@link #setMessageConverter MessageConverter} 设置为 {@code null}。）
 *
 * <p>如果目标监听器方法返回非 null 对象（通常是 {@code String} 或字节数组
 * 等消息内容类型），该对象将被包装到 JMS {@code Message} 中，并发送到
 * 响应目的地（JMS 的 "reply-to" 目的地，或通过
 * {@link #setDefaultResponseDestination(javax.jms.Destination) 指定的默认目的地}）。
 *
 * <p><b>注意：</b>只有在使用 {@link SessionAwareMessageListener} 入口点
 * （通常通过 Spring 消息监听器容器）时，才能发送响应消息。
 * 作为标准 JMS {@link MessageListener} 使用时<i>不</i>支持生成响应消息。
 *
 * <p>下面列出一些符合此适配器类的方法签名示例。第一个示例处理所有
 * {@code Message} 类型，并将每种 {@code Message} 类型的内容作为参数传入。
 * 由于所有这些方法都返回 {@code void}，因此不会发回任何 {@code Message}。
 *
 * <pre class="code">public interface MessageContentsDelegate {
 *    void handleMessage(String text);
 *    void handleMessage(Map map);
 *    void handleMessage(byte[] bytes);
 *    void handleMessage(Serializable obj);
 * }</pre>
 *
 * 下一个示例处理所有 {@code Message} 类型，并将实际的（原始）
 * {@code Message} 作为参数传入。同样，由于所有这些方法都返回
 * {@code void}，不会发回任何 {@code Message}。
 *
 * <pre class="code">public interface RawMessageDelegate {
 *    void handleMessage(TextMessage message);
 *    void handleMessage(MapMessage message);
 *    void handleMessage(BytesMessage message);
 *    void handleMessage(ObjectMessage message);
 * }</pre>
 *
 * 下一个示例展示了一个 {@code Message} 委托，它只消费
 * {@link javax.jms.TextMessage TextMessages} 的 {@code String} 内容。
 * 同时请注意，{@code Message} 处理方法名与
 * {@link #ORIGINAL_DEFAULT_LISTENER_METHOD 原始}方法名不同
 * （这需要在相应的 Bean 定义中配置）。同样，由于方法返回 {@code void}，
 * 不会发回任何 {@code Message}。
 *
 * <pre class="code">public interface TextMessageContentDelegate {
 *    void onMessage(String text);
 * }</pre>
 *
 * 最后一个示例展示了一个 {@code Message} 委托，它只消费
 * {@link javax.jms.TextMessage TextMessages} 的 {@code String} 内容。
 * 注意此方法的返回类型是 {@code String}：这将使配置的
 * {@link MessageListenerAdapter} 在响应中发送一条 {@link javax.jms.TextMessage}。
 *
 * <pre class="code">public interface ResponsiveTextMessageContentDelegate {
 *    String handleMessage(String text);
 * }</pre>
 *
 * 如需更多示例和讨论，请参阅 Spring 参考文档，
 * 其中详细描述了此类（及其相关 XML 配置）。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see #setDelegate
 * @see #setDefaultListenerMethod
 * @see #setDefaultResponseDestination
 * @see #setMessageConverter
 * @see org.springframework.jms.support.converter.SimpleMessageConverter
 * @see org.springframework.jms.listener.SessionAwareMessageListener
 * @see org.springframework.jms.listener.AbstractMessageListenerContainer#setMessageListener
 */
public class MessageListenerAdapter extends AbstractAdaptableMessageListener implements SubscriptionNameProvider {

	/**
	 * 默认监听器方法的开箱即用值："handleMessage"。
	 */
	public static final String ORIGINAL_DEFAULT_LISTENER_METHOD = "handleMessage";


	private Object delegate;

	private String defaultListenerMethod = ORIGINAL_DEFAULT_LISTENER_METHOD;


	/**
	 * 使用默认设置创建一个新的 {@link MessageListenerAdapter}。
	 */
	public MessageListenerAdapter() {
		this.delegate = this;
	}

	/**
	 * 为给定的委托对象创建一个新的 {@link MessageListenerAdapter}。
	 * @param delegate the delegate object
	 */
	public MessageListenerAdapter(Object delegate) {
		Assert.notNull(delegate, "Delegate must not be null");
		this.delegate = delegate;
	}


	/**
	 * 设置要委托消息监听的目标对象。
	 * 指定的监听器方法必须存在于该目标对象上。
	 * <p>如果没有指定显式的委托对象，则监听器方法应存在于
	 * 此适配器实例上，即在定义监听器方法的此适配器自定义子类上。
	 */
	public void setDelegate(Object delegate) {
		Assert.notNull(delegate, "Delegate must not be null");
		this.delegate = delegate;
	}

	/**
	 * 返回要委托消息监听的目标对象。
	 */
	protected Object getDelegate() {
		return this.delegate;
	}

	/**
	 * 指定要委托的默认监听器方法的名称，
	 * 用于未确定具体监听器方法的情况。
	 * 开箱即用值为 {@link #ORIGINAL_DEFAULT_LISTENER_METHOD "handleMessage"}。
	 * @see #getListenerMethodName
	 */
	public void setDefaultListenerMethod(String defaultListenerMethod) {
		this.defaultListenerMethod = defaultListenerMethod;
	}

	/**
	 * 返回要委托的默认监听器方法的名称。
	 */
	protected String getDefaultListenerMethod() {
		return this.defaultListenerMethod;
	}


	/**
	 * Spring {@link SessionAwareMessageListener} 入口点。
	 * <p>将消息委托给目标监听器方法，并对消息参数进行适当的转换。
	 * 如果目标方法返回非 null 对象，则将其包装为 JMS 消息并发送回去。
	 * @param message the incoming JMS message
	 * @param session the JMS session to operate on
	 * @throws JMSException if thrown by JMS API methods
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void onMessage(Message message, @Nullable Session session) throws JMSException {
		// Check whether the delegate is a MessageListener impl itself.
		// In that case, the adapter will simply act as a pass-through.
		Object delegate = getDelegate();
		if (delegate != this) {
			if (delegate instanceof SessionAwareMessageListener) {
				Assert.state(session != null, "Session is required for SessionAwareMessageListener");
				((SessionAwareMessageListener<Message>) delegate).onMessage(message, session);
				return;
			}
			if (delegate instanceof MessageListener) {
				((MessageListener) delegate).onMessage(message);
				return;
			}
		}

		// Regular case: find a handler method reflectively.
		Object convertedMessage = extractMessage(message);
		String methodName = getListenerMethodName(message, convertedMessage);

		// Invoke the handler method with appropriate arguments.
		Object[] listenerArguments = buildListenerArguments(convertedMessage);
		Object result = invokeListenerMethod(methodName, listenerArguments);
		if (result != null) {
			handleResult(result, message, session);
		}
		else {
			logger.trace("No result object given - no result to handle");
		}
	}

	@Override
	public String getSubscriptionName() {
		Object delegate = getDelegate();
		if (delegate != this && delegate instanceof SubscriptionNameProvider) {
			return ((SubscriptionNameProvider) delegate).getSubscriptionName();
		}
		else {
			return delegate.getClass().getName();
		}
	}

	/**
	 * 确定应处理给定消息的监听器方法的名称。
	 * <p>默认实现只是返回已配置的默认监听器方法（如果有）。
	 * @param originalMessage the JMS request message
	 * @param extractedMessage the converted JMS request message,
	 * to be passed into the listener method as argument
	 * @return the name of the listener method (never {@code null})
	 * @throws JMSException if thrown by JMS API methods
	 * @see #setDefaultListenerMethod
	 */
	protected String getListenerMethodName(Message originalMessage, Object extractedMessage) throws JMSException {
		return getDefaultListenerMethod();
	}

	/**
	 * 构建要传入目标监听器方法的参数数组。
	 * 允许从单个消息对象构建多个方法参数。
	 * <p>默认实现构建一个仅包含给定消息对象的数组。
	 * 这意味着提取出的消息将始终传入<i>单个</i>方法参数，
	 * 即使它是数组也是如此，目标方法需声明一个与该数组类型对应的单一参数。
	 * <p>可以覆盖此方法以区别对待数组等特殊消息内容，
	 * 例如将消息数组的每个元素作为独立的方法参数传入。
	 * @param extractedMessage the content of the message
	 * @return the array of arguments to be passed into the
	 * listener method (each element of the array corresponding
	 * to a distinct method argument)
	 */
	protected Object[] buildListenerArguments(Object extractedMessage) {
		return new Object[] {extractedMessage};
	}

	/**
	 * 调用指定的监听器方法。
	 * @param methodName the name of the listener method
	 * @param arguments the message arguments to be passed in
	 * @return the result returned from the listener method
	 * @throws JMSException if thrown by JMS API methods
	 * @see #getListenerMethodName
	 * @see #buildListenerArguments
	 */
	@Nullable
	protected Object invokeListenerMethod(String methodName, Object[] arguments) throws JMSException {
		try {
			MethodInvoker methodInvoker = new MethodInvoker();
			methodInvoker.setTargetObject(getDelegate());
			methodInvoker.setTargetMethod(methodName);
			methodInvoker.setArguments(arguments);
			methodInvoker.prepare();
			return methodInvoker.invoke();
		}
		catch (InvocationTargetException ex) {
			Throwable targetEx = ex.getTargetException();
			if (targetEx instanceof JMSException) {
				throw (JMSException) targetEx;
			}
			else {
				throw new ListenerExecutionFailedException(
						"Listener method '" + methodName + "' threw exception", targetEx);
			}
		}
		catch (Throwable ex) {
			throw new ListenerExecutionFailedException("Failed to invoke target method '" + methodName +
					"' with arguments " + ObjectUtils.nullSafeToString(arguments), ex);
		}
	}

}
