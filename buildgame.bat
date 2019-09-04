set OLDCLASSPATH=%CLASSPATH%
set CLASSPATH=.
javac -d . *.java
rmic -d . UtilityGame.GameMaster UtilityGame.GamePlayer
jar cvfm GamePlayer.jar player-manifest.mf UtilityGame
jarsigner GamePlayer.jar ugame
jar cvfm GameMaster.jar master-manifest.mf UtilityGame *.java *.mf Readme.txt *.bat gameplayer.html gameplayer.jnlp
set CLASSPATH=%OLDCLASSPATH%
set OLDCLASSPATH=

