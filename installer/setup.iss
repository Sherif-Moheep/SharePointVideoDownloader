; Inno Setup Script for SharePoint Video Downloader
; Note: Paths below are relative to this script file (located in /installer).

#define MyAppName "SharePoint Video Downloader"
#define MyAppVersion "1.0"
#define MyAppPublisher "Sherif Moheep"
#define MyAppURL "https://github.com/Sherif-Moheep/SharePointVideoDownloader"
#define MyAppExeName "SharePoint Video Downloader.exe"

[Setup]
AppId={{C9B54F22-4A73-478F-B2E9-B920CDC9092D}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
AppSupportURL={#MyAppURL}
AppUpdatesURL={#MyAppURL}

; Installs to LocalAppData so it doesn't require Admin privileges
DefaultDirName={localappdata}\SharePointDownloader
UninstallDisplayIcon={app}\{#MyAppExeName}

ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
DisableProgramGroupPage=yes

; Runs silently without Admin prompts
PrivilegesRequired=lowest

OutputDir=Output
OutputBaseFilename=SharePointVideoDownloader_Setup
SetupIconFile=..\desktop-app\composeApp\src\jvmMain\resources\app-icon.ico
SolidCompression=yes
WizardStyle=modern

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

[Files]
Source: "..\desktop-app\composeApp\build\compose\binaries\main\app\SharePoint Video Downloader\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{autoprograms}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"
Name: "{autodesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; Tasks: desktopicon

[Run]
; Auto-launch the app after installation
Filename: "{app}\{#MyAppExeName}"; Description: "{cm:LaunchProgram,{#StringChange(MyAppName, '&', '&&')}}"; Flags: nowait postinstall skipifsilent
