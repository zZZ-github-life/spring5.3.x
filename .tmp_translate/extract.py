import os, re, sys

ROOT = r"D:/a-project/source-code/spring-framework/spring-webflux/src/main/java/org/springframework/web/reactive"

def blocks(text):
    return list(re.finditer(r'/\*\*.*?\*/', text, re.S))

def body_region(block_text):
    """Return (body_lines, has_tags) where body_lines are the raw lines of the body."""
    lines = block_text.split('\n')
    if len(lines) == 1:
        return None, None
    # find first tag line after the opening
    tag_idx = None
    for i, ln in enumerate(lines):
        if i == 0:
            continue
        s = ln.strip()
        if s.startswith('* @') or s.startswith('@'):
            tag_idx = i
            break
    end = tag_idx if tag_idx is not None else len(lines) - 1
    return lines[1:end], tag_idx is not None

def clean_body(lines):
    out = []
    for ln in lines:
        s = ln.strip()
        if s.startswith('*'):
            s = s[1:]
            if s.startswith(' '):
                s = s[1:]
        out.append(s.rstrip())
    # strip leading/trailing empty lines
    while out and not out[0].strip():
        out.pop(0)
    while out and not out[-1].strip():
        out.pop()
    return out

def has_english(lines):
    txt = '\n'.join(lines)
    # remove inline tags content to avoid false positives
    return bool(re.search(r'[A-Za-z]{3,}', txt))

def main():
    sub = sys.argv[1] if len(sys.argv) > 1 else ''
    norec = len(sys.argv) > 2 and sys.argv[2] == '-n'
    base = os.path.join(ROOT, sub)
    files = []
    if os.path.isfile(base):
        files = [base]
    else:
        for root, dirs, fnames in os.walk(base):
            if sub == '.' or norec:
                # root package only: do not descend
                for fn in fnames:
                    if fn.endswith('.java'):
                        files.append(os.path.join(root, fn))
                break
            for fn in fnames:
                if fn.endswith('.java'):
                    files.append(os.path.join(root, fn))
    files.sort()
    for path in files:
        rel = os.path.relpath(path, ROOT).replace('\\', '/')
        text = open(path, encoding='utf-8').read()
        bs = blocks(text)
        printed_header = False
        for i, m in enumerate(bs, 1):
            body, hastags = body_region(m.group(0))
            if body is None:
                continue
            cb = clean_body(body)
            if not cb or not has_english(cb):
                continue
            if not printed_header:
                print('#### FILE ' + rel)
                printed_header = True
            print('@@ %s :: %d' % (rel, i))
            for ln in cb:
                print(ln)
            print('')

if __name__ == '__main__':
    main()
