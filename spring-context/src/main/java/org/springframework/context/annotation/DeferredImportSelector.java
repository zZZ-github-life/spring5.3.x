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

package org.springframework.context.annotation;

import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.Nullable;

/**
 * {@link ImportSelector} 的一种变体，在所有 {@code @Configuration} Bean 处理完毕之后运行。当所选的
 * 导入是 {@code @Conditional} 时，这种类型的选择器特别有用。
 *
 * <p>实现还可以扩展 {@link org.springframework.core.Ordered} 接口，或使用
 * {@link org.springframework.core.annotation.Order} 注解来表明相对于其他
 * {@link DeferredImportSelector DeferredImportSelectors} 的优先级。
 *
 * <p>实现还可以提供 {@link #getImportGroup() 导入组}，它可以在不同选择器之间提供额外的排序和过滤逻
 * 辑。
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 4.0
 */
public interface DeferredImportSelector extends ImportSelector {

	/**
	 * 返回特定的导入组。
	 * <p>默认实现返回 {@code null}，表示不需要分组。
	 * @return the import group class, or {@code null} if none
	 * @since 5.0
	 */
	@Nullable
	default Class<? extends Group> getImportGroup() {
		return null;
	}


	/**
	 * 用于对不同导入选择器的结果进行分组的接口。
	 * @since 5.0
	 */
	interface Group {

		/**
		 * 使用指定的 {@link DeferredImportSelector} 处理导入方 @{@link Configuration} 类的
		 * {@link AnnotationMetadata}。
		 */
		void process(AnnotationMetadata metadata, DeferredImportSelector selector);

		/**
		 * 返回此组中应导入哪些类的 {@link Entry 条目}。
		 */
		Iterable<Entry> selectImports();


		/**
		 * 一个条目，保存导入方 {@link Configuration} 类的 {@link AnnotationMetadata} 以及要导入的类名。
		 */
		class Entry {

			private final AnnotationMetadata metadata;

			private final String importClassName;

			public Entry(AnnotationMetadata metadata, String importClassName) {
				this.metadata = metadata;
				this.importClassName = importClassName;
			}

			/**
			 * 返回导入方 {@link Configuration} 类的 {@link AnnotationMetadata}。
			 */
			public AnnotationMetadata getMetadata() {
				return this.metadata;
			}

			/**
			 * 返回要导入的类的完全限定名。
			 */
			public String getImportClassName() {
				return this.importClassName;
			}

			@Override
			public boolean equals(@Nullable Object other) {
				if (this == other) {
					return true;
				}
				if (other == null || getClass() != other.getClass()) {
					return false;
				}
				Entry entry = (Entry) other;
				return (this.metadata.equals(entry.metadata) && this.importClassName.equals(entry.importClassName));
			}

			@Override
			public int hashCode() {
				return (this.metadata.hashCode() * 31 + this.importClassName.hashCode());
			}

			@Override
			public String toString() {
				return this.importClassName;
			}
		}
	}

}
