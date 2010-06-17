:: I hate Windows!!!

:: Current working directory
SET PWD=%CD%

:: Replacing \ to /
SET PWD2=%PWD:\=/%

FOR %%i IN (%PWD2%/repository/*.html) DO ruby coloringwin.rb -d "%PWD2%/annotated_repository/" "%PWD2%/repository/%%~ni.html" > "%PWD2%/annotated_repository/%%~ni.html"