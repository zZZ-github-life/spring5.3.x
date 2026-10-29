/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.core.type.filter;

import java.io.IOException;

import org.aspectj.bridge.IMessageHandler;
import org.aspectj.weaver.ResolvedType;
import org.aspectj.weaver.World;
import org.aspectj.weaver.bcel.BcelWorld;
import org.aspectj.weaver.patterns.Bindings;
import org.aspectj.weaver.patterns.FormalBinding;
import org.aspectj.weaver.patterns.IScope;
import org.aspectj.weaver.patterns.PatternParser;
import org.aspectj.weaver.patterns.SimpleScope;
import org.aspectj.weaver.patterns.TypePattern;

import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.lang.Nullable;

/**
 * 使用 AspectJ 类型模式进行匹配的类型过滤器。
 *
 * <p>此类型过滤器的一个关键实现细节是：它不会加载所检查的类来与类型模式进行匹配。
 *
 * @author Ramnivas Laddad
 * @author Juergen Hoeller
 * @since 2.5
 */
public class AspectJTypeFilter implements TypeFilter {

	private final World world;

	private final TypePattern typePattern;


	public AspectJTypeFilter(String typePatternExpression, @Nullable ClassLoader classLoader) {
		this.world = new BcelWorld(classLoader, IMessageHandler.THROW, null);
		this.world.setBehaveInJava5Way(true);
		PatternParser patternParser = new PatternParser(typePatternExpression);
		TypePattern typePattern = patternParser.parseTypePattern();
		typePattern.resolve(this.world);
		IScope scope = new SimpleScope(this.world, new FormalBinding[0]);
		this.typePattern = typePattern.resolveBindings(scope, Bindings.NONE, false, false);
	}


	@Override
	public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory)
			throws IOException {

		String className = metadataReader.getClassMetadata().getClassName();
		ResolvedType resolvedType = this.world.resolve(className);
		return this.typePattern.matchesStatically(resolvedType);
	}

}
