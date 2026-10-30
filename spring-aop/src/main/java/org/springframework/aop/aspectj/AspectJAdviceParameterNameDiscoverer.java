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

package org.springframework.aop.aspectj;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.weaver.tools.PointcutParser;
import org.aspectj.weaver.tools.PointcutPrimitive;

import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * {@link ParameterNameDiscoverer} 实现，尝试从切点表达式、returning 和 throwing 子句中推断通知方法
 * 的参数名称。如果无法获得无歧义的解释，则返回 {@code null}。
 *
 * <p>此类按以下方式解释参数：
 * <ol>
 * <li>如果方法的第一个参数类型为 {@link JoinPoint} 或 {@link ProceedingJoinPoint}，则假定该参数用于向通知传递 {@code thisJoinPoint}，其参数名将被赋值为 {@code "thisJoinPoint"}。</li>
 * <li>如果方法的第一个参数类型为 {@code JoinPoint.StaticPart}，则假定该参数用于向通知传递 {@code "thisJoinPointStaticPart"}，其参数名将被赋值为 {@code "thisJoinPointStaticPart"}。</li>
 * <li>如果已设置 {@link #setThrowingName(String) throwingName}，但不存在类型为 {@code Throwable+} 的未绑定参数，则抛出 {@link IllegalArgumentException}。如果存在多个类型为 {@code Throwable+} 的未绑定参数，则抛出 {@link AmbiguousBindingException}。如果恰好存在一个类型为 {@code Throwable+} 的未绑定参数，则相应参数名将被赋值为 &lt;throwingName&gt;。</li>
 * <li>如果仍有未绑定参数，则检查切点表达式。设 {@code a} 为以绑定形式使用的基于注解的切点表达式（&#64;annotation、&#64;this、&#64;target、&#64;args、&#64;within、&#64;withincode）的数量。是否以绑定形式使用本身需要推断：如果切点内的表达式是符合 Java 变量命名约定的单个字符串字面量，则假定它是变量名。如果 {@code a} 为零，则进入下一阶段。如果 {@code a} &gt; 1，则抛出 {@code AmbiguousBindingException}。如果 {@code a} == 1，且不存在类型为 {@code Annotation+} 的未绑定参数，则抛出 {@code IllegalArgumentException}；如果恰好存在一个这样的参数，则相应参数名将被赋值为切点表达式中的值。</li>
 * <li>如果已设置 returningName，但不存在未绑定参数，则抛出 {@code IllegalArgumentException}。如果存在多个未绑定参数，则抛出 {@code AmbiguousBindingException}。如果恰好存在一个未绑定参数，则相应参数名将被赋值为 &lt;returningName&gt;。</li>
 * <li>如果仍有未绑定参数，则再次检查切点表达式，以查找以绑定形式使用的 {@code this}、{@code target} 和 {@code args} 切点表达式（绑定形式的推断方式与基于注解的切点相同）。如果仍存在多个基本类型的未绑定参数（这类参数只能在 {@code args} 中绑定），则抛出 {@code AmbiguousBindingException}。如果恰好存在一个基本类型参数，那么若恰好找到一个 {@code args} 绑定变量，就将该变量名赋给相应参数名；如果没有找到任何 {@code args} 绑定变量，则抛出 {@code IllegalStateException}；如果有多个 {@code args} 绑定变量，则抛出 {@code AmbiguousBindingException}。此时，如果仍存在多个未绑定参数，则抛出 {@code AmbiguousBindingException}。如果没有剩余未绑定参数，则处理完成。如果恰好剩余一个未绑定参数，且只有一个来自 {@code this}、{@code target} 或 {@code args} 的候选变量名未绑定，则将其赋给相应参数名。如果存在多种可能，则抛出 {@code AmbiguousBindingException}。</li>
 * </ol>
 *
 * <p>抛出 {@code IllegalArgumentException} 或 {@code AmbiguousBindingException} 时的行为是可配置的，
 * 以便此发现器可作为责任链的一部分使用。默认情况下，该条件会被记录日志，并且
 * {@code getParameterNames(..)} 方法只会返回 {@code null}。如果将
 * {@link #setRaiseExceptions(boolean) raiseExceptions} 属性设置为 {@code true}，则这些条件将分别以
 * {@code IllegalArgumentException} 和 {@code AmbiguousBindingException} 的形式抛出。
 *
 * <p>现在是不是完全清楚了？;)
 *
 * <p>简而言之：如果可以推断出无歧义的绑定，那么就采用它。如果通知的要求根本不可能满足，则返回
 * {@code null}。通过将 {@link #setRaiseExceptions(boolean) raiseExceptions} 属性设置为
 * {@code true}，在无法发现参数名的情况下将抛出描述性的异常，而不是返回 {@code null}。
 *
 * @author Adrian Colyer
 * @author Juergen Hoeller
 * @since 2.0
 */
public class AspectJAdviceParameterNameDiscoverer implements ParameterNameDiscoverer {

	private static final String THIS_JOIN_POINT = "thisJoinPoint";
	private static final String THIS_JOIN_POINT_STATIC_PART = "thisJoinPointStaticPart";

	// Steps in the binding algorithm...
	private static final int STEP_JOIN_POINT_BINDING = 1;
	private static final int STEP_THROWING_BINDING = 2;
	private static final int STEP_ANNOTATION_BINDING = 3;
	private static final int STEP_RETURNING_BINDING = 4;
	private static final int STEP_PRIMITIVE_ARGS_BINDING = 5;
	private static final int STEP_THIS_TARGET_ARGS_BINDING = 6;
	private static final int STEP_REFERENCE_PCUT_BINDING = 7;
	private static final int STEP_FINISHED = 8;

	private static final Set<String> singleValuedAnnotationPcds = new HashSet<>();
	private static final Set<String> nonReferencePointcutTokens = new HashSet<>();


	static {
		singleValuedAnnotationPcds.add("@this");
		singleValuedAnnotationPcds.add("@target");
		singleValuedAnnotationPcds.add("@within");
		singleValuedAnnotationPcds.add("@withincode");
		singleValuedAnnotationPcds.add("@annotation");

		Set<PointcutPrimitive> pointcutPrimitives = PointcutParser.getAllSupportedPointcutPrimitives();
		for (PointcutPrimitive primitive : pointcutPrimitives) {
			nonReferencePointcutTokens.add(primitive.getName());
		}
		nonReferencePointcutTokens.add("&&");
		nonReferencePointcutTokens.add("!");
		nonReferencePointcutTokens.add("||");
		nonReferencePointcutTokens.add("and");
		nonReferencePointcutTokens.add("or");
		nonReferencePointcutTokens.add("not");
	}


	/** 与通知关联的切点表达式，以简单 String 表示。 */
	@Nullable
	private String pointcutExpression;

	private boolean raiseExceptions;

	/** 如果通知是 afterReturning 并绑定返回值，则这是所使用的参数名。 */
	@Nullable
	private String returningName;

	/** 如果通知是 afterThrowing 并绑定抛出的值，则这是所使用的参数名。 */
	@Nullable
	private String throwingName;

	private Class<?>[] argumentTypes = new Class<?>[0];

	private String[] parameterNameBindings = new String[0];

	private int numberOfRemainingUnboundArguments;


	/**
	 * 创建一个新的发现器，尝试从给定的切点表达式中发现参数名称。
	 */
	public AspectJAdviceParameterNameDiscoverer(@Nullable String pointcutExpression) {
		this.pointcutExpression = pointcutExpression;
	}


	/**
	 * 指示在无法推断通知参数名的情况下，是否必须视情况抛出 {@link IllegalArgumentException} 和
	 * {@link AmbiguousBindingException}。
	 * @param raiseExceptions 如果要抛出异常，则为 {@code true}
	 */
	public void setRaiseExceptions(boolean raiseExceptions) {
		this.raiseExceptions = raiseExceptions;
	}

	/**
	 * 如果 {@code afterReturning} 通知绑定返回值，则必须指定 returning 变量名。
	 * @param returningName 返回值变量的名称
	 */
	public void setReturningName(@Nullable String returningName) {
		this.returningName = returningName;
	}

	/**
	 * 如果 {@code afterThrowing} 通知绑定抛出的值，则必须指定 throwing 变量名。
	 * @param throwingName 异常值变量的名称
	 */
	public void setThrowingName(@Nullable String throwingName) {
		this.throwingName = throwingName;
	}


	/**
	 * 推断通知方法的参数名称。<p>有关所用算法的详细信息，请参见
	 * {@link AspectJAdviceParameterNameDiscoverer class level javadoc}。
	 * @param method 目标 {@link Method}
	 * @return 参数名称
	 */
	@Override
	@Nullable
	public String[] getParameterNames(Method method) {
		this.argumentTypes = method.getParameterTypes();
		this.numberOfRemainingUnboundArguments = this.argumentTypes.length;
		this.parameterNameBindings = new String[this.numberOfRemainingUnboundArguments];

		int minimumNumberUnboundArgs = 0;
		if (this.returningName != null) {
			minimumNumberUnboundArgs++;
		}
		if (this.throwingName != null) {
			minimumNumberUnboundArgs++;
		}
		if (this.numberOfRemainingUnboundArguments < minimumNumberUnboundArgs) {
			throw new IllegalStateException(
					"Not enough arguments in method to satisfy binding of returning and throwing variables");
		}

		try {
			int algorithmicStep = STEP_JOIN_POINT_BINDING;
			while ((this.numberOfRemainingUnboundArguments > 0) && algorithmicStep < STEP_FINISHED) {
				switch (algorithmicStep++) {
					case STEP_JOIN_POINT_BINDING:
						if (!maybeBindThisJoinPoint()) {
							maybeBindThisJoinPointStaticPart();
						}
						break;
					case STEP_THROWING_BINDING:
						maybeBindThrowingVariable();
						break;
					case STEP_ANNOTATION_BINDING:
						maybeBindAnnotationsFromPointcutExpression();
						break;
					case STEP_RETURNING_BINDING:
						maybeBindReturningVariable();
						break;
					case STEP_PRIMITIVE_ARGS_BINDING:
						maybeBindPrimitiveArgsFromPointcutExpression();
						break;
					case STEP_THIS_TARGET_ARGS_BINDING:
						maybeBindThisOrTargetOrArgsFromPointcutExpression();
						break;
					case STEP_REFERENCE_PCUT_BINDING:
						maybeBindReferencePointcutParameter();
						break;
					default:
						throw new IllegalStateException("Unknown algorithmic step: " + (algorithmicStep - 1));
				}
			}
		}
		catch (AmbiguousBindingException | IllegalArgumentException ex) {
			if (this.raiseExceptions) {
				throw ex;
			}
			else {
				return null;
			}
		}

		if (this.numberOfRemainingUnboundArguments == 0) {
			return this.parameterNameBindings;
		}
		else {
			if (this.raiseExceptions) {
				throw new IllegalStateException("Failed to bind all argument names: " +
						this.numberOfRemainingUnboundArguments + " argument(s) could not be bound");
			}
			else {
				// convention for failing is to return null, allowing participation in a chain of responsibility
				return null;
			}
		}
	}

	/**
	 * 在 Spring 中，通知方法永远不可能是构造器。
	 * @return {@code null}
	 * @throws UnsupportedOperationException 如果 {@link #setRaiseExceptions(boolean) raiseExceptions} 已被设置为 {@code true}
	 */
	@Override
	@Nullable
	public String[] getParameterNames(Constructor<?> ctor) {
		if (this.raiseExceptions) {
			throw new UnsupportedOperationException("An advice method can never be a constructor");
		}
		else {
			// we return null rather than throw an exception so that we behave well
			// in a chain-of-responsibility.
			return null;
		}
	}


	private void bindParameterName(int index, String name) {
		this.parameterNameBindings[index] = name;
		this.numberOfRemainingUnboundArguments--;
	}

	/**
	 * 如果第一个参数的类型为 JoinPoint 或 ProceedingJoinPoint，则绑定 "thisJoinPoint" 作为参数名并返回
	 * true，否则返回 false。
	 */
	private boolean maybeBindThisJoinPoint() {
		if ((this.argumentTypes[0] == JoinPoint.class) || (this.argumentTypes[0] == ProceedingJoinPoint.class)) {
			bindParameterName(0, THIS_JOIN_POINT);
			return true;
		}
		else {
			return false;
		}
	}

	private void maybeBindThisJoinPointStaticPart() {
		if (this.argumentTypes[0] == JoinPoint.StaticPart.class) {
			bindParameterName(0, THIS_JOIN_POINT_STATIC_PART);
		}
	}

	/**
	 * 如果指定了 throwing 名称，并且恰好只剩下一个选择（即 Throwable 子类型的参数），则绑定它。
	 */
	private void maybeBindThrowingVariable() {
		if (this.throwingName == null) {
			return;
		}

		// So there is binding work to do...
		int throwableIndex = -1;
		for (int i = 0; i < this.argumentTypes.length; i++) {
			if (isUnbound(i) && isSubtypeOf(Throwable.class, i)) {
				if (throwableIndex == -1) {
					throwableIndex = i;
				}
				else {
					// Second candidate we've found - ambiguous binding
					throw new AmbiguousBindingException("Binding of throwing parameter '" +
							this.throwingName + "' is ambiguous: could be bound to argument " +
							throwableIndex + " or argument " + i);
				}
			}
		}

		if (throwableIndex == -1) {
			throw new IllegalStateException("Binding of throwing parameter '" + this.throwingName
					+ "' could not be completed as no available arguments are a subtype of Throwable");
		}
		else {
			bindParameterName(throwableIndex, this.throwingName);
		}
	}

	/**
	 * 如果指定了 returning 变量，并且只剩下一个选择，则绑定它。
	 */
	private void maybeBindReturningVariable() {
		if (this.numberOfRemainingUnboundArguments == 0) {
			throw new IllegalStateException(
					"Algorithm assumes that there must be at least one unbound parameter on entry to this method");
		}

		if (this.returningName != null) {
			if (this.numberOfRemainingUnboundArguments > 1) {
				throw new AmbiguousBindingException("Binding of returning parameter '" + this.returningName +
						"' is ambiguous, there are " + this.numberOfRemainingUnboundArguments + " candidates.");
			}

			// We're all set... find the unbound parameter, and bind it.
			for (int i = 0; i < this.parameterNameBindings.length; i++) {
				if (this.parameterNameBindings[i] == null) {
					bindParameterName(i, this.returningName);
					break;
				}
			}
		}
	}


	/**
	 * 解析字符串切点表达式，查找：&#64;this、&#64;target、&#64;args、&#64;within、&#64;withincode、
	 * &#64;annotation。如果找到其中某个切点表达式，则尝试提取候选变量名（对于 args 则是多个变量名）。
	 * <p>如果 AspectJ 能在这一过程中提供更多支持就更好了…… :)
	 */
	private void maybeBindAnnotationsFromPointcutExpression() {
		List<String> varNames = new ArrayList<>();
		String[] tokens = StringUtils.tokenizeToStringArray(this.pointcutExpression, " ");
		for (int i = 0; i < tokens.length; i++) {
			String toMatch = tokens[i];
			int firstParenIndex = toMatch.indexOf('(');
			if (firstParenIndex != -1) {
				toMatch = toMatch.substring(0, firstParenIndex);
			}
			if (singleValuedAnnotationPcds.contains(toMatch)) {
				PointcutBody body = getPointcutBody(tokens, i);
				i += body.numTokensConsumed;
				String varName = maybeExtractVariableName(body.text);
				if (varName != null) {
					varNames.add(varName);
				}
			}
			else if (tokens[i].startsWith("@args(") || tokens[i].equals("@args")) {
				PointcutBody body = getPointcutBody(tokens, i);
				i += body.numTokensConsumed;
				maybeExtractVariableNamesFromArgs(body.text, varNames);
			}
		}

		bindAnnotationsFromVarNames(varNames);
	}

	/**
	 * 将给定的已提取变量名列表与参数槽进行匹配。
	 */
	private void bindAnnotationsFromVarNames(List<String> varNames) {
		if (!varNames.isEmpty()) {
			// we have work to do...
			int numAnnotationSlots = countNumberOfUnboundAnnotationArguments();
			if (numAnnotationSlots > 1) {
				throw new AmbiguousBindingException("Found " + varNames.size() +
						" potential annotation variable(s), and " +
						numAnnotationSlots + " potential argument slots");
			}
			else if (numAnnotationSlots == 1) {
				if (varNames.size() == 1) {
					// it's a match
					findAndBind(Annotation.class, varNames.get(0));
				}
				else {
					// multiple candidate vars, but only one slot
					throw new IllegalArgumentException("Found " + varNames.size() +
							" candidate annotation binding variables" +
							" but only one potential argument binding slot");
				}
			}
			else {
				// no slots so presume those candidate vars were actually type names
			}
		}
	}

	/*
	 * If the token starts meets Java identifier conventions, it's in.
	 */
	@Nullable
	private String maybeExtractVariableName(@Nullable String candidateToken) {
		if (AspectJProxyUtils.isVariableName(candidateToken)) {
			return candidateToken;
		}
		return null;
	}

	/**
	 * 给定一个 args 切点主体（可以是 {@code args} 或 {@code at_args}），将任何候选变量名添加到给定列表
	 * 中。
	 */
	private void maybeExtractVariableNamesFromArgs(@Nullable String argsSpec, List<String> varNames) {
		if (argsSpec == null) {
			return;
		}
		String[] tokens = StringUtils.tokenizeToStringArray(argsSpec, ",");
		for (int i = 0; i < tokens.length; i++) {
			tokens[i] = StringUtils.trimWhitespace(tokens[i]);
			String varName = maybeExtractVariableName(tokens[i]);
			if (varName != null) {
				varNames.add(varName);
			}
		}
	}

	/**
	 * 解析字符串切点表达式，查找 this()、target() 和 args() 表达式。如果找到，则尝试提取候选变量名并进
	 * 行绑定。
	 */
	private void maybeBindThisOrTargetOrArgsFromPointcutExpression() {
		if (this.numberOfRemainingUnboundArguments > 1) {
			throw new AmbiguousBindingException("Still " + this.numberOfRemainingUnboundArguments
					+ " unbound args at this(),target(),args() binding stage, with no way to determine between them");
		}

		List<String> varNames = new ArrayList<>();
		String[] tokens = StringUtils.tokenizeToStringArray(this.pointcutExpression, " ");
		for (int i = 0; i < tokens.length; i++) {
			if (tokens[i].equals("this") ||
					tokens[i].startsWith("this(") ||
					tokens[i].equals("target") ||
					tokens[i].startsWith("target(")) {
				PointcutBody body = getPointcutBody(tokens, i);
				i += body.numTokensConsumed;
				String varName = maybeExtractVariableName(body.text);
				if (varName != null) {
					varNames.add(varName);
				}
			}
			else if (tokens[i].equals("args") || tokens[i].startsWith("args(")) {
				PointcutBody body = getPointcutBody(tokens, i);
				i += body.numTokensConsumed;
				List<String> candidateVarNames = new ArrayList<>();
				maybeExtractVariableNamesFromArgs(body.text, candidateVarNames);
				// we may have found some var names that were bound in previous primitive args binding step,
				// filter them out...
				for (String varName : candidateVarNames) {
					if (!alreadyBound(varName)) {
						varNames.add(varName);
					}
				}
			}
		}


		if (varNames.size() > 1) {
			throw new AmbiguousBindingException("Found " + varNames.size() +
					" candidate this(), target() or args() variables but only one unbound argument slot");
		}
		else if (varNames.size() == 1) {
			for (int j = 0; j < this.parameterNameBindings.length; j++) {
				if (isUnbound(j)) {
					bindParameterName(j, varNames.get(0));
					break;
				}
			}
		}
		// else varNames.size must be 0 and we have nothing to bind.
	}

	private void maybeBindReferencePointcutParameter() {
		if (this.numberOfRemainingUnboundArguments > 1) {
			throw new AmbiguousBindingException("Still " + this.numberOfRemainingUnboundArguments
					+ " unbound args at reference pointcut binding stage, with no way to determine between them");
		}

		List<String> varNames = new ArrayList<>();
		String[] tokens = StringUtils.tokenizeToStringArray(this.pointcutExpression, " ");
		for (int i = 0; i < tokens.length; i++) {
			String toMatch = tokens[i];
			if (toMatch.startsWith("!")) {
				toMatch = toMatch.substring(1);
			}
			int firstParenIndex = toMatch.indexOf('(');
			if (firstParenIndex != -1) {
				toMatch = toMatch.substring(0, firstParenIndex);
			}
			else {
				if (tokens.length < i + 2) {
					// no "(" and nothing following
					continue;
				}
				else {
					String nextToken = tokens[i + 1];
					if (nextToken.charAt(0) != '(') {
						// next token is not "(" either, can't be a pc...
						continue;
					}
				}

			}

			// eat the body
			PointcutBody body = getPointcutBody(tokens, i);
			i += body.numTokensConsumed;

			if (!nonReferencePointcutTokens.contains(toMatch)) {
				// then it could be a reference pointcut
				String varName = maybeExtractVariableName(body.text);
				if (varName != null) {
					varNames.add(varName);
				}
			}
		}

		if (varNames.size() > 1) {
			throw new AmbiguousBindingException("Found " + varNames.size() +
					" candidate reference pointcut variables but only one unbound argument slot");
		}
		else if (varNames.size() == 1) {
			for (int j = 0; j < this.parameterNameBindings.length; j++) {
				if (isUnbound(j)) {
					bindParameterName(j, varNames.get(0));
					break;
				}
			}
		}
		// else varNames.size must be 0 and we have nothing to bind.
	}

	/*
	 * We've found the start of a binding pointcut at the given index into the
	 * token array. Now we need to extract the pointcut body and return it.
	 */
	private PointcutBody getPointcutBody(String[] tokens, int startIndex) {
		int numTokensConsumed = 0;
		String currentToken = tokens[startIndex];
		int bodyStart = currentToken.indexOf('(');
		if (currentToken.charAt(currentToken.length() - 1) == ')') {
			// It's an all in one... get the text between the first (and the last)
			return new PointcutBody(0, currentToken.substring(bodyStart + 1, currentToken.length() - 1));
		}
		else {
			StringBuilder sb = new StringBuilder();
			if (bodyStart >= 0 && bodyStart != (currentToken.length() - 1)) {
				sb.append(currentToken.substring(bodyStart + 1));
				sb.append(' ');
			}
			numTokensConsumed++;
			int currentIndex = startIndex + numTokensConsumed;
			while (currentIndex < tokens.length) {
				if (tokens[currentIndex].equals("(")) {
					currentIndex++;
					continue;
				}

				if (tokens[currentIndex].endsWith(")")) {
					sb.append(tokens[currentIndex], 0, tokens[currentIndex].length() - 1);
					return new PointcutBody(numTokensConsumed, sb.toString().trim());
				}

				String toAppend = tokens[currentIndex];
				if (toAppend.startsWith("(")) {
					toAppend = toAppend.substring(1);
				}
				sb.append(toAppend);
				sb.append(' ');
				currentIndex++;
				numTokensConsumed++;
			}

		}

		// We looked and failed...
		return new PointcutBody(numTokensConsumed, null);
	}

	/**
	 * 将 args 与基本类型中未绑定的参数进行匹配。
	 */
	private void maybeBindPrimitiveArgsFromPointcutExpression() {
		int numUnboundPrimitives = countNumberOfUnboundPrimitiveArguments();
		if (numUnboundPrimitives > 1) {
			throw new AmbiguousBindingException("Found '" + numUnboundPrimitives +
					"' unbound primitive arguments with no way to distinguish between them.");
		}
		if (numUnboundPrimitives == 1) {
			// Look for arg variable and bind it if we find exactly one...
			List<String> varNames = new ArrayList<>();
			String[] tokens = StringUtils.tokenizeToStringArray(this.pointcutExpression, " ");
			for (int i = 0; i < tokens.length; i++) {
				if (tokens[i].equals("args") || tokens[i].startsWith("args(")) {
					PointcutBody body = getPointcutBody(tokens, i);
					i += body.numTokensConsumed;
					maybeExtractVariableNamesFromArgs(body.text, varNames);
				}
			}
			if (varNames.size() > 1) {
				throw new AmbiguousBindingException("Found " + varNames.size() +
						" candidate variable names but only one candidate binding slot when matching primitive args");
			}
			else if (varNames.size() == 1) {
				// 1 primitive arg, and one candidate...
				for (int i = 0; i < this.argumentTypes.length; i++) {
					if (isUnbound(i) && this.argumentTypes[i].isPrimitive()) {
						bindParameterName(i, varNames.get(0));
						break;
					}
				}
			}
		}
	}

	/*
	 * Return true if the parameter name binding for the given parameter
	 * index has not yet been assigned.
	 */
	private boolean isUnbound(int i) {
		return this.parameterNameBindings[i] == null;
	}

	private boolean alreadyBound(String varName) {
		for (int i = 0; i < this.parameterNameBindings.length; i++) {
			if (!isUnbound(i) && varName.equals(this.parameterNameBindings[i])) {
				return true;
			}
		}
		return false;
	}

	/*
	 * Return {@code true} if the given argument type is a subclass
	 * of the given supertype.
	 */
	private boolean isSubtypeOf(Class<?> supertype, int argumentNumber) {
		return supertype.isAssignableFrom(this.argumentTypes[argumentNumber]);
	}

	private int countNumberOfUnboundAnnotationArguments() {
		int count = 0;
		for (int i = 0; i < this.argumentTypes.length; i++) {
			if (isUnbound(i) && isSubtypeOf(Annotation.class, i)) {
				count++;
			}
		}
		return count;
	}

	private int countNumberOfUnboundPrimitiveArguments() {
		int count = 0;
		for (int i = 0; i < this.argumentTypes.length; i++) {
			if (isUnbound(i) && this.argumentTypes[i].isPrimitive()) {
				count++;
			}
		}
		return count;
	}

	/*
	 * Find the argument index with the given type, and bind the given
	 * {@code varName} in that position.
	 */
	private void findAndBind(Class<?> argumentType, String varName) {
		for (int i = 0; i < this.argumentTypes.length; i++) {
			if (isUnbound(i) && isSubtypeOf(argumentType, i)) {
				bindParameterName(i, varName);
				return;
			}
		}
		throw new IllegalStateException("Expected to find an unbound argument of type '" +
				argumentType.getName() + "'");
	}


	/**
	 * 用于保存从切点主体提取的文本以及提取过程中消耗的 token 数量的简单结构体。
	 */
	private static class PointcutBody {

		private int numTokensConsumed;

		@Nullable
		private String text;

		public PointcutBody(int tokens, @Nullable String text) {
			this.numTokensConsumed = tokens;
			this.text = text;
		}
	}


	/**
	 * 在尝试解析方法的参数名时检测到歧义绑定时抛出。
	 */
	@SuppressWarnings("serial")
	public static class AmbiguousBindingException extends RuntimeException {

		/**
		 * 使用指定消息构造一个新的 AmbiguousBindingException。
		 * @param msg 详细消息
		 */
		public AmbiguousBindingException(String msg) {
			super(msg);
		}
	}

}
