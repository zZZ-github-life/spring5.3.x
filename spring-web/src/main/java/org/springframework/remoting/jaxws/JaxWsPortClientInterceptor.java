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

package org.springframework.remoting.jaxws;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.jws.WebService;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.ProtocolException;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;
import javax.xml.ws.soap.SOAPFaultException;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.remoting.RemoteAccessException;
import org.springframework.remoting.RemoteConnectFailureException;
import org.springframework.remoting.RemoteLookupFailureException;
import org.springframework.remoting.RemoteProxyFailureException;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@link org.aopalliance.intercept.MethodInterceptor}，用于访问
 * JAX-WS 服务的特定端口。
 *
 * <p>底层使用 {@link LocalJaxWsServiceFactory} 的设施，
 * 或持有对现有 JAX-WS Service 实例的显式引用
 * （例如通过 {@link org.springframework.jndi.JndiObjectFactoryBean} 获得）。
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see #setPortName
 * @see #setServiceInterface
 * @see javax.xml.ws.Service#getPort
 * @see org.springframework.remoting.RemoteAccessException
 * @see org.springframework.jndi.JndiObjectFactoryBean
 */
public class JaxWsPortClientInterceptor extends LocalJaxWsServiceFactory
		implements MethodInterceptor, BeanClassLoaderAware, InitializingBean {

	@Nullable
	private Service jaxWsService;

	@Nullable
	private String portName;

	@Nullable
	private String username;

	@Nullable
	private String password;

	@Nullable
	private String endpointAddress;

	private boolean maintainSession;

	private boolean useSoapAction;

	@Nullable
	private String soapActionUri;

	@Nullable
	private Map<String, Object> customProperties;

	@Nullable
	private WebServiceFeature[] portFeatures;

	@Nullable
	private Class<?> serviceInterface;

	private boolean lookupServiceOnStartup = true;

	@Nullable
	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	@Nullable
	private QName portQName;

	@Nullable
	private Object portStub;

	private final Object preparationMonitor = new Object();


	/**
	 * 设置对现有 JAX-WS Service 实例的引用，
	 * 例如通过 {@link org.springframework.jndi.JndiObjectFactoryBean} 获得。
	 * 如果未设置，则必须指定 {@link LocalJaxWsServiceFactory} 的各个属性。
	 * @see #setWsdlDocumentUrl
	 * @see #setNamespaceUri
	 * @see #setServiceName
	 * @see org.springframework.jndi.JndiObjectFactoryBean
	 */
	public void setJaxWsService(@Nullable Service jaxWsService) {
		this.jaxWsService = jaxWsService;
	}

	/**
	 * 返回对现有 JAX-WS Service 实例的引用（如果有）。
	 */
	@Nullable
	public Service getJaxWsService() {
		return this.jaxWsService;
	}

	/**
	 * 设置端口的名称。
	 * 对应于 "wsdl:port" 名称。
	 */
	public void setPortName(@Nullable String portName) {
		this.portName = portName;
	}

	/**
	 * 返回端口的名称。
	 */
	@Nullable
	public String getPortName() {
		return this.portName;
	}

	/**
	 * 设置要在 stub 上指定的用户名。
	 * @see javax.xml.ws.BindingProvider#USERNAME_PROPERTY
	 */
	public void setUsername(@Nullable String username) {
		this.username = username;
	}

	/**
	 * 返回要在 stub 上指定的用户名。
	 */
	@Nullable
	public String getUsername() {
		return this.username;
	}

	/**
	 * 设置要在 stub 上指定的密码。
	 * @see javax.xml.ws.BindingProvider#PASSWORD_PROPERTY
	 */
	public void setPassword(@Nullable String password) {
		this.password = password;
	}

	/**
	 * 返回要在 stub 上指定的密码。
	 */
	@Nullable
	public String getPassword() {
		return this.password;
	}

	/**
	 * 设置要在 stub 上指定的端点地址。
	 * @see javax.xml.ws.BindingProvider#ENDPOINT_ADDRESS_PROPERTY
	 */
	public void setEndpointAddress(@Nullable String endpointAddress) {
		this.endpointAddress = endpointAddress;
	}

	/**
	 * 返回要在 stub 上指定的端点地址。
	 */
	@Nullable
	public String getEndpointAddress() {
		return this.endpointAddress;
	}

	/**
	 * 设置要在 stub 上指定的 "session.maintain" 标志。
	 * @see javax.xml.ws.BindingProvider#SESSION_MAINTAIN_PROPERTY
	 */
	public void setMaintainSession(boolean maintainSession) {
		this.maintainSession = maintainSession;
	}

	/**
	 * 返回要在 stub 上指定的 "session.maintain" 标志。
	 */
	public boolean isMaintainSession() {
		return this.maintainSession;
	}

	/**
	 * 设置要在 stub 上指定的 "soapaction.use" 标志。
	 * @see javax.xml.ws.BindingProvider#SOAPACTION_USE_PROPERTY
	 */
	public void setUseSoapAction(boolean useSoapAction) {
		this.useSoapAction = useSoapAction;
	}

	/**
	 * 返回要在 stub 上指定的 "soapaction.use" 标志。
	 */
	public boolean isUseSoapAction() {
		return this.useSoapAction;
	}

	/**
	 * 设置要在 stub 上指定的 SOAP action URI。
	 * @see javax.xml.ws.BindingProvider#SOAPACTION_URI_PROPERTY
	 */
	public void setSoapActionUri(@Nullable String soapActionUri) {
		this.soapActionUri = soapActionUri;
	}

	/**
	 * 返回要在 stub 上指定的 SOAP action URI。
	 */
	@Nullable
	public String getSoapActionUri() {
		return this.soapActionUri;
	}

	/**
	 * 设置要在 stub 上设置的自定义属性。
	 * <p>可以通过 String "value"（由 PropertiesEditor 解析）
	 * 或 XML bean 定义中的 "props" 元素填充。
	 * @see javax.xml.ws.BindingProvider#getRequestContext()
	 */
	public void setCustomProperties(Map<String, Object> customProperties) {
		this.customProperties = customProperties;
	}

	/**
	 * 允许以 Map 方式访问要在 stub 上设置的自定义属性，
	 * 并可以选择添加或覆盖特定条目。
	 * <p>适用于直接指定条目，例如通过
	 * "customProperties[myKey]"。这对于在子 bean 定义中
	 * 添加或覆盖条目特别有用。
	 */
	public Map<String, Object> getCustomProperties() {
		if (this.customProperties == null) {
			this.customProperties = new HashMap<>();
		}
		return this.customProperties;
	}

	/**
	 * 为此 JAX-WS BindingProvider 添加自定义属性。
	 * @param name the name of the attribute to expose
	 * @param value the attribute value to expose
	 * @see javax.xml.ws.BindingProvider#getRequestContext()
	 */
	public void addCustomProperty(String name, Object value) {
		getCustomProperties().put(name, value);
	}

	/**
	 * 指定要应用于 JAX-WS 端口 stub 创建的 WebServiceFeature 对象
	 * （例如作为内部 bean 定义）。
	 * @since 4.0
	 * @see Service#getPort(Class, javax.xml.ws.WebServiceFeature...)
	 * @see #setServiceFeatures
	 */
	public void setPortFeatures(WebServiceFeature... features) {
		this.portFeatures = features;
	}

	/**
	 * 设置此工厂应为其创建代理的服务接口。
	 */
	public void setServiceInterface(@Nullable Class<?> serviceInterface) {
		if (serviceInterface != null) {
			Assert.isTrue(serviceInterface.isInterface(), "'serviceInterface' must be an interface");
		}
		this.serviceInterface = serviceInterface;
	}

	/**
	 * 返回此工厂应为其创建代理的服务接口。
	 */
	@Nullable
	public Class<?> getServiceInterface() {
		return this.serviceInterface;
	}

	/**
	 * 设置是否在启动时查找 JAX-WS 服务。
	 * <p>默认为 "true"。关闭此标志可允许目标服务器延迟启动。
	 * 在这种情况下，将在首次访问时延迟获取 JAX-WS 服务。
	 */
	public void setLookupServiceOnStartup(boolean lookupServiceOnStartup) {
		this.lookupServiceOnStartup = lookupServiceOnStartup;
	}

	/**
	 * 设置此拦截器使用的 bean ClassLoader：主要用于
	 * 在 {@link JaxWsPortProxyFactoryBean} 子类中构建客户端代理。
	 */
	@Override
	public void setBeanClassLoader(@Nullable ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	/**
	 * 返回此拦截器使用的 bean ClassLoader。
	 */
	@Nullable
	protected ClassLoader getBeanClassLoader() {
		return this.beanClassLoader;
	}


	@Override
	public void afterPropertiesSet() {
		if (this.lookupServiceOnStartup) {
			prepare();
		}
	}

	/**
	 * 为此拦截器初始化 JAX-WS 端口。
	 */
	public void prepare() {
		Class<?> ifc = getServiceInterface();
		Assert.notNull(ifc, "Property 'serviceInterface' is required");

		WebService ann = ifc.getAnnotation(WebService.class);
		if (ann != null) {
			applyDefaultsFromAnnotation(ann);
		}

		Service serviceToUse = getJaxWsService();
		if (serviceToUse == null) {
			serviceToUse = createJaxWsService();
		}

		this.portQName = getQName(getPortName() != null ? getPortName() : ifc.getName());
		Object stub = getPortStub(serviceToUse, (getPortName() != null ? this.portQName : null));
		preparePortStub(stub);
		this.portStub = stub;
	}

	/**
	 * 如有必要且可能，根据给定的 WebService 注解初始化此客户端拦截器的属性
	 * （即如果 "wsdlDocumentUrl"、"namespaceUri"、"serviceName"
	 * 和 "portName" 尚未设置，但相应值已在指定服务接口的
	 * 注解级别声明）。
	 * @param ann the WebService annotation found on the specified service interface
	 */
	protected void applyDefaultsFromAnnotation(WebService ann) {
		if (getWsdlDocumentUrl() == null) {
			String wsdl = ann.wsdlLocation();
			if (StringUtils.hasText(wsdl)) {
				try {
					setWsdlDocumentUrl(new URL(wsdl));
				}
				catch (MalformedURLException ex) {
					throw new IllegalStateException(
							"Encountered invalid @Service wsdlLocation value [" + wsdl + "]", ex);
				}
			}
		}
		if (getNamespaceUri() == null) {
			String ns = ann.targetNamespace();
			if (StringUtils.hasText(ns)) {
				setNamespaceUri(ns);
			}
		}
		if (getServiceName() == null) {
			String sn = ann.serviceName();
			if (StringUtils.hasText(sn)) {
				setServiceName(sn);
			}
		}
		if (getPortName() == null) {
			String pn = ann.portName();
			if (StringUtils.hasText(pn)) {
				setPortName(pn);
			}
		}
	}

	/**
	 * 返回此客户端拦截器是否已准备就绪，
	 * 即是否已查找 JAX-WS 服务和端口。
	 */
	protected boolean isPrepared() {
		synchronized (this.preparationMonitor) {
			return (this.portStub != null);
		}
	}

	/**
	 * 返回为端口准备好的 QName。
	 * @see #setPortName
	 * @see #getQName
	 */
	@Nullable
	protected final QName getPortQName() {
		return this.portQName;
	}

	/**
	 * 从给定的 JAX-WS Service 中获取端口 stub。
	 * @param service the Service object to obtain the port from
	 * @param portQName the name of the desired port, if specified
	 * @return the corresponding port object as returned from
	 * {@code Service.getPort(...)}
	 */
	protected Object getPortStub(Service service, @Nullable QName portQName) {
		if (this.portFeatures != null) {
			return (portQName != null ? service.getPort(portQName, getServiceInterface(), this.portFeatures) :
					service.getPort(getServiceInterface(), this.portFeatures));
		}
		else {
			return (portQName != null ? service.getPort(portQName, getServiceInterface()) :
					service.getPort(getServiceInterface()));
		}
	}

	/**
	 * 准备给定的 JAX-WS 端口 stub，并对其应用属性。
	 * 由 {@link #prepare} 调用。
	 * @param stub the current JAX-WS port stub
	 * @see #setUsername
	 * @see #setPassword
	 * @see #setEndpointAddress
	 * @see #setMaintainSession
	 * @see #setCustomProperties
	 */
	protected void preparePortStub(Object stub) {
		Map<String, Object> stubProperties = new HashMap<>();
		String username = getUsername();
		if (username != null) {
			stubProperties.put(BindingProvider.USERNAME_PROPERTY, username);
		}
		String password = getPassword();
		if (password != null) {
			stubProperties.put(BindingProvider.PASSWORD_PROPERTY, password);
		}
		String endpointAddress = getEndpointAddress();
		if (endpointAddress != null) {
			stubProperties.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointAddress);
		}
		if (isMaintainSession()) {
			stubProperties.put(BindingProvider.SESSION_MAINTAIN_PROPERTY, Boolean.TRUE);
		}
		if (isUseSoapAction()) {
			stubProperties.put(BindingProvider.SOAPACTION_USE_PROPERTY, Boolean.TRUE);
		}
		String soapActionUri = getSoapActionUri();
		if (soapActionUri != null) {
			stubProperties.put(BindingProvider.SOAPACTION_URI_PROPERTY, soapActionUri);
		}
		stubProperties.putAll(getCustomProperties());
		if (!stubProperties.isEmpty()) {
			if (!(stub instanceof BindingProvider)) {
				throw new RemoteLookupFailureException("Port stub of class [" + stub.getClass().getName() +
						"] is not a customizable JAX-WS stub: it does not implement interface [javax.xml.ws.BindingProvider]");
			}
			((BindingProvider) stub).getRequestContext().putAll(stubProperties);
		}
	}

	/**
	 * 返回此拦截器在代理上的每次方法调用时
	 * 委托给的底层 JAX-WS 端口 stub。
	 */
	@Nullable
	protected Object getPortStub() {
		return this.portStub;
	}


	@Override
	@Nullable
	public Object invoke(MethodInvocation invocation) throws Throwable {
		if (AopUtils.isToStringMethod(invocation.getMethod())) {
			return "JAX-WS proxy for port [" + getPortName() + "] of service [" + getServiceName() + "]";
		}
		// Lazily prepare service and stub if necessary.
		synchronized (this.preparationMonitor) {
			if (!isPrepared()) {
				prepare();
			}
		}
		return doInvoke(invocation);
	}

	/**
	 * 基于给定的方法调用执行 JAX-WS 服务调用。
	 * @param invocation the AOP method invocation
	 * @return the invocation result, if any
	 * @throws Throwable in case of invocation failure
	 * @see #getPortStub()
	 * @see #doInvoke(org.aopalliance.intercept.MethodInvocation, Object)
	 */
	@Nullable
	protected Object doInvoke(MethodInvocation invocation) throws Throwable {
		try {
			return doInvoke(invocation, getPortStub());
		}
		catch (SOAPFaultException ex) {
			throw new JaxWsSoapFaultException(ex);
		}
		catch (ProtocolException ex) {
			throw new RemoteConnectFailureException(
					"Could not connect to remote service [" + getEndpointAddress() + "]", ex);
		}
		catch (WebServiceException ex) {
			throw new RemoteAccessException(
					"Could not access remote service at [" + getEndpointAddress() + "]", ex);
		}
	}

	/**
	 * 在给定的端口 stub 上执行 JAX-WS 服务调用。
	 * @param invocation the AOP method invocation
	 * @param portStub the RMI port stub to invoke
	 * @return the invocation result, if any
	 * @throws Throwable in case of invocation failure
	 * @see #getPortStub()
	 */
	@Nullable
	protected Object doInvoke(MethodInvocation invocation, @Nullable Object portStub) throws Throwable {
		Method method = invocation.getMethod();
		try {
			return method.invoke(portStub, invocation.getArguments());
		}
		catch (InvocationTargetException ex) {
			throw ex.getTargetException();
		}
		catch (Throwable ex) {
			throw new RemoteProxyFailureException("Invocation of stub method failed: " + method, ex);
		}
	}

}
