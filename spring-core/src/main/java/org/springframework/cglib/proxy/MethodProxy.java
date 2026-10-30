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

package org.springframework.cglib.proxy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.springframework.cglib.core.AbstractClassGenerator;
import org.springframework.cglib.core.CodeGenerationException;
import org.springframework.cglib.core.GeneratorStrategy;
import org.springframework.cglib.core.NamingPolicy;
import org.springframework.cglib.core.Signature;
import org.springframework.cglib.reflect.FastClass;

/**
 * 由 {@link Enhancer} 生成的类在调用被拦截的方法时，
 * 会把此对象传递给已注册的 {@link MethodInterceptor} 对象。它可用于调用原始方法，
 * 或在相同类型的另一个对象上调用同一方法。
 * @version $Id: MethodProxy.java,v 1.16 2009/01/11 20:09:48 herbyderby Exp $
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class MethodProxy {

	private Signature sig1;

	private Signature sig2;

	private CreateInfo createInfo;

	private final Object initLock = new Object();

	private volatile FastClassInfo fastClassInfo;

	/**
	 * 仅供 {@link Enhancer} 内部使用；
	 * 类似功能请参见 {@link org.springframework.cglib.reflect.FastMethod} 类。
	 */
	public static MethodProxy create(Class c1, Class c2, String desc, String name1, String name2) {
		MethodProxy proxy = new MethodProxy();
		proxy.sig1 = new Signature(name1, desc);
		proxy.sig2 = new Signature(name2, desc);
		proxy.createInfo = new CreateInfo(c1, c2);
		return proxy;
	}

	private void init() {
		/*
		 * Using a volatile invariant allows us to initialize the FastClass and
		 * method index pairs atomically.
		 *
		 * Double-checked locking is safe with volatile in Java 5.  Before 1.5 this
		 * code could allow fastClassInfo to be instantiated more than once, which
		 * appears to be benign.
		 */
		if (fastClassInfo == null) {
			synchronized (initLock) {
				if (fastClassInfo == null) {
					CreateInfo ci = createInfo;

					FastClassInfo fci = new FastClassInfo();
					fci.f1 = helper(ci, ci.c1);
					fci.f2 = helper(ci, ci.c2);
					fci.i1 = fci.f1.getIndex(sig1);
					fci.i2 = fci.f2.getIndex(sig2);
					fastClassInfo = fci;
					createInfo = null;
				}
			}
		}
	}


	private static class FastClassInfo {

		FastClass f1;

		FastClass f2;

		int i1;

		int i2;
	}


	private static class CreateInfo {

		Class c1;

		Class c2;

		NamingPolicy namingPolicy;

		GeneratorStrategy strategy;

		boolean attemptLoad;

		public CreateInfo(Class c1, Class c2) {
			this.c1 = c1;
			this.c2 = c2;
			AbstractClassGenerator fromEnhancer = AbstractClassGenerator.getCurrent();
			if (fromEnhancer != null) {
				namingPolicy = fromEnhancer.getNamingPolicy();
				strategy = fromEnhancer.getStrategy();
				attemptLoad = fromEnhancer.getAttemptLoad();
			}
		}
	}


	private static FastClass helper(CreateInfo ci, Class type) {
		FastClass.Generator g = new FastClass.Generator();
		g.setType(type);
		// SPRING PATCH BEGIN
		g.setContextClass(type);
		// SPRING PATCH END
		g.setClassLoader(ci.c2.getClassLoader());
		g.setNamingPolicy(ci.namingPolicy);
		g.setStrategy(ci.strategy);
		g.setAttemptLoad(ci.attemptLoad);
		return g.create();
	}

	private MethodProxy() {
	}

	/**
	 * 返回被代理方法的签名。
	 */
	public Signature getSignature() {
		return sig1;
	}

	/**
	 * 返回由 CGLIB 创建的合成方法的名称，
	 * {@link #invokeSuper} 使用该方法调用父类的（未被拦截的）方法实现。
	 * 其参数类型与被代理方法相同。
	 */
	public String getSuperName() {
		return sig2.getName();
	}

	/**
	 * 返回 {@link #invokeSuper} 所使用的方法在
	 * {@link org.springframework.cglib.reflect.FastClass} 中的方法索引。
	 * 该索引在生成的代理中唯一标识该方法，因此可用于引用外部元数据。
	 * @see #getSuperName
	 */
	public int getSuperIndex() {
		init();
		return fastClassInfo.i2;
	}

	// For testing
	FastClass getFastClass() {
		init();
		return fastClassInfo.f1;
	}

	// For testing
	FastClass getSuperFastClass() {
		init();
		return fastClassInfo.f2;
	}

	/**
	 * 返回拦截与给定签名匹配的方法时所使用的 <code>MethodProxy</code>。
	 * @param type 由 Enhancer 生成的类
	 * @param sig 要匹配的签名
	 * @return MethodProxy 实例，若未找到适用的匹配方法则为 null
	 * @throws IllegalArgumentException 如果该类不是由 Enhancer 创建，或未使用 MethodInterceptor
	 */
	public static MethodProxy find(Class type, Signature sig) {
		try {
			Method m = type.getDeclaredMethod(MethodInterceptorGenerator.FIND_PROXY_NAME,
					MethodInterceptorGenerator.FIND_PROXY_TYPES);
			return (MethodProxy) m.invoke(null, new Object[]{sig});
		}
		catch (NoSuchMethodException ex) {
			throw new IllegalArgumentException("Class " + type + " does not use a MethodInterceptor");
		}
		catch (IllegalAccessException | InvocationTargetException ex) {
			throw new CodeGenerationException(ex);
		}
	}

	/**
	 * 在相同类型的另一个对象上调用原始方法。
	 * @param obj 兼容的对象；如果使用作为 MethodInterceptor 第一个参数传入的对象，会导致递归（通常不是你想要的）
	 * @param args 传递给被拦截方法的参数；只要类型兼容，可以替换为不同的参数数组
	 * @throws Throwable 被调用方法抛出的原始异常会直接传递，而不会被包装为 <code>InvocationTargetException</code>
	 * @see MethodInterceptor#intercept
	 */
	public Object invoke(Object obj, Object[] args) throws Throwable {
		try {
			init();
			FastClassInfo fci = fastClassInfo;
			return fci.f1.invoke(fci.i1, obj, args);
		}
		catch (InvocationTargetException ex) {
			throw ex.getTargetException();
		}
		catch (IllegalArgumentException ex) {
			if (fastClassInfo.i1 < 0)
				throw new IllegalArgumentException("Protected method: " + sig1);
			throw ex;
		}
	}

	/**
	 * 在指定的对象上调用原始（super）方法。
	 * @param obj 增强后的对象，必须是作为 MethodInterceptor 第一个参数传入的对象
	 * @param args 传递给被拦截方法的参数；只要类型兼容，可以替换为不同的参数数组
	 * @throws Throwable 被调用方法抛出的原始异常会直接传递，而不会被包装为 <code>InvocationTargetException</code>
	 * @see MethodInterceptor#intercept
	 */
	public Object invokeSuper(Object obj, Object[] args) throws Throwable {
		try {
			/*
			* 在这里会真正调用@bean的方法，实例化bean
			* 	1.fci.f2 使用动态代理创建的类并已经实例化。
			* 	2.调用invoke之后，内存中会调用obj.CGLIB$@Bean$1方法，再调用父类的方法（@Bean）生成对象。 obj-->代理类，配置类的子类
			* */
			init();
			FastClassInfo fci = fastClassInfo;
			return fci.f2.invoke(fci.i2, obj, args);
		}
		catch (InvocationTargetException e) {
			throw e.getTargetException();
		}
	}

}
