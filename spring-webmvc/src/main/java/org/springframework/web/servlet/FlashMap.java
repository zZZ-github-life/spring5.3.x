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

package org.springframework.web.servlet;

import java.util.HashMap;

import org.springframework.lang.Nullable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * FlashMap 提供了一种方式，使一个请求可以存储供另一个请求使用的属性。
 * 这在从一个 URL 重定向到另一个 URL 时最为常见 —— 例如
 * Post/Redirect/Get 模式。FlashMap 在重定向之前被保存
 * （通常保存在会话中），并在重定向之后变得可用，随后立即被移除。
 *
 * <p>可以为 FlashMap 设置请求路径和请求参数，以帮助识别目标请求。
 * 如果没有这些信息，FlashMap 将提供给下一个请求，而该请求
 * 不一定是预期的接收者。在重定向时，目标 URL 是已知的，
 * 可以用该信息更新 FlashMap。使用
 * {@code org.springframework.web.servlet.view.RedirectView} 时，
 * 这会自动完成。
 *
 * <p>注意：带注解的控制器通常不会直接使用 FlashMap。
 * 关于在带注解的控制器中使用 flash 属性的概述，请参见
 * {@code org.springframework.web.servlet.mvc.support.RedirectAttributes}。
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 * @see FlashMapManager
 */
@SuppressWarnings("serial")
public final class FlashMap extends HashMap<String, Object> implements Comparable<FlashMap> {

	@Nullable
	private String targetRequestPath;

	private final MultiValueMap<String, String> targetRequestParams = new LinkedMultiValueMap<>(3);

	private long expirationTime = -1;


	/**
	 * 提供一个 URL 路径，帮助识别此 FlashMap 的目标请求。
	 * <p>该路径可以是绝对路径（例如 "/application/resource"），
	 * 也可以是相对于当前请求的路径（例如 "../resource"）。
	 */
	public void setTargetRequestPath(@Nullable String path) {
		this.targetRequestPath = path;
	}

	/**
	 * 返回目标 URL 路径（如果未指定则返回 {@code null}）。
	 */
	@Nullable
	public String getTargetRequestPath() {
		return this.targetRequestPath;
	}

	/**
	 * 提供用于识别此 FlashMap 对应请求的请求参数。
	 * @param params a Map with the names and values of expected parameters
	 */
	public FlashMap addTargetRequestParams(@Nullable MultiValueMap<String, String> params) {
		if (params != null) {
			params.forEach((key, values) -> {
				for (String value : values) {
					addTargetRequestParam(key, value);
				}
			});
		}
		return this;
	}

	/**
	 * 提供一个用于识别此 FlashMap 对应请求的请求参数。
	 * @param name the expected parameter name (skipped if empty)
	 * @param value the expected value (skipped if empty)
	 */
	public FlashMap addTargetRequestParam(String name, String value) {
		if (StringUtils.hasText(name) && StringUtils.hasText(value)) {
			this.targetRequestParams.add(name, value);
		}
		return this;
	}

	/**
	 * 返回用于识别目标请求的参数，如果没有则返回空 Map。
	 */
	public MultiValueMap<String, String> getTargetRequestParams() {
		return this.targetRequestParams;
	}

	/**
	 * 开始此实例的过期计时。
	 * @param timeToLive the number of seconds before expiration
	 */
	public void startExpirationPeriod(int timeToLive) {
		this.expirationTime = System.currentTimeMillis() + timeToLive * 1000;
	}

	/**
	 * 设置 FlashMap 的过期时间。这是为序列化目的提供的，
	 * 但也可以用来替代 {@link #startExpirationPeriod(int)}。
	 * @since 4.2
	 */
	public void setExpirationTime(long expirationTime) {
		this.expirationTime = expirationTime;
	}

	/**
	 * 返回 FlashMap 的过期时间，如果过期计时尚未开始则返回 -1。
	 * @since 4.2
	 */
	public long getExpirationTime() {
		return this.expirationTime;
	}

	/**
	 * 根据自调用 {@link #startExpirationPeriod} 以来经过的时间，
	 * 返回此实例是否已过期。
	 */
	public boolean isExpired() {
		return (this.expirationTime != -1 && System.currentTimeMillis() > this.expirationTime);
	}


	/**
	 * 比较两个 FlashMap，优先选择指定了目标 URL 路径
	 * 或具有更多目标 URL 参数的那个。在比较 FlashMap 实例之前，
	 * 请确保它们与给定的请求匹配。
	 */
	@Override
	public int compareTo(FlashMap other) {
		int thisUrlPath = (this.targetRequestPath != null ? 1 : 0);
		int otherUrlPath = (other.targetRequestPath != null ? 1 : 0);
		if (thisUrlPath != otherUrlPath) {
			return otherUrlPath - thisUrlPath;
		}
		else {
			return other.targetRequestParams.size() - this.targetRequestParams.size();
		}
	}

	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof FlashMap)) {
			return false;
		}
		FlashMap otherFlashMap = (FlashMap) other;
		return (super.equals(otherFlashMap) &&
				ObjectUtils.nullSafeEquals(this.targetRequestPath, otherFlashMap.targetRequestPath) &&
				this.targetRequestParams.equals(otherFlashMap.targetRequestParams));
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + ObjectUtils.nullSafeHashCode(this.targetRequestPath);
		result = 31 * result + this.targetRequestParams.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "FlashMap [attributes=" + super.toString() + ", targetRequestPath=" +
				this.targetRequestPath + ", targetRequestParams=" + this.targetRequestParams + "]";
	}

}
