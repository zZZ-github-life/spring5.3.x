#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""List pending batches (no zh file, or zh file missing ids)."""
import json
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
WORK = os.path.join(ROOT, 'tools', 'javadoc-i18n', 'work', 'batches')
PRIORITY = ['spring-core', 'spring-beans', 'spring-context', 'spring-aop',
            'spring-expression', 'spring-jcl', 'spring-web', 'spring-webmvc',
            'spring-webflux', 'spring-tx', 'spring-jdbc', 'spring-test',
            'spring-messaging', 'spring-websocket', 'spring-jms', 'spring-orm',
            'spring-context-support', 'spring-r2dbc', 'spring-oxm',
            'spring-aspects', 'spring-context-indexer', 'spring-instrument']


def ids_of(path):
    out = set()
    with open(path, encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            try:
                out.add(json.loads(line)['id'])
            except Exception:
                return None
    return out


def main():
    limit = int(sys.argv[1]) if len(sys.argv) > 1 else 10
    manifest = json.load(open(os.path.join(WORK, 'manifest.json'), encoding='utf-8'))
    pending = []
    for m in manifest:
        batch = os.path.join(WORK, m['file'])
        zh = os.path.join(os.path.dirname(batch), os.path.basename(batch).replace('batch-', 'zh-'))
        need = ids_of(batch) or set()
        have = ids_of(zh) if os.path.exists(zh) else set()
        missing = need - (have or set())
        if missing:
            pending.append((m, len(missing)))
    pending.sort(key=lambda x: (PRIORITY.index(x[0]['module']) if x[0]['module'] in PRIORITY else 99,
                                x[0]['file']))
    print('pending batches: %d, chunks: %d' % (len(pending), sum(x[1] for x in pending)))
    for m, n in pending[:limit]:
        print('%-24s %-34s %4d chunks left' % (m['module'], m['file'], n))


if __name__ == '__main__':
    main()
