import os, re, sys

ROOT = r"D:/a-project/source-code/spring-framework/spring-webflux/src/main/java/org/springframework/web/reactive"

def blocks(text):
    return list(re.finditer(r'/\*\*.*?\*/', text, re.S))

def find_region(block_text):
    lines = block_text.split('\n')
    if len(lines) == 1:
        return None
    tag_idx = None
    for i, ln in enumerate(lines):
        if i == 0:
            continue
        s = ln.strip()
        if s.startswith('* @') or s.startswith('@'):
            tag_idx = i
            break
    end = tag_idx if tag_idx is not None else len(lines) - 1
    return 1, end  # body line index range [1, end)

def apply(replacements_file):
    content = open(replacements_file, encoding='utf-8').read()
    entries = []
    cur = None
    for line in content.split('\n'):
        m = re.match(r'^@@ (.+?) :: (\d+)\s*$', line)
        if m:
            if cur:
                entries.append(cur)
            cur = {'file': m.group(1), 'idx': int(m.group(2)), 'lines': []}
        elif cur is not None:
            cur['lines'].append(line)
    if cur:
        entries.append(cur)

    changed_files = set()
    per_file = {}
    for e in entries:
        per_file.setdefault(e['file'], []).append(e)

    total = 0
    for rel, es in per_file.items():
        path = os.path.join(ROOT, rel.replace('/', os.sep))
        text = open(path, encoding='utf-8').read()
        bs = blocks(text)
        # apply in descending block order using absolute char offsets
        es.sort(key=lambda x: x['idx'], reverse=True)
        for e in es:
            i = e['idx'] - 1
            if i < 0 or i >= len(bs):
                raise SystemExit('MISSING block %d in %s' % (e['idx'], rel))
            m = bs[i]
            bt = m.group(0)
            region = find_region(bt)
            if region is None:
                raise SystemExit('SINGLE-LINE block %d in %s' % (e['idx'], rel))
            lines = bt.split('\n')
            start, end = region
            # determine indentation from the '/**' line position in the file
            line_start = text.rfind('\n', 0, m.start()) + 1
            indent = text[line_start:m.start()]
            if indent.strip():
                indent = ''
            new_body = []
            # strip only trailing empty lines from provided replacement
            repl = list(e['lines'])
            while repl and not repl[-1].strip():
                repl.pop()
            for ln in repl:
                ln = ln.rstrip()
                if not ln.strip():
                    new_body.append(indent + ' *')
                else:
                    new_body.append(indent + ' * ' + ln)
            new_block_lines = [lines[0]] + new_body + lines[end:]
            new_block = '\n'.join(new_block_lines)
            text = text[:m.start()] + new_block + text[m.end():]
            total += 1
            changed_files.add(rel)
        open(path, 'w', encoding='utf-8', newline='\n').write(text)
    print('applied %d blocks in %d files' % (total, len(changed_files)))
    for f in sorted(changed_files):
        print('  ' + f)

if __name__ == '__main__':
    apply(sys.argv[1])
