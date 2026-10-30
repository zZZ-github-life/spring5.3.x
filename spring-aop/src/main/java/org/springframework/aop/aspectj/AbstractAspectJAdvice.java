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

package org.springframework.aop.aspectj;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInvocation;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.weaver.tools.JoinPointMatch;
import org.aspectj.weaver.tools.PointcutParameter;

import org.springframework.aop.AopInvocationException;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;
import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.aop.interceptor.ExposeInvocationInterceptor;
import org.springframework.aop.support.ComposablePointcut;
import org.springframework.aop.support.MethodMatchers;
import org.springframework.aop.support.StaticMethodMatcher;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * 用于包装 AspectJ 切面或带 AspectJ 注解的通知方法的 AOP Alliance
 * {@link org.aopalliance.aop.Advice} 类的基类。
 *
 * @author Rod Johnson
 * @author Adrian Colyer
 * @author Juergen Hoeller
 * @author Ramnivas Laddad
 * @since 2.0
 */
@SuppressWarnings("serial")
public abstract class AbstractAspectJAdvice implements Advice, AspectJPrecedenceInformation, Serializable {

	/**
	 * 用于当前连接点的 ReflectiveMethodInvocation userAttributes 映射中的键。
	 */
	protected static final String JOIN_POINT_KEY = JoinPoint.class.getName();


	/**
	 * 为当前调用延迟实例化连接点。要求 MethodInvocation 已通过 ExposeInvocationInterceptor 绑定。<p>如
	 * 果可以访问当前的 ReflectiveMethodInvocation（在环绕通知中），则不要使用。
	 * @return 当前的 AspectJ 连接点；如果我们不在 Spring AOP 调用中，则抛出异常。
	 */
	public static JoinPoint currentJoinPoint() {
		MethodInvocation mi = ExposeInvocationInterceptor.currentInvocation();
		if (!(mi instanceof ProxyMethodInvocation)) {
			throw new IllegalStateException("MethodInvocation is not a Spring ProxyMethodInvocation: " + mi);
		}
		ProxyMethodInvocation pmi = (ProxyMethodInvocation) mi;
		JoinPoint jp = (JoinPoint) pmi.getUserAttribute(JOIN_POINT_KEY);
		if (jp == null) {
			jp = new MethodInvocationProceedingJoinPoint(pmi);
			pmi.setUserAttribute(JOIN_POINT_KEY, jp);
		}
		return jp;
	}


	private final Class<?> declaringClass;

	private final String methodName;

	private final Class<?>[] parameterTypes;

	protected transient Method aspectJAdviceMethod;

	private final AspectJExpressionPointcut pointcut;

	private final AspectInstanceFactory aspectInstanceFactory;

	/**
	 * 定义此通知的切面（引用 bean）名称（在确定通知优先级时使用，以便我们可以判断两条通知是否来自同一
	 * 个切面）。
	 */
	private String aspectName = "";

	/**
	 * 此通知在切面中的声明顺序。
	 */
	private int declarationOrder;

	/**
	 * 如果此通知对象的创建者知道参数名称并显式设置它们，则此值将非 null。
	 */
	@Nullable
	private String[] argumentNames;

	/** 当 after throwing 通知绑定了抛出的值时非 null。 */
	@Nullable
	private String throwingName;

	/** 当 after returning 通知绑定了返回值时非 null。 */
	@Nullable
	private String returningName;

	private Class<?> discoveredReturningType = Object.class;

	private Class<?> discoveredThrowingType = Object.class;

	/**
	 * thisJoinPoint 参数的索引（目前如果存在，仅支持索引 0）。
	 */
	private int joinPointArgumentIndex = -1;

	/**
	 * thisJoinPointStaticPart 参数的索引（目前如果存在，仅支持索引 0）。
	 */
	private int joinPointStaticPartArgumentIndex = -1;

	@Nullable
	private Map<String, Integer> argumentBindings;

	private boolean argumentsIntrospected = false;

	@Nullable
	private Type discoveredReturningGenericType;
	// Note: Unlike return type, no such generic information is needed for the throwing type,
	// since Java doesn't allow exception types to be parameterized.


	/**
	 * 为给定的通知方法创建一个新的 AbstractAspectJAdvice。
	 * @param aspectJAdviceMethod AspectJ 风格的通知方法
	 * @param pointcut AspectJ 表达式切点
	 * @param aspectInstanceFactory 切面实例的工厂
	 */
	public AbstractAspectJAdvice(
			Method aspectJAdviceMethod, AspectJExpressionPointcut pointcut, AspectInstanceFactory aspectInstanceFactory) {

		Assert.notNull(aspectJAdviceMethod, "Advice method must not be null");
		this.declaringClass = aspectJAdviceMethod.getDeclaringClass();
		this.methodName = aspectJAdviceMethod.getName();
		this.parameterTypes = aspectJAdviceMethod.getParameterTypes();
		this.aspectJAdviceMethod = aspectJAdviceMethod;
		this.pointcut = pointcut;
		this.aspectInstanceFactory = aspectInstanceFactory;
	}


	/**
	 * 返回 AspectJ 风格的通知方法。
	 */
	public final Method getAspectJAdviceMethod() {
		return this.aspectJAdviceMethod;
	}

	/**
	 * 返回 AspectJ 表达式切点。
	 */
	public final AspectJExpressionPointcut getPointcut() {
		calculateArgumentBindings();
		return this.pointcut;
	}

	/**
	 * 构建一个“安全”的切点，排除 AspectJ 通知方法本身。
	 * @return 在原 AspectJ 表达式切点基础上构建的可组合切点
	 * @see #getPointcut()
	 */
	public final Pointcut buildSafePointcut() {
		Pointcut pc = getPointcut();
		MethodMatcher safeMethodMatcher = MethodMatchers.intersection(
				new AdviceExcludingMethodMatcher(this.aspectJAdviceMethod), pc.getMethodMatcher());
		return new ComposablePointcut(pc.getClassFilter(), safeMethodMatcher);
	}

	/**
	 * 返回切面实例的工厂。
	 */
	public final AspectInstanceFactory getAspectInstanceFactory() {
		return this.aspectInstanceFactory;
	}

	/**
	 * 返回切面实例的 ClassLoader。
	 */
	@Nullable
	public final ClassLoader getAspectClassLoader() {
		return this.aspectInstanceFactory.getAspectClassLoader();
	}

	@Override
	public int getOrder() {
		return this.aspectInstanceFactory.getOrder();
	}


	/**
	 * 设置声明该通知的切面（bean）的名称。
	 */
	public void setAspectName(String name) {
		this.aspectName = name;
	}

	@Override
	public String getAspectName() {
		return this.aspectName;
	}

	/**
	 * 设置此通知在切面中的声明顺序。
	 */
	public void setDeclarationOrder(int order) {
		this.declarationOrder = order;
	}

	@Override
	public int getDeclarationOrder() {
		return this.declarationOrder;
	}

	/**
	 * 如果已知参数名称，则由该通知对象的创建者设置。<p>例如，这可能是因为它们在 XML 或通知注解中被显式
	 * 指定。
	 * @param argNames 以逗号分隔的参数名列表
	 */
	public void setArgumentNames(String argNames) {
		String[] tokens = StringUtils.commaDelimitedListToStringArray(argNames);
		setArgumentNamesFromStringArray(tokens);
	}

	public void setArgumentNamesFromStringArray(String... args) {
		this.argumentNames = new String[args.length];
		for (int i = 0; i < args.length; i++) {
			this.argumentNames[i] = StringUtils.trimWhitespace(args[i]);
			if (!isVariableName(this.argumentNames[i])) {
				throw new IllegalArgumentException(
						"'argumentNames' property of AbstractAspectJAdvice contains an argument name '" +
						this.argumentNames[i] + "' that is not a valid Java identifier");
			}
		}
		if (this.argumentNames != null) {
			if (this.aspectJAdviceMethod.getParameterCount() == this.argumentNames.length + 1) {
				// May need to add implicit join point arg name...
				Class<?> firstArgType = this.aspectJAdviceMethod.getParameterTypes()[0];
				if (firstArgType == JoinPoint.class ||
						firstArgType == ProceedingJoinPoint.class ||
						firstArgType == JoinPoint.StaticPart.class) {
					String[] oldNames = this.argumentNames;
					this.argumentNames = new String[oldNames.length + 1];
					this.argumentNames[0] = "THIS_JOIN_POINT";
					System.arraycopy(oldNames, 0, this.argumentNames, 1, oldNames.length);
				}
			}
		}
	}

	public void setReturningName(String name) {
		throw new UnsupportedOperationException("Only afterReturning advice can be used to bind a return value");
	}

	/**
	 * 我们需要在此层级保存 returning 名称以进行参数绑定计算；此方法允许 afterReturning 通知子类设置该
	 * 名称。
	 */
	protected void setReturningNameNoCheck(String name) {
		// name could be a variable or a type...
		if (isVariableName(name)) {
			this.returningName = name;
		}
		else {
			// assume a type
			try {
				this.discoveredReturningType = ClassUtils.forName(name, getAspectClassLoader());
			}
			catch (Throwable ex) {
				throw new IllegalArgumentException("Returning name '" + name  +
						"' is neither a valid argument name nor the fully-qualified " +
						"name of a Java type on the classpath. Root cause: " + ex);
			}
		}
	}

	protected Class<?> getDiscoveredReturningType() {
		return this.discoveredReturningType;
	}

	@Nullable
	protected Type getDiscoveredReturningGenericType() {
		return this.discoveredReturningGenericType;
	}

	public void setThrowingName(String name) {
		throw new UnsupportedOperationException("Only afterThrowing advice can be used to bind a thrown exception");
	}

	/**
	 * 我们需要在此层级保存 throwing 名称以进行参数绑定计算；此方法允许 afterThrowing 通知子类设置该名
	 * 称。
	 */
	protected void setThrowingNameNoCheck(String name) {
		// name could be a variable or a type...
		if (isVariableName(name)) {
			this.throwingName = name;
		}
		else {
			// assume a type
			try {
				this.discoveredThrowingType = ClassUtils.forName(name, getAspectClassLoader());
			}
			catch (Throwable ex) {
				throw new IllegalArgumentException("Throwing name '" + name  +
						"' is neither a valid argument name nor the fully-qualified " +
						"name of a Java type on the classpath. Root cause: " + ex);
			}
		}
	}

	protected Class<?> getDiscoveredThrowingType() {
		return this.discoveredThrowingType;
	}

	private static boolean isVariableName(String name) {
		return AspectJProxyUtils.isVariableName(name);
	}


	/**
	 * 在设置阶段尽可能多做工作，以便后续通知调用中的参数绑定尽可能快。<p>如果第一个参数的类型为
	 * JoinPoint 或 ProceedingJoinPoint，那么我们在该位置传入 JoinPoint（对于环绕通知则是
	 * ProceedingJoinPoint）。<p>如果第一个参数的类型为 {@code JoinPoint.StaticPart}，那么我们在该位置
	 * 传入 {@code JoinPoint.StaticPart}。<p>其余参数必须在给定连接点处通过切点求值进行绑定。我们将得到
	 * 一个从参数名到值的映射。我们需要计算哪个通知参数需要绑定到哪个参数名。确定这种绑定有多种策略，它
	 * 们以责任链的形式组织。
	 */
	public final void calculateArgumentBindings() {
		// The simple case... nothing to bind.
		if (this.argumentsIntrospected || this.parameterTypes.length == 0) {
			return;
		}

		int numUnboundArgs = this.parameterTypes.length;
		Class<?>[] parameterTypes = this.aspectJAdviceMethod.getParameterTypes();
		if (maybeBindJoinPoint(parameterTypes[0]) || maybeBindProceedingJoinPoint(parameterTypes[0]) ||
				maybeBindJoinPointStaticPart(parameterTypes[0])) {
			numUnboundArgs--;
		}

		if (numUnboundArgs > 0) {
			// need to bind arguments by name as returned from the pointcut match
			bindArgumentsByName(numUnboundArgs);
		}

		this.argumentsIntrospected = true;
	}

	private boolean maybeBindJoinPoint(Class<?> candidateParameterType) {
		if (JoinPoint.class == candidateParameterType) {
			this.joinPointArgumentIndex = 0;
			return true;
		}
		else {
			return false;
		}
	}

	private boolean maybeBindProceedingJoinPoint(Class<?> candidateParameterType) {
		if (ProceedingJoinPoint.class == candidateParameterType) {
			if (!supportsProceedingJoinPoint()) {
				throw new IllegalArgumentException("ProceedingJoinPoint is only supported for around advice");
			}
			this.joinPointArgumentIndex = 0;
			return true;
		}
		else {
			return false;
		}
	}

	protected boolean supportsProceedingJoinPoint() {
		return false;
	}

	private boolean maybeBindJoinPointStaticPart(Class<?> candidateParameterType) {
		if (JoinPoint.StaticPart.class == candidateParameterType) {
			this.joinPointStaticPartArgumentIndex = 0;
			return true;
		}
		else {
			return false;
		}
	}

	private void bindArgumentsByName(int numArgumentsExpectingToBind) {
		if (this.argumentNames == null) {
			this.argumentNames = createParameterNameDiscoverer().getParameterNames(this.aspectJAdviceMethod);
		}
		if (this.argumentNames != null) {
			// We have been able to determine the arg names.
			bindExplicitArguments(numArgumentsExpectingToBind);
		}
		else {
			throw new IllegalStateException("Advice method [" + this.aspectJAdviceMethod.getName() + "] " +
					"requires " + numArgumentsExpectingToBind + " arguments to be bound by name, but " +
					"the argument names were not specified and could not be discovered.");
		}
	}

	/**
	 * 创建一个用于参数绑定的 ParameterNameDiscoverer。<p>默认实现会创建一个
	 * {@link DefaultParameterNameDiscoverer}，并添加一个专门配置的
	 * {@link AspectJAdviceParameterNameDiscoverer}。
	 */
	protected ParameterNameDiscoverer createParameterNameDiscoverer() {
		// We need to discover them, or if that fails, guess,
		// and if we can't guess with 100% accuracy, fail.
		DefaultParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();
		AspectJAdviceParameterNameDiscoverer adviceParameterNameDiscoverer =
				new AspectJAdviceParameterNameDiscoverer(this.pointcut.getExpression());
		adviceParameterNameDiscoverer.setReturningName(this.returningName);
		adviceParameterNameDiscoverer.setThrowingName(this.throwingName);
		// Last in chain, so if we're called and we fail, that's bad...
		adviceParameterNameDiscoverer.setRaiseExceptions(true);
		discoverer.addDiscoverer(adviceParameterNameDiscoverer);
		return discoverer;
	}

	private void bindExplicitArguments(int numArgumentsLeftToBind) {
		Assert.state(this.argumentNames != null, "No argument names available");
		this.argumentBindings = new HashMap<>();

		int numExpectedArgumentNames = this.aspectJAdviceMethod.getParameterCount();
		if (this.argumentNames.length != numExpectedArgumentNames) {
			throw new IllegalStateException("Expecting to find " + numExpectedArgumentNames +
					" arguments to bind by name in advice, but actually found " +
					this.argumentNames.length + " arguments.");
		}

		// So we match in number...
		int argumentIndexOffset = this.parameterTypes.length - numArgumentsLeftToBind;
		for (int i = argumentIndexOffset; i < this.argumentNames.length; i++) {
			this.argumentBindings.put(this.argumentNames[i], i);
		}

		// Check that returning and throwing were in the argument names list if
		// specified, and find the discovered argument types.
		if (this.returningName != null) {
			if (!this.argumentBindings.containsKey(this.returningName)) {
				throw new IllegalStateException("Returning argument name '" + this.returningName +
						"' was not bound in advice arguments");
			}
			else {
				Integer index = this.argumentBindings.get(this.returningName);
				this.discoveredReturningType = this.aspectJAdviceMethod.getParameterTypes()[index];
				this.discoveredReturningGenericType = this.aspectJAdviceMethod.getGenericParameterTypes()[index];
			}
		}
		if (this.throwingName != null) {
			if (!this.argumentBindings.containsKey(this.throwingName)) {
				throw new IllegalStateException("Throwing argument name '" + this.throwingName +
						"' was not bound in advice arguments");
			}
			else {
				Integer index = this.argumentBindings.get(this.throwingName);
				this.discoveredThrowingType = this.aspectJAdviceMethod.getParameterTypes()[index];
			}
		}

		// configure the pointcut expression accordingly.
		configurePointcutParameters(this.argumentNames, argumentIndexOffset);
	}

	/**
	 * 从 argumentIndexOffset 开始的所有参数都是切点参数的候选；但 returning 和 throwing 变量会以不同方
	 * 式处理，如果存在则必须从列表中移除。
	 */
	private void configurePointcutParameters(String[] argumentNames, int argumentIndexOffset) {
		int numParametersToRemove = argumentIndexOffset;
		if (this.returningName != null) {
			numParametersToRemove++;
		}
		if (this.throwingName != null) {
			numParametersToRemove++;
		}
		String[] pointcutParameterNames = new String[argumentNames.length - numParametersToRemove];
		Class<?>[] pointcutParameterTypes = new Class<?>[pointcutParameterNames.length];
		Class<?>[] methodParameterTypes = this.aspectJAdviceMethod.getParameterTypes();

		int index = 0;
		for (int i = 0; i < argumentNames.length; i++) {
			if (i < argumentIndexOffset) {
				continue;
			}
			if (argumentNames[i].equals(this.returningName) ||
				argumentNames[i].equals(this.throwingName)) {
				continue;
			}
			pointcutParameterNames[index] = argumentNames[i];
			pointcutParameterTypes[index] = methodParameterTypes[i];
			index++;
		}

		this.pointcut.setParameterNames(pointcutParameterNames);
		this.pointcut.setParameterTypes(pointcutParameterTypes);
	}

	/**
	 * 获取方法执行连接点处的参数，并输出一组用于通知方法的参数。
	 * @param jp 当前的 JoinPoint
	 * @param jpMatch 与此执行连接点匹配的连接点匹配对象
	 * @param returnValue 方法执行的返回值（可能为 null）
	 * @param ex 方法执行抛出的异常（可能为 null）
	 * @return 如果没有参数，则为空数组
	 */
	protected Object[] argBinding(JoinPoint jp, @Nullable JoinPointMatch jpMatch,
			@Nullable Object returnValue, @Nullable Throwable ex) {

		calculateArgumentBindings();

		// AMC start
		Object[] adviceInvocationArgs = new Object[this.parameterTypes.length];
		int numBound = 0;

		if (this.joinPointArgumentIndex != -1) {
			adviceInvocationArgs[this.joinPointArgumentIndex] = jp;
			numBound++;
		}
		else if (this.joinPointStaticPartArgumentIndex != -1) {
			adviceInvocationArgs[this.joinPointStaticPartArgumentIndex] = jp.getStaticPart();
			numBound++;
		}

		if (!CollectionUtils.isEmpty(this.argumentBindings)) {
			// binding from pointcut match
			if (jpMatch != null) {
				PointcutParameter[] parameterBindings = jpMatch.getParameterBindings();
				for (PointcutParameter parameter : parameterBindings) {
					String name = parameter.getName();
					Integer index = this.argumentBindings.get(name);
					adviceInvocationArgs[index] = parameter.getBinding();
					numBound++;
				}
			}
			// binding from returning clause
			if (this.returningName != null) {
				Integer index = this.argumentBindings.get(this.returningName);
				adviceInvocationArgs[index] = returnValue;
				numBound++;
			}
			// binding from thrown exception
			if (this.throwingName != null) {
				Integer index = this.argumentBindings.get(this.throwingName);
				adviceInvocationArgs[index] = ex;
				numBound++;
			}
		}

		if (numBound != this.parameterTypes.length) {
			throw new IllegalStateException("Required to bind " + this.parameterTypes.length +
					" arguments, but only bound " + numBound + " (JoinPointMatch " +
					(jpMatch == null ? "was NOT" : "WAS") + " bound in invocation)");
		}

		return adviceInvocationArgs;
	}


	/**
	 * 调用该通知方法。
	 * @param jpMatch 与此执行连接点匹配的 JoinPointMatch
	 * @param returnValue 方法执行的返回值（可能为 null）
	 * @param ex 方法执行抛出的异常（可能为 null）
	 * @return 调用结果
	 * @throws Throwable 如果调用失败
	 */
	protected Object invokeAdviceMethod(
			@Nullable JoinPointMatch jpMatch, @Nullable Object returnValue, @Nullable Throwable ex)
			throws Throwable {

		return invokeAdviceMethodWithGivenArgs(argBinding(getJoinPoint(), jpMatch, returnValue, ex));
	}

	// As above, but in this case we are given the join point.
	protected Object invokeAdviceMethod(JoinPoint jp, @Nullable JoinPointMatch jpMatch,
			@Nullable Object returnValue, @Nullable Throwable t) throws Throwable {

		return invokeAdviceMethodWithGivenArgs(argBinding(jp, jpMatch, returnValue, t));
	}

	protected Object invokeAdviceMethodWithGivenArgs(Object[] args) throws Throwable {
		Object[] actualArgs = args;
		if (this.aspectJAdviceMethod.getParameterCount() == 0) {
			actualArgs = null;
		}
		try {
			ReflectionUtils.makeAccessible(this.aspectJAdviceMethod);
			return this.aspectJAdviceMethod.invoke(this.aspectInstanceFactory.getAspectInstance(), actualArgs);
		}
		catch (IllegalArgumentException ex) {
			throw new AopInvocationException("Mismatch on arguments to advice method [" +
					this.aspectJAdviceMethod + "]; pointcut expression [" +
					this.pointcut.getPointcutExpression() + "]", ex);
		}
		catch (InvocationTargetException ex) {
			throw ex.getTargetException();
		}
	}

	/**
	 * 在环绕通知中重写，以返回正在继续的连接点。
	 */
	protected JoinPoint getJoinPoint() {
		return currentJoinPoint();
	}

	/**
	 * 获取我们正在被调度的连接点处的当前连接点匹配。
	 */
	@Nullable
	protected JoinPointMatch getJoinPointMatch() {
		MethodInvocation mi = ExposeInvocationInterceptor.currentInvocation();
		if (!(mi instanceof ProxyMethodInvocation)) {
			throw new IllegalStateException("MethodInvocation is not a Spring ProxyMethodInvocation: " + mi);
		}
		return getJoinPointMatch((ProxyMethodInvocation) mi);
	}

	// Note: We can't use JoinPointMatch.getClass().getName() as the key, since
	// Spring AOP does all the matching at a join point, and then all the invocations.
	// Under this scenario, if we just use JoinPointMatch as the key, then
	// 'last man wins' which is not what we want at all.
	// Using the expression is guaranteed to be safe, since 2 identical expressions
	// are guaranteed to bind in exactly the same way.
	@Nullable
	protected JoinPointMatch getJoinPointMatch(ProxyMethodInvocation pmi) {
		String expression = this.pointcut.getExpression();
		return (expression != null ? (JoinPointMatch) pmi.getUserAttribute(expression) : null);
	}


	@Override
	public String toString() {
		return getClass().getName() + ": advice method [" + this.aspectJAdviceMethod + "]; " +
				"aspect name '" + this.aspectName + "'";
	}

	private void readObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
		inputStream.defaultReadObject();
		try {
			this.aspectJAdviceMethod = this.declaringClass.getMethod(this.methodName, this.parameterTypes);
		}
		catch (NoSuchMethodException ex) {
			throw new IllegalStateException("Failed to find advice method on deserialization", ex);
		}
	}


	/**
	 * 排除指定通知方法的 MethodMatcher。
	 * @see AbstractAspectJAdvice#buildSafePointcut()
	 */
	private static class AdviceExcludingMethodMatcher extends StaticMethodMatcher {

		private final Method adviceMethod;

		public AdviceExcludingMethodMatcher(Method adviceMethod) {
			this.adviceMethod = adviceMethod;
		}

		@Override
		public boolean matches(Method method, Class<?> targetClass) {
			return !this.adviceMethod.equals(method);
		}

		@Override
		public boolean equals(@Nullable Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof AdviceExcludingMethodMatcher)) {
				return false;
			}
			AdviceExcludingMethodMatcher otherMm = (AdviceExcludingMethodMatcher) other;
			return this.adviceMethod.equals(otherMm.adviceMethod);
		}

		@Override
		public int hashCode() {
			return this.adviceMethod.hashCode();
		}

		@Override
		public String toString() {
			return getClass().getName() + ": " + this.adviceMethod;
		}
	}

}
