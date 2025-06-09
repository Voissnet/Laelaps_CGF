@ECHO OFF

IF [%1]==[/?] GOTO DO_NOTHING
set PROJECT_NAME=%1
call project.bat %PROJECT_NAME%
cd %PROJECT_NAME%
call ..\api.bat
call ..\webapp.bat
call ..\ear.bat
call ..\ejb.bat
GOTO DONE
:DO_NOTHING
echo Name is missing 
:DONE