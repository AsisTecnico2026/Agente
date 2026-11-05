#define NombreApp "SFMantto Agente"
#define Version "1.0"

[Setup]
; No cambies este identificador: así una versión nueva reemplaza a la anterior
AppId={{B6E9D1B4-3D0E-4C77-9F35-5C9A3B1E7A21}
AppName={#NombreApp}
AppVersion={#Version}
AppPublisher=SFMantto
DefaultDirName={autopf}\SFMantto
DisableDirPage=yes
DisableProgramGroupPage=yes
PrivilegesRequired=admin
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
OutputDir=salida
OutputBaseFilename=SFMantto-Agente-{#Version}-setup
Compression=lzma2
SolidCompression=yes
WizardStyle=modern
UninstallDisplayName={#NombreApp}

[Languages]
Name: "es"; MessagesFile: "compiler:Languages\Spanish.isl"

[Files]
Source: "payload\sfmantto-agente.jar"; DestDir: "{app}"; Flags: ignoreversion
Source: "payload\sfmantto-agente.exe"; DestDir: "{app}"; Flags: ignoreversion
Source: "payload\sfmantto-agente.xml"; DestDir: "{app}"; Flags: ignoreversion
Source: "payload\ca.crt"; DestDir: "{app}"; Flags: ignoreversion
Source: "payload\runtime\*"; DestDir: "{app}\runtime"; Flags: ignoreversion recursesubdirs createallsubdirs

[UninstallRun]
Filename: "{app}\sfmantto-agente.exe"; Parameters: "stop"; Flags: runhidden waituntilterminated skipifdoesntexist; RunOnceId: "DetenerServicio"
Filename: "{app}\sfmantto-agente.exe"; Parameters: "uninstall"; Flags: runhidden waituntilterminated skipifdoesntexist; RunOnceId: "QuitarServicio"

[Code]
var
  PaginaApi: TInputQueryWizardPage;

function UrlValida(const Url: String): Boolean;
begin
  Result := (Pos('https://', Lowercase(Trim(Url))) = 1) and (Length(Trim(Url)) > 10);
end;

function ClaveValida(const Clave: String): Boolean;
begin
  Result := Length(Trim(Clave)) >= 10;
end;

procedure InitializeWizard;
var
  Url: String;
begin
  PaginaApi := CreateInputQueryPage(wpWelcome,
    'Conexión con el servidor',
    'Datos para conectar este equipo',
    'Escribe la dirección de la API y la clave de activación que te dio el administrador.');
  PaginaApi.Add('Dirección de la API (debe empezar con https://):', False);
  PaginaApi.Add('Clave de activación:', False);

  // En instalaciones silenciosas los datos llegan como /API=... y /CLAVE=...
  Url := ExpandConstant('{param:API|}');
  if Url = '' then Url := 'https://';
  PaginaApi.Values[0] := Url;
  PaginaApi.Values[1] := ExpandConstant('{param:CLAVE|}');
end;

function NextButtonClick(CurPageID: Integer): Boolean;
begin
  Result := True;
  if CurPageID = PaginaApi.ID then
  begin
    if not UrlValida(PaginaApi.Values[0]) then
    begin
      MsgBox('La dirección debe empezar con https:// para que los datos viajen cifrados.', mbError, MB_OK);
      Result := False;
    end
    else if not ClaveValida(PaginaApi.Values[1]) then
    begin
      MsgBox('Escribe la clave de activación completa.', mbError, MB_OK);
      Result := False;
    end;
  end;
end;

function PrepareToInstall(var NeedsRestart: Boolean): String;
var
  Codigo: Integer;
  Winsw: String;
begin
  Result := '';
  if not UrlValida(PaginaApi.Values[0]) then
  begin
    Result := 'Falta la dirección de la API, o no empieza con https://.';
    Exit;
  end;
  if not ClaveValida(PaginaApi.Values[1]) then
  begin
    Result := 'Falta la clave de activación.';
    Exit;
  end;

  // Si ya había una versión instalada, se detiene para poder reemplazar sus archivos
  Winsw := ExpandConstant('{app}\sfmantto-agente.exe');
  if FileExists(Winsw) then
    Exec(Winsw, 'stop', '', SW_HIDE, ewWaitUntilTerminated, Codigo);
end;

procedure CurStepChanged(CurStep: TSetupStep);
var
  Codigo: Integer;
  Datos, Java, Jar, Winsw, Salida: String;
  Mensaje: AnsiString;
begin
  if CurStep <> ssPostInstall then Exit;

  Datos := ExpandConstant('{commonappdata}\SFMantto');
  Java := ExpandConstant('{app}\runtime\bin\java.exe');
  Jar := ExpandConstant('{app}\sfmantto-agente.jar');
  Winsw := ExpandConstant('{app}\sfmantto-agente.exe');
  Salida := ExpandConstant('{tmp}\activacion.txt');

  // Carpeta de datos con el certificado de la autoridad certificadora
  ForceDirectories(Datos);
  FileCopy(ExpandConstant('{app}\ca.crt'), Datos + '\ca.crt', False);

  // Activación del equipo con la clave
  if not Exec(ExpandConstant('{cmd}'),
      '/C ""' + Java + '" -jar "' + Jar + '" --activar "' + Trim(PaginaApi.Values[0]) +
      '" "' + Trim(PaginaApi.Values[1]) + '" > "' + Salida + '" 2>&1"',
      '', SW_HIDE, ewWaitUntilTerminated, Codigo) then
    Codigo := -1;

  if Codigo <> 0 then
  begin
    Mensaje := '';
    LoadStringFromFile(Salida, Mensaje);
    DeleteFile(Salida);
    Log('Activación fallida: ' + String(Mensaje));
    MsgBox('No se pudo activar este equipo.' + #13#10 + #13#10 + Trim(String(Mensaje)) + #13#10 + #13#10 +
      'La instalación quedó incompleta: el servicio no se creó. Revisa la dirección y la clave y ejecuta el instalador de nuevo.',
      mbError, MB_OK);
    Exit;
  end;
  DeleteFile(Salida);

  // Solo el sistema y los administradores pueden leer la carpeta con el token
  Exec(ExpandConstant('{sys}\icacls.exe'),
    '"' + Datos + '" /inheritance:r /grant:r "*S-1-5-18:(OI)(CI)F" "*S-1-5-32-544:(OI)(CI)F"',
    '', SW_HIDE, ewWaitUntilTerminated, Codigo);

  // Servicio de Windows (si ya existía uno anterior, se reemplaza)
  Exec(Winsw, 'uninstall', '', SW_HIDE, ewWaitUntilTerminated, Codigo);
  if (not Exec(Winsw, 'install', '', SW_HIDE, ewWaitUntilTerminated, Codigo)) or (Codigo <> 0) then
  begin
    Log('No se pudo crear el servicio. Código ' + IntToStr(Codigo));
    MsgBox('El equipo se activó, pero no se pudo crear el servicio de Windows (código ' + IntToStr(Codigo) + ').',
      mbError, MB_OK);
    Exit;
  end;
  if (not Exec(Winsw, 'start', '', SW_HIDE, ewWaitUntilTerminated, Codigo)) or (Codigo <> 0) then
  begin
    Log('No se pudo iniciar el servicio. Código ' + IntToStr(Codigo));
    MsgBox('El servicio se creó pero no pudo iniciar (código ' + IntToStr(Codigo) + ').' + #13#10 +
      'Revisa C:\ProgramData\SFMantto\agente.log', mbError, MB_OK);
  end;
end;

procedure CurUninstallStepChanged(CurUninstallStep: TUninstallStep);
begin
  if CurUninstallStep = usPostUninstall then
  begin
    if (not UninstallSilent) and
       (MsgBox('¿Quieres borrar también la configuración y el registro del agente?', mbConfirmation, MB_YESNO) = IDYES) then
      DelTree(ExpandConstant('{commonappdata}\SFMantto'), True, True, True);
  end;
end;