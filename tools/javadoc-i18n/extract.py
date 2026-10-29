#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Extract translatable Javadoc prose chunks from Java sources.

Output: JSONL, one task per contiguous prose chunk (lines between tag lines,
blank lines, and <pre> blocks).

Usage:
    python extract.py --root . --out work/tasks.jsonl
    python extract.py --root . --modules spring-core spring-beans --out work/tasks.jsonl
"""
import argparse
import json
import os
import re
import sys

JAVADOC_RE = re.compile(r'(?m)^([ \t]*)/\*\*(.*?)\*/', re.S)
TAG_RE = re.compile(r'^@\w+')
CJK_RE = re.compile(r'[\u4e00-\u9fff]')
LETTER_RE = re.compile(r'[A-Za-z]')
# Block-level html / javadoc constructs that must stay verbatim.
RAW_START_RE = re.compile(r'<(pre|table|ul|ol|dl)\b', re.I)
RAW_END_RE = re.compile(r'</(pre|table|ul|ol|dl)\s*>', re.I)


def discover_modules(root):
    mods = []
    for name in sorted(os.listdir(root)):
        p = os.path.join(root, name)
        if not os.path.isdir(p):
            continue
        if os.path.isdir(os.path.join(p, 'src', 'main', 'java')):
            mods.append(name)
    if os.path.isdir(os.path.join(root, 'src', 'main', 'java')):
        mods.append('.')
    return mods


def iter_java_files(root, modules):
    for mod in modules:
        base = os.path.join(root, '' if mod == '.' else mod, 'src', 'main', 'java')
        for dirpath, dirnames, filenames in os.walk(base):
            dirnames[:] = [d for d in dirnames if d not in ('build', '.gradle', 'node_modules')]
            for fn in sorted(filenames):
                if fn.endswith('.java'):
                    yield mod, os.path.join(dirpath, fn)


def strip_prefix(line):
    """Return (indent, content) where content has the leading '*' removed."""
    m = re.match(r'^([ \t]*)\*( ?)(.*)$', line)
    if not m:
        return None
    return m.group(1), m.group(3)


def is_chinese_enough(text):
    letters = len(LETTER_RE.findall(text))
    if letters == 0:
        return True
    cjk = len(CJK_RE.findall(text))
    return cjk >= 4 and cjk / (cjk + letters) > 0.10


def extract_file(root, mod, path, rel, skip_chinese):
    with open(path, 'r', encoding='utf-8') as f:
        text = f.read()
    if '\ufffd' in text:
        sys.stderr.write('WARN: replacement chars in %s\n' % rel)
    lines = text.split('\n')
    tasks = []
    for m in JAVADOC_RE.finditer(text):
        indent = m.group(1)
        block = m.group(0)
        start_line = text.count('\n', 0, m.start()) + 1
        body = m.group(2)
        body_lines = body.split('\n')
        # body_lines[0] is usually empty (same line as /**)
        # Collect line contents with their raw text.
        entries = []
        in_pre = False
        in_tags = False
        for i, raw in enumerate(body_lines[1:], start=1):
            parsed = strip_prefix(raw)
            if parsed is None:
                # closing line of block handled separately; skip
                continue
            ind, content = parsed
            stripped = content.strip()
            if stripped.startswith('@'):
                # First block tag: everything after it belongs to the tag
                # section (@param descriptions may span several lines).
                in_tags = True
            if in_tags:
                entries.append(('raw', content))
                continue
            if RAW_START_RE.search(stripped):
                in_pre = True
            if in_pre:
                entries.append(('raw', content))
                if RAW_END_RE.search(stripped):
                    in_pre = False
                continue
            if stripped == '' or TAG_RE.match(stripped):
                entries.append(('raw', content))
                continue
            entries.append(('text', content))

        # group consecutive text entries
        i = 0
        chunk_idx = 0
        while i < len(entries):
            if entries[i][0] != 'text':
                i += 1
                continue
            j = i
            while j < len(entries) and entries[j][0] == 'text':
                j += 1
            chunk = [entries[k][1] for k in range(i, j)]
            joined = ' '.join(x.strip() for x in chunk if x.strip())
            if joined and not (skip_chinese and is_chinese_enough(joined)):
                first_line = start_line + i + 1  # entries index -> source line offset
                tasks.append({
                    'id': '%s::%d::%d' % (rel, start_line, chunk_idx),
                    'file': rel,
                    'module': mod,
                    'block_start': start_line,
                    'start': first_line,
                    'end': first_line + len(chunk) - 1,
                    'indent': indent,
                    'lines': chunk,
                })
            chunk_idx += 1
            i = j
    return tasks


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--root', default='.')
    ap.add_argument('--modules', nargs='*')
    ap.add_argument('--out', required=True)
    ap.add_argument('--keep-chinese', action='store_true', help='also queue chunks already containing CJK')
    args = ap.parse_args()

    root = os.path.abspath(args.root)
    modules = args.modules or discover_modules(root)
    os.makedirs(os.path.dirname(os.path.abspath(args.out)), exist_ok=True)

    total = 0
    per_mod = {}
    with open(args.out, 'w', encoding='utf-8', newline='\n') as out:
        for mod, path in iter_java_files(root, modules):
            rel = os.path.relpath(path, root).replace(os.sep, '/')
            tasks = extract_file(root, mod, path, rel, not args.keep_chinese)
            for t in tasks:
                out.write(json.dumps(t, ensure_ascii=False) + '\n')
            if tasks:
                per_mod[mod] = per_mod.get(mod, 0) + len(tasks)
            total += len(tasks)

    for mod in sorted(per_mod):
        print('%-24s %6d chunks' % (mod, per_mod[mod]))
    print('TOTAL %d chunks -> %s' % (total, args.out))


if __name__ == '__main__':
    main()
