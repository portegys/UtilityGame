set OLDCLASSPATH=%CLASSPATH%
set CLASSPATH=.
start java -classpath GameMaster.jar UtilityGame.GameMaster
echo Wait for GameMaster to be ready
pause
appletviewer gameplayer.html
set CLASSPATH=%OLDCLASSPATH%
set OLDCLASSPATH=

