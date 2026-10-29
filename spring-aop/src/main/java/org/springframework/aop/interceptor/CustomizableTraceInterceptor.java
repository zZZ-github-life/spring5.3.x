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

package org.springframework.aop.interceptor;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;

import org.springframework.core.Constants;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StopWatch;
import org.springframework.util.StringUtils;

/**
 * {@code MethodInterceptor} 实现，使用占位符支持高度可定制的方法级跟踪。
 *
 * <p>跟踪消息在方法进入时写入；如果方法调用成功，还会在方法退出时写入。如果调用导致异常，则会写入
 * 一条异常消息。这些跟踪消息的内容完全可定制，并提供特殊占位符，以便在日志消息中包含运行时信息。可
 * 用的占位符有：
 *
 * <p><ul>
 * <li>{@code $[methodName]} - 替换为被调用方法的名称</li>
 * <li>{@code $[targetClassName]} - 替换为调用目标所属类的名称</li>
 * <li>{@code $[targetClassShortName]} - 替换为调用目标所属类的短名称</li>
 * <li>{@code $[returnValue]} - 替换为调用返回的值</li>
 * <li>{@code $[argumentTypes]} - 替换为以逗号分隔的方法参数短类名列表</li>
 * <li>{@code $[arguments]} - 替换为以逗号分隔的方法参数 {@code String} 表示列表</li>
 * <li>{@code $[exception]} - 替换为调用期间抛出的任何 {@code Throwable} 的 {@code String} 表示</li>
 * <li>{@code $[invocationTime]} - 替换为方法调用所花费的时间（毫秒）</li>
 * </ul>
 *
 * <p>哪些占位符可以用于哪些消息存在限制：有关有效占位符的详细信息，请参见各个消息属性。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 1.2
 * @see #setEnterMessage
 * @see #setExitMessage
 * @see #setExceptionMessage
 * @see SimpleTraceInterceptor
 */
@SuppressWarnings("serial")
public class CustomizableTraceInterceptor extends AbstractTraceInterceptor {

	/**
	 * {@code $[methodName]} 占位符。替换为被调用方法的名称。
	 */
	public static final String PLACEHOLDER_METHOD_NAME = "$[methodName]";

	/**
	 * {@code $[targetClassName]} 占位符。替换为方法调用目标的 {@code Class} 的完全限定名。
	 */
	public static final String PLACEHOLDER_TARGET_CLASS_NAME = "$[targetClassName]";

	/**
	 * {@code $[targetClassShortName]} 占位符。替换为方法调用目标的 {@code Class} 的短名称。
	 */
	public static final String PLACEHOLDER_TARGET_CLASS_SHORT_NAME = "$[targetClassShortName]";

	/**
	 * {@code $[returnValue]} 占位符。替换为方法调用返回值的 {@code String} 表示形式。
	 */
	public static final String PLACEHOLDER_RETURN_VALUE = "$[returnValue]";

	/**
	 * {@code $[argumentTypes]} 占位符。替换为方法调用的参数类型列表，以逗号分隔。参数类型以短类名形式
	 * 写出。
	 */
	public static final String PLACEHOLDER_ARGUMENT_TYPES = "$[argumentTypes]";

	/**
	 * {@code $[arguments]} 占位符。替换为方法调用的参数值列表，以逗号分隔。依赖于每种参数类型的
	 * {@code toString()} 方法。
	 */
	public static final String PLACEHOLDER_ARGUMENTS = "$[arguments]";

	/**
	 * {@code $[exception]} 占位符。替换为方法调用期间引发的任何 {@code Throwable} 的 {@code String} 表
	 * 示形式。
	 */
	public static final String PLACEHOLDER_EXCEPTION = "$[exception]";

	/**
	 * {@code $[invocationTime]} 占位符。替换为调用所耗时间（以毫秒为单位）。
	 */
	public static final String PLACEHOLDER_INVOCATION_TIME = "$[invocationTime]";

	/**
	 * 用于写入方法进入消息的默认消息。
	 */
	private static final String DEFAULT_ENTER_MESSAGE = "Entering method '" +
			PLACEHOLDER_METHOD_NAME + "' of class [" + PLACEHOLDER_TARGET_CLASS_NAME + "]";

	/**
	 * 用于写入方法退出消息的默认消息。
	 */
	private static final String DEFAULT_EXIT_MESSAGE = "Exiting method '" +
			PLACEHOLDER_METHOD_NAME + "' of class [" + PLACEHOLDER_TARGET_CLASS_NAME + "]";

	/**
	 * 用于写入异常消息的默认消息。
	 */
	private static final String DEFAULT_EXCEPTION_MESSAGE = "Exception thrown in method '" +
			PLACEHOLDER_METHOD_NAME + "' of class [" + PLACEHOLDER_TARGET_CLASS_NAME + "]";

	/**
	 * 用于匹配占位符的 {@code Pattern}。
	 */
	private static final Pattern PATTERN = Pattern.compile("\\$\\[\\p{Alpha}+]");

	/**
	 * 允许的占位符的 {@code Set}。
	 */
	private static final Set<Object> ALLOWED_PLACEHOLDERS =
			new Constants(CustomizableTraceInterceptor.class).getValues("PLACEHOLDER_");


	/**
	 * 方法进入时的消息。
	 */
	private String enterMessage = DEFAULT_ENTER_MESSAGE;

	/**
	 * 方法退出时的消息。
	 */
	private String exitMessage = DEFAULT_EXIT_MESSAGE;

	/**
	 * 方法执行期间发生异常时的消息。
	 */
	private String exceptionMessage = DEFAULT_EXCEPTION_MESSAGE;


	/**
	 * 设置用于方法进入日志消息的模板。此模板可以包含以下任一占位符：
	 * <ul>
	 * <li>{@code $[targetClassName]}</li>
	 * <li>{@code $[targetClassShortName]}</li>
	 * <li>{@code $[argumentTypes]}</li>
	 * <li>{@code $[arguments]}</li>
	 * </ul>
	 */
	public void setEnterMessage(String enterMessage) throws IllegalArgumentException {
		Assert.hasText(enterMessage, "enterMessage must not be empty");
		checkForInvalidPlaceholders(enterMessage);
		Assert.doesNotContain(enterMessage, PLACEHOLDER_RETURN_VALUE,
				"enterMessage cannot contain placeholder " + PLACEHOLDER_RETURN_VALUE);
		Assert.doesNotContain(enterMessage, PLACEHOLDER_EXCEPTION,
				"enterMessage cannot contain placeholder " + PLACEHOLDER_EXCEPTION);
		Assert.doesNotContain(enterMessage, PLACEHOLDER_INVOCATION_TIME,
				"enterMessage cannot contain placeholder " + PLACEHOLDER_INVOCATION_TIME);
		this.enterMessage = enterMessage;
	}

	/**
	 * 设置用于方法退出日志消息的模板。此模板可以包含以下任一占位符：
	 * <ul>
	 * <li>{@code $[targetClassName]}</li>
	 * <li>{@code $[targetClassShortName]}</li>
	 * <li>{@code $[argumentTypes]}</li>
	 * <li>{@code $[arguments]}</li>
	 * <li>{@code $[returnValue]}</li>
	 * <li>{@code $[invocationTime]}</li>
	 * </ul>
	 */
	public void setExitMessage(String exitMessage) {
		Assert.hasText(exitMessage, "exitMessage must not be empty");
		checkForInvalidPlaceholders(exitMessage);
		Assert.doesNotContain(exitMessage, PLACEHOLDER_EXCEPTION,
				"exitMessage cannot contain placeholder" + PLACEHOLDER_EXCEPTION);
		this.exitMessage = exitMessage;
	}

	/**
	 * 设置用于方法异常日志消息的模板。此模板可以包含以下任一占位符：
	 * <ul>
	 * <li>{@code $[targetClassName]}</li>
	 * <li>{@code $[targetClassShortName]}</li>
	 * <li>{@code $[argumentTypes]}</li>
	 * <li>{@code $[arguments]}</li>
	 * <li>{@code $[exception]}</li>
	 * </ul>
	 */
	public void setExceptionMessage(String exceptionMessage) {
		Assert.hasText(exceptionMessage, "exceptionMessage must not be empty");
		checkForInvalidPlaceholders(exceptionMessage);
		Assert.doesNotContain(exceptionMessage, PLACEHOLDER_RETURN_VALUE,
				"exceptionMessage cannot contain placeholder " + PLACEHOLDER_RETURN_VALUE);
		this.exceptionMessage = exceptionMessage;
	}


	/**
	 * 在调用之前根据 {@code enterMessage} 的值写入一条日志消息。如果调用成功，则在退出时根据
	 * {@code exitMessage} 的值写入一条日志消息。如果调用期间发生异常，则根据 {@code exceptionMessage}
	 * 的值写入一条消息。
	 * @see #setEnterMessage
	 * @see #setExitMessage
	 * @see #setExceptionMessage
	 */
	@Override
	protected Object invokeUnderTrace(MethodInvocation invocation, Log logger) throws Throwable {
		String name = ClassUtils.getQualifiedMethodName(invocation.getMethod());
		StopWatch stopWatch = new StopWatch(name);
		Object returnValue = null;
		boolean exitThroughException = false;
		try {
			stopWatch.start(name);
			writeToLog(logger,
					replacePlaceholders(this.enterMessage, invocation, null, null, -1));
			returnValue = invocation.proceed();
			return returnValue;
		}
		catch (Throwable ex) {
			if (stopWatch.isRunning()) {
				stopWatch.stop();
			}
			exitThroughException = true;
			writeToLog(logger, replacePlaceholders(
					this.exceptionMessage, invocation, null, ex, stopWatch.getTotalTimeMillis()), ex);
			throw ex;
		}
		finally {
			if (!exitThroughException) {
				if (stopWatch.isRunning()) {
					stopWatch.stop();
				}
				writeToLog(logger, replacePlaceholders(
						this.exitMessage, invocation, returnValue, null, stopWatch.getTotalTimeMillis()));
			}
		}
	}

	/**
	 * 将给定消息中的占位符替换为提供的值，或由这些值派生出的值。
	 * @param message the message template containing the placeholders to be replaced
	 * @param methodInvocation the {@code MethodInvocation} being logged.
	 * Used to derive values for all placeholders except {@code $[exception]}
	 * and {@code $[returnValue]}.
	 * @param returnValue any value returned by the invocation.
	 * Used to replace the {@code $[returnValue]} placeholder. May be {@code null}.
	 * @param throwable any {@code Throwable} raised during the invocation.
	 * The value of {@code Throwable.toString()} is replaced for the
	 * {@code $[exception]} placeholder. May be {@code null}.
	 * @param invocationTime the value to write in place of the
	 * {@code $[invocationTime]} placeholder
	 * @return the formatted output to write to the log
	 */
	protected String replacePlaceholders(String message, MethodInvocation methodInvocation,
			@Nullable Object returnValue, @Nullable Throwable throwable, long invocationTime) {

		Matcher matcher = PATTERN.matcher(message);
		Object target = methodInvocation.getThis();
		Assert.state(target != null, "Target must not be null");

		StringBuffer output = new StringBuffer();
		while (matcher.find()) {
			String match = matcher.group();
			if (PLACEHOLDER_METHOD_NAME.equals(match)) {
				matcher.appendReplacement(output, Matcher.quoteReplacement(methodInvocation.getMethod().getName()));
			}
			else if (PLACEHOLDER_TARGET_CLASS_NAME.equals(match)) {
				String className = getClassForLogging(target).getName();
				matcher.appendReplacement(output, Matcher.quoteReplacement(className));
			}
			else if (PLACEHOLDER_TARGET_CLASS_SHORT_NAME.equals(match)) {
				String shortName = ClassUtils.getShortName(getClassForLogging(target));
				matcher.appendReplacement(output, Matcher.quoteReplacement(shortName));
			}
			else if (PLACEHOLDER_ARGUMENTS.equals(match)) {
				matcher.appendReplacement(output,
						Matcher.quoteReplacement(StringUtils.arrayToCommaDelimitedString(methodInvocation.getArguments())));
			}
			else if (PLACEHOLDER_ARGUMENT_TYPES.equals(match)) {
				appendArgumentTypes(methodInvocation, matcher, output);
			}
			else if (PLACEHOLDER_RETURN_VALUE.equals(match)) {
				appendReturnValue(methodInvocation, matcher, output, returnValue);
			}
			else if (throwable != null && PLACEHOLDER_EXCEPTION.equals(match)) {
				matcher.appendReplacement(output, Matcher.quoteReplacement(throwable.toString()));
			}
			else if (PLACEHOLDER_INVOCATION_TIME.equals(match)) {
				matcher.appendReplacement(output, Long.toString(invocationTime));
			}
			else {
				// Should not happen since placeholders are checked earlier.
				throw new IllegalArgumentException("Unknown placeholder [" + match + "]");
			}
		}
		matcher.appendTail(output);

		return output.toString();
	}

	/**
	 * 将方法返回值的 {@code String} 表示形式添加到提供的 {@code StringBuffer} 中。能正确处理
	 * {@code null} 和 {@code void} 结果。
	 * @param methodInvocation the {@code MethodInvocation} that returned the value
	 * @param matcher the {@code Matcher} containing the matched placeholder
	 * @param output the {@code StringBuffer} to write output to
	 * @param returnValue the value returned by the method invocation.
	 */
	private void appendReturnValue(
			MethodInvocation methodInvocation, Matcher matcher, StringBuffer output, @Nullable Object returnValue) {

		if (methodInvocation.getMethod().getReturnType() == void.class) {
			matcher.appendReplacement(output, "void");
		}
		else if (returnValue == null) {
			matcher.appendReplacement(output, "null");
		}
		else {
			matcher.appendReplacement(output, Matcher.quoteReplacement(returnValue.toString()));
		}
	}

	/**
	 * 将方法参数类型的短 {@code Class} 名称列表（以逗号分隔）添加到输出中。例如，如果方法的签名为
	 * {@code put(java.lang.String, java.lang.Object)}，则返回值将是 {@code String, Object}。
	 * @param methodInvocation the {@code MethodInvocation} being logged.
	 * Arguments will be retrieved from the corresponding {@code Method}.
	 * @param matcher the {@code Matcher} containing the state of the output
	 * @param output the {@code StringBuffer} containing the output
	 */
	private void appendArgumentTypes(MethodInvocation methodInvocation, Matcher matcher, StringBuffer output) {
		Class<?>[] argumentTypes = methodInvocation.getMethod().getParameterTypes();
		String[] argumentTypeShortNames = new String[argumentTypes.length];
		for (int i = 0; i < argumentTypeShortNames.length; i++) {
			argumentTypeShortNames[i] = ClassUtils.getShortName(argumentTypes[i]);
		}
		matcher.appendReplacement(output,
				Matcher.quoteReplacement(StringUtils.arrayToCommaDelimitedString(argumentTypeShortNames)));
	}

	/**
	 * 检查提供的 {@code String} 是否包含未在此类中定义为常量的任何占位符，如果包含则抛出
	 * {@code IllegalArgumentException}。
	 */
	private void checkForInvalidPlaceholders(String message) throws IllegalArgumentException {
		Matcher matcher = PATTERN.matcher(message);
		while (matcher.find()) {
			String match = matcher.group();
			if (!ALLOWED_PLACEHOLDERS.contains(match)) {
				throw new IllegalArgumentException("Placeholder [" + match + "] is not valid");
			}
		}
	}

}
