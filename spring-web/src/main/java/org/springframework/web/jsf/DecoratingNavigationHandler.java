/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.web.jsf;

import javax.faces.application.NavigationHandler;
import javax.faces.context.FacesContext;

import org.springframework.lang.Nullable;

/**
 * JSF NavigationHandler 实现的基类，这些实现希望能够装饰原始的 NavigationHandler。
 *
 * <p>支持标准的 JSF 装饰方式（通过构造函数参数），也支持带有显式
 * NavigationHandler 参数的重载 {@code handleNavigation} 方法
 * （传入原始 NavigationHandler）。子类必须实现此重载的
 * {@code handleNavigation} 方法。标准 JSF 调用会自动委托给该重载方法，
 * 并将构造函数注入的 NavigationHandler 作为参数。
 *
 * @author Juergen Hoeller
 * @since 1.2.7
 * @see #handleNavigation(javax.faces.context.FacesContext, String, String, NavigationHandler)
 * @see DelegatingNavigationHandlerProxy
 */
public abstract class DecoratingNavigationHandler extends NavigationHandler {

	@Nullable
	private NavigationHandler decoratedNavigationHandler;


	/**
	 * 创建一个不带固定原始 NavigationHandler 的 DecoratingNavigationHandler。
	 */
	protected DecoratingNavigationHandler() {
	}

	/**
	 * 创建一个带固定原始 NavigationHandler 的 DecoratingNavigationHandler。
	 * @param originalNavigationHandler 要装饰的原始 NavigationHandler
	 */
	protected DecoratingNavigationHandler(NavigationHandler originalNavigationHandler) {
		this.decoratedNavigationHandler = originalNavigationHandler;
	}

	/**
	 * 返回由该处理器装饰的固定原始 NavigationHandler（如果有），
	 * 即通过构造函数传入的那个。
	 */
	@Nullable
	public final NavigationHandler getDecoratedNavigationHandler() {
		return this.decoratedNavigationHandler;
	}


	/**
	 * 标准 JSF {@code handleNavigation} 方法的此实现会委托给重载的变体，
	 * 并将构造函数注入的 NavigationHandler 作为参数传入。
	 * @see #handleNavigation(javax.faces.context.FacesContext, String, String, javax.faces.application.NavigationHandler)
	 */
	@Override
	public final void handleNavigation(FacesContext facesContext, String fromAction, String outcome) {
		handleNavigation(facesContext, fromAction, outcome, this.decoratedNavigationHandler);
	}

	/**
	 * 带显式 NavigationHandler 参数的特殊 {@code handleNavigation} 变体。
	 * 既可以由代码使用显式的原始处理器直接调用，也可以从标准的
	 * {@code handleNavigation} 方法中作为普通 JSF 定义的 NavigationHandler 调用。
	 * <p>实现应调用 {@code callNextHandlerInChain} 以委托给链中的下一个处理器。
	 * 这会始终调用最合适的下一个处理器（参见 {@code callNextHandlerInChain} 的
	 * javadoc）。或者，也可以直接调用被装饰的 NavigationHandler 或传入的原始
	 * NavigationHandler；不过，在应对链中可能的位置方面，这种方式不够灵活。
	 *
	 * @param facesContext 当前的 JSF 上下文
	 * @param fromAction 为获取指定结果而求值的动作绑定表达式，
	 * 如果结果是通过其他方式获得的，则为 {@code null}
	 * @param outcome 先前调用的应用动作返回的逻辑结果（可能为 {@code null}）
	 * @param originalNavigationHandler 原始 NavigationHandler，如果没有则为 {@code null}
	 * @see #callNextHandlerInChain
	 */
	public abstract void handleNavigation(FacesContext facesContext, @Nullable String fromAction,
			@Nullable String outcome, @Nullable NavigationHandler originalNavigationHandler);


	/**
	 * 子类在打算委托给 NavigationHandler 链中的下一个处理器时应调用的方法。
	 * 它将始终调用最合适的下一个处理器：根据此实例在链中的位置，可能是作为构造函数
	 * 参数传入的被装饰 NavigationHandler，也可能是传入此方法的原始 NavigationHandler。
	 * <p>如果指定了作为构造函数参数的被装饰 NavigationHandler，则会调用它。
	 * 如果目标是 DecoratingNavigationHandler，则传入此方法的原始 NavigationHandler
	 * 会被继续传递给链中的下一个元素：这样可以确保原始处理器得以传播，
	 * 处理器链中的最后一个元素可能会再委托回它。如果目标是标准的
	 * NavigationHandler，则原始处理器不会被传递；在这种情况下，链中后续元素
	 * 无法再委托回原始处理器。
	 * <p>如果没有指定作为构造函数参数的被装饰 NavigationHandler，则此实例就是链中的
	 * 最后一个元素。因此，此方法将调用传入此方法的原始 NavigationHandler。
	 * 如果未传入原始 NavigationHandler（例如此实例是链中最后一个元素，
	 * 而之前的元素都是标准 NavigationHandler），则此方法相当于空操作。
	 *
	 * @param facesContext 当前的 JSF 上下文
	 * @param fromAction 为获取指定结果而求值的动作绑定表达式，
	 * 如果结果是通过其他方式获得的，则为 {@code null}
	 * @param outcome 先前调用的应用动作返回的逻辑结果（可能为 {@code null}）
	 * @param originalNavigationHandler 原始 NavigationHandler，如果没有则为 {@code null}
	 */
	protected final void callNextHandlerInChain(FacesContext facesContext, @Nullable String fromAction,
			@Nullable String outcome, @Nullable NavigationHandler originalNavigationHandler) {

		NavigationHandler decoratedNavigationHandler = getDecoratedNavigationHandler();

		if (decoratedNavigationHandler instanceof DecoratingNavigationHandler) {
			// DecoratingNavigationHandler specified through constructor argument:
			// Call it with original NavigationHandler passed in.
			DecoratingNavigationHandler decHandler = (DecoratingNavigationHandler) decoratedNavigationHandler;
			decHandler.handleNavigation(facesContext, fromAction, outcome, originalNavigationHandler);
		}
		else if (decoratedNavigationHandler != null) {
			// Standard NavigationHandler specified through constructor argument:
			// Call it through standard API, without original NavigationHandler passed in.
			// The called handler will not be able to redirect to the original handler.
			decoratedNavigationHandler.handleNavigation(facesContext, fromAction, outcome);
		}
		else if (originalNavigationHandler != null) {
			// No NavigationHandler specified through constructor argument:
			// Call original handler, marking the end of this chain.
			originalNavigationHandler.handleNavigation(facesContext, fromAction, outcome);
		}
	}

}
