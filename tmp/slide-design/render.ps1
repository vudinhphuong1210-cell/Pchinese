param([string]$InputDeck='D:\schinese\tmp\slide-design\build\candidate.pptx',[string]$OutputDir='D:\schinese\tmp\slide-design\render')
$ErrorActionPreference='Stop'
New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
$pptApp=New-Object -ComObject PowerPoint.Application
$deck=$pptApp.Presentations.Open($InputDeck,$true,$false,$false)
$issues=[System.Collections.Generic.List[object]]::new()
$texts=[System.Collections.Generic.List[object]]::new()
try {
 foreach($slide in $deck.Slides){
  $no=$slide.SlideIndex
  $slide.Export((Join-Path $OutputDir ('slide-{0:D2}.png' -f $no)),'PNG',1440,810)
  foreach($sh in $slide.Shapes){
   if($sh.HasTable -eq -1){
    for($r=1;$r -le $sh.Table.Rows.Count;$r++) {for($c=1;$c -le $sh.Table.Columns.Count;$c++) {
     $cell=$sh.Table.Cell($r,$c).Shape
     $range=$cell.TextFrame.TextRange
     $texts.Add([PSCustomObject]@{slide=$no;kind='cell';text=$range.Text})
     $available=$cell.Height-$cell.TextFrame.MarginTop-$cell.TextFrame.MarginBottom
     if($range.BoundHeight -gt $available+2){$issues.Add([PSCustomObject]@{slide=$no;kind='cell';row=$r;column=$c;needed=$range.BoundHeight;available=$available;text=$range.Text})}
    }}
   }elseif($sh.HasTextFrame -eq -1 -and $sh.TextFrame.HasText -eq -1){
    $range=$sh.TextFrame.TextRange
    $texts.Add([PSCustomObject]@{slide=$no;kind='text';text=$range.Text})
    $available=$sh.Height-$sh.TextFrame.MarginTop-$sh.TextFrame.MarginBottom
    if($range.BoundHeight -gt $available+2){$issues.Add([PSCustomObject]@{slide=$no;kind='text';name=$sh.Name;needed=$range.BoundHeight;available=$available;text=$range.Text})}
    if($sh.Top+$range.BoundHeight -gt 756 -and $sh.Top -lt 756){$issues.Add([PSCustomObject]@{slide=$no;kind='footer-collision';name=$sh.Name;top=$sh.Top;bottom=$sh.Top+$range.BoundHeight;text=$range.Text})}
   }
  }
 }
 $issues | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $OutputDir 'overflow.json') -Encoding utf8
 $texts | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $OutputDir 'texts.json') -Encoding utf8
 Write-Output ('Rendered {0} slides. Layout flags: {1}' -f $deck.Slides.Count,$issues.Count)
}finally{$deck.Close();[System.Runtime.InteropServices.Marshal]::ReleaseComObject($pptApp)|Out-Null}
