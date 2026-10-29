import os
import re
import sys

CJK = re.compile(r'[\u4e00-\u9fff]')
WORD = re.compile(r'[A-Za-z]{2,}')


def translatable(content):
    if not content:
        return False
    if CJK.search(content):
        return False
    has_inline = '{@' in content
    s = re.sub(r'\{@[^}]*\}', ' ', content)
    s = re.sub(r'<[^>]+>', ' ', s)
    s = re.sub(r'&[a-z]+;', ' ', s)
    s = re.sub(r'https?://\S+', ' ', s)
    words = WORD.findall(s)
    return len(words) >= 1 or has_inline


def process(path):
    with open(path, encoding='utf-8') as f:
        lines = f.read().split('\n')
    out = []
    i = 0
    n = len(lines)
    while i < n:
        line = lines[i]
        if '/**' not in line:
            i += 1
            continue
        start = i
        # find end of javadoc
        j = i
        while j < n and '*/' not in lines[j]:
            j += 1
        if j >= n:
            break
        end = j
        # determine body line range (between /** line and first block tag)
        body_start = start + 1 if lines[start].strip() == '/**' else None
        k = start
        first_tag = None
        while k <= end:
            txt = lines[k].strip()
            if k > start and txt.startswith('*'):
                content = txt[1:].strip()
                if content.startswith('@') and not content.startswith('{@'):
                    first_tag = k
                    break
            k += 1
        stop = first_tag if first_tag is not None else end
        # collect body content lines (excluding /** and */ lines)
        seg = []
        in_pre = False
        for idx in range(start + 1, stop):
            raw = lines[idx]
            m = re.match(r'^(\s*)\*(.*)$', raw)
            if not m:
                if seg:
                    out.append((seg[0], seg[-1]))
                    seg = []
                continue
            content = m.group(2)
            if content.startswith(' '):
                content = content[1:]
            low = content.lower()
            if '<pre' in low:
                in_pre = True
            if in_pre:
                if '</pre>' in low:
                    in_pre = False
                if seg:
                    out.append((seg[0], seg[-1]))
                    seg = []
                continue
            if '</pre>' in low:
                if seg:
                    out.append((seg[0], seg[-1]))
                    seg = []
                continue
            if translatable(content):
                if not seg:
                    seg = [idx, idx]
                else:
                    seg[1] = idx
            else:
                if seg:
                    out.append((seg[0], seg[-1]))
                    seg = []
        if seg:
            out.append((seg[0], seg[-1]))
        i = end + 1
    if out:
        print('===FILE ' + path)
        for s, e in out:
            print(f'@@BODY {s+1} {e+1}')
            for idx in range(s, e + 1):
                m = re.match(r'^(\s*)\*(.*)$', lines[idx])
                content = m.group(2)
                if content.startswith(' '):
                    content = content[1:]
                print(content)


def main():
    for arg in sys.argv[1:]:
        if os.path.isdir(arg):
            for root, dirs, files in os.walk(arg):
                dirs.sort()
                for name in sorted(files):
                    if name.endswith('.java'):
                        process(os.path.join(root, name))
        else:
            process(arg)


if __name__ == '__main__':
    main()
