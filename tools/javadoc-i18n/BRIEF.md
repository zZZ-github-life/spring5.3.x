# Javadoc 中文化任务规范

目标：把 Spring Framework `src/main/java` 下的 Javadoc **正文**译为简体中文。
`src/test/java` 不在范围内。

## 输入 / 输出

输入：`tools/javadoc-i18n/work/batches/<module>/batch-NNN.jsonl`，每行一个任务：

```json
{"id":"文件::行号::序号","file":"相对路径","module":"模块","start":12,"end":15,"indent":"\t","lines":["英文第一行","英文第二行"]}
```

输出：同目录下的 `zh-NNN.jsonl`，每个任务一行：

```json
{"id":"...","zh":["中文第一行","中文第二行"]}
```

`zh` 也可以是一个字符串（用 `\n` 分段），但数组更清晰。

## 翻译规则

1. 只译正文。`@author`、`@since`、`@param`、`@return`、`@throws`、`@see`、`@deprecated`、`@link` 等标签行**不在任务里**，不要自行添加或翻译它们。
2. 行内标签和 HTML **原样保留**：`{@link X}`、`{@code x}`、`{@linkplain ...}`、`<code>`、`<p>`、`<b>`、`<em>`、`<a href="...">`、`&mdash;`、`&lt;` 等。可以调整它们周围的换行，但不能改写内容。
3. `<pre>` 代码块不在任务里，保持原样（不要复制到译文里）。
4. 保持原意，术语统一：
   - bean → Bean；container → 容器；application context → 应用上下文
   - dependency injection → 依赖注入；bean definition → Bean 定义
   - factory → 工厂；proxy → 代理；advice → 通知；pointcut → 切点
   - aspect → 切面；interceptor → 拦截器；callback → 回调
   - namespace → 命名空间；property → 属性；annotation → 注解
   - resource → 资源；lifecycle → 生命周期；scope → 作用域
   - `@Configuration` 等注解名、类名、方法名一律保留英文
5. 不要加入原文没有的解释、示例或“注：”之类内容。忠实翻译，可以意译。
6. 一行译文不宜过长，回写脚本会自动按显示宽度换行，所以 `zh` 里一个元素可以是一整段。
7. 原文有多个自然段时，用空字符串 `""` 分段；否则省略。

## 执行步骤

```powershell
$env:PYTHONIOENCODING='utf-8'
# 1. 写 zh-NNN.jsonl（用 Python 写文件，避免编码问题）
# 2. 先 dry-run 校验
python tools/javadoc-i18n/apply.py --root . --tasks tools/javadoc-i18n/work/batches/<module>/batch-NNN.jsonl --translations tools/javadoc-i18n/work/batches/<module>/zh-NNN.jsonl --dry-run
# 3. 确认 applied 数等于任务数、skipped=0 后正式回写
python tools/javadoc-i18n/apply.py --root . --tasks tools/javadoc-i18n/work/batches/<module>/batch-NNN.jsonl --translations tools/javadoc-i18n/work/batches/<module>/zh-NNN.jsonl
```

## 质量检查

- 每个 `id` 都必须出现在输出里，数量与输入一致。
- `skipped` 必须为 0；出现 `content mismatch` 说明文件被改动过，停止并报告。
- 抽查 2-3 个文件，确认中文可读、`{@link}` / `{@code}` / `<p>` 未被破坏、`@param` 仍是英文。
- **不要修改任务范围之外的任何文件**，不要动 `src/test/java`，不要格式化代码。
