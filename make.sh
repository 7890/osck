#!/bin/sh

FULLPATH="`pwd`/$0"
DIR=`dirname "$FULLPATH"`

src="$DIR"/src
#lib="$DIR"/lib
build="$DIR"/_build
archive="$DIR"/archive
#doc="$DIR"/doc

#linux / osx different mktemp call
#TMPFILE=`mktemp 2>/dev/null || mktemp -t /tmp`

#NOW=`date +"%s"`

jsource=1.6
jtarget=1.6

JAVAC="javac -source $jsource -target $jtarget -nowarn"
JAVA="java -Xms500M -Xmx1000M"

jetty_dist_name=jetty-distribution-9.2.10.v20150310
#jetty_dist_name=jetty-distribution-9.3.8.v20160314

jetty_tarball="$jetty_dist_name".tar.gz

java_osc_jar="$archive"/JavaOSC_1457817871.jar

#========================================================================
checkAvail()
{
	which "$1" >/dev/null 2>&1
	ret=$?
	if [ $ret -ne 0 ]
	then
		echo "tool \"$1\" not found. please install"
		exit 1
	fi
}

#========================================================================
compile_osck()
{
	echo "building OSCK"
	echo "============="

	cp "$archive"/"$jetty_tarball" "$build"
	cd "$build"
	tar xf "$jetty_tarball"
	cd "$DIR"
	jetty_home="$build"/"$jetty_dist_name"
	jetty_libs=`echo $(ls -1 "$jetty_home"/lib/*.jar) | sed 's/ /:/g'`":"`echo $(ls -1 "$jetty_home"/lib/websocket/*.jar) | sed 's/ /:/g'`

	$JAVAC -classpath "$build":"$java_osc_jar":"$jetty_libs" -sourcepath "$src" -d "$build" "$src"/*.java
}

#========================================================================
run_osck()
{
	echo "running OSCK"
	echo "============"
	jetty_home="$build"/"$jetty_dist_name"
#	jetty_libs=`echo $(ls -1 "$jetty_home"/lib/*.jar) | sed 's/ /:/g'`":"`echo $(ls -1 "$jetty_home"/lib/websocket/*.jar) | sed 's/ /:/g'`

	jlib="$jetty_home"/lib
	jetty_libs=""
	jetty_libs="${jetty_libs}:"
	jetty_libs="${jetty_libs}:"${jlib}/jetty-http-9.2.10.v20150310.jar
	jetty_libs="${jetty_libs}:"${jlib}/jetty-io-9.2.10.v20150310.jar
	jetty_libs="${jetty_libs}:"${jlib}/jetty-server-9.2.10.v20150310.jar
	jetty_libs="${jetty_libs}:"${jlib}/jetty-util-9.2.10.v20150310.jar
	jetty_libs="${jetty_libs}:"${jlib}/servlet-api-3.1.jar

	jetty_libs="${jetty_libs}:"${jlib}/websocket/websocket-api-9.2.10.v20150310.jar
	jetty_libs="${jetty_libs}:"${jlib}/websocket/websocket-common-9.2.10.v20150310.jar
	jetty_libs="${jetty_libs}:"${jlib}/websocket/websocket-server-9.2.10.v20150310.jar
	jetty_libs="${jetty_libs}:"${jlib}/websocket/websocket-servlet-9.2.10.v20150310.jar

echo	$JAVA -classpath "$build":"$java_osc_jar":"$jetty_libs" WebSocketServer
	$JAVA -classpath "$build":"$java_osc_jar":"$jetty_libs" WebSocketServer
}

for tool in java javac jar javadoc; \
	do checkAvail "$tool"; done

mkdir -p "$build"
rm -rf "$build"/*

compile_osck
run_osck
