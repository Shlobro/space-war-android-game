@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
pushd "%SCRIPT_DIR%editor" || (
    echo Failed to open the editor folder.
    exit /b 1
)

if not exist "node_modules" (
    echo Installing editor dependencies...
    call npm install
    if errorlevel 1 (
        popd
        exit /b 1
    )
)

echo Starting the level editor...
call npm run dev
set "EXIT_CODE=%ERRORLEVEL%"

popd
exit /b %EXIT_CODE%
