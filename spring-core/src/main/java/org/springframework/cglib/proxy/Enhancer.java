/*
 * Copyright 2002,2003,2004 The Apache Software Foundation
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

package org.springframework.cglib.proxy;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.asm.ClassVisitor;
import org.springframework.asm.Label;
import org.springframework.asm.Type;
import org.springframework.cglib.core.AbstractClassGenerator;
import org.springframework.cglib.core.ClassEmitter;
import org.springframework.cglib.core.CodeEmitter;
import org.springframework.cglib.core.CodeGenerationException;
import org.springframework.cglib.core.CollectionUtils;
import org.springframework.cglib.core.Constants;
import org.springframework.cglib.core.DuplicatesPredicate;
import org.springframework.cglib.core.EmitUtils;
import org.springframework.cglib.core.KeyFactory;
import org.springframework.cglib.core.Local;
import org.springframework.cglib.core.MethodInfo;
import org.springframework.cglib.core.MethodInfoTransformer;
import org.springframework.cglib.core.MethodWrapper;
import org.springframework.cglib.core.ObjectSwitchCallback;
import org.springframework.cglib.core.ProcessSwitchCallback;
import org.springframework.cglib.core.ReflectUtils;
import org.springframework.cglib.core.RejectModifierPredicate;
import org.springframework.cglib.core.Signature;
import org.springframework.cglib.core.Transformer;
import org.springframework.cglib.core.TypeUtils;
import org.springframework.cglib.core.VisibilityPredicate;
import org.springframework.cglib.core.WeakCacheKey;

/**
 * 生成动态子类以实现方法拦截。本类最初是作为 JDK 1.3 所附带的标准动态代理支持的替代品，
 * 但它除了实现接口之外，还允许代理扩展一个具体的基类。
 * 动态生成的子类会重写父类的非 final 方法，并提供回调到用户自定义拦截器实现的钩子。
 * <p>最初也是最基本的回调类型是 {@link MethodInterceptor}，用 AOP 术语来说，
 * 它支持“环绕通知”（around advice）——也就是说，
 * 你可以在调用“super”方法之前和之后执行自定义代码。此外，
 * 你还可以在调用 super 方法之前修改参数，或者完全不调用它。
 * <p>尽管 <code>MethodInterceptor</code> 足够通用，可以满足任何拦截需求，
 * 但它往往显得过于繁重。为了简单和性能，还提供了其他专门的回调类型，
 * 例如 {@link LazyLoader}。通常每个增强类只使用一个回调，
 * 但你可以借助 {@link CallbackFilter} 按方法控制使用哪个回调。
 * <p>本类最常见的用法体现在静态辅助方法中。对于更高级的需求，
 * 例如自定义要使用的 <code>ClassLoader</code>，应当创建 <code>Enhancer</code> 的新实例。
 * CGLIB 中的其他类也遵循类似的模式。
 * <p>所有增强对象都实现 {@link Factory} 接口，
 * 除非使用 {@link #setUseFactory} 显式禁用此特性。
 * <code>Factory</code> 接口提供了用于更改现有对象回调的 API，以及一种更快、
 * 更简便的方式来创建相同类型的新实例。
 * <p>关于几乎可以直接替换 <code>java.lang.reflect.Proxy</code> 的方案，
 * 请参见 {@link Proxy} 类。
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class Enhancer extends AbstractClassGenerator {

	private static final CallbackFilter ALL_ZERO = new CallbackFilter() {
		public int accept(Method method) {
			return 0;
		}
	};

	private static final Source SOURCE = new Source(Enhancer.class.getName());

	private static final EnhancerKey KEY_FACTORY =
			(EnhancerKey) KeyFactory.create(EnhancerKey.class, KeyFactory.HASH_ASM_TYPE, null);

	private static final String BOUND_FIELD = "CGLIB$BOUND";

	private static final String FACTORY_DATA_FIELD = "CGLIB$FACTORY_DATA";

	private static final String THREAD_CALLBACKS_FIELD = "CGLIB$THREAD_CALLBACKS";

	private static final String STATIC_CALLBACKS_FIELD = "CGLIB$STATIC_CALLBACKS";

	private static final String SET_THREAD_CALLBACKS_NAME = "CGLIB$SET_THREAD_CALLBACKS";

	private static final String SET_STATIC_CALLBACKS_NAME = "CGLIB$SET_STATIC_CALLBACKS";

	private static final String CONSTRUCTED_FIELD = "CGLIB$CONSTRUCTED";

	/**
	 * {@link org.springframework.cglib.core.AbstractClassGenerator.ClassLoaderData#generatedClasses}
	 * 要求保持缓存键处于良好状态（只要代理类还存活，
	 * 这些键就应当有效可用），而其中一个缓存键就是 {@link CallbackFilter}。
	 * 这就是生成的类中包含一个静态字段、持有对 {@link #filter} 的强引用的原因。
	 * <p>这一系列操作达成两个目标：确保生成的类可复用，并可通过 generatedClasses 缓存获取；
	 * 同时，在用户不再需要时，能够卸载类加载器及相关的 {@link CallbackFilter}</p>
	 */
	private static final String CALLBACK_FILTER_FIELD = "CGLIB$CALLBACK_FILTER";

	private static final Type OBJECT_TYPE =
			TypeUtils.parseType("Object");

	private static final Type FACTORY =
			TypeUtils.parseType("org.springframework.cglib.proxy.Factory");

	private static final Type ILLEGAL_STATE_EXCEPTION =
			TypeUtils.parseType("IllegalStateException");

	private static final Type ILLEGAL_ARGUMENT_EXCEPTION =
			TypeUtils.parseType("IllegalArgumentException");

	private static final Type THREAD_LOCAL =
			TypeUtils.parseType("ThreadLocal");

	private static final Type CALLBACK =
			TypeUtils.parseType("org.springframework.cglib.proxy.Callback");

	private static final Type CALLBACK_ARRAY =
			Type.getType(Callback[].class);

	private static final Signature CSTRUCT_NULL =
			TypeUtils.parseConstructor("");

	private static final Signature SET_THREAD_CALLBACKS =
			new Signature(SET_THREAD_CALLBACKS_NAME, Type.VOID_TYPE, new Type[]{CALLBACK_ARRAY});

	private static final Signature SET_STATIC_CALLBACKS =
			new Signature(SET_STATIC_CALLBACKS_NAME, Type.VOID_TYPE, new Type[]{CALLBACK_ARRAY});

	private static final Signature NEW_INSTANCE =
			new Signature("newInstance", Constants.TYPE_OBJECT, new Type[]{CALLBACK_ARRAY});

	private static final Signature MULTIARG_NEW_INSTANCE =
			new Signature("newInstance", Constants.TYPE_OBJECT, new Type[]{
					Constants.TYPE_CLASS_ARRAY,
					Constants.TYPE_OBJECT_ARRAY,
					CALLBACK_ARRAY,
			});

	private static final Signature SINGLE_NEW_INSTANCE =
			new Signature("newInstance", Constants.TYPE_OBJECT, new Type[]{CALLBACK});

	private static final Signature SET_CALLBACK =
			new Signature("setCallback", Type.VOID_TYPE, new Type[]{Type.INT_TYPE, CALLBACK});

	private static final Signature GET_CALLBACK =
			new Signature("getCallback", CALLBACK, new Type[]{Type.INT_TYPE});

	private static final Signature SET_CALLBACKS =
			new Signature("setCallbacks", Type.VOID_TYPE, new Type[]{CALLBACK_ARRAY});

	private static final Signature GET_CALLBACKS =
			new Signature("getCallbacks", CALLBACK_ARRAY, new Type[0]);

	private static final Signature THREAD_LOCAL_GET =
			TypeUtils.parseSignature("Object get()");

	private static final Signature THREAD_LOCAL_SET =
			TypeUtils.parseSignature("void set(Object)");

	private static final Signature BIND_CALLBACKS =
			TypeUtils.parseSignature("void CGLIB$BIND_CALLBACKS(Object)");

	private EnhancerFactoryData currentData;

	private Object currentKey;


	/**
	 * 内部接口，仅因 ClassLoader 相关问题而声明为 public。
	 */
	public interface EnhancerKey {

		public Object newInstance(String type,
				String[] interfaces,
				WeakCacheKey<CallbackFilter> filter,
				Type[] callbackTypes,
				boolean useFactory,
				boolean interceptDuringConstruction,
				Long serialVersionUID);
	}


	private Class[] interfaces;

	private CallbackFilter filter;

	private Callback[] callbacks;

	private Type[] callbackTypes;

	private boolean validateCallbackTypes;

	private boolean classOnly;

	private Class superclass;

	private Class[] argumentTypes;

	private Object[] arguments;

	private boolean useFactory = true;

	private Long serialVersionUID;

	private boolean interceptDuringConstruction = true;

	/**
	 * 创建一个新的 <code>Enhancer</code>。
	 * 每个生成的对象都应使用一个新的 <code>Enhancer</code> 实例，且不应跨线程共享。
	 * 要为生成的类创建更多实例，请使用 <code>Factory</code> 接口。
	 * @see Factory
	 */
	public Enhancer() {
		super(SOURCE);
	}

	/**
	 * 设置生成的类将要扩展的类。为方便起见，如果传入的父类实际上是一个接口，
	 * 则会改为以相应的参数调用 <code>setInterfaces</code>。非接口参数不能声明为 final，
	 * 并且必须具有可访问的构造器。
	 * @param superclass 要扩展的类或要实现的接口
	 * @see #setInterfaces(Class[])
	 */
	public void setSuperclass(Class superclass) {
		if (superclass != null && superclass.isInterface()) {
			setInterfaces(new Class[]{superclass});
			// SPRING PATCH BEGIN
			setContextClass(superclass);
			// SPRING PATCH END
		}
		else if (superclass != null && superclass.equals(Object.class)) {
			// affects choice of ClassLoader
			this.superclass = null;
		}
		else {
			this.superclass = superclass;
			// SPRING PATCH BEGIN
			setContextClass(superclass);
			// SPRING PATCH END
		}
	}

	/**
	 * 设置要实现的接口。无论此处指定什么，<code>Factory</code> 接口都始终会被实现。
	 * @param interfaces 要实现接口的数组，或 null
	 * @see Factory
	 */
	public void setInterfaces(Class[] interfaces) {
		this.interfaces = interfaces;
	}

	/**
	 * 设置用于将生成类的方法映射到特定回调索引的 {@link CallbackFilter}。
	 * 新的对象实例始终使用相同的映射，但可以使用不同的实际回调对象。
	 * @param filter 生成新类时使用的回调过滤器
	 * @see #setCallbacks
	 */
	public void setCallbackFilter(CallbackFilter filter) {
		this.filter = filter;
	}


	/**
	 * 设置要使用的单个 {@link Callback}。如果使用 {@link #createClass}，则忽略此项。
	 * @param callback 用于所有方法的回调
	 * @see #setCallbacks
	 */
	public void setCallback(final Callback callback) {
		setCallbacks(new Callback[]{callback});
	}

	/**
	 * 设置要使用的回调数组。如果使用 {@link #createClass}，则忽略此项。
	 * 必须使用 {@link CallbackFilter} 为被代理类中的每个方法指定在该数组中的索引。
	 * @param callbacks 回调数组
	 * @see #setCallbackFilter
	 * @see #setCallback
	 */
	public void setCallbacks(Callback[] callbacks) {
		if (callbacks != null && callbacks.length == 0) {
			throw new IllegalArgumentException("Array cannot be empty");
		}
		this.callbacks = callbacks;
	}

	/**
	 * 设置增强对象实例是否应实现 {@link Factory} 接口。
	 * 这一选项是为那些需要让代理与其目标更难区分的工具而添加的。此外，在某些情况下，
	 * 可能需要禁用 <code>Factory</code> 接口，以防止代码更改底层的回调。
	 * @param useFactory 是否实现 <code>Factory</code>；默认为 <code>true</code>
	 */
	public void setUseFactory(boolean useFactory) {
		this.useFactory = useFactory;
	}

	/**
	 * 设置是否拦截在代理构造器中调用的方法。默认值为 true。
	 * 未被拦截的方法将调用代理基类的对应方法（如果存在）。
	 * @param interceptDuringConstruction 是否拦截从构造方法中调用的方法
	 */
	public void setInterceptDuringConstruction(boolean interceptDuringConstruction) {
		this.interceptDuringConstruction = interceptDuringConstruction;
	}

	/**
	 * 设置要使用的单个 {@link Callback} 类型。在调用 {@link #createClass} 时，
	 * 可以用它代替 {@link #setCallback}，因为可能无法获得实际回调实例的数组。
	 * @param callbackType 用于所有方法的回调类型
	 * @see #setCallbackTypes
	 */
	public void setCallbackType(Class callbackType) {
		setCallbackTypes(new Class[]{callbackType});
	}

	/**
	 * 设置要使用的回调类型数组。在调用 {@link #createClass} 时，
	 * 可以用它代替 {@link #setCallbacks}，因为可能无法获得实际回调实例的数组。
	 * 必须使用 {@link CallbackFilter} 为被代理类中的每个方法指定在该数组中的索引。
	 * @param callbackTypes 回调类型数组
	 */
	public void setCallbackTypes(Class[] callbackTypes) {
		if (callbackTypes != null && callbackTypes.length == 0) {
			throw new IllegalArgumentException("Array cannot be empty");
		}
		this.callbackTypes = CallbackInfo.determineTypes(callbackTypes);
	}

	/**
	 * 如有必要则生成新类，并使用指定的回调（如果有）创建新的对象实例。使用父类的无参构造器。
	 * @return 新实例
	 */
	public Object create() {
		classOnly = false;
		argumentTypes = null;
		return createHelper();
	}

	/**
	 * 如有必要则生成新类，并使用指定的回调（如果有）创建新的对象实例。
	 * 使用父类中与 <code>argumentTypes</code> 参数匹配的构造器，并传入给定的参数。
	 * @param argumentTypes 构造方法签名
	 * @param arguments 传递给构造方法的兼容包装参数
	 * @return 新实例
	 */
	public Object create(Class[] argumentTypes, Object[] arguments) {
		classOnly = false;
		if (argumentTypes == null || arguments == null || argumentTypes.length != arguments.length) {
			throw new IllegalArgumentException("Arguments must be non-null and of equal length");
		}
		this.argumentTypes = argumentTypes;
		this.arguments = arguments;
		return createHelper();
	}

	/**
	 * 如有必要则生成新类，并直接返回该类，而不创建新实例。这会忽略已设置的所有回调。
	 * 要创建新实例，必须使用反射，并且构造器期间调用的方法不会被拦截。要避免此问题，
	 * 请使用多参数的 <code>create</code> 方法。
	 * @see #create(Class[], Object[])
	 */
	public Class createClass() {
		classOnly = true;
		return (Class) createHelper();
	}

	/**
	 * 在生成的类中插入一个静态 serialVersionUID 字段。
	 * @param sUID 字段值，为 null 则不生成该字段。
	 */
	public void setSerialVersionUID(Long sUID) {
		serialVersionUID = sUID;
	}

	private void preValidate() {
		if (callbackTypes == null) {
			callbackTypes = CallbackInfo.determineTypes(callbacks, false);
			validateCallbackTypes = true;
		}
		if (filter == null) {
			if (callbackTypes.length > 1) {
				throw new IllegalStateException("Multiple callback types possible but no filter specified");
			}
			filter = ALL_ZERO;
		}
	}

	private void validate() {
		if (classOnly ^ (callbacks == null)) {
			if (classOnly) {
				throw new IllegalStateException("createClass does not accept callbacks");
			}
			else {
				throw new IllegalStateException("Callbacks are required");
			}
		}
		if (classOnly && (callbackTypes == null)) {
			throw new IllegalStateException("Callback types are required");
		}
		if (validateCallbackTypes) {
			callbackTypes = null;
		}
		if (callbacks != null && callbackTypes != null) {
			if (callbacks.length != callbackTypes.length) {
				throw new IllegalStateException("Lengths of callback and callback types array must be the same");
			}
			Type[] check = CallbackInfo.determineTypes(callbacks);
			for (int i = 0; i < check.length; i++) {
				if (!check[i].equals(callbackTypes[i])) {
					throw new IllegalStateException("Callback " + check[i] + " is not assignable to " + callbackTypes[i]);
				}
			}
		}
		else if (callbacks != null) {
			callbackTypes = CallbackInfo.determineTypes(callbacks);
		}
		if (interfaces != null) {
			for (int i = 0; i < interfaces.length; i++) {
				if (interfaces[i] == null) {
					throw new IllegalStateException("Interfaces cannot be null");
				}
				if (!interfaces[i].isInterface()) {
					throw new IllegalStateException(interfaces[i] + " is not an interface");
				}
			}
		}
	}

	/**
	 * 本类的思路是缓存相关的 java.lang.reflect 实例，
	 * 从而比使用 {@link ReflectUtils#newInstance(Class, Class[], Object[])} 和
	 * {@link Enhancer#setThreadCallbacks(Class, Callback[])} 时更快地实例化代理类。
	 */
	static class EnhancerFactoryData {

		public final Class generatedClass;

		private final Method setThreadCallbacks;

		private final Class[] primaryConstructorArgTypes;

		private final Constructor primaryConstructor;

		public EnhancerFactoryData(Class generatedClass, Class[] primaryConstructorArgTypes, boolean classOnly) {
			this.generatedClass = generatedClass;
			try {
				setThreadCallbacks = getCallbacksSetter(generatedClass, SET_THREAD_CALLBACKS_NAME);
				if (classOnly) {
					this.primaryConstructorArgTypes = null;
					this.primaryConstructor = null;
				}
				else {
					this.primaryConstructorArgTypes = primaryConstructorArgTypes;
					this.primaryConstructor = ReflectUtils.getConstructor(generatedClass, primaryConstructorArgTypes);
				}
			}
			catch (NoSuchMethodException e) {
				throw new CodeGenerationException(e);
			}
		}

		/**
		 * 为给定的参数类型创建代理实例，并分配回调。理想情况下，每个代理类只应使用一组参数类型，
		 * 否则就需要花费时间进行构造器查找。从技术上说，
		 * 它是对 {@link Enhancer#createUsingReflection(Class)} 的重新实现，
		 * 带有“缓存 {@link #setThreadCallbacks} 和 {@link #primaryConstructor}”的机制。
		 * @param argumentTypes 构造方法参数类型
		 * @param arguments 构造方法参数
		 * @param callbacks 为新实例设置的回调
		 * @return 新建的代理
		 * @see #createUsingReflection(Class)
		 */
		public Object newInstance(Class[] argumentTypes, Object[] arguments, Callback[] callbacks) {
			setThreadCallbacks(callbacks);
			try {
				// Explicit reference equality is added here just in case Arrays.equals does not have one
				if (primaryConstructorArgTypes == argumentTypes ||
						Arrays.equals(primaryConstructorArgTypes, argumentTypes)) {
					// If we have relevant Constructor instance at hand, just call it
					// This skips "get constructors" machinery
					return ReflectUtils.newInstance(primaryConstructor, arguments);
				}
				// Take a slow path if observing unexpected argument types
				return ReflectUtils.newInstance(generatedClass, argumentTypes, arguments);
			}
			finally {
				// clear thread callbacks to allow them to be gc'd
				setThreadCallbacks(null);
			}

		}

		private void setThreadCallbacks(Callback[] callbacks) {
			try {
				setThreadCallbacks.invoke(generatedClass, (Object) callbacks);
			}
			catch (IllegalAccessException e) {
				throw new CodeGenerationException(e);
			}
			catch (InvocationTargetException e) {
				throw new CodeGenerationException(e.getTargetException());
			}
		}
	}

	private Object createHelper() {
		preValidate();
		Object key = KEY_FACTORY.newInstance((superclass != null) ? superclass.getName() : null,
				ReflectUtils.getNames(interfaces),
				filter == ALL_ZERO ? null : new WeakCacheKey<CallbackFilter>(filter),
				callbackTypes,
				useFactory,
				interceptDuringConstruction,
				serialVersionUID);
		this.currentKey = key;
		Object result = super.create(key);
		return result;
	}

	@Override
	protected Class generate(ClassLoaderData data) {
		validate();
		if (superclass != null) {
			setNamePrefix(superclass.getName());
		}
		else if (interfaces != null) {
			setNamePrefix(interfaces[ReflectUtils.findPackageProtected(interfaces)].getName());
		}
		return super.generate(data);
	}

	protected ClassLoader getDefaultClassLoader() {
		if (superclass != null) {
			return superclass.getClassLoader();
		}
		else if (interfaces != null) {
			return interfaces[0].getClassLoader();
		}
		else {
			return null;
		}
	}

	protected ProtectionDomain getProtectionDomain() {
		if (superclass != null) {
			return ReflectUtils.getProtectionDomain(superclass);
		}
		else if (interfaces != null) {
			return ReflectUtils.getProtectionDomain(interfaces[0]);
		}
		else {
			return null;
		}
	}

	private Signature rename(Signature sig, int index) {
		return new Signature("CGLIB$" + sig.getName() + "$" + index,
				sig.getDescriptor());
	}

	/**
	 * 查找使用指定父类和接口时、由 Enhancer 生成的类将会扩展的所有方法。
	 * 这在构建 Callback 对象列表时很有用。这些方法会被添加到给定列表的末尾。
	 * 由于 Enhancer 生成的类具有子类化特性，这些方法保证是非 static、
	 * 非 final 且非 private 的。每个方法签名只会出现一次，即使它出现在多个类中也是如此。
	 * @param superclass 将被扩展的类，或 null
	 * @param interfaces 将被实现的接口列表，或 null
	 * @param methods 用于复制适用方法的列表
	 */
	public static void getMethods(Class superclass, Class[] interfaces, List methods) {
		getMethods(superclass, interfaces, methods, null, null);
	}

	private static void getMethods(Class superclass, Class[] interfaces, List methods, List interfaceMethods, Set forcePublic) {
		ReflectUtils.addAllMethods(superclass, methods);
		List target = (interfaceMethods != null) ? interfaceMethods : methods;
		if (interfaces != null) {
			for (int i = 0; i < interfaces.length; i++) {
				if (interfaces[i] != Factory.class) {
					ReflectUtils.addAllMethods(interfaces[i], target);
				}
			}
		}
		if (interfaceMethods != null) {
			if (forcePublic != null) {
				forcePublic.addAll(MethodWrapper.createSet(interfaceMethods));
			}
			methods.addAll(interfaceMethods);
		}
		CollectionUtils.filter(methods, new RejectModifierPredicate(Constants.ACC_STATIC));
		CollectionUtils.filter(methods, new VisibilityPredicate(superclass, true));
		CollectionUtils.filter(methods, new DuplicatesPredicate());
		CollectionUtils.filter(methods, new RejectModifierPredicate(Constants.ACC_FINAL));
	}

	public void generateClass(ClassVisitor v) throws Exception {
		Class sc = (superclass == null) ? Object.class : superclass;

		if (TypeUtils.isFinal(sc.getModifiers()))
			throw new IllegalArgumentException("Cannot subclass final class " + sc.getName());
		List constructors = new ArrayList(Arrays.asList(sc.getDeclaredConstructors()));
		filterConstructors(sc, constructors);

		// Order is very important: must add superclass, then
		// its superclass chain, then each interface and
		// its superinterfaces.
		List actualMethods = new ArrayList();
		List interfaceMethods = new ArrayList();
		final Set forcePublic = new HashSet();
		getMethods(sc, interfaces, actualMethods, interfaceMethods, forcePublic);

		List methods = CollectionUtils.transform(actualMethods, new Transformer() {
			public Object transform(Object value) {
				Method method = (Method) value;
				int modifiers = Constants.ACC_FINAL
						| (method.getModifiers()
						& ~Constants.ACC_ABSTRACT
						& ~Constants.ACC_NATIVE
						& ~Constants.ACC_SYNCHRONIZED);
				if (forcePublic.contains(MethodWrapper.create(method))) {
					modifiers = (modifiers & ~Constants.ACC_PROTECTED) | Constants.ACC_PUBLIC;
				}
				return ReflectUtils.getMethodInfo(method, modifiers);
			}
		});

		ClassEmitter e = new ClassEmitter(v);
		if (currentData == null) {
			e.begin_class(Constants.V1_8,
					Constants.ACC_PUBLIC,
					getClassName(),
					Type.getType(sc),
					(useFactory ?
							TypeUtils.add(TypeUtils.getTypes(interfaces), FACTORY) :
							TypeUtils.getTypes(interfaces)),
					Constants.SOURCE_FILE);
		}
		else {
			e.begin_class(Constants.V1_8,
					Constants.ACC_PUBLIC,
					getClassName(),
					null,
					new Type[]{FACTORY},
					Constants.SOURCE_FILE);
		}
		List constructorInfo = CollectionUtils.transform(constructors, MethodInfoTransformer.getInstance());

		e.declare_field(Constants.ACC_PRIVATE, BOUND_FIELD, Type.BOOLEAN_TYPE, null);
		e.declare_field(Constants.ACC_PUBLIC | Constants.ACC_STATIC, FACTORY_DATA_FIELD, OBJECT_TYPE, null);
		if (!interceptDuringConstruction) {
			e.declare_field(Constants.ACC_PRIVATE, CONSTRUCTED_FIELD, Type.BOOLEAN_TYPE, null);
		}
		e.declare_field(Constants.PRIVATE_FINAL_STATIC, THREAD_CALLBACKS_FIELD, THREAD_LOCAL, null);
		e.declare_field(Constants.PRIVATE_FINAL_STATIC, STATIC_CALLBACKS_FIELD, CALLBACK_ARRAY, null);
		if (serialVersionUID != null) {
			e.declare_field(Constants.PRIVATE_FINAL_STATIC, Constants.SUID_FIELD_NAME, Type.LONG_TYPE, serialVersionUID);
		}

		for (int i = 0; i < callbackTypes.length; i++) {
			e.declare_field(Constants.ACC_PRIVATE, getCallbackField(i), callbackTypes[i], null);
		}
		// This is declared private to avoid "public field" pollution
		e.declare_field(Constants.ACC_PRIVATE | Constants.ACC_STATIC, CALLBACK_FILTER_FIELD, OBJECT_TYPE, null);

		if (currentData == null) {
			emitMethods(e, methods, actualMethods);
			emitConstructors(e, constructorInfo);
		}
		else {
			emitDefaultConstructor(e);
		}
		emitSetThreadCallbacks(e);
		emitSetStaticCallbacks(e);
		emitBindCallbacks(e);

		if (useFactory || currentData != null) {
			int[] keys = getCallbackKeys();
			emitNewInstanceCallbacks(e);
			emitNewInstanceCallback(e);
			emitNewInstanceMultiarg(e, constructorInfo);
			emitGetCallback(e, keys);
			emitSetCallback(e, keys);
			emitGetCallbacks(e);
			emitSetCallbacks(e);
		}

		e.end_class();
	}

	/**
	 * 过滤来自父类的构造器列表。保留下来的构造器将被包含在生成的类中。
	 * 默认实现会过滤掉所有 private 构造器，但子类可以扩展 Enhancer 来覆盖此行为。
	 * @param sc 超类
	 * @param constructors 来自超类的所有已声明构造方法的列表
	 * @throws IllegalArgumentException 如果不存在非私有构造方法
	 */
	protected void filterConstructors(Class sc, List constructors) {
		CollectionUtils.filter(constructors, new VisibilityPredicate(sc, true));
		if (constructors.size() == 0)
			throw new IllegalArgumentException("No visible constructors in " + sc);
	}

	/**
	 * 此方法不应在常规流程中调用。从技术上说，
	 * {@link #wrapCachedClass(Class)} 使用 {@link Enhancer.EnhancerFactoryData} 作为缓存值，
	 * 后者比普通的反射查找和调用能更快地实例化。此方法出于向后兼容的原因而保留原样：
	 * 以防它曾被使用过。
	 * @param type 要实例化的类
	 * @return 新建的代理实例
	 * @throws Exception 如果出现问题
	 */
	protected Object firstInstance(Class type) throws Exception {
		if (classOnly) {
			return type;
		}
		else {
			return createUsingReflection(type);
		}
	}

	protected Object nextInstance(Object instance) {
		EnhancerFactoryData data = (EnhancerFactoryData) instance;

		if (classOnly) {
			return data.generatedClass;
		}

		Class[] argumentTypes = this.argumentTypes;
		Object[] arguments = this.arguments;
		if (argumentTypes == null) {
			argumentTypes = Constants.EMPTY_CLASS_ARRAY;
			arguments = null;
		}
		return data.newInstance(argumentTypes, arguments, callbacks);
	}

	@Override
	protected Object wrapCachedClass(Class klass) {
		Class[] argumentTypes = this.argumentTypes;
		if (argumentTypes == null) {
			argumentTypes = Constants.EMPTY_CLASS_ARRAY;
		}
		EnhancerFactoryData factoryData = new EnhancerFactoryData(klass, argumentTypes, classOnly);
		Field factoryDataField = null;
		try {
			// The subsequent dance is performed just once for each class,
			// so it does not matter much how fast it goes
			factoryDataField = klass.getField(FACTORY_DATA_FIELD);
			factoryDataField.set(null, factoryData);
			Field callbackFilterField = klass.getDeclaredField(CALLBACK_FILTER_FIELD);
			callbackFilterField.setAccessible(true);
			callbackFilterField.set(null, this.filter);
		}
		catch (NoSuchFieldException e) {
			throw new CodeGenerationException(e);
		}
		catch (IllegalAccessException e) {
			throw new CodeGenerationException(e);
		}
		return new WeakReference<EnhancerFactoryData>(factoryData);
	}

	@Override
	protected Object unwrapCachedValue(Object cached) {
		if (currentKey instanceof EnhancerKey) {
			EnhancerFactoryData data = ((WeakReference<EnhancerFactoryData>) cached).get();
			return data;
		}
		return super.unwrapCachedValue(cached);
	}

	/**
	 * 在通过反射创建生成类的新实例之前，调用此方法来注册要使用的 {@link Callback} 数组。
	 * 如果你使用 <code>Enhancer</code> 实例或 {@link Factory} 接口来创建新实例，
	 * 则无需使用此方法。它的主要用途是：当你希望自行缓存并复用生成的类，
	 * 而生成的类<i>没有</i>实现 {@link Factory} 接口时。
	 * <p>请注意，此方法只在当前线程上注册回调。如果要为多个线程创建的实例注册回调，
	 * 请使用 {@link #registerStaticCallbacks}。
	 * <p>调用任何 <code>create</code> 方法（例如 {@link #create}）或任何 {@link Factory} 的
	 * <code>newInstance</code> 方法时，
	 * 已注册的回调会被覆盖并随后清除。否则它们<i>不会</i>被清除；如果担心内存泄漏，
	 * 在通过反射创建新实例后应小心地将它们重新设置为 <code>null</code>。
	 * @param generatedClass 先前由 {@link Enhancer} 创建的类
	 * @param callbacks 创建生成类的实例时使用的回调数组
	 * @see #setUseFactory
	 */
	public static void registerCallbacks(Class generatedClass, Callback[] callbacks) {
		setThreadCallbacks(generatedClass, callbacks);
	}

	/**
	 * 与 {@link #registerCallbacks} 类似，但适用于多个线程将创建生成类实例的场景。
	 * 线程级回调始终会覆盖静态回调。静态回调永远不会被清除。
	 * @param generatedClass 先前由 {@link Enhancer} 创建的类
	 * @param callbacks 创建生成类的实例时使用的回调数组
	 */
	public static void registerStaticCallbacks(Class generatedClass, Callback[] callbacks) {
		setCallbacksHelper(generatedClass, callbacks, SET_STATIC_CALLBACKS_NAME);
	}

	/**
	 * 判断某个类是否使用 <code>Enhancer</code> 生成。
	 * @param type 任意类
	 * @return 该类是否由 <code>Enhancer</code> 生成
	 */
	public static boolean isEnhanced(Class type) {
		try {
			getCallbacksSetter(type, SET_THREAD_CALLBACKS_NAME);
			return true;
		}
		catch (NoSuchMethodException e) {
			return false;
		}
	}

	private static void setThreadCallbacks(Class type, Callback[] callbacks) {
		setCallbacksHelper(type, callbacks, SET_THREAD_CALLBACKS_NAME);
	}

	private static void setCallbacksHelper(Class type, Callback[] callbacks, String methodName) {
		// TODO: optimize
		try {
			Method setter = getCallbacksSetter(type, methodName);
			setter.invoke(null, new Object[]{callbacks});
		}
		catch (NoSuchMethodException e) {
			throw new IllegalArgumentException(type + " is not an enhanced class");
		}
		catch (IllegalAccessException e) {
			throw new CodeGenerationException(e);
		}
		catch (InvocationTargetException e) {
			throw new CodeGenerationException(e);
		}
	}

	private static Method getCallbacksSetter(Class type, String methodName) throws NoSuchMethodException {
		return type.getDeclaredMethod(methodName, new Class[]{Callback[].class});
	}

	/**
	 * 实例化代理实例并分配回调值。实现细节：java.lang.reflect 实例不会被缓存，
	 * 因此此方法不应在热路径上使用。
	 * 当 {@link #setUseCache(boolean)} 设置为 {@code false} 时会使用此方法。
	 * @param type 要实例化的类
	 * @return 新建的实例
	 */
	private Object createUsingReflection(Class type) {
		setThreadCallbacks(type, callbacks);
		try {

			if (argumentTypes != null) {

				return ReflectUtils.newInstance(type, argumentTypes, arguments);

			}
			else {

				return ReflectUtils.newInstance(type);

			}
		}
		finally {
			// clear thread callbacks to allow them to be gc'd
			setThreadCallbacks(type, null);
		}
	}

	/**
	 * 用于创建被拦截对象的辅助方法。若要对生成的实例进行更精细的控制，
	 * 请使用 <code>Enhancer</code> 的新实例，而不是此静态方法。
	 * @param type 要扩展的类或要实现的接口
	 * @param callback 用于所有方法的回调
	 */
	public static Object create(Class type, Callback callback) {
		Enhancer e = new Enhancer();
		e.setSuperclass(type);
		e.setCallback(callback);
		return e.create();
	}

	/**
	 * 用于创建被拦截对象的辅助方法。若要对生成的实例进行更精细的控制，
	 * 请使用 <code>Enhancer</code> 的新实例，而不是此静态方法。
	 * @param superclass 要扩展的类或要实现的接口
	 * @param interfaces 要实现接口的数组，或 null
	 * @param callback 用于所有方法的回调
	 */
	public static Object create(Class superclass, Class interfaces[], Callback callback) {
		Enhancer e = new Enhancer();
		e.setSuperclass(superclass);
		e.setInterfaces(interfaces);
		e.setCallback(callback);
		return e.create();
	}

	/**
	 * 用于创建被拦截对象的辅助方法。若要对生成的实例进行更精细的控制，
	 * 请使用 <code>Enhancer</code> 的新实例，而不是此静态方法。
	 * @param superclass 要扩展的类或要实现的接口
	 * @param interfaces 要实现接口的数组，或 null
	 * @param filter 生成新类时使用的回调过滤器
	 * @param callbacks 用于增强对象的回调实现
	 */
	public static Object create(Class superclass, Class[] interfaces, CallbackFilter filter, Callback[] callbacks) {
		Enhancer e = new Enhancer();
		e.setSuperclass(superclass);
		e.setInterfaces(interfaces);
		e.setCallbackFilter(filter);
		e.setCallbacks(callbacks);
		return e.create();
	}

	private void emitDefaultConstructor(ClassEmitter ce) {
		Constructor<Object> declaredConstructor;
		try {
			declaredConstructor = Object.class.getDeclaredConstructor();
		}
		catch (NoSuchMethodException e) {
			throw new IllegalStateException("Object should have default constructor ", e);
		}
		MethodInfo constructor = (MethodInfo) MethodInfoTransformer.getInstance().transform(declaredConstructor);
		CodeEmitter e = EmitUtils.begin_method(ce, constructor, Constants.ACC_PUBLIC);
		e.load_this();
		e.dup();
		Signature sig = constructor.getSignature();
		e.super_invoke_constructor(sig);
		e.return_value();
		e.end_method();
	}

	private void emitConstructors(ClassEmitter ce, List constructors) {
		boolean seenNull = false;
		for (Iterator it = constructors.iterator(); it.hasNext(); ) {
			MethodInfo constructor = (MethodInfo) it.next();
			if (currentData != null && !"()V".equals(constructor.getSignature().getDescriptor())) {
				continue;
			}
			CodeEmitter e = EmitUtils.begin_method(ce, constructor, Constants.ACC_PUBLIC);
			e.load_this();
			e.dup();
			e.load_args();
			Signature sig = constructor.getSignature();
			seenNull = seenNull || sig.getDescriptor().equals("()V");
			e.super_invoke_constructor(sig);
			if (currentData == null) {
				e.invoke_static_this(BIND_CALLBACKS);
				if (!interceptDuringConstruction) {
					e.load_this();
					e.push(1);
					e.putfield(CONSTRUCTED_FIELD);
				}
			}
			e.return_value();
			e.end_method();
		}
		if (!classOnly && !seenNull && arguments == null)
			throw new IllegalArgumentException("Superclass has no null constructors but no arguments were given");
	}

	private int[] getCallbackKeys() {
		int[] keys = new int[callbackTypes.length];
		for (int i = 0; i < callbackTypes.length; i++) {
			keys[i] = i;
		}
		return keys;
	}

	private void emitGetCallback(ClassEmitter ce, int[] keys) {
		final CodeEmitter e = ce.begin_method(Constants.ACC_PUBLIC, GET_CALLBACK, null);
		e.load_this();
		e.invoke_static_this(BIND_CALLBACKS);
		e.load_this();
		e.load_arg(0);
		e.process_switch(keys, new ProcessSwitchCallback() {
			public void processCase(int key, Label end) {
				e.getfield(getCallbackField(key));
				e.goTo(end);
			}

			public void processDefault() {
				e.pop(); // stack height
				e.aconst_null();
			}
		});
		e.return_value();
		e.end_method();
	}

	private void emitSetCallback(ClassEmitter ce, int[] keys) {
		final CodeEmitter e = ce.begin_method(Constants.ACC_PUBLIC, SET_CALLBACK, null);
		e.load_arg(0);
		e.process_switch(keys, new ProcessSwitchCallback() {
			public void processCase(int key, Label end) {
				e.load_this();
				e.load_arg(1);
				e.checkcast(callbackTypes[key]);
				e.putfield(getCallbackField(key));
				e.goTo(end);
			}

			public void processDefault() {
				// TODO: error?
			}
		});
		e.return_value();
		e.end_method();
	}

	private void emitSetCallbacks(ClassEmitter ce) {
		CodeEmitter e = ce.begin_method(Constants.ACC_PUBLIC, SET_CALLBACKS, null);
		e.load_this();
		e.load_arg(0);
		for (int i = 0; i < callbackTypes.length; i++) {
			e.dup2();
			e.aaload(i);
			e.checkcast(callbackTypes[i]);
			e.putfield(getCallbackField(i));
		}
		e.return_value();
		e.end_method();
	}

	private void emitGetCallbacks(ClassEmitter ce) {
		CodeEmitter e = ce.begin_method(Constants.ACC_PUBLIC, GET_CALLBACKS, null);
		e.load_this();
		e.invoke_static_this(BIND_CALLBACKS);
		e.load_this();
		e.push(callbackTypes.length);
		e.newarray(CALLBACK);
		for (int i = 0; i < callbackTypes.length; i++) {
			e.dup();
			e.push(i);
			e.load_this();
			e.getfield(getCallbackField(i));
			e.aastore();
		}
		e.return_value();
		e.end_method();
	}

	private void emitNewInstanceCallbacks(ClassEmitter ce) {
		CodeEmitter e = ce.begin_method(Constants.ACC_PUBLIC, NEW_INSTANCE, null);
		Type thisType = getThisType(e);
		e.load_arg(0);
		e.invoke_static(thisType, SET_THREAD_CALLBACKS, false);
		emitCommonNewInstance(e);
	}

	private Type getThisType(CodeEmitter e) {
		if (currentData == null) {
			return e.getClassEmitter().getClassType();
		}
		else {
			return Type.getType(currentData.generatedClass);
		}
	}

	private void emitCommonNewInstance(CodeEmitter e) {
		Type thisType = getThisType(e);
		e.new_instance(thisType);
		e.dup();
		e.invoke_constructor(thisType);
		e.aconst_null();
		e.invoke_static(thisType, SET_THREAD_CALLBACKS, false);
		e.return_value();
		e.end_method();
	}

	private void emitNewInstanceCallback(ClassEmitter ce) {
		CodeEmitter e = ce.begin_method(Constants.ACC_PUBLIC, SINGLE_NEW_INSTANCE, null);
		switch (callbackTypes.length) {
			case 0:
				// TODO: make sure Callback is null
				break;
			case 1:
				// for now just make a new array; TODO: optimize
				e.push(1);
				e.newarray(CALLBACK);
				e.dup();
				e.push(0);
				e.load_arg(0);
				e.aastore();
				e.invoke_static(getThisType(e), SET_THREAD_CALLBACKS, false);
				break;
			default:
				e.throw_exception(ILLEGAL_STATE_EXCEPTION, "More than one callback object required");
		}
		emitCommonNewInstance(e);
	}

	private void emitNewInstanceMultiarg(ClassEmitter ce, List constructors) {
		final CodeEmitter e = ce.begin_method(Constants.ACC_PUBLIC, MULTIARG_NEW_INSTANCE, null);
		final Type thisType = getThisType(e);
		e.load_arg(2);
		e.invoke_static(thisType, SET_THREAD_CALLBACKS, false);
		e.new_instance(thisType);
		e.dup();
		e.load_arg(0);
		EmitUtils.constructor_switch(e, constructors, new ObjectSwitchCallback() {
			public void processCase(Object key, Label end) {
				MethodInfo constructor = (MethodInfo) key;
				Type types[] = constructor.getSignature().getArgumentTypes();
				for (int i = 0; i < types.length; i++) {
					e.load_arg(1);
					e.push(i);
					e.aaload();
					e.unbox(types[i]);
				}
				e.invoke_constructor(thisType, constructor.getSignature());
				e.goTo(end);
			}

			public void processDefault() {
				e.throw_exception(ILLEGAL_ARGUMENT_EXCEPTION, "Constructor not found");
			}
		});
		e.aconst_null();
		e.invoke_static(thisType, SET_THREAD_CALLBACKS, false);
		e.return_value();
		e.end_method();
	}

	private void emitMethods(final ClassEmitter ce, List methods, List actualMethods) {
		CallbackGenerator[] generators = CallbackInfo.getGenerators(callbackTypes);

		Map groups = new HashMap();
		final Map indexes = new HashMap();
		final Map originalModifiers = new HashMap();
		final Map positions = CollectionUtils.getIndexMap(methods);
		final Map declToBridge = new HashMap();

		Iterator it1 = methods.iterator();
		Iterator it2 = (actualMethods != null) ? actualMethods.iterator() : null;

		while (it1.hasNext()) {
			MethodInfo method = (MethodInfo) it1.next();
			Method actualMethod = (it2 != null) ? (Method) it2.next() : null;
			int index = filter.accept(actualMethod);
			if (index >= callbackTypes.length) {
				throw new IllegalArgumentException("Callback filter returned an index that is too large: " + index);
			}
			originalModifiers.put(method, (actualMethod != null ? actualMethod.getModifiers() : method.getModifiers()));
			indexes.put(method, index);
			List group = (List) groups.get(generators[index]);
			if (group == null) {
				groups.put(generators[index], group = new ArrayList(methods.size()));
			}
			group.add(method);

			// Optimization: build up a map of Class -> bridge methods in class
			// so that we can look up all the bridge methods in one pass for a class.
			if (TypeUtils.isBridge(actualMethod.getModifiers())) {
				Set bridges = (Set) declToBridge.get(actualMethod.getDeclaringClass());
				if (bridges == null) {
					bridges = new HashSet();
					declToBridge.put(actualMethod.getDeclaringClass(), bridges);
				}
				bridges.add(method.getSignature());
			}
		}

		final Map bridgeToTarget = new BridgeMethodResolver(declToBridge, getClassLoader()).resolveAll();

		Set seenGen = new HashSet();
		CodeEmitter se = ce.getStaticHook();
		se.new_instance(THREAD_LOCAL);
		se.dup();
		se.invoke_constructor(THREAD_LOCAL, CSTRUCT_NULL);
		se.putfield(THREAD_CALLBACKS_FIELD);

		CallbackGenerator.Context context = new CallbackGenerator.Context() {
			public ClassLoader getClassLoader() {
				return Enhancer.this.getClassLoader();
			}

			public int getOriginalModifiers(MethodInfo method) {
				return ((Integer) originalModifiers.get(method)).intValue();
			}

			public int getIndex(MethodInfo method) {
				return ((Integer) indexes.get(method)).intValue();
			}

			public void emitCallback(CodeEmitter e, int index) {
				emitCurrentCallback(e, index);
			}

			public Signature getImplSignature(MethodInfo method) {
				return rename(method.getSignature(), ((Integer) positions.get(method)).intValue());
			}

			public void emitLoadArgsAndInvoke(CodeEmitter e, MethodInfo method) {
				// If this is a bridge and we know the target was called from invokespecial,
				// then we need to invoke_virtual w/ the bridge target instead of doing
				// a super, because super may itself be using super, which would bypass
				// any proxies on the target.
				Signature bridgeTarget = (Signature) bridgeToTarget.get(method.getSignature());
				if (bridgeTarget != null) {
					// checkcast each argument against the target's argument types
					for (int i = 0; i < bridgeTarget.getArgumentTypes().length; i++) {
						e.load_arg(i);
						Type target = bridgeTarget.getArgumentTypes()[i];
						if (!target.equals(method.getSignature().getArgumentTypes()[i])) {
							e.checkcast(target);
						}
					}

					e.invoke_virtual_this(bridgeTarget);

					Type retType = method.getSignature().getReturnType();
					// Not necessary to cast if the target & bridge have
					// the same return type.
					// (This conveniently includes void and primitive types,
					// which would fail if casted.  It's not possible to
					// covariant from boxed to unbox (or vice versa), so no having
					// to box/unbox for bridges).
					// TODO: It also isn't necessary to checkcast if the return is
					// assignable from the target.  (This would happen if a subclass
					// used covariant returns to narrow the return type within a bridge
					// method.)
					if (!retType.equals(bridgeTarget.getReturnType())) {
						e.checkcast(retType);
					}
				}
				else {
					e.load_args();
					e.super_invoke(method.getSignature());
				}
			}

			public CodeEmitter beginMethod(ClassEmitter ce, MethodInfo method) {
				CodeEmitter e = EmitUtils.begin_method(ce, method);
				if (!interceptDuringConstruction &&
						!TypeUtils.isAbstract(method.getModifiers())) {
					Label constructed = e.make_label();
					e.load_this();
					e.getfield(CONSTRUCTED_FIELD);
					e.if_jump(CodeEmitter.NE, constructed);
					e.load_this();
					e.load_args();
					e.super_invoke();
					e.return_value();
					e.mark(constructed);
				}
				return e;
			}
		};
		for (int i = 0; i < callbackTypes.length; i++) {
			CallbackGenerator gen = generators[i];
			if (!seenGen.contains(gen)) {
				seenGen.add(gen);
				final List fmethods = (List) groups.get(gen);
				if (fmethods != null) {
					try {
						gen.generate(ce, context, fmethods);
						gen.generateStatic(se, context, fmethods);
					}
					catch (RuntimeException x) {
						throw x;
					}
					catch (Exception x) {
						throw new CodeGenerationException(x);
					}
				}
			}
		}
		se.return_value();
		se.end_method();
	}

	private void emitSetThreadCallbacks(ClassEmitter ce) {
		CodeEmitter e = ce.begin_method(Constants.ACC_PUBLIC | Constants.ACC_STATIC,
				SET_THREAD_CALLBACKS,
				null);
		e.getfield(THREAD_CALLBACKS_FIELD);
		e.load_arg(0);
		e.invoke_virtual(THREAD_LOCAL, THREAD_LOCAL_SET);
		e.return_value();
		e.end_method();
	}

	private void emitSetStaticCallbacks(ClassEmitter ce) {
		CodeEmitter e = ce.begin_method(Constants.ACC_PUBLIC | Constants.ACC_STATIC,
				SET_STATIC_CALLBACKS,
				null);
		e.load_arg(0);
		e.putfield(STATIC_CALLBACKS_FIELD);
		e.return_value();
		e.end_method();
	}

	private void emitCurrentCallback(CodeEmitter e, int index) {
		e.load_this();
		e.getfield(getCallbackField(index));
		e.dup();
		Label end = e.make_label();
		e.ifnonnull(end);
		e.pop(); // stack height
		e.load_this();
		e.invoke_static_this(BIND_CALLBACKS);
		e.load_this();
		e.getfield(getCallbackField(index));
		e.mark(end);
	}

	private void emitBindCallbacks(ClassEmitter ce) {
		CodeEmitter e = ce.begin_method(Constants.PRIVATE_FINAL_STATIC,
				BIND_CALLBACKS,
				null);
		Local me = e.make_local();
		e.load_arg(0);
		e.checkcast_this();
		e.store_local(me);

		Label end = e.make_label();
		e.load_local(me);
		e.getfield(BOUND_FIELD);
		e.if_jump(CodeEmitter.NE, end);
		e.load_local(me);
		e.push(1);
		e.putfield(BOUND_FIELD);

		e.getfield(THREAD_CALLBACKS_FIELD);
		e.invoke_virtual(THREAD_LOCAL, THREAD_LOCAL_GET);
		e.dup();
		Label found_callback = e.make_label();
		e.ifnonnull(found_callback);
		e.pop();

		e.getfield(STATIC_CALLBACKS_FIELD);
		e.dup();
		e.ifnonnull(found_callback);
		e.pop();
		e.goTo(end);

		e.mark(found_callback);
		e.checkcast(CALLBACK_ARRAY);
		e.load_local(me);
		e.swap();
		for (int i = callbackTypes.length - 1; i >= 0; i--) {
			if (i != 0) {
				e.dup2();
			}
			e.aaload(i);
			e.checkcast(callbackTypes[i]);
			e.putfield(getCallbackField(i));
		}

		e.mark(end);
		e.return_value();
		e.end_method();
	}

	private static String getCallbackField(int index) {
		return "CGLIB$CALLBACK_" + index;
	}

}
