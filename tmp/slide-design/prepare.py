import json,re,pathlib,collections
from PIL import Image,ImageOps,ImageDraw
import pdfplumber
root=pathlib.Path(r'D:\schinese\tmp\slide-design')
text=pathlib.Path(r'D:\schinese\output\presentations\kiem-soat-spec-noi-dung-slide.md').read_text(encoding='utf-8')
parts=re.split(r'^## Slide ([0-9A]+) — (.+)$',text,flags=re.M)
slides=[]
for i in range(1,len(parts),3):
    n,subject,body=parts[i:i+3]
    sections={}
    for key in 'ABCD':
        m=re.search(r'^### '+key+r'\..*?\n(.*?)(?=^### |^# |\Z)',body,flags=re.S|re.M)
        sections[key]=m.group(1).strip() if m else ''
    slides.append({'number':n,'subject':subject,**sections})
(root/'slides.json').write_text(json.dumps(slides,ensure_ascii=False,indent=2),encoding='utf-8')
(root/'slide-design-content.txt').write_text('\n\n'.join('SLIDE '+s['number']+'\n'+s['A']+'\nLAYOUT: '+s['B'] for s in slides),encoding='utf-8')
imgs=sorted((root/'reference').glob('page-*.png'))
for batch in range(0,len(imgs),8):
    canvas=Image.new('RGB',(1920,4*565),'#dddddd')
    for j,p in enumerate(imgs[batch:batch+8]):
        im=Image.open(p).convert('RGB');canvas.paste(im,((j%2)*960,(j//2)*565+25))
        ImageDraw.Draw(canvas).text(((j%2)*960+12,(j//2)*565+5),p.stem,fill='#000000')
    canvas.save(root/'reference'/f'montage-{batch//8+1}.jpg')
with pdfplumber.open(r'C:\Users\Admin\Downloads\10398_APHL_SET_490_G3_APHL_Presentation_Slides_bfb658f822-1.pdf') as pdf:
    for i in [0,1,10,19,27]:
        p=pdf.pages[i]
        print('PAGE',i+1,'FONTS',collections.Counter((c['fontname'],round(c['size'],1)) for c in p.chars).most_common(12))
        print('COLORS',collections.Counter(str(c['non_stroking_color']) for c in p.chars).most_common(8))
print('PARSED',len(slides))
