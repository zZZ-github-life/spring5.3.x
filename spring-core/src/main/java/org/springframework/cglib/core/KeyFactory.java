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

import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.List;

import org.springframework.asm.ClassVisitor;
import org.springframework.asm.Label;
import org.springframework.asm.Type;
import org.springframework.cglib.core.internal.CustomizerRegistry;

/**
 * 生成用于处理多值键的类，供 Maps 和 Sets 等场景使用。<code>equals</code> 和 <code>hashCode</code>
 * 方法的代码遵循 Joshua Bloch 在<i>Effective Java</i>中提出的规则。<p>要生成 <code>KeyFactory
 * </code>，需要提供一个描述键结构的接口。该接口应有一个名为 <code>newInstance</code> 的单一方法，
 * 该方法返回 <code>Object</code>。参数数组可以是<i>任意内容</i>——对象、基本类型值，或其中任一类型
 * 的单维或多维数组。例如：
 * <p><pre>
 *     private interface IntStringKey {
 *         public Object newInstance(int i, String s);
 *     }
 * </pre><p>
 * 创建 <code>KeyFactory</code> 之后，通过调用接口定义的 <code>newInstance</code> 方法生成新的键。
 * <p><pre>
 *     IntStringKey factory = (IntStringKey)KeyFactory.create(IntStringKey.class);
 *     Object key1 = factory.newInstance(4, "Hello");
 *     Object key2 = factory.newInstance(4, "World");
 * </pre><p>
 * <b>注意：</b><code>key1</code> 和 <code>key2</code> 两个键之间 <code>hashCode</code> 的相等性只
 * 有在 <code>key1.equals(key2)</code> <i>并且</i>这两个键由同一工厂生成时才能保证。
 * @version $Id: KeyFactory.java,v 1.26 2006/03/05 02:43:19 herbyderby Exp $
 */
@SuppressWarnings({"rawtypes", "unchecked"})
abstract public class KeyFactory {

	private static final Signature GET_NAME =
			TypeUtils.parseSignature("String getName()");

	private static final Signature GET_CLASS =
			TypeUtils.parseSignature("Class getClass()");

	private static final Signature HASH_CODE =
			TypeUtils.parseSignature("int hashCode()");

	private static final Signature EQUALS =
			TypeUtils.parseSignature("boolean equals(Object)");

	private static final Signature TO_STRING =
			TypeUtils.parseSignature("String toString()");

	private static final Signature APPEND_STRING =
			TypeUtils.parseSignature("StringBuffer append(String)");

	private static final Type KEY_FACTORY =
			TypeUtils.parseType("org.springframework.cglib.core.KeyFactory");

	private static final Signature GET_SORT =
			TypeUtils.parseSignature("int getSort()");

	//generated numbers:
	private final static int PRIMES[] = {
			11, 73, 179, 331,
			521, 787, 1213, 1823,
			2609, 3691, 5189, 7247,
			10037, 13931, 19289, 26627,
			36683, 50441, 69403, 95401,
			131129, 180179, 247501, 340057,
			467063, 641371, 880603, 1209107,
			1660097, 2279161, 3129011, 4295723,
			5897291, 8095873, 11114263, 15257791,
			20946017, 28754629, 39474179, 54189869,
			74391461, 102123817, 140194277, 192456917,
			264202273, 362693231, 497900099, 683510293,
			938313161, 1288102441, 1768288259};


	public static final Customizer CLASS_BY_NAME = new Customizer() {
		public void customize(CodeEmitter e, Type type) {
			if (type.equals(Constants.TYPE_CLASS)) {
				e.invoke_virtual(Constants.TYPE_CLASS, GET_NAME);
			}
		}
	};

	public static final FieldTypeCustomizer STORE_CLASS_AS_STRING = new FieldTypeCustomizer() {
		public void customize(CodeEmitter e, int index, Type type) {
			if (type.equals(Constants.TYPE_CLASS)) {
				e.invoke_virtual(Constants.TYPE_CLASS, GET_NAME);
			}
		}
		public Type getOutType(int index, Type type) {
			if (type.equals(Constants.TYPE_CLASS)) {
				return Constants.TYPE_STRING;
			}
			return type;
		}
	};

	/**
	 * {@link Type#hashCode()} 的开销很大，因为它会遍历完整描述符来计算哈希码。此定制器使用
	 * {@link Type#getSort()} 作为哈希码。
	 */
	public static final HashCodeCustomizer HASH_ASM_TYPE = new HashCodeCustomizer() {
		public boolean customize(CodeEmitter e, Type type) {
			if (Constants.TYPE_TYPE.equals(type)) {
				e.invoke_virtual(type, GET_SORT);
				return true;
			}
			return false;
		}
	};

	/**
	 * @deprecated this customizer might result in unexpected class leak since key object still holds a strong reference to the Object and class.
	 * It is recommended to have pre-processing method that would strip Objects and represent Classes as Strings
	 */
	@Deprecated
	public static final Customizer OBJECT_BY_CLASS = new Customizer() {
		public void customize(CodeEmitter e, Type type) {
			e.invoke_virtual(Constants.TYPE_OBJECT, GET_CLASS);
		}
	};

	protected KeyFactory() {
	}

	public static KeyFactory create(Class keyInterface) {
		return create(keyInterface, null);
	}

	public static KeyFactory create(Class keyInterface, Customizer customizer) {
		return create(keyInterface.getClassLoader(), keyInterface, customizer);
	}

	public static KeyFactory create(Class keyInterface, KeyFactoryCustomizer first, List<KeyFactoryCustomizer> next) {
		return create(keyInterface.getClassLoader(), keyInterface, first, next);
	}

	public static KeyFactory create(ClassLoader loader, Class keyInterface, Customizer customizer) {
		return create(loader, keyInterface, customizer, Collections.<KeyFactoryCustomizer>emptyList());
	}

	public static KeyFactory create(ClassLoader loader, Class keyInterface, KeyFactoryCustomizer customizer,
			List<KeyFactoryCustomizer> next) {
		Generator gen = new Generator();
		gen.setInterface(keyInterface);
		// SPRING PATCH BEGIN
		gen.setContextClass(keyInterface);
		// SPRING PATCH END

		if (customizer != null) {
			gen.addCustomizer(customizer);
		}
		if (next != null && !next.isEmpty()) {
			for (KeyFactoryCustomizer keyFactoryCustomizer : next) {
				gen.addCustomizer(keyFactoryCustomizer);
			}
		}
		gen.setClassLoader(loader);
		return gen.create();
	}


	public static class Generator extends AbstractClassGenerator {

		private static final Source SOURCE = new Source(KeyFactory.class.getName());

		private static final Class[] KNOWN_CUSTOMIZER_TYPES = new Class[]{Customizer.class, FieldTypeCustomizer.class};

		private Class keyInterface;

		// TODO: Make me final when deprecated methods are removed
		private CustomizerRegistry customizers = new CustomizerRegistry(KNOWN_CUSTOMIZER_TYPES);

		private int constant;

		private int multiplier;

		public Generator() {
			super(SOURCE);
		}

		protected ClassLoader getDefaultClassLoader() {
			return keyInterface.getClassLoader();
		}

		protected ProtectionDomain getProtectionDomain() {
			return ReflectUtils.getProtectionDomain(keyInterface);
		}

		/**
		 * @deprecated Use {@link #addCustomizer(KeyFactoryCustomizer)} instead.
		 */
		@Deprecated
		public void setCustomizer(Customizer customizer) {
			customizers = CustomizerRegistry.singleton(customizer);
		}

		public void addCustomizer(KeyFactoryCustomizer customizer) {
			customizers.add(customizer);
		}

		public <T> List<T> getCustomizers(Class<T> klass) {
			return customizers.get(klass);
		}

		public void setInterface(Class keyInterface) {
			this.keyInterface = keyInterface;
		}

		public KeyFactory create() {
			setNamePrefix(keyInterface.getName());
			return (KeyFactory) super.create(keyInterface.getName());
		}

		public void setHashConstant(int constant) {
			this.constant = constant;
		}

		public void setHashMultiplier(int multiplier) {
			this.multiplier = multiplier;
		}

		protected Object firstInstance(Class type) {
			return ReflectUtils.newInstance(type);
		}

		protected Object nextInstance(Object instance) {
			return instance;
		}

		public void generateClass(ClassVisitor v) {
			ClassEmitter ce = new ClassEmitter(v);

			Method newInstance = ReflectUtils.findNewInstance(keyInterface);
			if (!newInstance.getReturnType().equals(Object.class)) {
				throw new IllegalArgumentException("newInstance method must return Object");
			}

			Type[] parameterTypes = TypeUtils.getTypes(newInstance.getParameterTypes());
			ce.begin_class(Constants.V1_8,
					Constants.ACC_PUBLIC,
					getClassName(),
					KEY_FACTORY,
					new Type[]{Type.getType(keyInterface)},
					Constants.SOURCE_FILE);
			EmitUtils.null_constructor(ce);
			EmitUtils.factory_method(ce, ReflectUtils.getSignature(newInstance));

			int seed = 0;
			CodeEmitter e = ce.begin_method(Constants.ACC_PUBLIC,
					TypeUtils.parseConstructor(parameterTypes),
					null);
			e.load_this();
			e.super_invoke_constructor();
			e.load_this();
			List<FieldTypeCustomizer> fieldTypeCustomizers = getCustomizers(FieldTypeCustomizer.class);
			for (int i = 0; i < parameterTypes.length; i++) {
				Type parameterType = parameterTypes[i];
				Type fieldType = parameterType;
				for (FieldTypeCustomizer customizer : fieldTypeCustomizers) {
					fieldType = customizer.getOutType(i, fieldType);
				}
				seed += fieldType.hashCode();
				ce.declare_field(Constants.ACC_PRIVATE | Constants.ACC_FINAL,
						getFieldName(i),
						fieldType,
						null);
				e.dup();
				e.load_arg(i);
				for (FieldTypeCustomizer customizer : fieldTypeCustomizers) {
					customizer.customize(e, i, parameterType);
				}
				e.putfield(getFieldName(i));
			}
			e.return_value();
			e.end_method();

			// hash code
			e = ce.begin_method(Constants.ACC_PUBLIC, HASH_CODE, null);
			int hc = (constant != 0) ? constant : PRIMES[(Math.abs(seed) % PRIMES.length)];
			int hm = (multiplier != 0) ? multiplier : PRIMES[(Math.abs(seed * 13) % PRIMES.length)];
			e.push(hc);
			for (int i = 0; i < parameterTypes.length; i++) {
				e.load_this();
				e.getfield(getFieldName(i));
				EmitUtils.hash_code(e, parameterTypes[i], hm, customizers);
			}
			e.return_value();
			e.end_method();

			// equals
			e = ce.begin_method(Constants.ACC_PUBLIC, EQUALS, null);
			Label fail = e.make_label();
			e.load_arg(0);
			e.instance_of_this();
			e.if_jump(CodeEmitter.EQ, fail);
			for (int i = 0; i < parameterTypes.length; i++) {
				e.load_this();
				e.getfield(getFieldName(i));
				e.load_arg(0);
				e.checkcast_this();
				e.getfield(getFieldName(i));
				EmitUtils.not_equals(e, parameterTypes[i], fail, customizers);
			}
			e.push(1);
			e.return_value();
			e.mark(fail);
			e.push(0);
			e.return_value();
			e.end_method();

			// toString
			e = ce.begin_method(Constants.ACC_PUBLIC, TO_STRING, null);
			e.new_instance(Constants.TYPE_STRING_BUFFER);
			e.dup();
			e.invoke_constructor(Constants.TYPE_STRING_BUFFER);
			for (int i = 0; i < parameterTypes.length; i++) {
				if (i > 0) {
					e.push(", ");
					e.invoke_virtual(Constants.TYPE_STRING_BUFFER, APPEND_STRING);
				}
				e.load_this();
				e.getfield(getFieldName(i));
				EmitUtils.append_string(e, parameterTypes[i], EmitUtils.DEFAULT_DELIMITERS, customizers);
			}
			e.invoke_virtual(Constants.TYPE_STRING_BUFFER, TO_STRING);
			e.return_value();
			e.end_method();

			ce.end_class();
		}

		private String getFieldName(int arg) {
			return "FIELD_" + arg;
		}
	}

}
