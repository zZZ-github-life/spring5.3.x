import re
import sys
from collections import defaultdict

# Patch format:
# ===FILE <abs path>
# @@BODY <start> <end>
# <new content lines, no leading " * ">
# @@RAW <start> <end>
# <raw replacement lines>
# @@END

def parse(path):
    files = defaultdict(list)
    cur = None
    edit = None
    with open(path, encoding='utf-8') as f:
        for raw in f.read().split('\n'):
            line = raw.rstrip('\r')
            if line.startswith('===FILE '):
                if edit is not None and cur is not None:
                    files[cur].append(edit)
                cur = line[len('===FILE '):].strip()
                edit = None
                continue
            if line.startswith('@@END'):
                if edit is not None and cur is not None:
                    files[cur].append(edit)
                edit = None
                continue
            m = re.match(r'^@@(BODY|RAW)\s+(\d+)\s+(\d+)\s*$', line)
            if m:
                if edit is not None and cur is not None:
                    files[cur].append(edit)
                edit = {'kind': m.group(1), 'start': int(m.group(2)), 'end': int(m.group(3)), 'lines': []}
                continue
            if edit is not None:
                edit['lines'].append(line)
            elif line.strip():
                raise SystemExit('Unexpected line outside edit: ' + line)
    if edit is not None and cur is not None:
        files[cur].append(edit)
    return files

def apply_file(path, edits):
    with open(path, encoding='utf-8') as f:
        lines = f.read().split('\n')
    edits = sorted(edits, key=lambda e: e['start'], reverse=True)
    prev_start = None
    for e in edits:
        s, t = e['start'], e['end']
        if s < 1 or t < s or t > len(lines):
            raise SystemExit(f'{path}: bad range {s}-{t} (file has {len(lines)} lines)')
        if prev_start is not None and t >= prev_start:
            raise SystemExit(f'{path}: overlapping edits at {s}-{t}')
        prev_start = s
        orig = lines[s-1:t]
        if e['kind'] == 'BODY':
            if not orig or not re.match(r'^\s*\*', orig[0]) or orig[0].strip().startswith('*/'):
                raise SystemExit(f'{path}:{s}: not a javadoc body line: {orig[0]!r}')
            indent = re.match(r'^(\s*)\*', orig[0]).group(1)
            if indent.endswith(' '):
                indent = indent[:-1]
            new = []
            for c in e['lines']:
                new.append(indent + ' *' if c == '' else indent + ' * ' + c)
            lines[s-1:t] = new
        else:
            lines[s-1:t] = e['lines']
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        f.write('\n'.join(lines))

def main():
    files = parse(sys.argv[1])
    total = 0
    for path, edits in files.items():
        apply_file(path, edits)
        total += len(edits)
        print(f'{path}: {len(edits)} edits')
    print(f'TOTAL {total} edits in {len(files)} files')

if __name__ == '__main__':
    main()
