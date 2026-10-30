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

package org.springframework.aop.support;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.IntroductionInfo;
import org.springframework.util.ClassUtils;

/**
 * 为 {@link org.springframework.aop.IntroductionInfo} 的实现提供支持。
 *
 * <p>允许子类方便地添加给定对象的所有接口，并抑制不应添加的接口。还允许查询所有已引入的接口。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class IntroductionInfoSupport implements IntroductionInfo, Serializable {

	protected final Set<Class<?>> publishedInterfaces = new LinkedHashSet<>();

	private transient Map<Method, Boolean> rememberedMethods = new ConcurrentHashMap<>(32);


	/**
	 * 抑制指定的接口，该接口可能因委托实现了它而被自动检测到。调用此方法可将内部接口排除在代理层可见范
	 * 围之外。<p>如果委托未实现该接口，则此方法不执行任何操作。
	 * @param ifc 要抑制的接口
	 */
	public void suppressInterface(Class<?> ifc) {
		this.publishedInterfaces.remove(ifc);
	}

	@Override
	public Class<?>[] getInterfaces() {
		return ClassUtils.toClassArray(this.publishedInterfaces);
	}

	/**
	 * 检查指定的接口是否为已发布的引入接口。
	 * @param ifc 要检查的接口
	 * @return 该接口是否属于此引介
	 */
	public boolean implementsInterface(Class<?> ifc) {
		for (Class<?> pubIfc : this.publishedInterfaces) {
			if (ifc.isInterface() && ifc.isAssignableFrom(pubIfc)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 在代理层发布给定委托所实现的所有接口。
	 * @param delegate 委托对象
	 */
	protected void implementInterfacesOnObject(Object delegate) {
		this.publishedInterfaces.addAll(ClassUtils.getAllInterfacesAsSet(delegate));
	}

	/**
	 * 此方法是否位于已引入的接口上？
	 * @param mi 方法调用
	 * @return 所调用的方法是否位于被引介的接口上
	 */
	protected final boolean isMethodOnIntroducedInterface(MethodInvocation mi) {
		Boolean rememberedResult = this.rememberedMethods.get(mi.getMethod());
		if (rememberedResult != null) {
			return rememberedResult;
		}
		else {
			// Work it out and cache it.
			boolean result = implementsInterface(mi.getMethod().getDeclaringClass());
			this.rememberedMethods.put(mi.getMethod(), result);
			return result;
		}
	}


	//---------------------------------------------------------------------
	// Serialization support
	//---------------------------------------------------------------------

	/**
	 * 实现此方法仅是为了恢复 logger。我们没有将 logger 设为静态，因为那样意味着子类将使用此类的日志类
	 * 别。
	 */
	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		// Rely on default serialization; just initialize state after deserialization.
		ois.defaultReadObject();
		// Initialize transient fields.
		this.rememberedMethods = new ConcurrentHashMap<>(32);
	}

}
