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

package org.springframework.web.servlet.view.tiles3;

import org.apache.tiles.request.render.Renderer;

import org.springframework.lang.Nullable;
import org.springframework.web.servlet.view.AbstractUrlBasedView;
import org.springframework.web.servlet.view.UrlBasedViewResolver;

/**
 * {@link UrlBasedViewResolver} 的便捷子类，支持
 * {@link TilesView}（即 Tiles 定义）及其自定义子类。
 *
 * @author Nicolas Le Bas
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @since 3.2
 */
public class TilesViewResolver extends UrlBasedViewResolver {

	@Nullable
	private Renderer renderer;

	@Nullable
	private Boolean alwaysInclude;


	/**
	 * 此解析器要求使用 {@link TilesView}。
	 */
	public TilesViewResolver() {
		setViewClass(requiredViewClass());
	}


	/**
	 * 设置要使用的 {@link Renderer}。如果未指定，将使用默认的
	 * {@link org.apache.tiles.renderer.DefinitionRenderer}。
	 * @see TilesView#setRenderer(Renderer)
	 */
	public void setRenderer(Renderer renderer) {
		this.renderer = renderer;
	}

	/**
	 * 指定是否始终包含视图而不是转发到视图。
	 * <p>默认值为 "false"。开启此标志可强制使用
	 * Servlet include，即使可以进行转发也是如此。
	 * @since 4.1.2
	 * @see TilesView#setAlwaysInclude
	 */
	public void setAlwaysInclude(Boolean alwaysInclude) {
		this.alwaysInclude = alwaysInclude;
	}


	@Override
	protected Class<?> requiredViewClass() {
		return TilesView.class;
	}

	@Override
	protected AbstractUrlBasedView instantiateView() {
		return (getViewClass() == TilesView.class ? new TilesView() : super.instantiateView());
	}

	@Override
	protected TilesView buildView(String viewName) throws Exception {
		TilesView view = (TilesView) super.buildView(viewName);
		if (this.renderer != null) {
			view.setRenderer(this.renderer);
		}
		if (this.alwaysInclude != null) {
			view.setAlwaysInclude(this.alwaysInclude);
		}
		return view;
	}

}
