import re
import sys
import os

root = sys.argv[1] if len(sys.argv) > 1 else '.'
STOP = r'\b(the|The|and|of|to|for|with|is|are|as|in|on|if|this|that|only|from|by|not|or|be|has|have|whether|Whether|when|When|return|Return|returns|Returns|set|Set|Sets|get|Get|Gets|create|Create|Creates|parse|Parse|remove|Remove|add|Add|indicate|Indicate|represents|Represents|construct|Construct|apply|Apply|encode|Encode|helps|Helps|retrieve|Retrieve|turn|Turn|sorts|Sorts|specify|Specifies|build|Build|Builds|copy|Copy|copies|Copies|variant|Variant|factory|Factory|private|Private|public|Public|shortcut|A|An|a|an|value|values|header|headers|request|response|method|methods|body|type|types|class|classes)\b'


def strip_extras(txt):
    prev = None
    while prev != txt:
        prev = txt
        txt = re.sub(r'\{@[^{}]*\}', ' ', txt)
    txt = re.sub(r'<[^>]*>', ' ', txt)
    txt = re.sub(r'"[^"]*"', ' ', txt)
    txt = re.sub(r'&#?\w+;', ' ', txt)
    return txt


def has_cjk(txt):
    return re.search(r'[\u4e00-\u9fff]', txt) is not None


for dirpath, dirnames, filenames in os.walk(root):
    for fn in sorted(filenames):
        if not fn.endswith('.java'):
            continue
        p = os.path.join(dirpath, fn)
        lines = open(p, encoding='utf-8').read().split('\n')
        inblk = False
        for i, l in enumerate(lines, 1):
            s = l.strip()
            if s.startswith('/**'):
                inblk = True
                continue
            if inblk:
                if s.startswith('*/'):
                    inblk = False
                    continue
                if s.startswith('*') and not s.startswith('* @') and s != '*':
                    txt = s[1:].strip()
                    clean = strip_extras(txt)
                    if has_cjk(txt):
                        continue
                    words = re.findall(r'[A-Za-z]{2,}', clean)
                    if len(words) >= 3 and re.search(STOP, clean):
                        print(f'{p}:{i}: {txt[:110]}')
