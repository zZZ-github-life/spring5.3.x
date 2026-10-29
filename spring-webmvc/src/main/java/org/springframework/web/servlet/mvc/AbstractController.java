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

package org.springframework.web.servlet.mvc;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.WebContentGenerator;
import org.springframework.web.util.WebUtils;

/**
 * 使用模板方法设计模式的控制器实现的便捷超类。
 *
 * <p><b>工作流程
 * (<a href="Controller.html#workflow">以及由接口定义的工作流程</a>):</b><br>
 * <ol>
 * <li>{@link #handleRequest(HttpServletRequest, HttpServletResponse) handleRequest()}
 * 将由 DispatcherServlet 调用</li>
 * <li>检查受支持的请求方法（如果请求方法不受支持，则抛出 ServletException）</li>
 * <li>如果需要会话，则尝试获取会话（如果未找到，则抛出 ServletException）</li>
 * <li>根据 cacheSeconds 属性按需设置缓存响应头</li>
 * <li>调用抽象方法
 * {@link #handleRequestInternal(HttpServletRequest, HttpServletResponse) handleRequestInternal()}
 * （可选择在 HttpSession 上对该调用进行同步），
 * 该方法应由子类实现，以提供实际功能并返回
 * {@link org.springframework.web.servlet.ModelAndView ModelAndView} 对象。</li>
 * </ol>
 *
 * <p><b><a name="config">对外暴露的配置属性</a>
 * (<a href="Controller.html#config">以及由接口定义的那些属性</a>):</b><br>
 * <table border="1">
 * <tr>
 * <td><b>名称</b></td>
 * <td><b>默认值</b></td>
 * <td><b>描述</b></td>
 * </tr>
 * <tr>
 * <td>supportedMethods</td>
 * <td>GET,POST</td>
 * <td>此控制器支持的方法的逗号分隔（CSV）列表，
 * 例如 GET、POST 和 PUT</td>
 * </tr>
 * <tr>
 * <td>requireSession</td>
 * <td>false</td>
 * <td>是否要求请求必须携带会话，才能由此控制器处理。
 * 这可以确保派生控制器在调用 request.getSession() 获取会话时
 * 无需担心空指针。如果在处理请求时找不到会话，
 * 将抛出 ServletException</td>
 * </tr>
 * <tr>
 * <td>cacheSeconds</td>
 * <td>-1</td>
 * <td>表示在此请求之后的响应缓存头中应包含的秒数。
 * 0（零）表示包含不进行任何缓存的头，-1（默认值）表示不生成
 * <i>任何头</i>，而任何正数都会生成头，
 * 其值为指定的内容缓存秒数</td>
 * </tr>
 * <tr>
 * <td>synchronizeOnSession</td>
 * <td>false</td>
 * <td>对 {@code handleRequestInternal} 的调用是否应
 * 在 HttpSession 上同步，以串行化同一客户端的调用。
 * 如果没有 HttpSession，则此设置不起作用。
 * </td>
 * </tr>
 * </table>
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @see WebContentInterceptor
 */
public abstract class AbstractController extends WebContentGenerator implements Controller {

	private boolean synchronizeOnSession = false;


	/**
	 * 创建一个新的 AbstractController，默认支持
	 * HTTP 方法 GET、HEAD 和 POST。
	 */
	public AbstractController() {
		this(true);
	}

	/**
	 * 创建一个新的 AbstractController。
	 * @param restrictDefaultSupportedMethods {@code true} if this
	 * controller should support HTTP methods GET, HEAD and POST by default,
	 * or {@code false} if it should be unrestricted
	 * @since 4.3
	 */
	public AbstractController(boolean restrictDefaultSupportedMethods) {
		super(restrictDefaultSupportedMethods);
	}


	/**
	 * 设置控制器执行是否应在会话上同步，
	 * 以串行化来自同一客户端的并行调用。
	 * <p>更具体地说，如果此标志为 "true"，{@code handleRequestInternal}
	 * 方法的执行将被同步。同步将使用最可用的
	 * 会话互斥量；理想情况下，它将是由 HttpSessionMutexListener 暴露的互斥量。
	 * <p>该会话互斥量保证在会话的整个生命周期内都是同一个对象，
	 * 可通过 {@code SESSION_MUTEX_ATTRIBUTE} 常量定义的键获取。
	 * 它可用作对当前会话加锁的安全同步引用。
	 * <p>在许多情况下，HttpSession 引用本身也是一个安全的互斥量，
	 * 因为对于同一个活动逻辑会话，它始终是同一个对象引用。
	 * 然而，这一点在不同 Servlet 容器之间并无保证；
	 * 唯一 100% 安全的方式是使用会话互斥量。
	 * @see AbstractController#handleRequestInternal
	 * @see org.springframework.web.util.HttpSessionMutexListener
	 * @see org.springframework.web.util.WebUtils#getSessionMutex(javax.servlet.http.HttpSession)
	 */
	public final void setSynchronizeOnSession(boolean synchronizeOnSession) {
		this.synchronizeOnSession = synchronizeOnSession;
	}

	/**
	 * 返回控制器执行是否应在会话上同步。
	 */
	public final boolean isSynchronizeOnSession() {
		return this.synchronizeOnSession;
	}


	@Override
	@Nullable
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		if (HttpMethod.OPTIONS.matches(request.getMethod())) {
			response.setHeader("Allow", getAllowHeader());
			return null;
		}

		// Delegate to WebContentGenerator for checking and preparing.
		checkRequest(request);
		prepareResponse(response);

		// Execute handleRequestInternal in synchronized block if required.
		if (this.synchronizeOnSession) {
			HttpSession session = request.getSession(false);
			if (session != null) {
				Object mutex = WebUtils.getSessionMutex(session);
				synchronized (mutex) {
					return handleRequestInternal(request, response);
				}
			}
		}

		return handleRequestInternal(request, response);
	}

	/**
	 * 模板方法。子类必须实现此方法。
	 * 约定与 {@code handleRequest} 相同。
	 * @see #handleRequest
	 */
	@Nullable
	protected abstract ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
			throws Exception;

}
