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

package org.springframework.web.servlet.handler;

import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.Ordered;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

/**
 * {@link HandlerExceptionResolver} 实现的抽象基类。
 *
 * <p>支持映射的{@linkplain #setMappedHandlers 处理器}和
 * {@linkplain #setMappedHandlerClasses 处理器类}，解析器将应用于它们，
 * 并实现 {@link Ordered} 接口。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 */
public abstract class AbstractHandlerExceptionResolver implements HandlerExceptionResolver, Ordered {

	private static final String HEADER_CACHE_CONTROL = "Cache-Control";


	/** 可供子类使用的 Logger。 */
	protected final Log logger = LogFactory.getLog(getClass());

	private int order = Ordered.LOWEST_PRECEDENCE;

	@Nullable
	private Set<?> mappedHandlers;

	@Nullable
	private Class<?>[] mappedHandlerClasses;

	@Nullable
	private Log warnLogger;

	private boolean preventResponseCaching = false;


	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	/**
	 * 指定此异常解析器应应用的处理器集合。
	 * <p>异常映射和默认错误视图将仅应用于指定的处理器。
	 * <p>如果未设置任何处理器或处理器类，则异常映射和默认错误
	 * 视图将应用于所有处理器。这意味着指定的默认错误视图将用作
	 * 所有异常的兜底；在这种情况下，链中后续的任何 HandlerExceptionResolver
	 * 都将被忽略。
	 */
	public void setMappedHandlers(Set<?> mappedHandlers) {
		this.mappedHandlers = mappedHandlers;
	}

	/**
	 * 指定此异常解析器应应用的类集合。
	 * <p>异常映射和默认错误视图将仅应用于指定类型的处理器；
	 * 指定类型也可以是处理器的接口或超类。
	 * <p>如果未设置任何处理器或处理器类，则异常映射和默认错误
	 * 视图将应用于所有处理器。这意味着指定的默认错误视图将用作
	 * 所有异常的兜底；在这种情况下，链中后续的任何 HandlerExceptionResolver
	 * 都将被忽略。
	 */
	public void setMappedHandlerClasses(Class<?>... mappedHandlerClasses) {
		this.mappedHandlerClasses = mappedHandlerClasses;
	}

	/**
	 * 设置警告日志的日志类别。该名称将通过 Commons Logging 传递给
	 * 底层日志记录器实现，并根据日志记录器的配置
	 * 解释为日志类别。如果传入 {@code null} 或空字符串，
	 * 则关闭警告日志。
	 * <p>默认情况下没有警告日志，尽管像
	 * {@link org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver}
	 * 这样的子类可以更改该默认行为。指定此设置可激活到特定
	 * 类别的警告日志。或者，重写 {@link #logException} 方法以自定义日志记录。
	 * @see org.apache.commons.logging.LogFactory#getLog(String)
	 * @see java.util.logging.Logger#getLogger(String)
	 */
	public void setWarnLogCategory(String loggerName) {
		this.warnLogger = (StringUtils.hasLength(loggerName) ? LogFactory.getLog(loggerName) : null);
	}

	/**
	 * 指定是否禁止对此异常解析器解析的任何视图
	 * 进行 HTTP 响应缓存。
	 * <p>默认值为 {@code false}。将其切换为 {@code true} 可
	 * 自动生成抑制响应缓存的 HTTP 响应头。
	 */
	public void setPreventResponseCaching(boolean preventResponseCaching) {
		this.preventResponseCaching = preventResponseCaching;
	}


	/**
	 * 检查此解析器是否应适用（即所提供的处理器是否匹配所配置的
	 * 任何{@linkplain #setMappedHandlers 处理器}或
	 * {@linkplain #setMappedHandlerClasses 处理器类}），然后委托给
	 * {@link #doResolveException} 模板方法。
	 */
	@Override
	@Nullable
	public ModelAndView resolveException(
			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler, Exception ex) {

		if (shouldApplyTo(request, handler)) {
			prepareResponse(ex, response);
			ModelAndView result = doResolveException(request, response, handler, ex);
			if (result != null) {
				// Print debug message when warn logger is not enabled.
				if (logger.isDebugEnabled() && (this.warnLogger == null || !this.warnLogger.isWarnEnabled())) {
					logger.debug(buildLogMessage(ex, request) + (result.isEmpty() ? "" : " to " + result));
				}
				// Explicitly configured warn logger in logException method.
				logException(ex, request);
			}
			return result;
		}
		else {
			return null;
		}
	}

	/**
	 * 检查此解析器是否应应用于给定处理器。
	 * <p>默认实现会检查所配置的
	 * {@linkplain #setMappedHandlers 处理器}和
	 * {@linkplain #setMappedHandlerClasses 处理器类}（如果有）。
	 * @param request current HTTP request
	 * @param handler the executed handler, or {@code null} if none chosen
	 * at the time of the exception (for example, if multipart resolution failed)
	 * @return whether this resolved should proceed with resolving the exception
	 * for the given request and handler
	 * @see #setMappedHandlers
	 * @see #setMappedHandlerClasses
	 */
	protected boolean shouldApplyTo(HttpServletRequest request, @Nullable Object handler) {
		if (handler != null) {
			if (this.mappedHandlers != null && this.mappedHandlers.contains(handler)) {
				return true;
			}
			if (this.mappedHandlerClasses != null) {
				for (Class<?> handlerClass : this.mappedHandlerClasses) {
					if (handlerClass.isInstance(handler)) {
						return true;
					}
				}
			}
		}
		return !hasHandlerMappings();
	}

	/**
	 * 是否存在通过
	 * {@link #setMappedHandlers(Set)} 或 {@link #setMappedHandlerClasses(Class[])} 注册的任何处理器映射。
	 * @since 5.3
	 */
	protected boolean hasHandlerMappings() {
		return (this.mappedHandlers != null || this.mappedHandlerClasses != null);
	}

	/**
	 * 在警告级别记录给定异常，前提是已通过
	 * {@link #setWarnLogCategory "warnLogCategory"} 属性激活警告日志。
	 * <p>调用 {@link #buildLogMessage} 以确定要记录的具体消息。
	 * @param ex the exception that got thrown during handler execution
	 * @param request current HTTP request (useful for obtaining metadata)
	 * @see #setWarnLogCategory
	 * @see #buildLogMessage
	 * @see org.apache.commons.logging.Log#warn(Object, Throwable)
	 */
	protected void logException(Exception ex, HttpServletRequest request) {
		if (this.warnLogger != null && this.warnLogger.isWarnEnabled()) {
			this.warnLogger.warn(buildLogMessage(ex, request));
		}
	}

	/**
	 * 为处理给定请求期间发生的给定异常构建日志消息。
	 * @param ex the exception that got thrown during handler execution
	 * @param request current HTTP request (useful for obtaining metadata)
	 * @return the log message to use
	 */
	protected String buildLogMessage(Exception ex, HttpServletRequest request) {
		return "Resolved [" + LogFormatUtils.formatValue(ex, -1, true) + "]";
	}

	/**
	 * 为异常情况准备响应。
	 * <p>如果已将
	 * {@link #setPreventResponseCaching "preventResponseCaching"} 属性
	 * 设置为 "true"，则默认实现会阻止对响应进行缓存。
	 * @param ex the exception that got thrown during handler execution
	 * @param response current HTTP response
	 * @see #preventCaching
	 */
	protected void prepareResponse(Exception ex, HttpServletResponse response) {
		if (this.preventResponseCaching) {
			preventCaching(response);
		}
	}

	/**
	 * 通过设置相应的 HTTP {@code Cache-Control: no-store} 响应头，
	 * 阻止对响应进行缓存。
	 * @param response current HTTP response
	 */
	protected void preventCaching(HttpServletResponse response) {
		response.addHeader(HEADER_CACHE_CONTROL, "no-store");
	}


	/**
	 * 实际解析处理器执行期间抛出的给定异常，
	 * 如果合适，返回表示特定错误页面的 {@link ModelAndView}。
	 * <p>可以在子类中重写，以应用特定的异常检查。
	 * 请注意，此模板方法会在检查此解析器是否适用（"mappedHandlers" 等）
	 * <i>之后</i>被调用，因此实现可以简单地继续执行
	 * 实际的异常处理。
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param handler the executed handler, or {@code null} if none chosen at the time
	 * of the exception (for example, if multipart resolution failed)
	 * @param ex the exception that got thrown during handler execution
	 * @return a corresponding {@code ModelAndView} to forward to,
	 * or {@code null} for default processing in the resolution chain
	 */
	@Nullable
	protected abstract ModelAndView doResolveException(
			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler, Exception ex);

}
