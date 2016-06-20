' Script: check_sophos.vbs
' Ripped from the check_av plugin by Matt White
' Version: 0.2
' changelog: added check x64 support 6-DEC-2012
' Updated code by robert.fults001@gmail.com
' Date: Nov. 11, 2011
' Details: Check that the current virus definition date for Sophos Antivirus is within acceptable bounds
' NSC.ini:
' check_sophos=cscript.exe //NoLogo scripts\check_sophos.vbs /W:$ARG1$ /c:$ARG2$

' Call with:
' ./check_nrpe -H 10.9.48.185 -c check_sophos -a 0 1

' Define Constants for the script exiting
Const intOK = 0
Const intWarning = 1
Const intCritical = 2
Const intUnknown = 3

Dim intWarnLevel, intCritLevel, intDate, intDateDifference

' Create required objects
Set ObjShell = CreateObject("WScript.Shell")
Set ObjProcess = ObjShell.Environment("Process")

' Parse Arguments to find Warning and Critical Levels
If Wscript.Arguments.Named.Exists("w") Then
  intWarnLevel = Cint(Wscript.Arguments.Named("w"))
Else
  intWarnLevel = 2
End If

If Wscript.Arguments.Named.Exists("c") Then
  intCritLevel = Cint(Wscript.Arguments.Named("c"))
Else
  intCritLevel = 4
End If

' Sophos
Dim tZ, uKey, shell, lastUp
dim dateValue
tZ = +1 'time relative to GMT
' Determine CPU architecture for correct location of the registry key
strCPUArch = objProcess("PROCESSOR_ARCHITECTURE")
If InStr(1, strCPUArch, "x86") > 0 Then
	uKey ="HKLM\Software\sophos\AutoUpdate\UpdateStatus\LastUpdateTime"
ElseIf InStr(1, strCPUArch, "64") > 0 Then
	uKey ="HKLM\SOFTWARE\Wow6432Node\Sophos\AutoUpdate\UpdateStatus\LastUpdateTime"
End If

Set shell =CreateObject("WScript.Shell")
lastUp = shell.RegRead (uKey)
dateValue = DateAdd ("h",tZ,(DateAdd("s",lastUp,"01/01/1970 00:00:00")))

if isDate(dateValue) = false then
wscript.echo "Not a valid date format: " & dateValue
wscript.quit(intUnknown)
end if

intDateDifference = DateDiff("d", dateValue, Now)

' Output current version and definition age as Performance data
Wscript.Echo("Sophos virus definitions are " & intDateDifference & " days old" & VbCrLf & "Last Updated: " & FormatDateTime(dateValue,1))

If intDateDifference > intCritLevel Then
  Wscript.Quit(intCritical)
ElseIf intDateDifference > intWarnLevel Then
  Wscript.Quit(intWarning)
ElseIf intDateDifference <= intWarnLevel Then
  Wscript.Quit(intOK)
End If
Wscript.Quit(intUnknown)
