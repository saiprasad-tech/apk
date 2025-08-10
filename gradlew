#!/usr/bin/env sh
##############################################################################
# Gradle start up script for UN*X
##############################################################################

APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`

# Resolve APP_HOME (the directory of this script, resolving any symlinks)
PRG="$0"
while [ -h "$PRG" ]; do
  ls -ld "$PRG" >/dev/null 2>&1
  link=`readlink "$PRG"`
  case "$link" in
    /*) PRG="$link" ;;
    *) PRG="`dirname "$PRG"`/$link" ;;
  esac
done
SAVED="`pwd`"
cd "`dirname "$PRG"`" >/dev/null 2>&1
APP_HOME="`pwd -P`"
cd "$SAVED" >/dev/null 2>&1

# Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

# OS specific support (must be 'true' or 'false').
cygwin=false
msys=false
darwin=false
nonstop=false
case "`uname`" in
  CYGWIN* ) cygwin=true ;;
  Darwin* ) darwin=true ;;
  MINGW* )  msys=true ;;
  MSYS* )   msys=true ;;
  NONSTOP* ) nonstop=true ;;
esac

CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

# Determine the Java command to use to start the JVM.
if [ -n "$JAVA_HOME" ] ; then
  if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
    # IBM's JDK on AIX uses strange locations for the executables
    JAVACMD="$JAVA_HOME/jre/sh/java"
  else
    JAVACMD="$JAVA_HOME/bin/java"
  fi
  if [ ! -x "$JAVACMD" ] ; then
    echo
    echo "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME"
    echo
    echo "Please set the JAVA_HOME variable in your environment to match the"
    echo "location of your Java installation."
    echo
    exit 1
  fi
else
  JAVACMD="java"
  command -v java >/dev/null 2>&1 || {
    echo
    echo "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH."
    echo
    echo "Please set the JAVA_HOME variable in your environment to match the"
    echo "location of your Java installation."
    echo
    exit 1
  }
fi

# For Cygwin/MSYS, ensure paths are Windows-style for Java
if $cygwin || $msys ; then
  APP_HOME=`cygpath --path --mixed "$APP_HOME"`
  CLASSPATH=`cygpath --path --mixed "$CLASSPATH"`
  JAVACMD=`cygpath --unix "$JAVACMD"`
fi

# Escape application args
save () {
  for i do printf %s\\n "$i" | sed "s/'/'\\\\''/g;1s/^/'/;\$s/\$/' \\\\/" ; done
  echo " "
}
APP_ARGS=$(save "$@")

# Collect all arguments for the java command
set -- $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS \
  "-Dorg.gradle.appname=$APP_BASE_NAME" \
  -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$APP_ARGS"

# by using the same arguments order for the exec call.
exec "$JAVACMD" "$@"