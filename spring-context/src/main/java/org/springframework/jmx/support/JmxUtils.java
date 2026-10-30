/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.jmx.support;

import java.beans.PropertyDescriptor;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.List;

import javax.management.DynamicMBean;
import javax.management.JMX;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.jmx.MBeanServerNotFoundException;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * 支持 Spring JMX 的通用工具方法集合。包含一个用于定位 MBeanServer 的便捷方法。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 1.2
 * @see #locateMBeanServer
 */
public abstract class JmxUtils {

	/**
	 * 用受管资源对应的 identity 哈希码扩展已有 {@link ObjectName} 时所使用的键。
	 */
	public static final String IDENTITY_OBJECT_NAME_KEY = "identity";

	/**
	 * 用于标识 MBean 接口的后缀。
	 */
	private static final String MBEAN_SUFFIX = "MBean";


	private static final Log logger = LogFactory.getLog(JmxUtils.class);


	/**
	 * 尝试查找本地运行的 {@code MBeanServer}。如果找不到任何 {@code MBeanServer}，则失败。如果找到多个
	 * {@code MBeanServer}，会记录一条警告，并返回列表中的第一个。
	 * @return 找到的 {@code MBeanServer}（如果存在）
	 * @throws MBeanServerNotFoundException 如果找不到任何 {@code MBeanServer}
	 * @see javax.management.MBeanServerFactory#findMBeanServer
	 */
	public static MBeanServer locateMBeanServer() throws MBeanServerNotFoundException {
		return locateMBeanServer(null);
	}

	/**
	 * 尝试查找本地运行的 {@code MBeanServer}。如果找不到任何 {@code MBeanServer}，则失败。如果找到多个
	 * {@code MBeanServer}，会记录一条警告，并返回列表中的第一个。
	 * @param agentId 要获取的 MBeanServer 的代理标识符。
	 * 如果该参数为 {@code null}，则考虑所有已注册的 MBeanServer。
	 * 如果给定空字符串，则返回平台 MBeanServer。
	 * @return 找到的 {@code MBeanServer}（如果存在）
	 * @throws MBeanServerNotFoundException 如果找不到任何 {@code MBeanServer}
	 * @see javax.management.MBeanServerFactory#findMBeanServer(String)
	 */
	public static MBeanServer locateMBeanServer(@Nullable String agentId) throws MBeanServerNotFoundException {
		MBeanServer server = null;

		// null means any registered server, but "" specifically means the platform server
		if (!"".equals(agentId)) {
			List<MBeanServer> servers = MBeanServerFactory.findMBeanServer(agentId);
			if (!CollectionUtils.isEmpty(servers)) {
				// Check to see if an MBeanServer is registered.
				if (servers.size() > 1 && logger.isInfoEnabled()) {
					logger.info("Found more than one MBeanServer instance" +
							(agentId != null ? " with agent id [" + agentId + "]" : "") +
							". Returning first from list.");
				}
				server = servers.get(0);
			}
		}

		if (server == null && !StringUtils.hasLength(agentId)) {
			// Attempt to load the PlatformMBeanServer.
			try {
				server = ManagementFactory.getPlatformMBeanServer();
			}
			catch (SecurityException ex) {
				throw new MBeanServerNotFoundException("No specific MBeanServer found, " +
						"and not allowed to obtain the Java platform MBeanServer", ex);
			}
		}

		if (server == null) {
			throw new MBeanServerNotFoundException(
					"Unable to locate an MBeanServer instance" +
					(agentId != null ? " with agent id [" + agentId + "]" : ""));
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Found MBeanServer: " + server);
		}
		return server;
	}

	/**
	 * 将 {@code MBeanParameterInfo} 数组转换为与各参数对应的 {@code Class} 实例数组。
	 * @param paramInfo JMX 参数信息
	 * @return 以类表示的参数类型
	 * @throws ClassNotFoundException 如果无法解析某个参数类型
	 */
	@Nullable
	public static Class<?>[] parameterInfoToTypes(@Nullable MBeanParameterInfo[] paramInfo)
			throws ClassNotFoundException {

		return parameterInfoToTypes(paramInfo, ClassUtils.getDefaultClassLoader());
	}

	/**
	 * 将 {@code MBeanParameterInfo} 数组转换为与各参数对应的 {@code Class} 实例数组。
	 * @param paramInfo JMX 参数信息
	 * @param classLoader 用于加载参数类型的 ClassLoader
	 * @return 以类表示的参数类型
	 * @throws ClassNotFoundException 如果无法解析某个参数类型
	 */
	@Nullable
	public static Class<?>[] parameterInfoToTypes(
			@Nullable MBeanParameterInfo[] paramInfo, @Nullable ClassLoader classLoader)
			throws ClassNotFoundException {

		Class<?>[] types = null;
		if (paramInfo != null && paramInfo.length > 0) {
			types = new Class<?>[paramInfo.length];
			for (int x = 0; x < paramInfo.length; x++) {
				types[x] = ClassUtils.forName(paramInfo[x].getType(), classLoader);
			}
		}
		return types;
	}

	/**
	 * 创建一个表示方法参数签名的 {@code String[]}。数组中的每个元素是该方法签名中对应参数的完全限定类
	 * 名。
	 * @param method 要为其构建参数签名的方法
	 * @return 以参数类型数组表示的签名
	 */
	public static String[] getMethodSignature(Method method) {
		Class<?>[] types = method.getParameterTypes();
		String[] signature = new String[types.length];
		for (int x = 0; x < types.length; x++) {
			signature[x] = types[x].getName();
		}
		return signature;
	}

	/**
	 * 返回给定 JavaBeans 属性要使用的 JMX 属性名。
	 * <p>使用严格大小写时，带有 {@code getFoo()} 这类 getter 方法的 JavaBean 属性会转换为名为
	 * {@code Foo} 的属性。禁用严格大小写后，{@code getFoo()} 将只转换为 {@code foo}。
	 * @param property JavaBeans 属性描述符
	 * @param useStrictCasing 是否使用严格的大小写形式
	 * @return 要使用的 JMX 属性名
	 */
	public static String getAttributeName(PropertyDescriptor property, boolean useStrictCasing) {
		if (useStrictCasing) {
			return StringUtils.capitalize(property.getName());
		}
		else {
			return property.getName();
		}
	}

	/**
	 * 在现有的 {@link ObjectName} 后追加一个额外的键/值对，键为静态值 {@code identity}，值为所提供
	 * {@link ObjectName} 上暴露的受管资源的 identity 哈希码。这可用于为某个特定 Bean 或类的每个不同实
	 * 例提供唯一的 {@link ObjectName}。当基于
	 * {@link org.springframework.jmx.export.naming.ObjectNamingStrategy} 提供的模板值，为一组受管资源
	 * 在运行时生成 {@link ObjectName ObjectNames} 时很有用。
	 * @param objectName 原始 JMX ObjectName
	 * @param managedResource MBean 实例
	 * @return 添加了 MBean 标识后的 ObjectName
	 * @throws MalformedObjectNameException 在对象名规范无效的情况下
	 * @see org.springframework.util.ObjectUtils#getIdentityHexString(Object)
	 */
	public static ObjectName appendIdentityToObjectName(ObjectName objectName, Object managedResource)
			throws MalformedObjectNameException {

		Hashtable<String, String> keyProperties = objectName.getKeyPropertyList();
		keyProperties.put(IDENTITY_OBJECT_NAME_KEY, ObjectUtils.getIdentityHexString(managedResource));
		return ObjectNameManager.getInstance(objectName.getDomain(), keyProperties);
	}

	/**
	 * 返回给定 Bean 要暴露的类或接口。这是将要在其中搜索属性和操作的类（例如，检查注解）。
	 * <p>此实现对于 CGLIB 代理返回其父类，其他情况（JDK 代理或普通 Bean 类）返回给定 Bean 的类。
	 * @param managedBean Bean 实例（可能是 AOP 代理）
	 * @return 要暴露的 Bean 类
	 * @see org.springframework.util.ClassUtils#getUserClass(Object)
	 */
	public static Class<?> getClassToExpose(Object managedBean) {
		return ClassUtils.getUserClass(managedBean);
	}

	/**
	 * 返回给定 Bean 类要暴露的类或接口。这是将要在其中搜索属性和操作的类（例如，检查注解）。
	 * <p>此实现对于 CGLIB 代理返回其父类，其他情况（JDK 代理或普通 Bean 类）返回给定的 Bean 类本身。
	 * @param clazz Bean 类（可能是 AOP 代理类）
	 * @return 要暴露的 Bean 类
	 * @see org.springframework.util.ClassUtils#getUserClass(Class)
	 */
	public static Class<?> getClassToExpose(Class<?> clazz) {
		return ClassUtils.getUserClass(clazz);
	}

	/**
	 * 判断给定的 Bean 类是否本身就可作为 MBean。
	 * <p>此实现会检查 {@link javax.management.DynamicMBean} 类，以及带有对应 "*MBean" 接口（标准 MBean）
	 * 或对应 "*MXBean" 接口（Java MXBean）的类。
	 * @param clazz 要分析的 Bean 类
	 * @return 该类是否符合 MBean 的要求
	 * @see org.springframework.jmx.export.MBeanExporter#isMBean(Class)
	 */
	public static boolean isMBean(@Nullable Class<?> clazz) {
		return (clazz != null &&
				(DynamicMBean.class.isAssignableFrom(clazz) ||
						(getMBeanInterface(clazz) != null || getMXBeanInterface(clazz) != null)));
	}

	/**
	 * 返回给定类的标准 MBean 接口（如果有），即名称与给定类的类名匹配但带有 "MBean" 后缀的接口。
	 * @param clazz 要检查的类
	 * @return 给定类的 Standard MBean 接口
	 */
	@Nullable
	public static Class<?> getMBeanInterface(@Nullable Class<?> clazz) {
		if (clazz == null || clazz.getSuperclass() == null) {
			return null;
		}
		String mbeanInterfaceName = clazz.getName() + MBEAN_SUFFIX;
		Class<?>[] implementedInterfaces = clazz.getInterfaces();
		for (Class<?> iface : implementedInterfaces) {
			if (iface.getName().equals(mbeanInterfaceName)) {
				return iface;
			}
		}
		return getMBeanInterface(clazz.getSuperclass());
	}

	/**
	 * 返回给定类的 Java MXBean 接口（如果有），即名称以 "MXBean" 结尾和/或带有相应 MXBean 注解的接口。
	 * @param clazz 要检查的类
	 * @return 给定类是否存在 MXBean 接口
	 */
	@Nullable
	public static Class<?> getMXBeanInterface(@Nullable Class<?> clazz) {
		if (clazz == null || clazz.getSuperclass() == null) {
			return null;
		}
		Class<?>[] implementedInterfaces = clazz.getInterfaces();
		for (Class<?> iface : implementedInterfaces) {
			if (JMX.isMXBeanInterface(iface)) {
				return iface;
			}
		}
		return getMXBeanInterface(clazz.getSuperclass());
	}

}
