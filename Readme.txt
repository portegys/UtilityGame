The Utility Game

Reference:
T. E. Portegys and J. R. Wolf, Jr.,
"Technology Adoption in the Presence of Network Externalities: A Web-Based Classroom Game",
Informs Transactions on Education, Vol. 8, No. 1, September 2007

Installing the Software

The Game Master application requires a recent version of Java, e.g. 1.4,
available at java.sun.com. The application can be downloaded from:

tom.portegys.com/research/UtilityGame/GameMaster.jar

The jar file contains all the source code, associated files, build command,
HTML files, and the Master executable code. It can be started on most Windows
systems by simply double-clicking the jar file; otherwise, the following
command will start it:

java -jar GameMaster.jar

The source files can be unpacked from the jar file with the jar tool that
comes with Java, or with an archiving tool such as WinRAR, available at
www.rarlab.com. The system can then be built by clicking the buildgame.bat
file, and tested with the rungame.bat file.

The Game Player is a signed Java jar that will run as an application:

java -jar GamePlayer.jar

or using appletviewer:

appletviewer gameplayer.html

The MasterHost parameter in gameplayer.html can be set to the IP address of
the Master host, which will allow players to connect to it.

The Player communicates with the Master via Java RMI (Remote Method
Invocation), necessitating the signing of the GamePlayer.jar with the jarsigner
command, invoked from the buildgame.bat script. The jarsigner command expects
to find a public/private key pair on your computer having the alias "ugame".
This can be created with the keytool command as follows:

keytool -genkey -alias ugame -keypass your_password

Upon connecting to the Master, your computer may prompt you to unblock a
network connection to allow RMI to proceed.
