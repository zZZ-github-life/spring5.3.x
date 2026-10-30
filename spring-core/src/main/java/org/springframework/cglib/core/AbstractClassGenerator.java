/*
 * Copyright 2003,2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cglib.core;

import java.lang.ref.WeakReference;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.springframework.asm.ClassReader;
import org.springframework.cglib.core.internal.Function;
import org.springframework.cglib.core.internal.LoadingCache;

/**
 * 所有生成代码的 CGLIB 工具的抽象类。除了为提升性能而缓存生成的类之外，它还提供了用于定制 <code>
 * ClassLoader</code>、生成类的名称以及在生成之前应用的转换的钩子。
 */
@SuppressWarnings({"rawtypes", "unchecked"})
abstract public class AbstractClassGenerator<T> implements ClassGenerator {

	private static final ThreadLocal CURRENT = new ThreadLocal();

	private static volatile Map<ClassLoader, ClassLoaderData> CACHE = new WeakHashMap<ClassLoader, ClassLoaderData>();

	private static final boolean DEFAULT_USE_CACHE =
			Boolean.parseBoolean(System.getProperty("cglib.useCache", "true"));


	private GeneratorStrategy strategy = DefaultGeneratorStrategy.INSTANCE;

	private NamingPolicy namingPolicy = DefaultNamingPolicy.INSTANCE;

	private Source source;

	private ClassLoader classLoader;

	private Class contextClass;

	private String namePrefix;

	private Object key;

	private boolean useCache = DEFAULT_USE_CACHE;

	private String className;

	private boolean attemptLoad;


	protected static class ClassLoaderData {

		private final Set<String> reservedClassNames = new HashSet<String>();

		/**
		 * 此处的 {@link AbstractClassGenerator} 持有“缓存键”（例如
		 * {@link org.springframework.cglib.proxy.Enhancer} 配置），而值则是生成的类加上一些附加值（参见
		 * {@link #unwrapCachedValue(Object)}）。<p>只要生成的类加载器可达，生成的类就可以被复用。</p> <p>
		 * 注意：访问类的唯一方式是通过 generatedClasses 缓存找到它，因此只要类本身存活（其类加载器存活），
		 * 键就不应过期。</p>
		 */
		private final LoadingCache<AbstractClassGenerator, Object, Object> generatedClasses;

		/**
		 * 注意：ClassLoaderData 对象作为 {@code WeakHashMap<ClassLoader, ...>} 的值存储，因此此
		 * classLoader 引用应当是弱引用，否则会使 classLoader 强可达并永远存活。无需引用队列，因为清理工作
		 * 由 {@link WeakHashMap} 处理。
		 */
		private final WeakReference<ClassLoader> classLoader;

		private final Predicate uniqueNamePredicate = new Predicate() {
			public boolean evaluate(Object name) {
				return reservedClassNames.contains(name);
			}
		};

		private static final Function<AbstractClassGenerator, Object> GET_KEY = new Function<AbstractClassGenerator, Object>() {
			public Object apply(AbstractClassGenerator gen) {
				return gen.key;
			}
		};

		public ClassLoaderData(ClassLoader classLoader) {
			if (classLoader == null) {
				throw new IllegalArgumentException("classLoader == null is not yet supported");
			}
			this.classLoader = new WeakReference<ClassLoader>(classLoader);
			Function<AbstractClassGenerator, Object> load =
					new Function<AbstractClassGenerator, Object>() {
						public Object apply(AbstractClassGenerator gen) {
							Class klass = gen.generate(ClassLoaderData.this);
							return gen.wrapCachedClass(klass);
						}
					};
			generatedClasses = new LoadingCache<AbstractClassGenerator, Object, Object>(GET_KEY, load);
		}

		public ClassLoader getClassLoader() {
			return classLoader.get();
		}

		public void reserveName(String name) {
			reservedClassNames.add(name);
		}

		public Predicate getUniqueNamePredicate() {
			return uniqueNamePredicate;
		}

		public Object get(AbstractClassGenerator gen, boolean useCache) {
			if (!useCache) {
				return gen.generate(ClassLoaderData.this);
			}
			else {
				Object cachedValue = generatedClasses.get(gen);
				return gen.unwrapCachedValue(cachedValue);
			}
		}
	}


	protected T wrapCachedClass(Class klass) {
		return (T) new WeakReference(klass);
	}

	protected Object unwrapCachedValue(T cached) {
		return ((WeakReference) cached).get();
	}


	protected static class Source {

		String name;

		public Source(String name) {
			this.name = name;
		}
	}


	protected AbstractClassGenerator(Source source) {
		this.source = source;
	}

	protected void setNamePrefix(String namePrefix) {
		this.namePrefix = namePrefix;
	}

	final protected String getClassName() {
		return className;
	}

	private void setClassName(String className) {
		this.className = className;
	}

	private String generateClassName(Predicate nameTestPredicate) {
		return namingPolicy.getClassName(namePrefix, source.name, key, nameTestPredicate);
	}

	/**
	 * 设置生成类所在的 <code>ClassLoader</code>。如果未设置，<code>AbstractClassGenerator</code> 的具
	 * 体子类（如 <code>Enhancer</code>）会尝试选择合适的默认值。<p>类按 <code>ClassLoader</code> 使用
	 * <code>WeakHashMap</code> 进行缓存，以便在关联的加载器被垃圾回收时移除生成的类。
	 * @param classLoader 用于生成新类的类加载器，为 null 则使用默认的
	 */
	public void setClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	// SPRING PATCH BEGIN
	public void setContextClass(Class contextClass) {
		this.contextClass = contextClass;
	}
	// SPRING PATCH END

	/**
	 * 覆盖默认命名策略。
	 * @param namingPolicy 自定义策略，为 null 则使用默认的
	 * @see DefaultNamingPolicy
	 */
	public void setNamingPolicy(NamingPolicy namingPolicy) {
		if (namingPolicy == null)
			namingPolicy = DefaultNamingPolicy.INSTANCE;
		this.namingPolicy = namingPolicy;
	}

	/**
	 * @see #setNamingPolicy
	 */
	public NamingPolicy getNamingPolicy() {
		return namingPolicy;
	}

	/**
	 * 是否对具有相同属性的类使用并更新生成类的静态缓存。默认为 <code>true</code>。
	 */
	public void setUseCache(boolean useCache) {
		this.useCache = useCache;
	}

	/**
	 * @see #setUseCache
	 */
	public boolean getUseCache() {
		return useCache;
	}

	/**
	 * 如果设置，CGLIB 会在生成类之前尝试从指定的 <code>ClassLoader</code> 加载类。由于生成的类名不保证
	 * 唯一，默认值为 <code>false</code>。
	 */
	public void setAttemptLoad(boolean attemptLoad) {
		this.attemptLoad = attemptLoad;
	}

	public boolean getAttemptLoad() {
		return attemptLoad;
	}

	/**
	 * 设置用于从此生成器创建字节码的策略。默认使用 {@link DefaultGeneratorStrategy} 的实例。
	 */
	public void setStrategy(GeneratorStrategy strategy) {
		if (strategy == null)
			strategy = DefaultGeneratorStrategy.INSTANCE;
		this.strategy = strategy;
	}

	/**
	 * @see #setStrategy
	 */
	public GeneratorStrategy getStrategy() {
		return strategy;
	}

	/**
	 * 供 CGLIB 内部使用。返回当前线程中用于生成类的 <code>AbstractClassGenerator</code>。
	 */
	public static AbstractClassGenerator getCurrent() {
		return (AbstractClassGenerator) CURRENT.get();
	}

	public ClassLoader getClassLoader() {
		ClassLoader t = classLoader;
		if (t == null) {
			t = getDefaultClassLoader();
		}
		if (t == null) {
			t = getClass().getClassLoader();
		}
		if (t == null) {
			t = Thread.currentThread().getContextClassLoader();
		}
		if (t == null) {
			throw new IllegalStateException("Cannot determine classloader");
		}
		return t;
	}

	abstract protected ClassLoader getDefaultClassLoader();

	/**
	 * 返回定义类时要使用的保护域。<p>默认实现返回 <code>null</code>，以使用默认保护域。子类可以覆盖此
	 * 方法以使用更具体的保护域。</p>
	 * @return 保护域（使用默认值时则为 <code>null</code>）
	 */
	protected ProtectionDomain getProtectionDomain() {
		return null;
	}

	protected Object create(Object key) {
		try {
			ClassLoader loader = getClassLoader();
			Map<ClassLoader, ClassLoaderData> cache = CACHE;
			ClassLoaderData data = cache.get(loader);
			if (data == null) {
				synchronized (AbstractClassGenerator.class) {
					cache = CACHE;
					data = cache.get(loader);
					if (data == null) {
						Map<ClassLoader, ClassLoaderData> newCache = new WeakHashMap<ClassLoader, ClassLoaderData>(cache);
						data = new ClassLoaderData(loader);
						newCache.put(loader, data);
						CACHE = newCache;
					}
				}
			}
			this.key = key;
			Object obj = data.get(this, getUseCache());
			if (obj instanceof Class) {
				return firstInstance((Class) obj);
			}
			return nextInstance(obj);
		}
		catch (RuntimeException | Error ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new CodeGenerationException(ex);
		}
	}

	protected Class generate(ClassLoaderData data) {
		Class gen;
		Object save = CURRENT.get();
		CURRENT.set(this);
		try {
			ClassLoader classLoader = data.getClassLoader();
			if (classLoader == null) {
				throw new IllegalStateException("ClassLoader is null while trying to define class " +
						getClassName() + ". It seems that the loader has been expired from a weak reference somehow. " +
						"Please file an issue at cglib's issue tracker.");
			}
			synchronized (classLoader) {
				String name = generateClassName(data.getUniqueNamePredicate());
				data.reserveName(name);
				this.setClassName(name);
			}
			if (attemptLoad) {
				try {
					gen = classLoader.loadClass(getClassName());
					return gen;
				}
				catch (ClassNotFoundException e) {
					// ignore
				}
			}
			byte[] b = strategy.generate(this);
			String className = ClassNameReader.getClassName(new ClassReader(b));
			ProtectionDomain protectionDomain = getProtectionDomain();
			synchronized (classLoader) { // just in case
				// SPRING PATCH BEGIN
				gen = ReflectUtils.defineClass(className, b, classLoader, protectionDomain, contextClass);
				// SPRING PATCH END
			}
			return gen;
		}
		catch (RuntimeException | Error ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new CodeGenerationException(ex);
		}
		finally {
			CURRENT.set(save);
		}
	}

	abstract protected Object firstInstance(Class type) throws Exception;

	abstract protected Object nextInstance(Object instance) throws Exception;

}
