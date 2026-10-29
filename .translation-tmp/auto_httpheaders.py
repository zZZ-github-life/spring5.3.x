import re

path = r'D:/a-project/source-code/spring-framework/spring-web/src/main/java/org/springframework/http/HttpHeaders.java'
lines = open(path, encoding='utf-8').read().split('\n')
counts = {}

rules = [
    (r'^(?P<ind>\s*\*) The HTTP (\{@code [^}]+\}) header field name\.$',
     r'\g<ind> HTTP \2 头字段名称。'),
    (r'^(?P<ind>\s*\*) The CORS (\{@code [^}]+\}) response header field name\.$',
     r'\g<ind> CORS \2 响应头字段名称。'),
    (r'^(?P<ind>\s*\*) The CORS (\{@code [^}]+\}) request header field name\.$',
     r'\g<ind> CORS \2 请求头字段名称。'),
    (r'^(?P<ind>\s*\*) Set the \(new\) value of the (\{@code [^}]+\}) response header\.$',
     r'\g<ind> 设置 \2 响应头的（新）值。'),
    (r'^(?P<ind>\s*\*) Return the value of the (\{@code [^}]+\}) response header\.$',
     r'\g<ind> 返回 \2 响应头的值。'),
    (r'^(?P<ind>\s*\*) Set the \(new\) value of the (\{@code [^}]+\}) request header\.$',
     r'\g<ind> 设置 \2 请求头的（新）值。'),
    (r'^(?P<ind>\s*\*) Return the value of the (\{@code [^}]+\}) request header\.$',
     r'\g<ind> 返回 \2 请求头的值。'),
    (r'^(?P<ind>\s*\*) Set the \(new\) values? of the (\{@code [^}]+\}) header\.$',
     r'\g<ind> 设置 \2 头的（新）值。'),
    (r'^(?P<ind>\s*\*) Set the \(new\) value of the (\{@code [^}]+\}) header\.$',
     r'\g<ind> 设置 \2 头的（新）值。'),
    (r'^(?P<ind>\s*\*) Return the value of the (\{@code [^}]+\}) header\.$',
     r'\g<ind> 返回 \2 头的值。'),
    (r'^(?P<ind>\s*\*) Set the value of the (\{@code [^}]+\}) header\.$',
     r'\g<ind> 设置 \2 头的值。'),
]

for i, line in enumerate(lines):
    for rx, repl in rules:
        m = re.match(rx, line)
        if m:
            lines[i] = m.expand(repl)
            key = rx[:55]
            counts[key] = counts.get(key, 0) + 1
            break

open(path, 'w', encoding='utf-8', newline='\n').write('\n'.join(lines))
for k, v in counts.items():
    print(v, k)
print('total', sum(counts.values()))
