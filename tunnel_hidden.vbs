' ============================================
' CLOUDFLARE TUNNEL - QADRI STORE LAUNCHER
' ============================================

Option Explicit

Dim WshShell, objFSO, strLogFile

' Configuration
Const SCRIPT_PATH = "D:\Excel\QadriStore.py"  ' ✅ CORRECT FILENAME!
Const LOG_FILE = "D:\Excel\hidden_launcher.log"

' Create objects
Set WshShell = CreateObject("WScript.Shell")
Set objFSO = CreateObject("Scripting.FileSystemObject")

' Log function
Sub LogMessage(msg)
    On Error Resume Next
    Dim f, timestamp
    timestamp = Now()
    Set f = objFSO.OpenTextFile(LOG_FILE, 8, True)
    f.WriteLine "[" & timestamp & "] " & msg
    f.Close
    Set f = Nothing
End Sub

' Main
On Error Resume Next

LogMessage "============================================"
LogMessage "LAUNCHER STARTED"
LogMessage "Looking for: " & SCRIPT_PATH

' Check if script exists
If Not objFSO.FileExists(SCRIPT_PATH) Then
    LogMessage "❌ ERROR: Script not found!"
    WScript.Quit 1
End If

LogMessage "✅ Script found!"

' Run hidden
Dim result
result = WshShell.Run("cmd /c ""cd /d D:\Excel && python QadriStore.py""", 0, True)

If result = 0 Then
    LogMessage "✅ SUCCESS: Tunnel completed!"
Else
    LogMessage "⚠️ Exit code: " & result
End If

LogMessage "LAUNCHER ENDED"
LogMessage "============================================"

Set WshShell = Nothing
Set objFSO = Nothing
WScript.Quit 0