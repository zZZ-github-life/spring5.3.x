#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Apply Chinese translations back into Java sources.

Input:
  --tasks        JSONL produced by extract.py
  --translations JSONL or JSON object: {"id": "..."} -> string or [lines]
                 (element type can be string or {"zh": ...})

The applier verifies the original lines still match, then rewrites the chunk
with the same indentation and a " * " prefix. Paragraphs are re-wrapped to
--wrap display columns (CJK counts as 2 columns).

Usage:
    python apply.py --tasks work/tasks.jsonl --translations work/zh.jsonl
    python apply.py ... --dry-run
"""
import argparse
import json
import os
import re
import sys
import unicodedata

PREFIX_RE = re.compile(r'^([ \t]*)\* ?(.*)$')
INLINE_RE = re.compile(r'\{@[^{}]*\}|<[^<>]+>|https?://\S+')


def display_width(s):
    w = 0
    for ch in s:
        w += 2 if unicodedata.east_asian_width(ch) in ('W', 'F') else 1
    return w


def atoms(text):
    """Split into atomic units: inline javadoc/html tags, CJK chars, words, spaces."""
    out = []
    i = 0
    while i < len(text):
        m = INLINE_RE.match(text, i)
        if m:
            out.append(m.group(0))
            i = m.end()
            continue
        ch = text[i]
        if ch.isspace():
            j = i
            while j < len(text) and text[j].isspace():
                j += 1
            out.append(' ')
            i = j
            continue
        if unicodedata.east_asian_width(ch) in ('W', 'F'):
            out.append(ch)
            i += 1
            continue
        j = i
        while j < len(text) and not text[j].isspace() and unicodedata.east_asian_width(text[j]) not in ('W', 'F'):
            if INLINE_RE.match(text, j):
                break
            j += 1
        out.append(text[i:j])
        i = j
    return out


def wrap(text, width):
    lines = []
    cur = ''
    cur_w = 0
    for a in atoms(text):
        if a == ' ':
            if cur and not cur.endswith(' '):
                cur += ' '
                cur_w += 1
            continue
        aw = display_width(a)
        if cur_w + aw > width and cur.strip():
            lines.append(cur.rstrip())
            cur = a
            cur_w = aw
        else:
            cur += a
            cur_w += aw
    if cur.strip():
        lines.append(cur.rstrip())
    lines = lines or ['']
    # Avoid lines that start with closing punctuation or end with opening
    # punctuation, which looks broken in Chinese.
    closing = '。，、；：！？）》」』】〕｝”’%…'
    opening = '（《「『【〔｛“‘'
    for i in range(1, len(lines)):
        while lines[i] and lines[i][0] in closing:
            lines[i - 1] += lines[i][0]
            lines[i] = lines[i][1:]
        if lines[i - 1] and lines[i - 1][-1] in opening:
            ch = lines[i - 1][-1]
            lines[i - 1] = lines[i - 1][:-1]
            lines[i] = ch + lines[i]
    # Moving punctuation can leave an empty line behind; drop those.
    lines = [l for l in lines if l.strip()]
    return lines or ['']


def load_tasks(path):
    tasks = {}
    with open(path, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if line:
                t = json.loads(line)
                tasks[t['id']] = t
    return tasks


def load_translations(path):
    tr = {}
    with open(path, 'r', encoding='utf-8') as f:
        raw = f.read()
    stripped = raw.lstrip()
    if stripped.startswith('{'):
        # A single JSON object holding all translations. A JSONL file also
        # starts with '{', so fall back to line-by-line parsing in that case.
        try:
            obj = json.loads(raw)
        except json.JSONDecodeError:
            obj = None
        if isinstance(obj, dict):
            for k, v in obj.items():
                tr[k] = v
            return tr
    for line in raw.splitlines():
        line = line.strip()
        if not line:
            continue
        obj = json.loads(line)
        tr[obj['id']] = obj.get('zh', '')
    return tr


def as_paragraphs(value):
    if isinstance(value, str):
        return value.split('\n')
    out = []
    for v in value:
        out.extend(str(v).split('\n'))
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--root', default='.')
    ap.add_argument('--tasks', required=True)
    ap.add_argument('--translations', required=True)
    ap.add_argument('--dry-run', action='store_true')
    ap.add_argument('--wrap', type=int, default=96)
    args = ap.parse_args()

    root = os.path.abspath(args.root)
    tasks = load_tasks(args.tasks)
    translations = load_translations(args.translations)

    by_file = {}
    unknown = 0
    for tid, value in translations.items():
        t = tasks.get(tid)
        if t is None:
            sys.stderr.write('WARN: unknown task id %s\n' % tid)
            unknown += 1
            continue
        by_file.setdefault(t['file'], []).append((t, value))

    applied = 0
    skipped = 0
    for rel, items in by_file.items():
        path = os.path.join(root, rel)
        with open(path, 'r', encoding='utf-8') as f:
            lines = f.read().split('\n')
        items.sort(key=lambda x: x[0]['start'], reverse=True)
        changed = False
        for t, value in items:
            paragraphs = as_paragraphs(value)
            s, e = t['start'], t['end']
            current = lines[s - 1:e]
            orig = [x.rstrip('\r') for x in t['lines']]
            if len(current) != len(orig):
                sys.stderr.write('SKIP (line count) %s\n' % t['id'])
                skipped += 1
                continue
            ok = True
            for cur, exp in zip(current, orig):
                m = PREFIX_RE.match(cur)
                if not m or m.group(2).rstrip() != exp.rstrip():
                    ok = False
                    break
            if not ok:
                sys.stderr.write('SKIP (content mismatch) %s\n' % t['id'])
                skipped += 1
                continue
            indent = t['indent']
            new_lines = []
            for p in paragraphs:
                if not p.strip():
                    new_lines.append(indent + ' *')
                    continue
                for z in wrap(p.strip(), args.wrap):
                    if z.lstrip().startswith('@'):
                        z = '&#64;' + z.lstrip()[1:]
                    new_lines.append(indent + ' * ' + z)
            lines[s - 1:e] = new_lines
            applied += 1
            changed = True
        if changed and not args.dry_run:
            with open(path, 'w', encoding='utf-8', newline='\n') as f:
                f.write('\n'.join(lines))

    print('applied %d chunks, skipped %d, unknown ids %d' % (applied, skipped, unknown))


if __name__ == '__main__':
    main()
