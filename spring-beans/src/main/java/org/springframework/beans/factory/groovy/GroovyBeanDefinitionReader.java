/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.beans.factory.groovy;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.GString;
import groovy.lang.GroovyObject;
import groovy.lang.GroovyObjectSupport;
import groovy.lang.GroovyShell;
import groovy.lang.GroovySystem;
import groovy.lang.MetaClass;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.InvokerHelper;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.beans.factory.parsing.Location;
import org.springframework.beans.factory.parsing.Problem;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinitionReader;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.NamespaceHandler;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.springframework.core.io.DescriptiveResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * 基于 Groovy 的 Spring Bean 定义读取器：类似 Groovy builder，但更像是用于 Spring 配置的 DSL。
 *
 * <p>该 Bean 定义读取器也支持 XML Bean 定义文件，从而可以与 Groovy Bean 定义文件无缝混合搭配使用。
 *
 * <p>通常应用于 {@link org.springframework.beans.factory.support.DefaultListableBeanFactory} 或
 * {@link org.springframework.context.support.GenericApplicationContext}，但也可以用于任何
 * {@link BeanDefinitionRegistry} 实现。
 *
 * <h3>示例语法</h3>
 * <pre class="code">
 * import org.hibernate.SessionFactory
 * import org.apache.commons.dbcp.BasicDataSource
 *
 * def reader = new GroovyBeanDefinitionReader(myApplicationContext)
 * reader.beans {
 *     dataSource(BasicDataSource) {                  // &lt;--- invokeMethod
 *         driverClassName = "org.hsqldb.jdbcDriver"
 *         url = "jdbc:hsqldb:mem:grailsDB"
 *         username = "sa"                            // &lt;-- setProperty
 *         password = ""
 *         settings = [mynew:"setting"]
 *     }
 *     sessionFactory(SessionFactory) {
 *         dataSource = dataSource                    // &lt;-- getProperty for retrieving references
 *     }
 *     myService(MyService) {
 *         nestedBean = { AnotherBean bean -&gt;         // &lt;-- setProperty with closure for nested bean
 *             dataSource = dataSource
 *         }
 *     }
 * }</pre>
 *
 * <p>还可以使用 {@link #loadBeanDefinitions(Resource...)} 或
 * {@link #loadBeanDefinitions(String...)} 方法加载包含 Groovy 脚本中所定义 Bean 的资源，脚本与下面
 * 的示例类似。
 *
 * <pre class="code">
 * import org.hibernate.SessionFactory
 * import org.apache.commons.dbcp.BasicDataSource
 *
 * beans {
 *     dataSource(BasicDataSource) {
 *         driverClassName = "org.hsqldb.jdbcDriver"
 *         url = "jdbc:hsqldb:mem:grailsDB"
 *         username = "sa"
 *         password = ""
 *         settings = [mynew:"setting"]
 *     }
 *     sessionFactory(SessionFactory) {
 *         dataSource = dataSource
 *     }
 *     myService(MyService) {
 *         nestedBean = { AnotherBean bean -&gt;
 *             dataSource = dataSource
 *         }
 *     }
 * }</pre>
 *
 * @author Jeff Brown
 * @author Graeme Rocher
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 4.0
 * @see BeanDefinitionRegistry
 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory
 * @see org.springframework.context.support.GenericApplicationContext
 * @see org.springframework.context.support.GenericGroovyApplicationContext
 */
public class GroovyBeanDefinitionReader extends AbstractBeanDefinitionReader implements GroovyObject {

	/**
	 * 使用默认设置创建的标准 {@code XmlBeanDefinitionReader}，用于从 XML 文件加载 Bean 定义。
	 */
	private final XmlBeanDefinitionReader standardXmlBeanDefinitionReader;

	/**
	 * 用于通过 Groovy DSL 加载 Bean 定义的 Groovy DSL {@code XmlBeanDefinitionReader}，通常配置为禁用
	 * XML 校验。
	 */
	private final XmlBeanDefinitionReader groovyDslXmlBeanDefinitionReader;

	private final Map<String, String> namespaces = new HashMap<>();

	private final Map<String, DeferredProperty> deferredProperties = new HashMap<>();

	private MetaClass metaClass = GroovySystem.getMetaClassRegistry().getMetaClass(getClass());

	private Binding binding;

	private GroovyBeanDefinitionWrapper currentBeanDefinition;


	/**
	 * 为给定的 {@link BeanDefinitionRegistry} 创建一个新的 {@code GroovyBeanDefinitionReader}。
	 * @param registry 用于加载 Bean 定义的 {@code BeanDefinitionRegistry}
	 */
	public GroovyBeanDefinitionReader(BeanDefinitionRegistry registry) {
		super(registry);
		this.standardXmlBeanDefinitionReader = new XmlBeanDefinitionReader(registry);
		this.groovyDslXmlBeanDefinitionReader = new XmlBeanDefinitionReader(registry);
		this.groovyDslXmlBeanDefinitionReader.setValidating(false);
	}

	/**
	 * 基于给定的 {@link XmlBeanDefinitionReader} 创建一个新的 {@code GroovyBeanDefinitionReader}，将
	 * Bean 定义加载到其 {@code BeanDefinitionRegistry} 中，并将 Groovy DSL 加载委托给它。
	 *
	 * <p>所提供的 {@code XmlBeanDefinitionReader} 通常应预先配置为禁用 XML 校验。
	 * @param xmlBeanDefinitionReader 用于从中获取注册表并委托 Groovy DSL 加载的
	 *  {@code XmlBeanDefinitionReader}
	 */
	public GroovyBeanDefinitionReader(XmlBeanDefinitionReader xmlBeanDefinitionReader) {
		super(xmlBeanDefinitionReader.getRegistry());
		this.standardXmlBeanDefinitionReader = new XmlBeanDefinitionReader(xmlBeanDefinitionReader.getRegistry());
		this.groovyDslXmlBeanDefinitionReader = xmlBeanDefinitionReader;
	}


	@Override
	public void setMetaClass(MetaClass metaClass) {
		this.metaClass = metaClass;
	}

	@Override
	public MetaClass getMetaClass() {
		return this.metaClass;
	}

	/**
	 * 设置绑定，即在 {@code GroovyBeanDefinitionReader} 闭包作用域内可用的 Groovy 变量。
	 */
	public void setBinding(Binding binding) {
		this.binding = binding;
	}

	/**
	 * 返回指定的 Groovy 变量绑定（如果有）。
	 */
	public Binding getBinding() {
		return this.binding;
	}


	// TRADITIONAL BEAN DEFINITION READER METHODS

	/**
	 * 从指定的 Groovy 脚本或 XML 文件加载 Bean 定义。
	 *
	 * <p>请注意，{@code ".xml"} 文件将按 XML 内容解析；所有其他类型的资源将按 Groovy 脚本解析。
	 * @param resource Groovy 脚本或 XML 文件的资源描述符
	 * @return 找到的 Bean 定义数量
	 * @throws BeanDefinitionStoreException 在加载或解析出错时
	 */
	@Override
	public int loadBeanDefinitions(Resource resource) throws BeanDefinitionStoreException {
		return loadBeanDefinitions(new EncodedResource(resource));
	}

	/**
	 * 从指定的 Groovy 脚本或 XML 文件加载 Bean 定义。
	 *
	 * <p>请注意，{@code ".xml"} 文件将按 XML 内容解析；所有其他类型的资源将按 Groovy 脚本解析。
	 * @param encodedResource Groovy 脚本或 XML 文件的资源描述符，可指定用于解析该文件的编码
	 * @return 找到的 Bean 定义数量
	 * @throws BeanDefinitionStoreException 在加载或解析出错时
	 */
	public int loadBeanDefinitions(EncodedResource encodedResource) throws BeanDefinitionStoreException {
		// Check for XML files and redirect them to the "standard" XmlBeanDefinitionReader
		String filename = encodedResource.getResource().getFilename();
		if (StringUtils.endsWithIgnoreCase(filename, ".xml")) {
			return this.standardXmlBeanDefinitionReader.loadBeanDefinitions(encodedResource);
		}

		if (logger.isTraceEnabled()) {
			logger.trace("Loading Groovy bean definitions from " + encodedResource);
		}

		@SuppressWarnings("serial")
		Closure<Object> beans = new Closure<Object>(this) {
			@Override
			public Object call(Object... args) {
				invokeBeanDefiningClosure((Closure<?>) args[0]);
				return null;
			}
		};
		Binding binding = new Binding() {
			@Override
			public void setVariable(String name, Object value) {
				if (currentBeanDefinition != null) {
					applyPropertyToBeanDefinition(name, value);
				}
				else {
					super.setVariable(name, value);
				}
			}
		};
		binding.setVariable("beans", beans);

		int countBefore = getRegistry().getBeanDefinitionCount();
		try {
			GroovyShell shell = new GroovyShell(getBeanClassLoader(), binding);
			shell.evaluate(encodedResource.getReader(), "beans");
		}
		catch (Throwable ex) {
			throw new BeanDefinitionParsingException(new Problem("Error evaluating Groovy script: " + ex.getMessage(),
					new Location(encodedResource.getResource()), null, ex));
		}

		int count = getRegistry().getBeanDefinitionCount() - countBefore;
		if (logger.isDebugEnabled()) {
			logger.debug("Loaded " + count + " bean definitions from " + encodedResource);
		}
		return count;
	}


	// METHODS FOR CONSUMPTION IN A GROOVY CLOSURE

	/**
	 * 为给定的 block 或闭包定义一组 Bean。
	 * @param closure 该代码块或闭包
	 * @return 该 {@code GroovyBeanDefinitionReader} 实例
	 */
	public GroovyBeanDefinitionReader beans(Closure<?> closure) {
		return invokeBeanDefiningClosure(closure);
	}

	/**
	 * 定义一个内部 Bean 定义。
	 * @param type Bean 类型
	 * @return Bean 定义
	 */
	public GenericBeanDefinition bean(Class<?> type) {
		GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
		beanDefinition.setBeanClass(type);
		return beanDefinition;
	}

	/**
	 * 定义一个内部 Bean 定义。
	 * @param type Bean 类型
	 * @param args 构造器参数与闭包配置器
	 * @return Bean 定义
	 */
	public AbstractBeanDefinition bean(Class<?> type, Object...args) {
		GroovyBeanDefinitionWrapper current = this.currentBeanDefinition;
		try {
			Closure<?> callable = null;
			Collection<Object> constructorArgs = null;
			if (!ObjectUtils.isEmpty(args)) {
				int index = args.length;
				Object lastArg = args[index - 1];
				if (lastArg instanceof Closure<?>) {
					callable = (Closure<?>) lastArg;
					index--;
				}
				constructorArgs = resolveConstructorArguments(args, 0, index);
			}
			this.currentBeanDefinition = new GroovyBeanDefinitionWrapper(null, type, constructorArgs);
			if (callable != null) {
				callable.call(this.currentBeanDefinition);
			}
			return this.currentBeanDefinition.getBeanDefinition();
		}
		finally {
			this.currentBeanDefinition = current;
		}
	}

	/**
	 * 定义要使用的 Spring XML 命名空间定义。
	 * @param definition 命名空间定义
	 */
	public void xmlns(Map<String, String> definition) {
		if (!definition.isEmpty()) {
			for (Map.Entry<String,String> entry : definition.entrySet()) {
				String namespace = entry.getKey();
				String uri = entry.getValue();
				if (uri == null) {
					throw new IllegalArgumentException("Namespace definition must supply a non-null URI");
				}
				NamespaceHandler namespaceHandler =
						this.groovyDslXmlBeanDefinitionReader.getNamespaceHandlerResolver().resolve(uri);
				if (namespaceHandler == null) {
					throw new BeanDefinitionParsingException(new Problem("No namespace handler found for URI: " + uri,
							new Location(new DescriptiveResource(("Groovy")))));
				}
				this.namespaces.put(namespace, uri);
			}
		}
	}

	/**
	 * 从 XML 或 Groovy 源中将 Spring Bean 定义导入当前 Bean 构建器实例。
	 * @param resourcePattern 资源模式
	 */
	public void importBeans(String resourcePattern) throws IOException {
		loadBeanDefinitions(resourcePattern);
	}


	// INTERNAL HANDLING OF GROOVY CLOSURES AND PROPERTIES

	/**
	 * 此方法重写方法调用，以便为每个接受 class 参数的方法名创建 Bean。
	 */
	@Override
	public Object invokeMethod(String name, Object arg) {
		Object[] args = (Object[])arg;
		if ("beans".equals(name) && args.length == 1 && args[0] instanceof Closure) {
			return beans((Closure<?>) args[0]);
		}
		else if ("ref".equals(name)) {
			String refName;
			if (args[0] == null) {
				throw new IllegalArgumentException("Argument to ref() is not a valid bean or was not found");
			}
			if (args[0] instanceof RuntimeBeanReference) {
				refName = ((RuntimeBeanReference) args[0]).getBeanName();
			}
			else {
				refName = args[0].toString();
			}
			boolean parentRef = false;
			if (args.length > 1 && args[1] instanceof Boolean) {
				parentRef = (Boolean) args[1];
			}
			return new RuntimeBeanReference(refName, parentRef);
		}
		else if (this.namespaces.containsKey(name) && args.length > 0 && args[0] instanceof Closure) {
			GroovyDynamicElementReader reader = createDynamicElementReader(name);
			reader.invokeMethod("doCall", args);
		}
		else if (args.length > 0 && args[0] instanceof Closure) {
			// abstract bean definition
			return invokeBeanDefiningMethod(name, args);
		}
		else if (args.length > 0 &&
				(args[0] instanceof Class || args[0] instanceof RuntimeBeanReference || args[0] instanceof Map)) {
			return invokeBeanDefiningMethod(name, args);
		}
		else if (args.length > 1 && args[args.length -1] instanceof Closure) {
			return invokeBeanDefiningMethod(name, args);
		}
		MetaClass mc = DefaultGroovyMethods.getMetaClass(getRegistry());
		if (!mc.respondsTo(getRegistry(), name, args).isEmpty()){
			return mc.invokeMethod(getRegistry(), name, args);
		}
		return this;
	}

	private boolean addDeferredProperty(String property, Object newValue) {
		if (newValue instanceof List || newValue instanceof Map) {
			this.deferredProperties.put(this.currentBeanDefinition.getBeanName() + '.' + property,
					new DeferredProperty(this.currentBeanDefinition, property, newValue));
			return true;
		}
		return false;
	}

	private void finalizeDeferredProperties() {
		for (DeferredProperty dp : this.deferredProperties.values()) {
			if (dp.value instanceof List) {
				dp.value = manageListIfNecessary((List<?>) dp.value);
			}
			else if (dp.value instanceof Map) {
				dp.value = manageMapIfNecessary((Map<?, ?>) dp.value);
			}
			dp.apply();
		}
		this.deferredProperties.clear();
	}

	/**
	 * 当方法参数仅为闭包时，它是一组 Bean 定义。
	 * @param callable 闭包参数
	 * @return 该 {@code GroovyBeanDefinitionReader} 实例
	 */
	protected GroovyBeanDefinitionReader invokeBeanDefiningClosure(Closure<?> callable) {
		callable.setDelegate(this);
		callable.call();
		finalizeDeferredProperties();
		return this;
	}

	/**
	 * 当调用 Bean 定义节点时会调用此方法。
	 * @param beanName 要定义的 Bean 的名称
	 * @param args 传递给该 Bean 的参数。第一个参数是类名，最后一个参数有时是闭包。
	 * 其间的所有参数都是构造器参数。
	 * @return Bean 定义包装器
	 */
	private GroovyBeanDefinitionWrapper invokeBeanDefiningMethod(String beanName, Object[] args) {
		boolean hasClosureArgument = (args[args.length - 1] instanceof Closure);
		if (args[0] instanceof Class) {
			Class<?> beanClass = (Class<?>) args[0];
			if (hasClosureArgument) {
				if (args.length - 1 != 1) {
					this.currentBeanDefinition = new GroovyBeanDefinitionWrapper(
							beanName, beanClass, resolveConstructorArguments(args, 1, args.length - 1));
				}
				else {
					this.currentBeanDefinition = new GroovyBeanDefinitionWrapper(beanName, beanClass);
				}
			}
			else  {
				this.currentBeanDefinition = new GroovyBeanDefinitionWrapper(
						beanName, beanClass, resolveConstructorArguments(args, 1, args.length));
			}
		}
		else if (args[0] instanceof RuntimeBeanReference) {
			this.currentBeanDefinition = new GroovyBeanDefinitionWrapper(beanName);
			this.currentBeanDefinition.getBeanDefinition().setFactoryBeanName(((RuntimeBeanReference) args[0]).getBeanName());
		}
		else if (args[0] instanceof Map) {
			// named constructor arguments
			if (args.length > 1 && args[1] instanceof Class) {
				List<Object> constructorArgs =
						resolveConstructorArguments(args, 2, hasClosureArgument ? args.length - 1 : args.length);
				this.currentBeanDefinition = new GroovyBeanDefinitionWrapper(beanName, (Class<?>) args[1], constructorArgs);
				Map<?, ?> namedArgs = (Map<?, ?>) args[0];
				for (Map.Entry<?, ?> entity : namedArgs.entrySet()) {
					String propName = (String) entity.getKey();
					setProperty(propName, entity.getValue());
				}
			}
			// factory method syntax
			else {
				this.currentBeanDefinition = new GroovyBeanDefinitionWrapper(beanName);
				// First arg is the map containing factoryBean : factoryMethod
				Map.Entry<?, ?> factoryBeanEntry = ((Map<?, ?>) args[0]).entrySet().iterator().next();
				// If we have a closure body, that will be the last argument.
				// In between are the constructor args
				int constructorArgsTest = (hasClosureArgument ? 2 : 1);
				// If we have more than this number of args, we have constructor args
				if (args.length > constructorArgsTest){
					// factory-method requires args
					int endOfConstructArgs = (hasClosureArgument ? args.length - 1 : args.length);
					this.currentBeanDefinition = new GroovyBeanDefinitionWrapper(beanName, null,
							resolveConstructorArguments(args, 1, endOfConstructArgs));
				}
				else {
					this.currentBeanDefinition = new GroovyBeanDefinitionWrapper(beanName);
				}
				this.currentBeanDefinition.getBeanDefinition().setFactoryBeanName(factoryBeanEntry.getKey().toString());
				this.currentBeanDefinition.getBeanDefinition().setFactoryMethodName(factoryBeanEntry.getValue().toString());
			}

		}
		else if (args[0] instanceof Closure) {
			this.currentBeanDefinition = new GroovyBeanDefinitionWrapper(beanName);
			this.currentBeanDefinition.getBeanDefinition().setAbstract(true);
		}
		else {
			List<Object> constructorArgs =
					resolveConstructorArguments(args, 0, hasClosureArgument ? args.length - 1 : args.length);
			this.currentBeanDefinition = new GroovyBeanDefinitionWrapper(beanName, null, constructorArgs);
		}

		if (hasClosureArgument) {
			Closure<?> callable = (Closure<?>) args[args.length - 1];
			callable.setDelegate(this);
			callable.setResolveStrategy(Closure.DELEGATE_FIRST);
			callable.call(this.currentBeanDefinition);
		}

		GroovyBeanDefinitionWrapper beanDefinition = this.currentBeanDefinition;
		this.currentBeanDefinition = null;
		beanDefinition.getBeanDefinition().setAttribute(GroovyBeanDefinitionWrapper.class.getName(), beanDefinition);
		getRegistry().registerBeanDefinition(beanName, beanDefinition.getBeanDefinition());
		return beanDefinition;
	}

	protected List<Object> resolveConstructorArguments(Object[] args, int start, int end) {
		Object[] constructorArgs = Arrays.copyOfRange(args, start, end);
		for (int i = 0; i < constructorArgs.length; i++) {
			if (constructorArgs[i] instanceof GString) {
				constructorArgs[i] = constructorArgs[i].toString();
			}
			else if (constructorArgs[i] instanceof List) {
				constructorArgs[i] = manageListIfNecessary((List<?>) constructorArgs[i]);
			}
			else if (constructorArgs[i] instanceof Map){
				constructorArgs[i] = manageMapIfNecessary((Map<?, ?>) constructorArgs[i]);
			}
		}
		return Arrays.asList(constructorArgs);
	}

	/**
	 * 检查 {@link Map} 中是否存在 {@link RuntimeBeanReference RuntimeBeanReferences}，如有必要将其转换
	 * 为 {@link ManagedMap}。
	 * @param map 原始 Map
	 * @return 原始映射或其托管副本
	 */
	private Object manageMapIfNecessary(Map<?, ?> map) {
		boolean containsRuntimeRefs = false;
		for (Object element : map.values()) {
			if (element instanceof RuntimeBeanReference) {
				containsRuntimeRefs = true;
				break;
			}
		}
		if (containsRuntimeRefs) {
			Map<Object, Object> managedMap = new ManagedMap<>();
			managedMap.putAll(map);
			return managedMap;
		}
		return map;
	}

	/**
	 * 检查 {@link List} 中是否存在 {@link RuntimeBeanReference RuntimeBeanReferences}，如有必要将其转
	 * 换为 {@link ManagedList}。
	 * @param list 原始 List
	 * @return 原始列表或其托管副本
	 */
	private Object manageListIfNecessary(List<?> list) {
		boolean containsRuntimeRefs = false;
		for (Object element : list) {
			if (element instanceof RuntimeBeanReference) {
				containsRuntimeRefs = true;
				break;
			}
		}
		if (containsRuntimeRefs) {
			List<Object> managedList = new ManagedList<>();
			managedList.addAll(list);
			return managedList;
		}
		return list;
	}

	/**
	 * 此方法重写 {@code GroovyBeanDefinitionReader} 作用域内的属性设置，以在当前 Bean 定义上设置属性。
	 */
	@Override
	public void setProperty(String name, Object value) {
		if (this.currentBeanDefinition != null) {
			applyPropertyToBeanDefinition(name, value);
		}
	}

	protected void applyPropertyToBeanDefinition(String name, Object value) {
		if (value instanceof GString) {
			value = value.toString();
		}
		if (addDeferredProperty(name, value)) {
			return;
		}
		else if (value instanceof Closure) {
			GroovyBeanDefinitionWrapper current = this.currentBeanDefinition;
			try {
				Closure<?> callable = (Closure<?>) value;
				Class<?> parameterType = callable.getParameterTypes()[0];
				if (Object.class == parameterType) {
					this.currentBeanDefinition = new GroovyBeanDefinitionWrapper("");
					callable.call(this.currentBeanDefinition);
				}
				else {
					this.currentBeanDefinition = new GroovyBeanDefinitionWrapper(null, parameterType);
					callable.call((Object) null);
				}

				value = this.currentBeanDefinition.getBeanDefinition();
			}
			finally {
				this.currentBeanDefinition = current;
			}
		}
		this.currentBeanDefinition.addProperty(name, value);
	}

	/**
	 * 此方法重写 {@code GroovyBeanDefinitionReader} 作用域内的属性获取。属性获取将执行以下操作之一：
	 * <ul>
	 * <li>如果 bean builder 的绑定中存在变量，则从该绑定中获取变量
	 * <li>如果存在特定 Bean，则获取其对应的 RuntimeBeanReference
	 * <li>否则直接委托给 MetaClass.getProperty，由它从 {@code GroovyBeanDefinitionReader} 本身解析属性
	 * </ul>
	 */
	@Override
	public Object getProperty(String name) {
		Binding binding = getBinding();
		if (binding != null && binding.hasVariable(name)) {
			return binding.getVariable(name);
		}
		else {
			if (this.namespaces.containsKey(name)) {
				return createDynamicElementReader(name);
			}
			if (getRegistry().containsBeanDefinition(name)) {
				GroovyBeanDefinitionWrapper beanDefinition = (GroovyBeanDefinitionWrapper)
						getRegistry().getBeanDefinition(name).getAttribute(GroovyBeanDefinitionWrapper.class.getName());
				if (beanDefinition != null) {
					return new GroovyRuntimeBeanReference(name, beanDefinition, false);
				}
				else {
					return new RuntimeBeanReference(name, false);
				}
			}
			// This is to deal with the case where the property setter is the last
			// statement in a closure (hence the return value)
			else if (this.currentBeanDefinition != null) {
				MutablePropertyValues pvs = this.currentBeanDefinition.getBeanDefinition().getPropertyValues();
				if (pvs.contains(name)) {
					return pvs.get(name);
				}
				else {
					DeferredProperty dp = this.deferredProperties.get(this.currentBeanDefinition.getBeanName() + name);
					if (dp != null) {
						return dp.value;
					}
					else {
						return getMetaClass().getProperty(this, name);
					}
				}
			}
			else {
				return getMetaClass().getProperty(this, name);
			}
		}
	}

	private GroovyDynamicElementReader createDynamicElementReader(String namespace) {
		XmlReaderContext readerContext = this.groovyDslXmlBeanDefinitionReader.createReaderContext(
				new DescriptiveResource("Groovy"));
		BeanDefinitionParserDelegate delegate = new BeanDefinitionParserDelegate(readerContext);
		boolean decorating = (this.currentBeanDefinition != null);
		if (!decorating) {
			this.currentBeanDefinition = new GroovyBeanDefinitionWrapper(namespace);
		}
		return new GroovyDynamicElementReader(namespace, this.namespaces, delegate, this.currentBeanDefinition, decorating) {
			@Override
			protected void afterInvocation() {
				if (!this.decorating) {
					currentBeanDefinition = null;
				}
			}
		};
	}


	/**
	 * 此类用于将对 Bean 定义的属性添加推迟到稍后进行。这适用于以下情况：将属性赋给一个列表时，该列表在
	 * 赋值时可能不包含 Bean 引用，但之后可能包含；因此需要对其进行管理。
	 */
	private static class DeferredProperty {

		private final GroovyBeanDefinitionWrapper beanDefinition;

		private final String name;

		public Object value;

		public DeferredProperty(GroovyBeanDefinitionWrapper beanDefinition, String name, Object value) {
			this.beanDefinition = beanDefinition;
			this.name = name;
			this.value = value;
		}

		public void apply() {
			this.beanDefinition.addProperty(this.name, this.value);
		}
	}


	/**
	 * 一个 RuntimeBeanReference，负责向运行时引用添加新属性。
	 */
	private class GroovyRuntimeBeanReference extends RuntimeBeanReference implements GroovyObject {

		private final GroovyBeanDefinitionWrapper beanDefinition;

		private MetaClass metaClass;

		public GroovyRuntimeBeanReference(String beanName, GroovyBeanDefinitionWrapper beanDefinition, boolean toParent) {
			super(beanName, toParent);
			this.beanDefinition = beanDefinition;
			this.metaClass = InvokerHelper.getMetaClass(this);
		}

		@Override
		public MetaClass getMetaClass() {
			return this.metaClass;
		}

		@Override
		public Object getProperty(String property) {
			if (property.equals("beanName")) {
				return getBeanName();
			}
			else if (property.equals("source")) {
				return getSource();
			}
			else if (this.beanDefinition != null) {
				return new GroovyPropertyValue(
						property, this.beanDefinition.getBeanDefinition().getPropertyValues().get(property));
			}
			else {
				return this.metaClass.getProperty(this, property);
			}
		}

		@Override
		public Object invokeMethod(String name, Object args) {
			return this.metaClass.invokeMethod(this, name, args);
		}

		@Override
		public void setMetaClass(MetaClass metaClass) {
			this.metaClass = metaClass;
		}

		@Override
		public void setProperty(String property, Object newValue) {
			if (!addDeferredProperty(property, newValue)) {
				this.beanDefinition.getBeanDefinition().getPropertyValues().add(property, newValue);
			}
		}


		/**
		 * 包装 Bean 定义属性，并确保对其所做的任何 RuntimeBeanReference 添加都会推迟到稍后解析。
		 */
		private class GroovyPropertyValue extends GroovyObjectSupport {

			private final String propertyName;

			private final Object propertyValue;

			public GroovyPropertyValue(String propertyName, Object propertyValue) {
				this.propertyName = propertyName;
				this.propertyValue = propertyValue;
			}

			@SuppressWarnings("unused")
			public void leftShift(Object value) {
				InvokerHelper.invokeMethod(this.propertyValue, "leftShift", value);
				updateDeferredProperties(value);
			}

			@SuppressWarnings("unused")
			public boolean add(Object value) {
				boolean retVal = (Boolean) InvokerHelper.invokeMethod(this.propertyValue, "add", value);
				updateDeferredProperties(value);
				return retVal;
			}

			@SuppressWarnings("unused")
			public boolean addAll(Collection<?> values) {
				boolean retVal = (Boolean) InvokerHelper.invokeMethod(this.propertyValue, "addAll", values);
				for (Object value : values) {
					updateDeferredProperties(value);
				}
				return retVal;
			}

			@Override
			public Object invokeMethod(String name, Object args) {
				return InvokerHelper.invokeMethod(this.propertyValue, name, args);
			}

			@Override
			public Object getProperty(String name) {
				return InvokerHelper.getProperty(this.propertyValue, name);
			}

			@Override
			public void setProperty(String name, Object value) {
				InvokerHelper.setProperty(this.propertyValue, name, value);
			}

			private void updateDeferredProperties(Object value) {
				if (value instanceof RuntimeBeanReference) {
					deferredProperties.put(beanDefinition.getBeanName(),
							new DeferredProperty(beanDefinition, this.propertyName, this.propertyValue));
				}
			}
		}
	}

}
