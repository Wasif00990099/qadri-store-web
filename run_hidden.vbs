Set WshShell = CreateObject("WScript.Shell")

batPath = "C:\Users\Qadri\Desktop\Qadri Store Data\start.bat"

WshShell.CurrentDirectory = "C:\Users\Qadri\Desktop\Qadri Store Data"
WshShell.Run """" & batPath & """", 0, False

Set WshShell = Nothing