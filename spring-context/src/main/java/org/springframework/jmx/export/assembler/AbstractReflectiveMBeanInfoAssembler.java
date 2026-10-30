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

package org.springframework.jmx.export.assembler;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.management.Descriptor;
import javax.management.JMException;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanOperationInfo;

import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.jmx.support.JmxUtils;
import org.springframework.lang.Nullable;

/**
 * 在 {@link AbstractMBeanInfoAssembler} 超类的基础上，添加了基于 MBean 类的反射元数据构建元数据的
 * 基本算法。
 *
 * <p>从反射元数据创建 MBean 元数据的逻辑包含在此类中，但此类并不决定要暴露哪些方法和属性。相反，它
 * 通过 {@code includeXXX} 方法让子类对每个属性或方法进行“投票”。
 *
 * <p>在元数据装配完成后，子类还可以通过 {@code populateXXXDescriptor} 方法，为属性和操作元数据填充
 * 额外的描述符。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author David Boden
 * @since 1.2
 * @see #includeOperation
 * @see #includeReadAttribute
 * @see #includeWriteAttribute
 * @see #populateAttributeDescriptor
 * @see #populateOperationDescriptor
 */
public abstract class AbstractReflectiveMBeanInfoAssembler extends AbstractMBeanInfoAssembler {

	/**
	 * 在 JMX {@link Descriptor} 中标识 getter 方法。
	 */
	protected static final String FIELD_GET_METHOD = "getMethod";

	/**
	 * 在 JMX {@link Descriptor} 中标识 setter 方法。
	 */
	protected static final String FIELD_SET_METHOD = "setMethod";

	/**
	 * JMX {@link Descriptor} 中 role 字段的常量标识。
	 */
	protected static final String FIELD_ROLE = "role";

	/**
	 * JMX {@link Descriptor} 中 getter role 字段值的常量标识。
	 */
	protected static final String ROLE_GETTER = "getter";

	/**
	 * JMX {@link Descriptor} 中 setter role 字段值的常量标识。
	 */
	protected static final String ROLE_SETTER = "setter";

	/**
	 * 在 JMX {@link Descriptor} 中标识操作（方法）。
	 */
	protected static final String ROLE_OPERATION = "operation";

	/**
	 * JMX {@link Descriptor} 中 visibility 字段的常量标识。
	 */
	protected static final String FIELD_VISIBILITY = "visibility";

	/**
	 * 最低可见性，用于对应于属性访问器或修改器的操作。
	 * @see #FIELD_VISIBILITY
	 */
	protected static final int ATTRIBUTE_OPERATION_VISIBILITY = 4;

	/**
	 * JMX {@link Descriptor} 中 class 字段的常量标识。
	 */
	protected static final String FIELD_CLASS = "class";
	/**
	 * JMX {@link Descriptor} 中 log 字段的常量标识。
	 */
	protected static final String FIELD_LOG = "log";

	/**
	 * JMX {@link Descriptor} 中 logfile 字段的常量标识。
	 */
	protected static final String FIELD_LOG_FILE = "logFile";

	/**
	 * JMX {@link Descriptor} 中 currency time limit 字段的常量标识。
	 */
	protected static final String FIELD_CURRENCY_TIME_LIMIT = "currencyTimeLimit";

	/**
	 * JMX {@link Descriptor} 中 default 字段的常量标识。
	 */
	protected static final String FIELD_DEFAULT = "default";

	/**
	 * JMX {@link Descriptor} 中 persistPolicy 字段的常量标识。
	 */
	protected static final String FIELD_PERSIST_POLICY = "persistPolicy";

	/**
	 * JMX {@link Descriptor} 中 persistPeriod 字段的常量标识。
	 */
	protected static final String FIELD_PERSIST_PERIOD = "persistPeriod";

	/**
	 * JMX {@link Descriptor} 中 persistLocation 字段的常量标识。
	 */
	protected static final String FIELD_PERSIST_LOCATION = "persistLocation";

	/**
	 * JMX {@link Descriptor} 中 persistName 字段的常量标识。
	 */
	protected static final String FIELD_PERSIST_NAME = "persistName";

	/**
	 * JMX {@link Descriptor} 中 displayName 字段的常量标识。
	 */
	protected static final String FIELD_DISPLAY_NAME = "displayName";

	/**
	 * JMX {@link Descriptor} 中 units 字段的常量标识。
	 */
	protected static final String FIELD_UNITS = "units";

	/**
	 * JMX {@link Descriptor} 中 metricType 字段的常量标识。
	 */
	protected static final String FIELD_METRIC_TYPE = "metricType";

	/**
	 * JMX {@link Descriptor} 中自定义 metricCategory 字段的常量标识。
	 */
	protected static final String FIELD_METRIC_CATEGORY = "metricCategory";


	/**
	 * JMX 字段 "currencyTimeLimit" 的默认值。
	 */
	@Nullable
	private Integer defaultCurrencyTimeLimit;

	/**
	 * 指示属性是否使用严格大小写。
	 */
	private boolean useStrictCasing = true;

	private boolean exposeClassDescriptor = false;

	@Nullable
	private ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();


	/**
	 * 设置 JMX 字段 "currencyTimeLimit" 的默认值。该默认值通常表示从不缓存属性值。
	 * <p>默认值为无，即不显式设置该字段，这是 JMX 1.2 规范所建议的。这应导致“从不缓存”的行为，即始终重
	 * 新读取属性值（在 JMX 1.2 中对应于 {@code -1} 的 "currencyTimeLimit"）。
	 * <p>不过，某些 JMX 实现（在这方面未遵循 JMX 1.2 规范）可能要求在此处设置显式值才能获得“从不缓存”
	 * 的行为：例如 JBoss 3.2.x。
	 * <p>请注意，"currencyTimeLimit" 值也可以在受管理的属性或操作上指定。如果那里没有用 {@code >= 0}
	 * 的 "currencyTimeLimit" 值覆盖，则将应用默认值：元数据中 {@code -1} 的 "currencyTimeLimit" 值表示
	 * 使用默认值；{@code 0} 的值表示“始终缓存”，将被转换为 {@code Integer.MAX_VALUE}；正值表示缓存秒数。
	 * @see org.springframework.jmx.export.metadata.AbstractJmxAttribute#setCurrencyTimeLimit
	 * @see #applyCurrencyTimeLimit(javax.management.Descriptor, int)
	 */
	public void setDefaultCurrencyTimeLimit(@Nullable Integer defaultCurrencyTimeLimit) {
		this.defaultCurrencyTimeLimit = defaultCurrencyTimeLimit;
	}

	/**
	 * 返回 JMX 字段 "currencyTimeLimit" 的默认值（如果有）。
	 */
	@Nullable
	protected Integer getDefaultCurrencyTimeLimit() {
		return this.defaultCurrencyTimeLimit;
	}

	/**
	 * 设置属性是否使用严格大小写。默认启用。
	 * <p>使用严格大小时，带有 getter 的 JavaBean 属性（如 {@code getFoo()}）会转换为名为 {@code Foo}
	 * 的属性。禁用严格大小写时，{@code getFoo()} 将仅转换为 {@code foo}。
	 */
	public void setUseStrictCasing(boolean useStrictCasing) {
		this.useStrictCasing = useStrictCasing;
	}

	/**
	 * 返回是否启用了属性的严格大小写。
	 */
	protected boolean isUseStrictCasing() {
		return this.useStrictCasing;
	}

	/**
	 * 设置是否为受管理操作暴露 JMX 描述符字段 "class"。默认值为 "false"，由 JMX 实现通过反射确定实际类。
	 * <p>对于要求指定 "class" 字段的 JMX 实现（例如 WebLogic 的实现），请将此属性设置为 {@code true}。
	 * 在这种情况下，如果是普通 Bean 实例或 CGLIB 代理，Spring 将在其中暴露目标类名。遇到 JDK 动态代理
	 * 时，将指定该代理实现的<b>第一个</b>接口。
	 * <p><b>警告：</b>通过 JMX 暴露 JDK 动态代理时，尤其是将此属性设置为 {@code true} 时，请检查你的代
	 * 理定义：此时指定的接口列表应以你的管理接口开头，所有其他接口随后。一般来说，建议直接暴露你的目标
	 * Bean 或其 CGLIB 代理。
	 * @see #getClassForDescriptor(Object)
	 */
	public void setExposeClassDescriptor(boolean exposeClassDescriptor) {
		this.exposeClassDescriptor = exposeClassDescriptor;
	}

	/**
	 * 返回是否为受管理操作暴露 JMX 描述符字段 "class"。
	 */
	protected boolean isExposeClassDescriptor() {
		return this.exposeClassDescriptor;
	}

	/**
	 * 设置用于在需要时解析方法参数名称的 ParameterNameDiscoverer（例如用于 MBean 操作方法的参数名）。
	 * <p>默认是 {@link DefaultParameterNameDiscoverer}。
	 */
	public void setParameterNameDiscoverer(@Nullable ParameterNameDiscoverer parameterNameDiscoverer) {
		this.parameterNameDiscoverer = parameterNameDiscoverer;
	}

	/**
	 * 返回用于在需要时解析方法参数名称的 ParameterNameDiscoverer（可以为 {@code null} 以跳过参数检测）。
	 */
	@Nullable
	protected ParameterNameDiscoverer getParameterNameDiscoverer() {
		return this.parameterNameDiscoverer;
	}


	/**
	 * 遍历 MBean 类上的所有属性，并让子类有机会对访问器和修改器两者的包含进行投票。如果某个访问器或修
	 * 改器被投票赞成包含，则装配相应的元数据并将其传递给子类以填充描述符。
	 * @param managedBean Bean 实例（可能是 AOP 代理）
	 * @param beanKey MBean 在 {@code MBeanExporter} 的 beans 映射中
	 * 关联的键
	 * @return 属性元数据
	 * @throws JMException 如果发生错误
	 * @see #populateAttributeDescriptor
	 */
	@Override
	protected ModelMBeanAttributeInfo[] getAttributeInfo(Object managedBean, String beanKey) throws JMException {
		PropertyDescriptor[] props = BeanUtils.getPropertyDescriptors(getClassToExpose(managedBean));
		List<ModelMBeanAttributeInfo> infos = new ArrayList<>();

		for (PropertyDescriptor prop : props) {
			Method getter = prop.getReadMethod();
			if (getter != null && getter.getDeclaringClass() == Object.class) {
				continue;
			}
			if (getter != null && !includeReadAttribute(getter, beanKey)) {
				getter = null;
			}

			Method setter = prop.getWriteMethod();
			if (setter != null && !includeWriteAttribute(setter, beanKey)) {
				setter = null;
			}

			if (getter != null || setter != null) {
				// If both getter and setter are null, then this does not need exposing.
				String attrName = JmxUtils.getAttributeName(prop, isUseStrictCasing());
				String description = getAttributeDescription(prop, beanKey);
				ModelMBeanAttributeInfo info = new ModelMBeanAttributeInfo(attrName, description, getter, setter);

				Descriptor desc = info.getDescriptor();
				if (getter != null) {
					desc.setField(FIELD_GET_METHOD, getter.getName());
				}
				if (setter != null) {
					desc.setField(FIELD_SET_METHOD, setter.getName());
				}

				populateAttributeDescriptor(desc, getter, setter, beanKey);
				info.setDescriptor(desc);
				infos.add(info);
			}
		}

		return infos.toArray(new ModelMBeanAttributeInfo[0]);
	}

	/**
	 * 遍历 MBean 类上的所有方法，并让子类有机会对其包含进行投票。如果某个方法对应于管理接口中包含的某
	 * 个属性的访问器或修改器，则暴露相应的操作，并将 &quot;role&quot; 描述符字段设置为适当的值。
	 * @param managedBean Bean 实例（可能是 AOP 代理）
	 * @param beanKey MBean 在 {@code MBeanExporter} 的 beans 映射中
	 * 关联的键
	 * @return 操作元数据
	 * @see #populateOperationDescriptor
	 */
	@Override
	protected ModelMBeanOperationInfo[] getOperationInfo(Object managedBean, String beanKey) {
		Method[] methods = getClassToExpose(managedBean).getMethods();
		List<ModelMBeanOperationInfo> infos = new ArrayList<>();

		for (Method method : methods) {
			if (method.isSynthetic()) {
				continue;
			}
			if (Object.class == method.getDeclaringClass()) {
				continue;
			}

			ModelMBeanOperationInfo info = null;
			PropertyDescriptor pd = BeanUtils.findPropertyForMethod(method);
			if (pd != null && ((method.equals(pd.getReadMethod()) && includeReadAttribute(method, beanKey)) ||
						(method.equals(pd.getWriteMethod()) && includeWriteAttribute(method, beanKey)))) {
				// Attributes need to have their methods exposed as
				// operations to the JMX server as well.
				info = createModelMBeanOperationInfo(method, pd.getName(), beanKey);
				Descriptor desc = info.getDescriptor();
				if (method.equals(pd.getReadMethod())) {
					desc.setField(FIELD_ROLE, ROLE_GETTER);
				}
				else {
					desc.setField(FIELD_ROLE, ROLE_SETTER);
				}
				desc.setField(FIELD_VISIBILITY, ATTRIBUTE_OPERATION_VISIBILITY);
				if (isExposeClassDescriptor()) {
					desc.setField(FIELD_CLASS, getClassForDescriptor(managedBean).getName());
				}
				info.setDescriptor(desc);
			}

			// allow getters and setters to be marked as operations directly
			if (info == null && includeOperation(method, beanKey)) {
				info = createModelMBeanOperationInfo(method, method.getName(), beanKey);
				Descriptor desc = info.getDescriptor();
				desc.setField(FIELD_ROLE, ROLE_OPERATION);
				if (isExposeClassDescriptor()) {
					desc.setField(FIELD_CLASS, getClassForDescriptor(managedBean).getName());
				}
				populateOperationDescriptor(desc, method, beanKey);
				info.setDescriptor(desc);
			}

			if (info != null) {
				infos.add(info);
			}
		}

		return infos.toArray(new ModelMBeanOperationInfo[0]);
	}

	/**
	 * 为给定方法创建 {@code ModelMBeanOperationInfo} 实例。填充该操作的参数信息。
	 * @param method 要为其创建 {@code ModelMBeanOperationInfo} 的
	 * {@code Method}
	 * @param name 操作的逻辑名称（方法名或属性名）；
	 * 默认实现不使用，但子类可能会使用
	 * @param beanKey MBean 在 {@code MBeanExporter} 的 beans 映射中
	 * 关联的键
	 * @return 该 {@code ModelMBeanOperationInfo}
	 */
	protected ModelMBeanOperationInfo createModelMBeanOperationInfo(Method method, String name, String beanKey) {
		MBeanParameterInfo[] params = getOperationParameters(method, beanKey);
		if (params.length == 0) {
			return new ModelMBeanOperationInfo(getOperationDescription(method, beanKey), method);
		}
		else {
			return new ModelMBeanOperationInfo(method.getName(),
				getOperationDescription(method, beanKey),
				getOperationParameters(method, beanKey),
				method.getReturnType().getName(),
				MBeanOperationInfo.UNKNOWN);
		}
	}

	/**
	 * 返回用于 JMX 描述符字段 "class" 的类。仅在 "exposeClassDescriptor" 属性为 "true" 时应用。
	 * <p>默认实现对于 JDK 代理返回第一个实现的接口，否则返回目标类。
	 * @param managedBean Bean 实例（可能是 AOP 代理）
	 * @return 要在描述符字段 "class" 中公开的类
	 * @see #setExposeClassDescriptor
	 * @see #getClassToExpose(Class)
	 * @see org.springframework.aop.framework.AopProxyUtils#proxiedUserInterfaces(Object)
	 */
	protected Class<?> getClassForDescriptor(Object managedBean) {
		if (AopUtils.isJdkDynamicProxy(managedBean)) {
			return AopProxyUtils.proxiedUserInterfaces(managedBean)[0];
		}
		return getClassToExpose(managedBean);
	}


	/**
	 * 允许子类对某个属性访问器的包含进行投票。
	 * @param method 访问器 {@code Method}
	 * @param beanKey MBean 在 {@code MBeanExporter} 的 beans 映射中
	 * 关联的键
	 * @return 如果该访问器应包含在管理接口中则返回 {@code true}，
	 * 否则返回 {@code false}
	 */
	protected abstract boolean includeReadAttribute(Method method, String beanKey);

	/**
	 * 允许子类对某个属性修改器的包含进行投票。
	 * @param method 修改器 {@code Method}。
	 * @param beanKey MBean 在 {@code MBeanExporter} 的 beans 映射中
	 * 关联的键
	 * @return 如果该修改器应包含在管理接口中则返回 {@code true}，
	 * 否则返回 {@code false}
	 */
	protected abstract boolean includeWriteAttribute(Method method, String beanKey);

	/**
	 * 允许子类对某个操作的包含进行投票。
	 * @param method 操作方法
	 * @param beanKey MBean 在 {@code MBeanExporter} 的 beans 映射中
	 * 关联的键
	 * @return 该操作是否应包含在管理接口中
	 */
	protected abstract boolean includeOperation(Method method, String beanKey);

	/**
	 * 获取某个属性的描述。
	 * <p>默认实现返回该操作的描述，即对应 {@code Method} 的名称。
	 * @param propertyDescriptor 该属性的 PropertyDescriptor
	 * @param beanKey MBean 在 {@code MBeanExporter} 的 beans 映射中
	 * 关联的键
	 * @return 该属性的描述
	 */
	protected String getAttributeDescription(PropertyDescriptor propertyDescriptor, String beanKey) {
		return propertyDescriptor.getDisplayName();
	}

	/**
	 * 获取某个操作的描述。
	 * <p>默认实现返回该操作的描述，即对应 {@code Method} 的名称。
	 * @param method 操作方法
	 * @param beanKey MBean 在 {@code MBeanExporter} 的 beans 映射中
	 * 关联的键
	 * @return 该操作的描述
	 */
	protected String getOperationDescription(Method method, String beanKey) {
		return method.getName();
	}

	/**
	 * 为给定方法创建参数信息。
	 * <p>默认实现返回空的 {@code MBeanParameterInfo} 数组。
	 * @param method 要获取参数信息的 {@code Method}
	 * @param beanKey MBean 在 {@code MBeanExporter} 的 beans 映射中
	 * 关联的键
	 * @return {@code MBeanParameterInfo} 数组
	 */
	protected MBeanParameterInfo[] getOperationParameters(Method method, String beanKey) {
		ParameterNameDiscoverer paramNameDiscoverer = getParameterNameDiscoverer();
		String[] paramNames = (paramNameDiscoverer != null ? paramNameDiscoverer.getParameterNames(method) : null);
		if (paramNames == null) {
			return new MBeanParameterInfo[0];
		}

		MBeanParameterInfo[] info = new MBeanParameterInfo[paramNames.length];
		Class<?>[] typeParameters = method.getParameterTypes();
		for (int i = 0; i < info.length; i++) {
			info[i] = new MBeanParameterInfo(paramNames[i], typeParameters[i].getName(), paramNames[i]);
		}

		return info;
	}

	/**
	 * 允许子类为 MBean 的 {@code Descriptor} 添加额外字段。
	 * <p>默认实现将 {@code currencyTimeLimit} 字段设置为指定的 "defaultCurrencyTimeLimit"（如果有；默
	 * 认没有）。
	 * @param descriptor MBean 资源的 {@code Descriptor}。
	 * @param managedBean Bean 实例（可能是 AOP 代理）
	 * @param beanKey MBean 在 {@code MBeanExporter} 的 beans 映射中
	 * 关联的键
	 * @see #setDefaultCurrencyTimeLimit(Integer)
	 * @see #applyDefaultCurrencyTimeLimit(javax.management.Descriptor)
	 */
	@Override
	protected void populateMBeanDescriptor(Descriptor descriptor, Object managedBean, String beanKey) {
		applyDefaultCurrencyTimeLimit(descriptor);
	}

	/**
	 * 允许子类为某个属性的 {@code Descriptor} 添加额外字段。
	 * <p>默认实现将 {@code currencyTimeLimit} 字段设置为指定的 "defaultCurrencyTimeLimit"（如果有；默
	 * 认没有）。
	 * @param desc 属性描述符
	 * @param getter 该属性的访问器方法
	 * @param setter 该属性的修改器方法
	 * @param beanKey MBean 在 {@code MBeanExporter} 的 beans 映射中
	 * 关联的键
	 * @see #setDefaultCurrencyTimeLimit(Integer)
	 * @see #applyDefaultCurrencyTimeLimit(javax.management.Descriptor)
	 */
	protected void populateAttributeDescriptor(
			Descriptor desc, @Nullable Method getter, @Nullable Method setter, String beanKey) {

		applyDefaultCurrencyTimeLimit(desc);
	}

	/**
	 * 允许子类为某个操作的 {@code Descriptor} 添加额外字段。
	 * <p>默认实现将 {@code currencyTimeLimit} 字段设置为指定的 "defaultCurrencyTimeLimit"（如果有；默
	 * 认没有）。
	 * @param desc 操作描述符
	 * @param method 与该操作相对应的方法
	 * @param beanKey MBean 在 {@code MBeanExporter} 的 beans 映射中
	 * 关联的键
	 * @see #setDefaultCurrencyTimeLimit(Integer)
	 * @see #applyDefaultCurrencyTimeLimit(javax.management.Descriptor)
	 */
	protected void populateOperationDescriptor(Descriptor desc, Method method, String beanKey) {
		applyDefaultCurrencyTimeLimit(desc);
	}

	/**
	 * 将 {@code currencyTimeLimit} 字段设置为指定的 "defaultCurrencyTimeLimit"（如果有；默认没有）。
	 * @param desc JMX 属性或操作描述符
	 * @see #setDefaultCurrencyTimeLimit(Integer)
	 */
	protected final void applyDefaultCurrencyTimeLimit(Descriptor desc) {
		if (getDefaultCurrencyTimeLimit() != null) {
			desc.setField(FIELD_CURRENCY_TIME_LIMIT, getDefaultCurrencyTimeLimit().toString());
		}
	}

	/**
	 * 将给定的 JMX "currencyTimeLimit" 值应用于给定描述符。
	 * <p>默认实现将 {@code >0} 的值原样设置（作为缓存秒数），将 {@code 0} 的值转换为
	 * {@code Integer.MAX_VALUE}（“始终缓存”），并在值为 {@code <0} 时设置 "defaultCurrencyTimeLimit"
	 * （如果有，表示“从不缓存”）。这遵循 JMX 1.2 规范的建议。
	 * @param desc JMX 属性或操作描述符
	 * @param currencyTimeLimit 要应用的 "currencyTimeLimit" 值
	 * @see #setDefaultCurrencyTimeLimit(Integer)
	 * @see #applyDefaultCurrencyTimeLimit(javax.management.Descriptor)
	 */
	protected void applyCurrencyTimeLimit(Descriptor desc, int currencyTimeLimit) {
		if (currencyTimeLimit > 0) {
			// number of cache seconds
			desc.setField(FIELD_CURRENCY_TIME_LIMIT, Integer.toString(currencyTimeLimit));
		}
		else if (currencyTimeLimit == 0) {
			// "always cache"
			desc.setField(FIELD_CURRENCY_TIME_LIMIT, Integer.toString(Integer.MAX_VALUE));
		}
		else {
			// "never cache"
			applyDefaultCurrencyTimeLimit(desc);
		}
	}

}
