#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
pushd $DIR

rm -rf out
mkdir out
cd src

CP=
for jar in ../lib/*.jar
do
	CP=$CP$jar:
done

echo "Building java source files. "
echo javac -classpath $CP -d ../out ./com/okta/*.java

javac -classpath $CP -d ../out ./com/okta/*.java
if [ $? -ne 0 ]; then
	echo "Javac failed. Error code $?"
	exit $?
fi

mkdir out/META-INF
cp -r ./META-INF/* out/META-INF

echo "Building jar file"

cd ../out
ln -s ../lib lib
jar cvfm jradius.jar ../src/META-INF/MANIFEST.MF com lib
if [ $? -ne 0 ]; then
	echo "Building jar failed. Error code $?"
	exit $?
fi

echo "You can now invoke the jar like this, "
echo "java -jar jradius.jar -r <ip-of-radius-server> -s <shared-secret> -u <user-name> -p <user's password>"

popd
