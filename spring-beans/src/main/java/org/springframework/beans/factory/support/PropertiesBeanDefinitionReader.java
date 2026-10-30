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

package org.springframework.beans.factory.support;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;

import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyAccessor;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.CannotLoadBeanClassException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.ResourcePropertiesPersister;
import org.springframework.lang.Nullable;
import org.springframework.util.PropertiesPersister;
import org.springframework.util.StringUtils;

/**
 * 用于简单 properties 格式的 Bean 定义读取器。
 *
 * <p>为 Map/Properties 和 ResourceBundle 提供 Bean 定义注册方法。通常应用于
 * DefaultListableBeanFactory。
 *
 * <p><b>示例：</b>
 *
 * <pre class="code">
 * employee.(class)=MyClass       // bean is of class MyClass
 * employee.(abstract)=true       // this bean can't be instantiated directly
 * employee.group=Insurance       // real property
 * employee.usesDialUp=false      // real property (potentially overridden)
 *
 * salesrep.(parent)=employee     // derives from "employee" bean definition
 * salesrep.(lazy-init)=true      // lazily initialize this singleton bean
 * salesrep.manager(ref)=tony     // reference to another bean
 * salesrep.department=Sales      // real property
 *
 * techie.(parent)=employee       // derives from "employee" bean definition
 * techie.(scope)=prototype       // bean is a prototype (not a shared instance)
 * techie.manager(ref)=jeff       // reference to another bean
 * techie.department=Engineering  // real property
 * techie.usesDialUp=true         // real property (overriding parent value)
 *
 * ceo.$0(ref)=secretary          // inject 'secretary' bean as 0th constructor arg
 * ceo.$1=1000000                 // inject value '1000000' at 1st constructor arg
 * </pre>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @since 26.11.2003
 * @see DefaultListableBeanFactory
 * @deprecated as of 5.3, in favor of Spring's common bean definition formats
 * and/or custom reader implementations
 */
@Deprecated
public class PropertiesBeanDefinitionReader extends AbstractBeanDefinitionReader {

	/**
	 * 表示 true 的 T/F 属性值。其他任何值表示 false。区分大小写。
	 */
	public static final String TRUE_VALUE = "true";

	/**
	 * Bean 名称与属性名称之间的分隔符。我们遵循常规的 Java 约定。
	 */
	public static final String SEPARATOR = ".";

	/**
	 * 用于区分 {@code owner.(class)=com.myapp.MyClass} 的特殊键。
	 */
	public static final String CLASS_KEY = "(class)";

	/**
	 * 用于区分 {@code owner.(parent)=parentBeanName} 的特殊键。
	 */
	public static final String PARENT_KEY = "(parent)";

	/**
	 * 用于区分 {@code owner.(scope)=prototype} 的特殊键。默认为 "true"。
	 */
	public static final String SCOPE_KEY = "(scope)";

	/**
	 * 用于区分 {@code owner.(singleton)=false} 的特殊键。默认为 "true"。
	 */
	public static final String SINGLETON_KEY = "(singleton)";

	/**
	 * 用于区分 {@code owner.(abstract)=true} 的特殊键。默认为 "false"。
	 */
	public static final String ABSTRACT_KEY = "(abstract)";

	/**
	 * 用于区分 {@code owner.(lazy-init)=true} 的特殊键。默认为 "false"。
	 */
	public static final String LAZY_INIT_KEY = "(lazy-init)";

	/**
	 * 当前 BeanFactory 中对其他 Bean 的引用所使用的属性后缀：例如 {@code owner.dog(ref)=fido}。这是对
	 * 单例还是原型的引用，取决于目标 Bean 的定义。
	 */
	public static final String REF_SUFFIX = "(ref)";

	/**
	 * 引用其他 Bean 的值前面的前缀。
	 */
	public static final String REF_PREFIX = "*";

	/**
	 * 用于表示构造器参数定义的前缀。
	 */
	public static final String CONSTRUCTOR_ARG_PREFIX = "$";


	@Nullable
	private String defaultParentBean;

	private PropertiesPersister propertiesPersister = ResourcePropertiesPersister.INSTANCE;


	/**
	 * 为给定 Bean 工厂创建新的 PropertiesBeanDefinitionReader。
	 * @param registry 要将 Bean 定义加载到的 BeanFactory，以 BeanDefinitionRegistry 的形式提供
	 */
	public PropertiesBeanDefinitionReader(BeanDefinitionRegistry registry) {
		super(registry);
	}


	/**
	 * 为此 Bean 工厂设置默认父 Bean。如果此工厂处理的子 Bean 定义既未提供 parent 属性也未提供 class 属
	 * 性，则使用此默认值。
	 * <p>例如，可用于视图定义文件，定义一个具有默认视图类和所有视图公共属性的父。定义了自己的父或带有
	 * 自己的类的视图定义仍可覆盖此项。
	 * <p>严格来说，默认父设置不适用于带有类的 Bean 定义这条规则是出于向后兼容的原因，它仍然符合典型的
	 * 使用场景。
	 */
	public void setDefaultParentBean(@Nullable String defaultParentBean) {
		this.defaultParentBean = defaultParentBean;
	}

	/**
	 * 返回此 Bean 工厂的默认父 Bean。
	 */
	@Nullable
	public String getDefaultParentBean() {
		return this.defaultParentBean;
	}

	/**
	 * 设置用于解析 properties 文件的 PropertiesPersister。默认为 ResourcePropertiesPersister。
	 * @see ResourcePropertiesPersister#INSTANCE
	 */
	public void setPropertiesPersister(@Nullable PropertiesPersister propertiesPersister) {
		this.propertiesPersister =
				(propertiesPersister != null ? propertiesPersister : ResourcePropertiesPersister.INSTANCE);
	}

	/**
	 * 返回用于解析 properties 文件的 PropertiesPersister。
	 */
	public PropertiesPersister getPropertiesPersister() {
		return this.propertiesPersister;
	}


	/**
	 * 从指定的 properties 文件加载 Bean 定义，使用所有属性键（即不按前缀过滤）。
	 * @param resource properties 文件的资源描述符
	 * @return 找到的 Bean 定义数量
	 * @throws BeanDefinitionStoreException 如果发生加载或解析错误
	 * @see #loadBeanDefinitions(org.springframework.core.io.Resource, String)
	 */
	@Override
	public int loadBeanDefinitions(Resource resource) throws BeanDefinitionStoreException {
		return loadBeanDefinitions(new EncodedResource(resource), null);
	}

	/**
	 * 从指定的 properties 文件加载 Bean 定义。
	 * @param resource properties 文件的资源描述符
	 * @param prefix 对 Map 中键的过滤器：例如 'beans.'（可以为空或 {@code null}）
	 * @return 找到的 Bean 定义数量
	 * @throws BeanDefinitionStoreException 如果发生加载或解析错误
	 */
	public int loadBeanDefinitions(Resource resource, @Nullable String prefix) throws BeanDefinitionStoreException {
		return loadBeanDefinitions(new EncodedResource(resource), prefix);
	}

	/**
	 * 从指定的 properties 文件加载 Bean 定义。
	 * @param encodedResource properties 文件的资源描述符，允许指定解析该文件时使用的编码
	 * @return 找到的 Bean 定义数量
	 * @throws BeanDefinitionStoreException 如果发生加载或解析错误
	 */
	public int loadBeanDefinitions(EncodedResource encodedResource) throws BeanDefinitionStoreException {
		return loadBeanDefinitions(encodedResource, null);
	}

	/**
	 * 从指定的 properties 文件加载 Bean 定义。
	 * @param encodedResource properties 文件的资源描述符，允许指定解析该文件时使用的编码
	 * @param prefix 对 Map 中键的过滤器：例如 'beans.'（可以为空或 {@code null}）
	 * @return 找到的 Bean 定义数量
	 * @throws BeanDefinitionStoreException 如果发生加载或解析错误
	 */
	public int loadBeanDefinitions(EncodedResource encodedResource, @Nullable String prefix)
			throws BeanDefinitionStoreException {

		if (logger.isTraceEnabled()) {
			logger.trace("Loading properties bean definitions from " + encodedResource);
		}

		Properties props = new Properties();
		try {
			try (InputStream is = encodedResource.getResource().getInputStream()) {
				if (encodedResource.getEncoding() != null) {
					getPropertiesPersister().load(props, new InputStreamReader(is, encodedResource.getEncoding()));
				}
				else {
					getPropertiesPersister().load(props, is);
				}
			}

			int count = registerBeanDefinitions(props, prefix, encodedResource.getResource().getDescription());
			if (logger.isDebugEnabled()) {
				logger.debug("Loaded " + count + " bean definitions from " + encodedResource);
			}
			return count;
		}
		catch (IOException ex) {
			throw new BeanDefinitionStoreException("Could not parse properties from " + encodedResource.getResource(), ex);
		}
	}

	/**
	 * 注册资源包中包含的 Bean 定义，使用所有属性键（即不按前缀过滤）。
	 * @param rb 要从中加载的 ResourceBundle
	 * @return 找到的 Bean 定义数量
	 * @throws BeanDefinitionStoreException 如果发生加载或解析错误
	 * @see #registerBeanDefinitions(java.util.ResourceBundle, String)
	 */
	public int registerBeanDefinitions(ResourceBundle rb) throws BeanDefinitionStoreException {
		return registerBeanDefinitions(rb, null);
	}

	/**
	 * 注册 ResourceBundle 中包含的 Bean 定义。
	 * <p>语法与 Map 类似。此方法有助于启用标准 Java 国际化支持。
	 * @param rb 要从中加载的 ResourceBundle
	 * @param prefix 对 Map 中键的过滤器：例如 'beans.'（可以为空或 {@code null}）
	 * @return 找到的 Bean 定义数量
	 * @throws BeanDefinitionStoreException 如果发生加载或解析错误
	 */
	public int registerBeanDefinitions(ResourceBundle rb, @Nullable String prefix) throws BeanDefinitionStoreException {
		// Simply create a map and call overloaded method.
		Map<String, Object> map = new HashMap<>();
		Enumeration<String> keys = rb.getKeys();
		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			map.put(key, rb.getObject(key));
		}
		return registerBeanDefinitions(map, prefix);
	}


	/**
	 * 注册 Map 中包含的 Bean 定义，使用所有属性键（即不按前缀过滤）。
	 * @param map 从 {@code name} 到 {@code property}（String 或 Object）的映射。
	 * 如果来自 Properties 文件等，属性值将是字符串。
	 * 属性名称（键）<b>必须</b>是 String。类键必须是 String。
	 * @return 找到的 Bean 定义数量
	 * @throws BeansException 如果发生加载或解析错误
	 * @see #registerBeanDefinitions(java.util.Map, String, String)
	 */
	public int registerBeanDefinitions(Map<?, ?> map) throws BeansException {
		return registerBeanDefinitions(map, null);
	}

	/**
	 * 注册 Map 中包含的 Bean 定义。忽略不合格的属性。
	 * @param map 从 {@code name} 到 {@code property}（String 或 Object）的映射。
	 * 如果来自 Properties 文件等，属性值将是字符串。
	 * 属性名称（键）<b>必须</b>是 String。类键必须是 String。
	 * @param prefix 对 Map 中键的过滤器：例如 'beans.'（可以为空或 {@code null}）
	 * @return 找到的 Bean 定义数量
	 * @throws BeansException 如果发生加载或解析错误
	 */
	public int registerBeanDefinitions(Map<?, ?> map, @Nullable String prefix) throws BeansException {
		return registerBeanDefinitions(map, prefix, "Map " + map);
	}

	/**
	 * 注册 Map 中包含的 Bean 定义。忽略不合格的属性。
	 * @param map 从 {@code name} 到 {@code property}（String 或 Object）的映射。
	 * 如果来自 Properties 文件等，属性值将是字符串。
	 * 属性名称（键）<b>必须</b>是 String。类键必须是 String。
	 * @param prefix 对 Map 中键的过滤器：例如 'beans.'（可以为空或 {@code null}）
	 * @param resourceDescription 该 Map 来源资源的描述（用于日志记录）
	 * @return 找到的 Bean 定义数量
	 * @throws BeansException 如果发生加载或解析错误
	 * @see #registerBeanDefinitions(Map, String)
	 */
	public int registerBeanDefinitions(Map<?, ?> map, @Nullable String prefix, String resourceDescription)
			throws BeansException {

		if (prefix == null) {
			prefix = "";
		}
		int beanCount = 0;

		for (Object key : map.keySet()) {
			if (!(key instanceof String)) {
				throw new IllegalArgumentException("Illegal key [" + key + "]: only Strings allowed");
			}
			String keyString = (String) key;
			if (keyString.startsWith(prefix)) {
				// Key is of form: prefix<name>.property
				String nameAndProperty = keyString.substring(prefix.length());
				// Find dot before property name, ignoring dots in property keys.
				int sepIdx ;
				int propKeyIdx = nameAndProperty.indexOf(PropertyAccessor.PROPERTY_KEY_PREFIX);
				if (propKeyIdx != -1) {
					sepIdx = nameAndProperty.lastIndexOf(SEPARATOR, propKeyIdx);
				}
				else {
					sepIdx = nameAndProperty.lastIndexOf(SEPARATOR);
				}
				if (sepIdx != -1) {
					String beanName = nameAndProperty.substring(0, sepIdx);
					if (logger.isTraceEnabled()) {
						logger.trace("Found bean name '" + beanName + "'");
					}
					if (!getRegistry().containsBeanDefinition(beanName)) {
						// If we haven't already registered it...
						registerBeanDefinition(beanName, map, prefix + beanName, resourceDescription);
						++beanCount;
					}
				}
				else {
					// Ignore it: It wasn't a valid bean name and property,
					// although it did start with the required prefix.
					if (logger.isDebugEnabled()) {
						logger.debug("Invalid bean name and property [" + nameAndProperty + "]");
					}
				}
			}
		}

		return beanCount;
	}

	/**
	 * 获取所有属性值（给定前缀将被去除），并将它们定义的 Bean 以给定名称添加到工厂中。
	 * @param beanName 要定义的 Bean 的名称
	 * @param map 包含字符串对的 Map
	 * @param prefix 每个条目的前缀，该前缀将被去除
	 * @param resourceDescription 该 Map 来源资源的描述（用于日志记录）
	 * @throws BeansException 如果无法解析或注册 Bean 定义
	 */
	protected void registerBeanDefinition(String beanName, Map<?, ?> map, String prefix, String resourceDescription)
			throws BeansException {

		String className = null;
		String parent = null;
		String scope = BeanDefinition.SCOPE_SINGLETON;
		boolean isAbstract = false;
		boolean lazyInit = false;

		ConstructorArgumentValues cas = new ConstructorArgumentValues();
		MutablePropertyValues pvs = new MutablePropertyValues();

		String prefixWithSep = prefix + SEPARATOR;
		int beginIndex = prefixWithSep.length();

		for (Map.Entry<?, ?> entry : map.entrySet()) {
			String key = StringUtils.trimWhitespace((String) entry.getKey());
			if (key.startsWith(prefixWithSep)) {
				String property = key.substring(beginIndex);
				if (CLASS_KEY.equals(property)) {
					className = StringUtils.trimWhitespace((String) entry.getValue());
				}
				else if (PARENT_KEY.equals(property)) {
					parent = StringUtils.trimWhitespace((String) entry.getValue());
				}
				else if (ABSTRACT_KEY.equals(property)) {
					String val = StringUtils.trimWhitespace((String) entry.getValue());
					isAbstract = TRUE_VALUE.equals(val);
				}
				else if (SCOPE_KEY.equals(property)) {
					// Spring 2.0 style
					scope = StringUtils.trimWhitespace((String) entry.getValue());
				}
				else if (SINGLETON_KEY.equals(property)) {
					// Spring 1.2 style
					String val = StringUtils.trimWhitespace((String) entry.getValue());
					scope = (!StringUtils.hasLength(val) || TRUE_VALUE.equals(val) ?
							BeanDefinition.SCOPE_SINGLETON : BeanDefinition.SCOPE_PROTOTYPE);
				}
				else if (LAZY_INIT_KEY.equals(property)) {
					String val = StringUtils.trimWhitespace((String) entry.getValue());
					lazyInit = TRUE_VALUE.equals(val);
				}
				else if (property.startsWith(CONSTRUCTOR_ARG_PREFIX)) {
					if (property.endsWith(REF_SUFFIX)) {
						int index = Integer.parseInt(property.substring(1, property.length() - REF_SUFFIX.length()));
						cas.addIndexedArgumentValue(index, new RuntimeBeanReference(entry.getValue().toString()));
					}
					else {
						int index = Integer.parseInt(property.substring(1));
						cas.addIndexedArgumentValue(index, readValue(entry));
					}
				}
				else if (property.endsWith(REF_SUFFIX)) {
					// This isn't a real property, but a reference to another prototype
					// Extract property name: property is of form dog(ref)
					property = property.substring(0, property.length() - REF_SUFFIX.length());
					String ref = StringUtils.trimWhitespace((String) entry.getValue());

					// It doesn't matter if the referenced bean hasn't yet been registered:
					// this will ensure that the reference is resolved at runtime.
					Object val = new RuntimeBeanReference(ref);
					pvs.add(property, val);
				}
				else {
					// It's a normal bean property.
					pvs.add(property, readValue(entry));
				}
			}
		}

		if (logger.isTraceEnabled()) {
			logger.trace("Registering bean definition for bean name '" + beanName + "' with " + pvs);
		}

		// Just use default parent if we're not dealing with the parent itself,
		// and if there's no class name specified. The latter has to happen for
		// backwards compatibility reasons.
		if (parent == null && className == null && !beanName.equals(this.defaultParentBean)) {
			parent = this.defaultParentBean;
		}

		try {
			AbstractBeanDefinition bd = BeanDefinitionReaderUtils.createBeanDefinition(
					parent, className, getBeanClassLoader());
			bd.setScope(scope);
			bd.setAbstract(isAbstract);
			bd.setLazyInit(lazyInit);
			bd.setConstructorArgumentValues(cas);
			bd.setPropertyValues(pvs);
			getRegistry().registerBeanDefinition(beanName, bd);
		}
		catch (ClassNotFoundException ex) {
			throw new CannotLoadBeanClassException(resourceDescription, beanName, className, ex);
		}
		catch (LinkageError err) {
			throw new CannotLoadBeanClassException(resourceDescription, beanName, className, err);
		}
	}

	/**
	 * 读取条目的值。对于以星号开头的值，会正确解释为 Bean 引用。
	 */
	private Object readValue(Map.Entry<?, ?> entry) {
		Object val = entry.getValue();
		if (val instanceof String) {
			String strVal = (String) val;
			// If it starts with a reference prefix...
			if (strVal.startsWith(REF_PREFIX)) {
				// Expand the reference.
				String targetName = strVal.substring(1);
				if (targetName.startsWith(REF_PREFIX)) {
					// Escaped prefix -> use plain value.
					val = targetName;
				}
				else {
					val = new RuntimeBeanReference(targetName);
				}
			}
		}
		return val;
	}

}
