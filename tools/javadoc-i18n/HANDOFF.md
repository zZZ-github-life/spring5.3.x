# Javadoc 中文化 —— 交接说明

本目录是一套“提取 → 翻译 → 回写 → 校验”的流水线，用于把 Spring Framework
`src/main/java` 下的 Javadoc 正文译为简体中文。`src/test/java` 不在范围内。

## 当前进度

- ✅ `spring-core`：34 / 34 批完成（3,939 段）
- ✅ `spring-beans`：20 / 20 批完成（2,531 段）
- ✅ `spring-jcl`（45 段，未走批次）、`spring-context-indexer`（1 / 1 批，24 段）
- 🔄 `spring-context`：8 / 34 批（已完成 942 段，剩 2,848 段）
- 🔄 `spring-aop`：2 / 12 批（已完成 175 段，剩 1,071 段）
- ⬜ 其余模块：未开始

校验：`python tools\javadoc-i18n\verify.py` → `checked 984 modified java files, 0 problems`；
`git diff --check` 无尾随空白。

## 目录结构

```
tools/javadoc-i18n/
  extract.py     从 Java 源码提取待译 Javadoc 正文片段
  split.py       把片段按“文件不重叠”的规则切成批次
  apply.py       校验并把译文写回源码（自动换行、中文避头尾）
  pending.py     列出还没译完的批次
  progress.py    总体进度
  verify.py      结构校验（{@link} 配平、@param 无中文、无乱码）
  BRIEF.md       翻译规范（子代理必读）
  work/
    tasks-all.jsonl    全量任务快照（提取时生成）
    baseline.json      进度基数
    batches/<module>/batch-NNN.jsonl   输入批次
    batches/<module>/zh-NNN.jsonl      译文（每行 {"id":..,"zh":[...]}，也支持单个 JSON 对象）
```

## 在新窗口继续

**不要重新运行 `extract.py` / `split.py`。** 批次是按当时的文件内容生成的，每个文件只属于
一个批次，回写时从后往前替换，行号不会互相干扰。重新分批会打乱已有 `zh-*.jsonl` 的对应关系。

1. 看还有哪些批次没完成：

```powershell
$env:PYTHONIOENCODING='utf-8'
python tools\javadoc-i18n\pending.py 30
```

2. 对每个待处理批次，让子代理按下面模板执行（把 `<module>`、`<NNN>`、`<N>` 换掉）：

```
仓库：D:\a-project\source-code\spring-framework

先读 tools/javadoc-i18n/BRIEF.md，严格遵守。

任务：处理 tools/javadoc-i18n/work/batches/<module>/batch-<NNN>.jsonl，输出到同目录 zh-<NNN>.jsonl。
- 先统计输入行数 N。每行 {"id":"...","zh":["中文", ...]}，用 Python 以 UTF-8、ensure_ascii=False 写入。
- 输入里每个 id 都必须出现在输出中，禁止遗漏。
- 保留 {@link}/{@code}/{@linkplain}/{@value}/<code>/<p>/<b>/<strong>/<i>/<a href> 等标签原样，
  尤其不要翻译 {@code ...} 内部内容；类名、方法名、注解名、包名不译。
- zh 数组里一个自然段一个元素，不要在末尾放空字符串。

校验与回写：
python tools/javadoc-i18n/apply.py --root . --tasks tools/javadoc-i18n/work/batches/<module>/batch-<NNN>.jsonl --translations tools/javadoc-i18n/work/batches/<module>/zh-<NNN>.jsonl --dry-run
必须 applied=N、skipped=0；若不符，修复 zh-<NNN>.jsonl 后重试。然后去掉 --dry-run 正式回写。

约束：只修改本批次涉及的 Java 文件和 zh-<NNN>.jsonl；不要修改 tools/ 下的脚本，
不要动 src/test/java，不要格式化代码。

报告：任务数、applied、skipped、1 个中文样例。
```

3. 全部完成后统一校验：

```powershell
python tools\javadoc-i18n\verify.py     # 期望 checked N files, 0 problems
python tools\javadoc-i18n\progress.py
```

## 规则要点

- 只译 Javadoc 正文；`@param`、`@return`、`@throws`、`@see`、`@since`、`@author` 等标签行保持英文。
- 遇到第一个块标签后，后续行都视为标签区，不翻译（`extract.py` 已这样切分）。
- `<pre>` 代码块整体跳过。
- 已含中文的片段会被自动跳过（`extract.py` 的 CJK 比例判断）。
- `{@link #getLog(Class)}` 这类行内标签必须原样保留，包括标签内的英文说明。

## 注意事项

- **不要在批次运行期间对模块目录执行 `git checkout`**：会覆盖并行批次已写回的内容。
- 每个批次只碰自己的文件，理论上可以任意并行；建议同时不超过 12-16 个。
- 回写后若 `skipped>0`，说明该块内容已不是原文（可能已译过或被改动），先人工确认再处理。
- 终端里 `git diff` 出现中文乱码是 PowerShell 控制台编码问题，文件本身是 UTF-8；用 `read`
  或 `git diff --no-color > out.txt` 查看。
- `zh-NNN.jsonl` 允许两种格式：逐行 JSONL，或单个 `{id: 译文}` JSON 对象（单任务批次常用后者）。
