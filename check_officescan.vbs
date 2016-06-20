' Script: check_officescan.vbs
' Ripped from the check_av plugin by Matt White
' Version: 0.1
' Updated code by robert.fults001@gmail.com
' Date: Nov. 8, 2011
' Updated 5/1/2012 for 64-bit version
' Details: Check that the current virus pattern date for Trend Micro Officescan is within acceptable bounds
' NSC.ini:
' check_officescan=cscript.exe //NoLogo scripts\check_officescan.vbs /W:$ARG1$ /c:$ARG2$

' Call with:
' ./check_nrpe -H 10.9.48.185 -c check_officescan -a 0 1

' Define Constants for the script exiting
Const intOK = 0
Const intWarning = 1
Const intCritical = 2
Const intUnknown = 3

' Create required objects
Set ObjShell = CreateObject("WScript.Shell")
Set ObjProcess = ObjShell.Environment("Process")

const HKEY_CURRENT_USER = &H80000001
const HKEY_LOCAL_MACHINE = &H80000002

Dim strKeyPath
Dim intWarnLevel, intCritLevel, intDate, intDateDifference
Dim strValue


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

' Determine CPU architecture for correct location of the registry key
strCPUArch = objProcess("PROCESSOR_ARCHITECTURE")
If InStr(1, strCPUArch, "x86") > 0 Then
  strKeyPath = "SOFTWARE\TrendMicro\PC-cillinNTCorp\CurrentVersion\Misc."
ElseIf InStr(1, strCPUArch, "64") > 0 Then
  strKeyPath = "SOFTWARE\wow6432node\TrendMicro\PC-cillinNTCorp\CurrentVersion\Misc."
End If

' Query Registry using WMI to obtain the definition value
Set oReg=GetObject("winmgmts:{impersonationLevel=impersonate}!\\.\root\default:StdRegProv")
oReg.GetStringValue HKEY_LOCAL_MACHINE,strKeyPath,"PatternDate",strValue

' Generate output from the registry value
dim dateValue
'strValue = 20111102
intDate = strValue

dateValue = PatternMon_ToDate(intDate)

if isDate(dateValue) = false then
wscript.echo "Not a valid date format: " & dateValue
wscript.quit(intUnknown)
end if

intDateDifference = DateDiff("d", dateValue, Now)

' Output current version and definition age as Performance data
Wscript.Echo("Officescan virus definitions are " & intDateDifference & " days old" & VbCrLf & "Last Updated: " & FormatDateTime(dateValue,1))

If intDateDifference > intCritLevel Then
  Wscript.Quit(intCritical)
ElseIf intDateDifference > intWarnLevel Then
  Wscript.Quit(intWarning)
ElseIf intDateDifference <= intWarnLevel Then
  Wscript.Quit(intOK)
End If
Wscript.Quit(intUnknown)


' Converts YYYYMMDDHHmmss dates to standard date format
Function PatternMon_ToDate(osceDate)
PatternMon_ToDate = Left(osceDate, 4) & "/" & Mid(osceDate, 5, 2) _
& "/" & Mid(osceDate, 7, 2) & " " & Mid(osceDate, 9, 2)
End Function