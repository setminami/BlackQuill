use strict;
use warnings;

my $file = "./build.sbt";
open(my $fh, "<", $file)||die "Cannot open $file: $!";

my @name = map /^name\s?:=\s?\"(.*)\"\s*$/i, <$fh>;
seek $fh,0,0;
my @version = map /^Version\s?:=\s?\"(.*)\"\s*$/i,<$fh>;
seek $fh,0,0;
my @scala_version = map /^scalaVersion\s?:=\s?\"(.*)\"\s*$/i,<$fh>;

close($fh);

my $AppName = $name[0];
my $version = $version[0];
my $scalaVersion = "";
if( $scala_version[0] =~ /(.*)\.0$/){
	$scalaVersion = $1;
}else{
	$scalaVersion = $scala_version[0];
}
print "AppName = " . $AppName;
print "\n";
print "Version = " . $version;
print "\n";
print "Scala Version = " . $scalaVersion;
print "\n";

my $whereami = `pwd`;
chomp $whereami;

my $jarFile = $whereami . "/target/scala-" . $scalaVersion . "/" . $AppName . "-assembly-" . $version . ".jar";
if (-e $jarFile) {
	print "Found executable jar @ " . $jarFile . "\n";
}else{
	print "executable jar file not found.";
}
