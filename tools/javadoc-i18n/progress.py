#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Report translation progress by re-extracting remaining chunks."""
import json
import os
import subprocess
import sys
import tempfile

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
TOOL = os.path.join(ROOT, 'tools', 'javadoc-i18n', 'extract.py')
BASELINE = os.path.join(ROOT, 'tools', 'javadoc-i18n', 'work', 'baseline.json')


def count(path):
    n = 0
    with open(path, encoding='utf-8') as f:
        for line in f:
            if line.strip():
                n += 1
    return n


def main():
    with open(BASELINE, encoding='utf-8') as f:
        total = json.load(f)['total']
    tmp = tempfile.NamedTemporaryFile(delete=False, suffix='.jsonl')
    tmp.close()
    subprocess.run([sys.executable, TOOL, '--root', ROOT, '--out', tmp.name],
                   check=True, stdout=subprocess.DEVNULL)
    remaining = count(tmp.name)
    os.unlink(tmp.name)
    done = total - remaining
    pct = 100.0 * done / total if total else 100.0
    print('translated %d / %d chunks (%.1f%%), remaining %d' % (done, total, pct, remaining))


if __name__ == '__main__':
    main()
