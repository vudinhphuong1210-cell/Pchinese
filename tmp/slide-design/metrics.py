import json,pathlib
from PIL import ImageFont
root=pathlib.Path(r'D:\schinese\tmp\slide-design')
chars=set((root/'slide-design-content.txt').read_text(encoding='utf-8'))|set(chr(i) for i in range(32,128))|set('•–—“”→')
result={}
for name,file in [('regular','calibri.ttf'),('bold','calibrib.ttf')]:
 f=ImageFont.truetype('C:/Windows/Fonts/'+file,100)
 result[name]={c:f.getlength(c) for c in chars}
(root/'font-metrics.json').write_text(json.dumps(result,ensure_ascii=False),encoding='utf-8')
