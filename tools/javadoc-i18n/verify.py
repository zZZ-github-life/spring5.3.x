#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Sanity-check translated Java files (comments only).

Checks, for every modified .java file:
  * every Javadoc block is terminated
  * inline tags {@...} are balanced (handles nested braces)
  * no block-tag line contains CJK
  * no U+FFFD replacement characters
Usage: python verify.py
"""
import os
import re
import subprocess
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
CJK = re.compile(r'[\u4e00-\u9fff]')
TAG_LINE = re.compile(r'^[ \t]*\*[ \t]*@\w+')


def inline_errors(body, base_line):
    errs = []
    i = 0
    while True:
        k = body.find('{@', i)
        if k < 0:
            break
        depth = 0
        j = k
        closed = False
        while j < len(body):
            ch = body[j]
            if ch == '{':
                depth += 1
            elif ch == '}':
                depth -= 1
                if depth == 0:
                    closed = True
                    break
            j += 1
        if not closed:
            line = base_line + body.count('\n', 0, k)
            errs.append('unterminated inline tag at line %d: %s' % (line, body[k:k + 50].replace('\n', ' ')))
        i = k + 2
    return errs


def main():
    out = subprocess.run(['git', 'diff', '--name-only', '--', '*.java'],
                         cwd=ROOT, capture_output=True, text=True, encoding='utf-8')
    files = [f for f in out.stdout.splitlines() if f.strip()]
    errors = []
    for rel in files:
        path = os.path.join(ROOT, rel)
        if not os.path.exists(path):
            continue
        text = open(path, encoding='utf-8').read()
        head = subprocess.run(['git', 'show', 'HEAD:' + rel], cwd=ROOT,
                              capture_output=True, text=True, encoding='utf-8').stdout
        head_tags = set(l.strip() for l in head.split('\n') if TAG_LINE.match(l))
        if '\ufffd' in text:
            errors.append('%s: U+FFFD' % rel)
        pos = 0
        while True:
            start = text.find('/**', pos)
            if start < 0:
                break
            end = text.find('*/', start + 3)
            if end < 0:
                errors.append('%s: unterminated javadoc at line %d' % (rel, text.count('\n', 0, start) + 1))
                break
            body = text[start + 3:end]
            base = text.count('\n', 0, start) + 1
            for i, line in enumerate(body.split('\n')):
                if TAG_LINE.match(line) and CJK.search(line) and line.strip() not in head_tags:
                    errors.append('%s:%d: CJK in tag line: %s' % (rel, base + i, line.strip()[:80]))
            errors.extend('%s: %s' % (rel, e) for e in inline_errors(body, base))
            pos = end + 2
    print('checked %d modified java files, %d problems' % (len(files), len(errors)))
    for e in errors[:50]:
        print(' -', e)


if __name__ == '__main__':
    main()
