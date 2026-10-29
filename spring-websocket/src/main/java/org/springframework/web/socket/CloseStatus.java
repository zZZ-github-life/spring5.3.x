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

package org.springframework.web.socket;

import java.io.Serializable;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * 表示 WebSocket 关闭状态码和原因。1xxx 范围内的状态码由协议预先定义。
 * 状态码可以选择性地与原因一起发送。
 *
 * <p>参见 <a href="https://tools.ietf.org/html/rfc6455#section-7.4.1">RFC 6455 第 7.4.1 节
 * "Defined Status Codes"</a>。
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public final class CloseStatus implements Serializable {

	private static final long serialVersionUID = 5199057709285570947L;


	/**
	 * "1000 表示正常关闭，即建立连接的目的已经达成。"
	 */
	public static final CloseStatus NORMAL = new CloseStatus(1000);

	/**
	 * "1001 表示端点正在'离开'，例如服务器关闭或浏览器已导航离开某个页面。"
	 */
	public static final CloseStatus GOING_AWAY = new CloseStatus(1001);

	/**
	 * "1002 表示端点由于协议错误而终止连接。"
	 */
	public static final CloseStatus PROTOCOL_ERROR  = new CloseStatus(1002);

	/**
	 * "1003 表示端点终止连接，因为它收到了无法接受的数据类型（例如，仅理解文本数据的
	 * 端点在收到二进制消息时可以发送此状态码）。"
	 */
	public static final CloseStatus NOT_ACCEPTABLE = new CloseStatus(1003);

	// 10004: Reserved.
	// The specific meaning might be defined in the future.

	/**
	 * "1005 是一个保留值，端点绝不能（MUST NOT）将其设置为 Close 控制帧中的状态码。
	 * 它被指定用于期望状态码的应用程序中，以指示实际上不存在任何状态码。"
	 */
	public static final CloseStatus NO_STATUS_CODE = new CloseStatus(1005);

	/**
	 * "1006 是一个保留值，端点绝不能（MUST NOT）将其设置为 Close 控制帧中的状态码。
	 * 它被指定用于期望状态码的应用程序中，以指示连接被异常关闭，例如未发送或接收
	 * Close 控制帧。"
	 */
	public static final CloseStatus NO_CLOSE_FRAME = new CloseStatus(1006);

	/**
	 * "1007 表示端点终止连接，因为它收到的消息中包含与消息类型不一致的数据
	 * （例如，文本消息中包含非 UTF-8 [RFC3629] 数据）。"
	 */
	public static final CloseStatus BAD_DATA = new CloseStatus(1007);

	/**
	 * "1008 表示端点终止连接，因为它收到了违反其策略的消息。这是一个通用状态码，
	 * 当没有其他更合适的状态码（例如 1003 或 1009）可用，或者需要隐藏有关策略的
	 * 具体细节时可以返回此状态码。"
	 */
	public static final CloseStatus POLICY_VIOLATION = new CloseStatus(1008);

	/**
	 * "1009 表示端点终止连接，因为它收到的消息太大而无法处理。"
	 */
	public static final CloseStatus TOO_BIG_TO_PROCESS = new CloseStatus(1009);

	/**
	 * "1010 表示端点（客户端）终止连接，因为它期望服务器协商一个或多个扩展，但服务器
	 * 未在 WebSocket 握手的响应消息中返回这些扩展。所需扩展的列表应该（SHOULD）出现在
	 * Close 帧的 /reason/ 部分中。请注意，服务器不使用此状态码，因为它可以直接令
	 * WebSocket 握手失败。"
	 */
	public static final CloseStatus REQUIRED_EXTENSION = new CloseStatus(1010);

	/**
	 * "1011 表示服务器终止连接，因为它遇到了阻止其完成请求的意外情况。"
	 */
	public static final CloseStatus SERVER_ERROR = new CloseStatus(1011);

	/**
	 * "1012 表示服务正在重启。客户端可以重新连接，如果选择重新连接，
	 * 应使用 5 - 30 秒的随机延迟进行重连。"
	 */
	public static final CloseStatus SERVICE_RESTARTED = new CloseStatus(1012);

	/**
	 * "1013 表示服务当前过载。客户端只应连接到不同的 IP（当目标存在多个 IP 时），
	 * 或者在用户操作后重新连接到同一 IP。"
	 */
	public static final CloseStatus SERVICE_OVERLOAD = new CloseStatus(1013);

	/**
	 * "1015 是一个保留值，端点绝不能（MUST NOT）将其设置为 Close 控制帧中的状态码。
	 * 它被指定用于期望状态码的应用程序中，以指示连接由于执行 TLS 握手失败而被关闭
	 * （例如，无法验证服务器证书）。"
	 */
	public static final CloseStatus TLS_HANDSHAKE_FAILURE = new CloseStatus(1015);

	/**
	 * 框架内部使用的状态码，表示会话已变得不可靠（例如发送消息时超时），
	 * 应当格外小心，例如避免向客户端发送正常关闭过程中可能发送的任何后续数据。
	 * @since 4.0.3
	 */
	public static final CloseStatus SESSION_NOT_RELIABLE = new CloseStatus(4500);


	private final int code;

	@Nullable
	private final String reason;


	/**
	 * 创建一个新的 {@link CloseStatus} 实例。
	 * @param code the status code
	 */
	public CloseStatus(int code) {
		this(code, null);
	}

	/**
	 * 创建一个新的 {@link CloseStatus} 实例。
	 * @param code the status code
	 * @param reason the reason
	 */
	public CloseStatus(int code, @Nullable String reason) {
		Assert.isTrue((code >= 1000 && code < 5000), () -> "Invalid status code: " + code);
		this.code = code;
		this.reason = reason;
	}


	/**
	 * 返回状态码。
	 */
	public int getCode() {
		return this.code;
	}

	/**
	 * 返回原因，如果没有则返回 {@code null}。
	 */
	@Nullable
	public String getReason() {
		return this.reason;
	}

	/**
	 * 根据当前实例创建一个带有指定原因的新 {@link CloseStatus}。
	 * @param reason the reason
	 * @return a new {@link CloseStatus} instance
	 */
	public CloseStatus withReason(String reason) {
		Assert.hasText(reason, "Reason must not be empty");
		return new CloseStatus(this.code, reason);
	}


	public boolean equalsCode(CloseStatus other) {
		return (this.code == other.code);
	}

	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof CloseStatus)) {
			return false;
		}
		CloseStatus otherStatus = (CloseStatus) other;
		return (this.code == otherStatus.code && ObjectUtils.nullSafeEquals(this.reason, otherStatus.reason));
	}

	@Override
	public int hashCode() {
		return this.code * 29 + ObjectUtils.nullSafeHashCode(this.reason);
	}

	@Override
	public String toString() {
		return "CloseStatus[code=" + this.code + ", reason=" + this.reason + "]";
	}

}
