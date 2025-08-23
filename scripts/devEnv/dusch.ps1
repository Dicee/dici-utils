Get-ChildItem -Force | 
  Where-Object { $_.PSIsContainer } |
  ForEach-Object {
    $size = (Get-ChildItem $_.FullName -Recurse -Force | Measure-Object -Property Length -Sum).Sum
    [PSCustomObject]@{
      Name = $_.Name
      SizeMB = "{0:N2}" -f ($size / 1MB)
    }
  } | Sort-Object SizeMB -Descending