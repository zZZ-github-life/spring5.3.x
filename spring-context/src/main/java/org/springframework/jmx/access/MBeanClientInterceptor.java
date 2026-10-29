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

package org.springframework.jmx.access;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.management.Attribute;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.JMException;
import javax.management.JMX;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.OperationsException;
import javax.management.ReflectionException;
import javax.management.RuntimeErrorException;
import javax.management.RuntimeMBeanException;
import javax.management.RuntimeOperationsException;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import javax.management.remote.JMXServiceURL;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.CollectionFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.jmx.support.JmxUtils;
import org.springframework.jmx.support.ObjectNameManager;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * {@link org.aopalliance.intercept.MethodInterceptor}，将调用路由到运行于所提供的
 * {@code MBeanServerConnection} 上的 MBean。既适用于本地，也适用于远程
 * {@code MBeanServerConnection}。
 *
 * <p>默认情况下，{@code MBeanClientInterceptor} 会在启动时连接到 {@code MBeanServer} 并缓存 MBean
 * 元数据。当连接的是远程 {@code MBeanServer}，而它可能在应用启动时尚未运行时，这种行为可能并不合适。
 * 通过将 {@link #setConnectOnStartup(boolean) connectOnStartup} 属性设置为 "false"，可以将此过程
 * 推迟到首次对代理进行调用时。
 *
 * <p>此功能通常通过 {@link MBeanProxyFactoryBean} 使用。有关更多信息，请参阅该类的 javadoc。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 1.2
 * @see MBeanProxyFactoryBean
 * @see #setConnectOnStartup
 */
public class MBeanClientInterceptor
		implements MethodInterceptor, BeanClassLoaderAware, InitializingBean, DisposableBean {

	/** 可供子类使用的日志记录器。 */
	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private MBeanServerConnection server;

	@Nullable
	private JMXServiceURL serviceUrl;

	@Nullable
	private Map<String, ?> environment;

	@Nullable
	private String agentId;

	private boolean connectOnStartup = true;

	private boolean refreshOnConnectFailure = false;

	@Nullable
	private ObjectName objectName;

	private boolean useStrictCasing = true;

	@Nullable
	private Class<?> managementInterface;

	@Nullable
	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	private final ConnectorDelegate connector = new ConnectorDelegate();

	@Nullable
	private MBeanServerConnection serverToUse;

	@Nullable
	private MBeanServerInvocationHandler invocationHandler;

	private Map<String, MBeanAttributeInfo> allowedAttributes = Collections.emptyMap();

	private Map<MethodCacheKey, MBeanOperationInfo> allowedOperations = Collections.emptyMap();

	private final Map<Method, String[]> signatureCache = new HashMap<>();

	private final Object preparationMonitor = new Object();


	/**
	 * 设置用于连接到 MBean 的 {@code MBeanServerConnection}，所有调用都会被路由到该 MBean。
	 */
	public void setServer(MBeanServerConnection server) {
		this.server = server;
	}

	/**
	 * 设置远程 {@code MBeanServer} 的服务 URL。
	 */
	public void setServiceUrl(String url) throws MalformedURLException {
		this.serviceUrl = new JMXServiceURL(url);
	}

	/**
	 * 指定 JMX 连接器的环境。
	 * @see javax.management.remote.JMXConnectorFactory#connect(javax.management.remote.JMXServiceURL, java.util.Map)
	 */
	public void setEnvironment(@Nullable Map<String, ?> environment) {
		this.environment = environment;
	}

	/**
	 * 允许以 {@code Map} 方式访问要为连接器设置的环境，并可选择添加或覆盖特定条目。<p>适用于直接指定条
	 * 目，例如通过 {@code environment[myKey]}。这在添加或覆盖子 Bean 定义中的条目时特别有用。
	 */
	@Nullable
	public Map<String, ?> getEnvironment() {
		return this.environment;
	}

	/**
	 * 设置要定位的 {@code MBeanServer} 的 agent id。<p>默认无。如果指定，则会尝试定位相应的
	 * MBeanServer，除非已设置 {@link #setServiceUrl "serviceUrl"} 属性。
	 * @see javax.management.MBeanServerFactory#findMBeanServer(String)
	 * <p>Specifying the empty String indicates the platform MBeanServer.
	 */
	public void setAgentId(String agentId) {
		this.agentId = agentId;
	}

	/**
	 * 设置代理是在创建时（{@code true}）还是首次被调用时（{@code false}）连接到 {@code MBeanServer}。
	 * 默认值为 {@code true}。
	 */
	public void setConnectOnStartup(boolean connectOnStartup) {
		this.connectOnStartup = connectOnStartup;
	}

	/**
	 * 设置连接失败时是否刷新 MBeanServer 连接。默认值为 {@code false}。<p>可以开启此选项以支持 JMX 服
	 * 务器的热重启，在发生 IOException 时自动重新连接并重试。
	 */
	public void setRefreshOnConnectFailure(boolean refreshOnConnectFailure) {
		this.refreshOnConnectFailure = refreshOnConnectFailure;
	}

	/**
	 * 设置调用所路由到的 MBean 的 {@code ObjectName}，可以是 {@code ObjectName} 实例或 {@code String}。
	 */
	public void setObjectName(Object objectName) throws MalformedObjectNameException {
		this.objectName = ObjectNameManager.getInstance(objectName);
	}

	/**
	 * 设置属性是否使用严格大小写。默认启用。<p>使用严格大小写时，带有 {@code getFoo()} 之类 getter 的
	 * JavaBean 属性会转换为名为 {@code Foo} 的属性。禁用严格大小写后，{@code getFoo()} 将仅转换为
	 * {@code foo}。
	 */
	public void setUseStrictCasing(boolean useStrictCasing) {
		this.useStrictCasing = useStrictCasing;
	}

	/**
	 * 设置目标 MBean 的管理接口，为 MBean 属性暴露 Bean 属性的 setter 和 getter，并为 MBean 操作暴露常
	 * 规 Java 方法。
	 */
	public void setManagementInterface(@Nullable Class<?> managementInterface) {
		this.managementInterface = managementInterface;
	}

	/**
	 * 返回目标 MBean 的管理接口，如果未指定则返回 {@code null}。
	 */
	@Nullable
	protected final Class<?> getManagementInterface() {
		return this.managementInterface;
	}

	@Override
	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader;
	}


	/**
	 * 如果 "connectOnStartup" 已开启（默认即开启），则准备 {@code MBeanServerConnection}。
	 */
	@Override
	public void afterPropertiesSet() {
		if (this.server != null && this.refreshOnConnectFailure) {
			throw new IllegalArgumentException("'refreshOnConnectFailure' does not work when setting " +
					"a 'server' reference. Prefer 'serviceUrl' etc instead.");
		}
		if (this.connectOnStartup) {
			prepare();
		}
	}

	/**
	 * 确保已配置 {@code MBeanServerConnection}，如果未提供，则尝试检测本地连接。
	 */
	public void prepare() {
		synchronized (this.preparationMonitor) {
			if (this.server != null) {
				this.serverToUse = this.server;
			}
			else {
				this.serverToUse = null;
				this.serverToUse = this.connector.connect(this.serviceUrl, this.environment, this.agentId);
			}
			this.invocationHandler = null;
			if (this.useStrictCasing) {
				Assert.state(this.objectName != null, "No ObjectName set");
				// Use the JDK's own MBeanServerInvocationHandler, in particular for native MXBean support.
				this.invocationHandler = new MBeanServerInvocationHandler(this.serverToUse, this.objectName,
						(this.managementInterface != null && JMX.isMXBeanInterface(this.managementInterface)));
			}
			else {
				// Non-strict casing can only be achieved through custom invocation handling.
				// Only partial MXBean support available!
				retrieveMBeanInfo(this.serverToUse);
			}
		}
	}
	/**
	 * 将所配置 MBean 的管理接口信息加载到缓存中。代理在判断某个调用是否匹配受管资源管理接口上的有效操
	 * 作或属性时会使用此信息。
	 */
	private void retrieveMBeanInfo(MBeanServerConnection server) throws MBeanInfoRetrievalException {
		try {
			MBeanInfo info = server.getMBeanInfo(this.objectName);

			MBeanAttributeInfo[] attributeInfo = info.getAttributes();
			this.allowedAttributes = CollectionUtils.newHashMap(attributeInfo.length);
			for (MBeanAttributeInfo infoEle : attributeInfo) {
				this.allowedAttributes.put(infoEle.getName(), infoEle);
			}

			MBeanOperationInfo[] operationInfo = info.getOperations();
			this.allowedOperations = CollectionUtils.newHashMap(operationInfo.length);
			for (MBeanOperationInfo infoEle : operationInfo) {
				Class<?>[] paramTypes = JmxUtils.parameterInfoToTypes(infoEle.getSignature(), this.beanClassLoader);
				this.allowedOperations.put(new MethodCacheKey(infoEle.getName(), paramTypes), infoEle);
			}
		}
		catch (ClassNotFoundException ex) {
			throw new MBeanInfoRetrievalException("Unable to locate class specified in method signature", ex);
		}
		catch (IntrospectionException ex) {
			throw new MBeanInfoRetrievalException("Unable to obtain MBean info for bean [" + this.objectName + "]", ex);
		}
		catch (InstanceNotFoundException ex) {
			// if we are this far this shouldn't happen, but...
			throw new MBeanInfoRetrievalException("Unable to obtain MBean info for bean [" + this.objectName +
					"]: it is likely that this bean was unregistered during the proxy creation process",
					ex);
		}
		catch (ReflectionException ex) {
			throw new MBeanInfoRetrievalException("Unable to read MBean info for bean [ " + this.objectName + "]", ex);
		}
		catch (IOException ex) {
			throw new MBeanInfoRetrievalException("An IOException occurred when communicating with the " +
					"MBeanServer. It is likely that you are communicating with a remote MBeanServer. " +
					"Check the inner exception for exact details.", ex);
		}
	}

	/**
	 * 返回此客户端拦截器是否已准备就绪，即是否已查找服务器并缓存所有元数据。
	 */
	protected boolean isPrepared() {
		synchronized (this.preparationMonitor) {
			return (this.serverToUse != null);
		}
	}


	/**
	 * 将调用路由到所配置的受管资源。
	 * @param invocation the {@code MethodInvocation} to re-route
	 * @return the value returned as a result of the re-routed invocation
	 * @throws Throwable an invocation error propagated to the user
	 * @see #doInvoke
	 * @see #handleConnectFailure
	 */
	@Override
	@Nullable
	public Object invoke(MethodInvocation invocation) throws Throwable {
		// Lazily connect to MBeanServer if necessary.
		synchronized (this.preparationMonitor) {
			if (!isPrepared()) {
				prepare();
			}
		}
		try {
			return doInvoke(invocation);
		}
		catch (MBeanConnectFailureException | IOException ex) {
			return handleConnectFailure(invocation, ex);
		}
	}

	/**
	 * 刷新连接，并在可能的情况下重试 MBean 调用。<p>如果未配置为在连接失败时刷新，则此方法仅重新抛出原
	 * 始异常。
	 * @param invocation the invocation that failed
	 * @param ex the exception raised on remote invocation
	 * @return the result value of the new invocation, if succeeded
	 * @throws Throwable an exception raised by the new invocation,
	 * if it failed as well
	 * @see #setRefreshOnConnectFailure
	 * @see #doInvoke
	 */
	@Nullable
	protected Object handleConnectFailure(MethodInvocation invocation, Exception ex) throws Throwable {
		if (this.refreshOnConnectFailure) {
			String msg = "Could not connect to JMX server - retrying";
			if (logger.isDebugEnabled()) {
				logger.warn(msg, ex);
			}
			else if (logger.isWarnEnabled()) {
				logger.warn(msg);
			}
			prepare();
			return doInvoke(invocation);
		}
		else {
			throw ex;
		}
	}

	/**
	 * 将调用路由到所配置的受管资源。正确地将 JavaBean 属性访问路由到
	 * {@code MBeanServerConnection.get/setAttribute}，将方法调用路由到
	 * {@code MBeanServerConnection.invoke}。
	 * @param invocation the {@code MethodInvocation} to re-route
	 * @return the value returned as a result of the re-routed invocation
	 * @throws Throwable an invocation error propagated to the user
	 */
	@Nullable
	protected Object doInvoke(MethodInvocation invocation) throws Throwable {
		Method method = invocation.getMethod();
		try {
			Object result;
			if (this.invocationHandler != null) {
				result = this.invocationHandler.invoke(invocation.getThis(), method, invocation.getArguments());
			}
			else {
				PropertyDescriptor pd = BeanUtils.findPropertyForMethod(method);
				if (pd != null) {
					result = invokeAttribute(pd, invocation);
				}
				else {
					result = invokeOperation(method, invocation.getArguments());
				}
			}
			return convertResultValueIfNecessary(result, new MethodParameter(method, -1));
		}
		catch (MBeanException ex) {
			throw ex.getTargetException();
		}
		catch (RuntimeMBeanException ex) {
			throw ex.getTargetException();
		}
		catch (RuntimeErrorException ex) {
			throw ex.getTargetError();
		}
		catch (RuntimeOperationsException ex) {
			// This one is only thrown by the JMX 1.2 RI, not by the JDK 1.5 JMX code.
			RuntimeException rex = ex.getTargetException();
			if (rex instanceof RuntimeMBeanException) {
				throw ((RuntimeMBeanException) rex).getTargetException();
			}
			else if (rex instanceof RuntimeErrorException) {
				throw ((RuntimeErrorException) rex).getTargetError();
			}
			else {
				throw rex;
			}
		}
		catch (OperationsException ex) {
			if (ReflectionUtils.declaresException(method, ex.getClass())) {
				throw ex;
			}
			else {
				throw new InvalidInvocationException(ex.getMessage());
			}
		}
		catch (JMException ex) {
			if (ReflectionUtils.declaresException(method, ex.getClass())) {
				throw ex;
			}
			else {
				throw new InvocationFailureException("JMX access failed", ex);
			}
		}
		catch (IOException ex) {
			if (ReflectionUtils.declaresException(method, ex.getClass())) {
				throw ex;
			}
			else {
				throw new MBeanConnectFailureException("I/O failure during JMX access", ex);
			}
		}
	}

	@Nullable
	private Object invokeAttribute(PropertyDescriptor pd, MethodInvocation invocation)
			throws JMException, IOException {

		Assert.state(this.serverToUse != null, "No MBeanServerConnection available");

		String attributeName = JmxUtils.getAttributeName(pd, this.useStrictCasing);
		MBeanAttributeInfo inf = this.allowedAttributes.get(attributeName);
		// If no attribute is returned, we know that it is not defined in the
		// management interface.
		if (inf == null) {
			throw new InvalidInvocationException(
					"Attribute '" + pd.getName() + "' is not exposed on the management interface");
		}

		if (invocation.getMethod().equals(pd.getReadMethod())) {
			if (inf.isReadable()) {
				return this.serverToUse.getAttribute(this.objectName, attributeName);
			}
			else {
				throw new InvalidInvocationException("Attribute '" + attributeName + "' is not readable");
			}
		}
		else if (invocation.getMethod().equals(pd.getWriteMethod())) {
			if (inf.isWritable()) {
				this.serverToUse.setAttribute(this.objectName, new Attribute(attributeName, invocation.getArguments()[0]));
				return null;
			}
			else {
				throw new InvalidInvocationException("Attribute '" + attributeName + "' is not writable");
			}
		}
		else {
			throw new IllegalStateException(
					"Method [" + invocation.getMethod() + "] is neither a bean property getter nor a setter");
		}
	}

	/**
	 * 将方法调用（而非属性 get/set）路由到受管资源上对应的操作。
	 * @param method the method corresponding to operation on the managed resource.
	 * @param args the invocation arguments
	 * @return the value returned by the method invocation.
	 */
	private Object invokeOperation(Method method, Object[] args) throws JMException, IOException {
		Assert.state(this.serverToUse != null, "No MBeanServerConnection available");

		MethodCacheKey key = new MethodCacheKey(method.getName(), method.getParameterTypes());
		MBeanOperationInfo info = this.allowedOperations.get(key);
		if (info == null) {
			throw new InvalidInvocationException("Operation '" + method.getName() +
					"' is not exposed on the management interface");
		}

		String[] signature;
		synchronized (this.signatureCache) {
			signature = this.signatureCache.get(method);
			if (signature == null) {
				signature = JmxUtils.getMethodSignature(method);
				this.signatureCache.put(method, signature);
			}
		}

		return this.serverToUse.invoke(this.objectName, method.getName(), args, signature);
	}

	/**
	 * 将给定结果对象（来自属性访问或操作调用）转换为指定的目标类，以便从代理方法返回。
	 * @param result the result object as returned by the {@code MBeanServer}
	 * @param parameter the method parameter of the proxy method that's been invoked
	 * @return the converted result object, or the passed-in object if no conversion
	 * is necessary
	 */
	@Nullable
	protected Object convertResultValueIfNecessary(@Nullable Object result, MethodParameter parameter) {
		Class<?> targetClass = parameter.getParameterType();
		try {
			if (result == null) {
				return null;
			}
			if (ClassUtils.isAssignableValue(targetClass, result)) {
				return result;
			}
			if (result instanceof CompositeData) {
				Method fromMethod = targetClass.getMethod("from", CompositeData.class);
				return ReflectionUtils.invokeMethod(fromMethod, null, result);
			}
			else if (result instanceof CompositeData[]) {
				CompositeData[] array = (CompositeData[]) result;
				if (targetClass.isArray()) {
					return convertDataArrayToTargetArray(array, targetClass);
				}
				else if (Collection.class.isAssignableFrom(targetClass)) {
					Class<?> elementType =
							ResolvableType.forMethodParameter(parameter).asCollection().resolveGeneric();
					if (elementType != null) {
						return convertDataArrayToTargetCollection(array, targetClass, elementType);
					}
				}
			}
			else if (result instanceof TabularData) {
				Method fromMethod = targetClass.getMethod("from", TabularData.class);
				return ReflectionUtils.invokeMethod(fromMethod, null, result);
			}
			else if (result instanceof TabularData[]) {
				TabularData[] array = (TabularData[]) result;
				if (targetClass.isArray()) {
					return convertDataArrayToTargetArray(array, targetClass);
				}
				else if (Collection.class.isAssignableFrom(targetClass)) {
					Class<?> elementType =
							ResolvableType.forMethodParameter(parameter).asCollection().resolveGeneric();
					if (elementType != null) {
						return convertDataArrayToTargetCollection(array, targetClass, elementType);
					}
				}
			}
			throw new InvocationFailureException(
					"Incompatible result value [" + result + "] for target type [" + targetClass.getName() + "]");
		}
		catch (NoSuchMethodException ex) {
			throw new InvocationFailureException(
					"Could not obtain 'from(CompositeData)' / 'from(TabularData)' method on target type [" +
							targetClass.getName() + "] for conversion of MXBean data structure [" + result + "]");
		}
	}

	private Object convertDataArrayToTargetArray(Object[] array, Class<?> targetClass) throws NoSuchMethodException {
		Class<?> targetType = targetClass.getComponentType();
		Method fromMethod = targetType.getMethod("from", array.getClass().getComponentType());
		Object resultArray = Array.newInstance(targetType, array.length);
		for (int i = 0; i < array.length; i++) {
			Array.set(resultArray, i, ReflectionUtils.invokeMethod(fromMethod, null, array[i]));
		}
		return resultArray;
	}

	private Collection<?> convertDataArrayToTargetCollection(Object[] array, Class<?> collectionType, Class<?> elementType)
			throws NoSuchMethodException {

		Method fromMethod = elementType.getMethod("from", array.getClass().getComponentType());
		Collection<Object> resultColl = CollectionFactory.createCollection(collectionType, Array.getLength(array));
		for (int i = 0; i < array.length; i++) {
			resultColl.add(ReflectionUtils.invokeMethod(fromMethod, null, array[i]));
		}
		return resultColl;
	}


	@Override
	public void destroy() {
		this.connector.close();
	}


	/**
	 * 围绕方法名及其签名的简单包装类。用作缓存方法时的键。
	 */
	private static final class MethodCacheKey implements Comparable<MethodCacheKey> {

		private final String name;

		private final Class<?>[] parameterTypes;

		/**
		 * 使用提供的方法名和参数列表创建 {@code MethodCacheKey} 的新实例。
		 * @param name the name of the method
		 * @param parameterTypes the arguments in the method signature
		 */
		public MethodCacheKey(String name, @Nullable Class<?>[] parameterTypes) {
			this.name = name;
			this.parameterTypes = (parameterTypes != null ? parameterTypes : new Class<?>[0]);
		}

		@Override
		public boolean equals(@Nullable Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof MethodCacheKey)) {
				return false;
			}
			MethodCacheKey otherKey = (MethodCacheKey) other;
			return (this.name.equals(otherKey.name) && Arrays.equals(this.parameterTypes, otherKey.parameterTypes));
		}

		@Override
		public int hashCode() {
			return this.name.hashCode();
		}

		@Override
		public String toString() {
			return this.name + "(" + StringUtils.arrayToCommaDelimitedString(this.parameterTypes) + ")";
		}

		@Override
		public int compareTo(MethodCacheKey other) {
			int result = this.name.compareTo(other.name);
			if (result != 0) {
				return result;
			}
			if (this.parameterTypes.length < other.parameterTypes.length) {
				return -1;
			}
			if (this.parameterTypes.length > other.parameterTypes.length) {
				return 1;
			}
			for (int i = 0; i < this.parameterTypes.length; i++) {
				result = this.parameterTypes[i].getName().compareTo(other.parameterTypes[i].getName());
				if (result != 0) {
					return result;
				}
			}
			return 0;
		}
	}

}
