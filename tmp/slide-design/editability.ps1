$ErrorActionPreference='Stop'
$pptApp=New-Object -ComObject PowerPoint.Application
$file='D:\schinese\output\presentations\schinese-specification-control-editable.pptx'
$copy='D:\schinese\tmp\slide-design\build\editability-check.pptx'
$deck=$pptApp.Presentations.Open($file,$true,$false,$false)
try{
 $table=($deck.Slides.Item(2).Shapes | Where-Object {$_.HasTable -eq -1} | Select-Object -First 1)
 $table.Table.Cell(2,1).Shape.TextFrame.TextRange.Text='Editable table check'
 $textShape=($deck.Slides.Item(1).Shapes | Where-Object {$_.HasTextFrame -eq -1 -and $_.TextFrame.HasText -eq -1} | Select-Object -Last 1)
 $textShape.TextFrame.TextRange.Text='Editable text check'
 $diagram=($deck.Slides.Item(18).Shapes | Where-Object {$_.Name -eq 'React SPA'} | Select-Object -First 1)
 $diagram.Left=$diagram.Left+4
 $deck.SaveCopyAs($copy,24)
}finally{$deck.Close()}
$check=$pptApp.Presentations.Open($copy,$true,$false,$false)
try{
 $table2=($check.Slides.Item(2).Shapes | Where-Object {$_.HasTable -eq -1} | Select-Object -First 1)
 if($table2.Table.Cell(2,1).Shape.TextFrame.TextRange.Text -ne 'Editable table check'){throw 'Table edit did not persist'}
 if(-not ($check.Slides.Item(1).Shapes | Where-Object {$_.HasTextFrame -eq -1 -and $_.TextFrame.HasText -eq -1 -and $_.TextFrame.TextRange.Text -eq 'Editable text check'})){throw 'Text edit did not persist'}
 Write-Output 'Native text, table and diagram edits saved and reopened in a temporary PowerPoint copy.'
}finally{$check.Close();[System.Runtime.InteropServices.Marshal]::ReleaseComObject($pptApp)|Out-Null}
