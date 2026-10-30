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

package org.springframework.jms.support;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.Constants;
import org.springframework.jms.JmsException;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.jms.core.JmsTemplate} 及其他 JMS 访问网关辅助类的基类，
 * 定义了诸如要操作的 JMS {@link ConnectionFactory} 等公共属性。
 * 子类 {@link org.springframework.jms.support.destination.JmsDestinationAccessor}
 * 还添加了与目的地相关的属性。
 *
 * <p>不打算直接使用。
 * 请参见 {@link org.springframework.jms.core.JmsTemplate}。
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see org.springframework.jms.support.destination.JmsDestinationAccessor
 * @see org.springframework.jms.core.JmsTemplate
 */
public abstract class JmsAccessor implements InitializingBean {

	/** {@code javax.jms.Session} 的常量实例。 */
	private static final Constants sessionConstants = new Constants(Session.class);


	/** 子类可用的 Logger。 */
	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private ConnectionFactory connectionFactory;

	private boolean sessionTransacted = false;

	private int sessionAcknowledgeMode = Session.AUTO_ACKNOWLEDGE;


	/**
	 * 设置用于获取 JMS {@link Connection Connection} 的 ConnectionFactory。
	 */
	public void setConnectionFactory(@Nullable ConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
	}

	/**
	 * 返回此访问器用于获取 JMS {@link Connection Connection} 的 ConnectionFactory。
	 */
	@Nullable
	public ConnectionFactory getConnectionFactory() {
		return this.connectionFactory;
	}

	/**
	 * 获取实际使用的 ConnectionFactory。
	 * @return ConnectionFactory（永不为 {@code null}）
	 * @throws IllegalStateException 未设置 ConnectionFactory 时
	 * @since 5.0
	 */
	protected final ConnectionFactory obtainConnectionFactory() {
		ConnectionFactory connectionFactory = getConnectionFactory();
		Assert.state(connectionFactory != null, "No ConnectionFactory set");
		return connectionFactory;
	}

	/**
	 * 设置创建 JMS {@link Session} 时使用的事务模式。默认值为 "false"。
	 * <p>请注意，在 JTA 事务中，传递给
	 * {@code create(Queue/Topic)Session(boolean transacted, int acknowledgeMode)}
	 * 方法的参数不会被考虑。根据 Java EE 事务上下文，容器会自行决定这些值。
	 * 类似地，在本地管理的事务中也不会考虑这些参数，因为在此情况下访问器
	 * 操作的是现有的 JMS Session。
	 * <p>将此标志设置为 "true" 时，在受管事务之外运行会使用一个短的本地 JMS 事务，
	 * 而在存在受管事务（非 XA 事务）的情况下则使用同步的本地 JMS 事务。
	 * 这样做的效果是让本地 JMS 事务与主事务（可能是原生 JDBC 事务）一起管理，
	 * 并在主事务之后立即提交 JMS 事务。
	 * @see javax.jms.Connection#createSession(boolean, int)
	 */
	public void setSessionTransacted(boolean sessionTransacted) {
		this.sessionTransacted = sessionTransacted;
	}

	/**
	 * 返回此访问器使用的 JMS {@link Session session} 是否应处于事务中。
	 * @see #setSessionTransacted(boolean)
	 */
	public boolean isSessionTransacted() {
		return this.sessionTransacted;
	}

	/**
	 * 通过 JMS {@link Session} 接口中对应常量的名称来设置 JMS 确认模式，
	 * 例如 "CLIENT_ACKNOWLEDGE"。
	 * <p>如果要使用供应商对确认模式的扩展，请改用 {@link #setSessionAcknowledgeMode(int)}。
	 * @param constantName {@link Session} 确认模式常量的名称
	 * @see javax.jms.Session#AUTO_ACKNOWLEDGE
	 * @see javax.jms.Session#CLIENT_ACKNOWLEDGE
	 * @see javax.jms.Session#DUPS_OK_ACKNOWLEDGE
	 * @see javax.jms.Connection#createSession(boolean, int)
	 */
	public void setSessionAcknowledgeModeName(String constantName) {
		setSessionAcknowledgeMode(sessionConstants.asNumber(constantName).intValue());
	}

	/**
	 * 设置创建 JMS {@link Session} 以发送消息时使用的 JMS 确认模式。
	 * <p>默认是 {@link Session#AUTO_ACKNOWLEDGE}。
	 * <p>也可以在此设置供应商对确认模式的扩展。
	 * <p>请注意，在 EJB 内部，{@code create(Queue/Topic)Session(boolean transacted, int acknowledgeMode)}
	 * 方法的参数不会被考虑。根据 EJB 中的事务上下文，容器会自行决定这些值。
	 * 参见 EJB 规范第 17.3.5 节。
	 * @param sessionAcknowledgeMode 确认模式常量
	 * @see javax.jms.Session#AUTO_ACKNOWLEDGE
	 * @see javax.jms.Session#CLIENT_ACKNOWLEDGE
	 * @see javax.jms.Session#DUPS_OK_ACKNOWLEDGE
	 * @see javax.jms.Connection#createSession(boolean, int)
	 */
	public void setSessionAcknowledgeMode(int sessionAcknowledgeMode) {
		this.sessionAcknowledgeMode = sessionAcknowledgeMode;
	}

	/**
	 * 返回 JMS {@link Session session} 的确认模式。
	 */
	public int getSessionAcknowledgeMode() {
		return this.sessionAcknowledgeMode;
	}

	@Override
	public void afterPropertiesSet() {
		if (getConnectionFactory() == null) {
			throw new IllegalArgumentException("Property 'connectionFactory' is required");
		}
	}


	/**
	 * 将指定的受检 {@link javax.jms.JMSException JMSException} 转换为
	 * 等价的 Spring 运行时 {@link org.springframework.jms.JmsException JmsException}。
	 * <p>默认实现委托给 {@link JmsUtils#convertJmsAccessException} 方法。
	 * @param ex 要转换的原始受检 {@link JMSException}
	 * @return 包装 {@code ex} 的 Spring 运行时 {@link JmsException}
	 * @see JmsUtils#convertJmsAccessException
	 */
	protected JmsException convertJmsAccessException(JMSException ex) {
		return JmsUtils.convertJmsAccessException(ex);
	}

	/**
	 * 通过此模板的 ConnectionFactory 创建一个 JMS Connection。
	 * <p>此实现使用 JMS 1.1 API。
	 * @return 新的 JMS Connection
	 * @throws JMSException 如果由 JMS API 方法抛出
	 * @see javax.jms.ConnectionFactory#createConnection()
	 */
	protected Connection createConnection() throws JMSException {
		ConnectionFactory cf = obtainConnectionFactory();
		Connection con = cf.createConnection();
		if (con == null) {
			throw new javax.jms.IllegalStateException(
					"ConnectionFactory returned null from createConnection(): " + cf);
		}
		return con;
	}

	/**
	 * 为给定的 Connection 创建一个 JMS Session。
	 * <p>此实现使用 JMS 1.1 API。
	 * @param con 要为其创建 Session 的 JMS Connection
	 * @return 新的 JMS Session
	 * @throws JMSException 如果由 JMS API 方法抛出
	 * @see javax.jms.Connection#createSession(boolean, int)
	 */
	protected Session createSession(Connection con) throws JMSException {
		return con.createSession(isSessionTransacted(), getSessionAcknowledgeMode());
	}

	/**
	 * 判断给定的 Session 是否处于客户端确认模式。
	 * <p>此实现使用 JMS 1.1 API。
	 * @param session 要检查的 Session
	 * @return 给定的 Session 是否处于客户端确认模式
	 * @throws javax.jms.JMSException 如果由 JMS API 方法抛出
	 * @see javax.jms.Session#getAcknowledgeMode()
	 * @see javax.jms.Session#CLIENT_ACKNOWLEDGE
	 */
	protected boolean isClientAcknowledge(Session session) throws JMSException {
		return (session.getAcknowledgeMode() == Session.CLIENT_ACKNOWLEDGE);
	}

}
