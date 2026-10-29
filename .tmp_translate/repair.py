import os, re, sys

ROOT = r"D:/a-project/source-code/spring-framework/spring-webflux/src/main/java/org/springframework/web/reactive"
APPLY = len(sys.argv) > 1 and sys.argv[1] == '--apply'

changed_files = 0
changed_lines = 0
removed_markers = 0

for root, dirs, fnames in os.walk(ROOT):
    for fn in fnames:
        if not fn.endswith('.java'):
            continue
        path = os.path.join(root, fn)
        text = open(path, encoding='utf-8').read()
        matches = list(re.finditer(r'/\*\*.*?\*/', text, re.S))
        new_text = text
        file_changed = False
        for m in reversed(matches):
            bt = m.group(0)
            lines = bt.split('\n')
            if len(lines) == 1:
                continue
            line_start = new_text.rfind('\n', 0, m.start()) + 1
            indent = new_text[line_start:m.start()]
            if indent.strip():
                continue
            out = [lines[0]]
            local_changed = False
            for ln in lines[1:]:
                s = ln.lstrip()
                if s.startswith('*') and s[1:].strip().startswith('#### FILE'):
                    removed_markers += 1
                    local_changed = True
                    if len(out) > 1 and re.match(r'^\s*\*$', out[-1]):
                        out.pop()
                        changed_lines += 1
                    continue
                if not s.startswith('*'):
                    out.append(ln)
                    continue
                desired = indent + ' ' + s
                if ln != desired:
                    out.append(desired)
                    local_changed = True
                    changed_lines += 1
                else:
                    out.append(ln)
            if local_changed:
                nb = '\n'.join(out)
                new_text = new_text[:m.start()] + nb + new_text[m.end():]
                file_changed = True
        if file_changed:
            changed_files += 1
            if APPLY:
                open(path, 'w', encoding='utf-8', newline='\n').write(new_text)

print(('APPLIED' if APPLY else 'DRY-RUN') + ': files=%d lines=%d markers=%d' % (changed_files, changed_lines, removed_markers))
