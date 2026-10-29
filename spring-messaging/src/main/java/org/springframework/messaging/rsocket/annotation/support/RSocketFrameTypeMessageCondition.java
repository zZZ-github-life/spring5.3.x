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

package org.springframework.messaging.rsocket.annotation.support;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import io.rsocket.frame.FrameType;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.AbstractMessageCondition;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * 用于协助基于 RSocket 帧类型映射到处理器方法的条件。这有助于将
 * 连接级帧类型（即 {@code SETUP} 和 {@code METADATA_PUSH}）的处理与
 * 流请求的处理区分开来。
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
public class RSocketFrameTypeMessageCondition extends AbstractMessageCondition<RSocketFrameTypeMessageCondition> {

	/** 包含正在处理的 RSocket 帧类型的头部名称。 */
	public static final String FRAME_TYPE_HEADER = "rsocketFrameType";


	/** 匹配连接级帧 "SETUP" 或 "METADATA_PUSH"。 */
	public static final RSocketFrameTypeMessageCondition CONNECT_CONDITION =
			new RSocketFrameTypeMessageCondition(FrameType.SETUP, FrameType.METADATA_PUSH);

	/** 匹配 RSocket 帧 "REQUEST_FNF" 或 "REQUEST_RESPONSE"。 */
	public static final RSocketFrameTypeMessageCondition REQUEST_FNF_OR_RESPONSE_CONDITION =
			new RSocketFrameTypeMessageCondition(FrameType.REQUEST_FNF, FrameType.REQUEST_RESPONSE);

	/** 匹配 RSocket 帧 "REQUEST_RESPONSE"。 */
	public static final RSocketFrameTypeMessageCondition REQUEST_RESPONSE_CONDITION =
			new RSocketFrameTypeMessageCondition(FrameType.REQUEST_RESPONSE);

	/** 匹配 RSocket 帧 "REQUEST_STREAM"。 */
	public static final RSocketFrameTypeMessageCondition REQUEST_STREAM_CONDITION =
			new RSocketFrameTypeMessageCondition(FrameType.REQUEST_STREAM);

	/** 匹配 RSocket 帧 "REQUEST_CHANNEL"。 */
	public static final RSocketFrameTypeMessageCondition REQUEST_CHANNEL_CONDITION =
			new RSocketFrameTypeMessageCondition(FrameType.REQUEST_CHANNEL);

	/** 不匹配任何 RSocket 帧的空条件（例如用于类型级映射）。 */
	public static final RSocketFrameTypeMessageCondition EMPTY_CONDITION = new RSocketFrameTypeMessageCondition();


	/**
	 * 用于匹配 "REQUEST_FNF"、"REQUEST_RESPONSE"、"REQUEST_STREAM"
	 * 和 "REQUEST_CHANNEL" 的条件。
	 * @deprecated as of 5.2.2 because matching to all interaction types is too
	 * flexible. Please use one of the other constants in this class that match
	 * to specific frames.
	 */
	@Deprecated
	public static final RSocketFrameTypeMessageCondition REQUEST_CONDITION =
			new RSocketFrameTypeMessageCondition(
					FrameType.REQUEST_FNF,
					FrameType.REQUEST_RESPONSE,
					FrameType.REQUEST_STREAM,
					FrameType.REQUEST_CHANNEL);

	/** 按 FrameType 缓存，以便从 getMatchingCondition 返回现成的实例。 */
	private static final Map<String, RSocketFrameTypeMessageCondition> frameTypeConditionCache;

	static {
		frameTypeConditionCache = CollectionUtils.newHashMap(FrameType.values().length);
		for (FrameType type : FrameType.values()) {
			frameTypeConditionCache.put(type.name(), new RSocketFrameTypeMessageCondition(type));
		}
	}


	private final Set<FrameType> frameTypes;


	public RSocketFrameTypeMessageCondition(FrameType... frameType) {
		this(Arrays.asList(frameType));
	}

	public RSocketFrameTypeMessageCondition(Collection<FrameType> frameTypes) {
		Assert.notEmpty(frameTypes, "`frameTypes` are required");
		this.frameTypes = Collections.unmodifiableSet(new LinkedHashSet<>(frameTypes));
	}

	private RSocketFrameTypeMessageCondition() {
		this.frameTypes = Collections.emptySet();
	}


	public Set<FrameType> getFrameTypes() {
		return this.frameTypes;
	}

	@Override
	protected Collection<?> getContent() {
		return this.frameTypes;
	}

	@Override
	protected String getToStringInfix() {
		return " || ";
	}

	/**
	 * 在消息头部中查找 RSocket 帧类型。
	 * @param message the current message
	 * @return the frame type or {@code null} if not found
	 */
	@Nullable
	public static FrameType getFrameType(Message<?> message) {
		return (FrameType) message.getHeaders().get(RSocketFrameTypeMessageCondition.FRAME_TYPE_HEADER);
	}


	@Override
	public RSocketFrameTypeMessageCondition combine(RSocketFrameTypeMessageCondition other) {
		if (this.frameTypes.equals(other.frameTypes)) {
			return other;
		}
		Set<FrameType> set = new LinkedHashSet<>(this.frameTypes);
		set.addAll(other.frameTypes);
		return new RSocketFrameTypeMessageCondition(set);
	}

	@Override
	public RSocketFrameTypeMessageCondition getMatchingCondition(Message<?> message) {
		FrameType actual = message.getHeaders().get(FRAME_TYPE_HEADER, FrameType.class);
		if (actual != null) {
			for (FrameType type : this.frameTypes) {
				if (actual == type) {
					return frameTypeConditionCache.get(type.name());
				}
			}
		}
		return  null;
	}

	@Override
	public int compareTo(RSocketFrameTypeMessageCondition other, Message<?> message) {
		return other.frameTypes.size() - this.frameTypes.size();
	}


	/**
	 * 返回一个条件，用于匹配根据某个处理器方法声明所推断出的请求和响应
	 * 基数（cardinality）而选择的 RSocket 请求交互类型。
	 * <p>下表列出了所做的选择：
	 * <table>
	 * <tr>
	 * <th>请求基数</th>
	 * <th>响应基数</th>
	 * <th>交互类型</th>
	 * </tr>
	 * <tr>
	 * <td>0,1</td>
	 * <td>0</td>
	 * <td>Fire-And-Forget, Request-Response</td>
	 * </tr>
	 * <tr>
	 * <td>0,1</td>
	 * <td>1</td>
	 * <td>Request-Response</td>
	 * </tr>
	 * <tr>
	 * <td>0,1</td>
	 * <td>2</td>
	 * <td>Request-Stream</td>
	 * </tr>
	 * <tr>
	 * <td>2</td>
	 * <td>Any</td>
	 * <td>Request-Channel</td>
	 * </tr>
	 * </table>
	 * @param cardinalityIn -- the request cardinality: 1 for a single payload,
	 * 2 for many payloads, and 0 if input is not handled.
	 * @param cardinalityOut -- the response cardinality: 0 for no output
	 * payloads, 1 for a single payload, and 2 for many payloads.
	 * @return a condition to use for matching the interaction type
	 * @since 5.2.2
	 */
	public static RSocketFrameTypeMessageCondition getCondition(int cardinalityIn, int cardinalityOut) {
		switch (cardinalityIn) {
			case 0:
			case 1:
				switch (cardinalityOut) {
					case 0: return REQUEST_FNF_OR_RESPONSE_CONDITION;
					case 1: return REQUEST_RESPONSE_CONDITION;
					case 2: return REQUEST_STREAM_CONDITION;
					default: throw new IllegalStateException("Invalid cardinality: " + cardinalityOut);
				}
			case 2:
				return REQUEST_CHANNEL_CONDITION;
			default:
				throw new IllegalStateException("Invalid cardinality: " + cardinalityIn);
		}
	}

}
