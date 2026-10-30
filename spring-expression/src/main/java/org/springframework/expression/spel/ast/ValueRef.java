/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.expression.spel.ast;

import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.lang.Nullable;

/**
 * 表示对某个值的引用。借助引用可以获取或设置该值。传递值引用而非值本身，可以避免操作数求值的错误重
 * 复。例如在 'list[index++]++' 中，如果没有针对 'list[index++]' 的值引用，就不得不对 list[index++]
 * 求值两次（一次用于获取值，一次用于确定值的写入位置），这会使 index 递增两次。
 *
 * @author Andy Clement
 * @since 3.2
 */
public interface ValueRef {

	/**
	 * 返回此 ValueRef 指向的值，不应需要重新求值表达式组件。
	 * @return 值
	 */
	TypedValue getValue();

	/**
	 * 设置此 ValueRef 指向的值，不应需要重新求值表达式组件。
	 * @param newValue 新值
	 */
	void setValue(@Nullable Object newValue);

	/**
	 * 指示是否支持调用 setValue(Object)。
	 * @return 如果此值引用支持 setValue() 则返回 true。
	 */
	boolean isWritable();


	/**
	 * 用于 null 值的 ValueRef。
	 */
	class NullValueRef implements ValueRef {

		static final NullValueRef INSTANCE = new NullValueRef();

		@Override
		public TypedValue getValue() {
			return TypedValue.NULL;
		}

		@Override
		public void setValue(@Nullable Object newValue) {
			// The exception position '0' isn't right but the overhead of creating
			// instances of this per node (where the node is solely for error reporting)
			// would be unfortunate.
			throw new SpelEvaluationException(0, SpelMessage.NOT_ASSIGNABLE, "null");
		}

		@Override
		public boolean isWritable() {
			return false;
		}
	}


	/**
	 * 用于单个值的 ValueRef 持有者，该值不能被设置。
	 */
	class TypedValueHolderValueRef implements ValueRef {

		private final TypedValue typedValue;

		private final SpelNodeImpl node;  // used only for error reporting

		public TypedValueHolderValueRef(TypedValue typedValue, SpelNodeImpl node) {
			this.typedValue = typedValue;
			this.node = node;
		}

		@Override
		public TypedValue getValue() {
			return this.typedValue;
		}

		@Override
		public void setValue(@Nullable Object newValue) {
			throw new SpelEvaluationException(
					this.node.getStartPosition(), SpelMessage.NOT_ASSIGNABLE, this.node.toStringAST());
		}

		@Override
		public boolean isWritable() {
			return false;
		}
	}

}
