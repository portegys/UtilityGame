set OLDCLASSPATH=%CLASSPATH%
set CLASSPATH=.
start java -jar GameMaster.jar
echo Wait for GameMaster to be ready
pause
start java -jar GamePlayer.jar
set CLASSPATH=%OLDCLASSPATH%
set OLDCLASSPATH=

