#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Split extracted tasks into file-disjoint batches for parallel translation.

Usage: python split.py --tasks work/tasks-all.jsonl --out work/batches --max-files 20 --max-chunks 400
"""
import argparse
import json
import os
from collections import OrderedDict


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--tasks', required=True)
    ap.add_argument('--out', required=True)
    ap.add_argument('--max-files', type=int, default=20)
    ap.add_argument('--max-chunks', type=int, default=400)
    args = ap.parse_args()

    per_file = OrderedDict()
    with open(args.tasks, encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            t = json.loads(line)
            per_file.setdefault(t['file'], []).append(t)

    os.makedirs(args.out, exist_ok=True)
    manifest = []
    batch = []
    files_in_batch = 0
    idx_by_mod = {}

    def flush():
        nonlocal batch, files_in_batch
        if not batch:
            return
        mod = batch[0]['module']
        n = idx_by_mod.get(mod, 0)
        idx_by_mod[mod] = n + 1
        d = os.path.join(args.out, mod)
        os.makedirs(d, exist_ok=True)
        p = os.path.join(d, 'batch-%03d.jsonl' % n)
        with open(p, 'w', encoding='utf-8', newline='\n') as o:
            for t in batch:
                o.write(json.dumps(t, ensure_ascii=False) + '\n')
        manifest.append({'file': os.path.relpath(p, args.out).replace(os.sep, '/'),
                         'module': mod, 'files': files_in_batch, 'chunks': len(batch)})
        batch = []
        files_in_batch = 0

    for rel, tasks in per_file.items():
        mod = tasks[0]['module']
        if batch and batch[0]['module'] != mod:
            flush()
        if len(batch) + len(tasks) > args.max_chunks and batch:
            flush()
        if files_in_batch >= args.max_files:
            flush()
        batch.extend(tasks)
        files_in_batch += 1
    flush()

    with open(os.path.join(args.out, 'manifest.json'), 'w', encoding='utf-8') as f:
        json.dump(manifest, f, ensure_ascii=False, indent=1)
    print('batches: %d, files: %d, chunks: %d' % (
        len(manifest), sum(m['files'] for m in manifest), sum(m['chunks'] for m in manifest)))
    for m in manifest:
        print('%-24s %-28s files=%3d chunks=%4d' % (m['module'], m['file'], m['files'], m['chunks']))


if __name__ == '__main__':
    main()
