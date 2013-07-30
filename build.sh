#!/bin/sh
if which sbt>/dev/null ; then
	sbt compile ; sbt assembly
else
	echo "sbt is not installed in this system."
fi

if which perl>/dev/null ; then
	perl ./build.pl
else
	echo "perl is not found"
fi