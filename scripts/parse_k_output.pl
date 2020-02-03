#!/usr/bin/perl -w

use POSIX;
use strict;

my $currentK = undef;
my $currentBound = undef;
my %currentRowSpeed;
my %currentRowMemory;

sub printRow() {
    print "$currentK & ";
    foreach my $bound (sort {$a <=> $b } keys %currentRowSpeed) {
	print $currentRowSpeed{$bound};
	print " & ";
    }
    foreach my $bound (sort {$a <=> $b } keys %currentRowMemory) {
	print $currentRowMemory{$bound};
	print " & ";
    }
    print "\\\\\n";
    %currentRowSpeed = ();
    %currentRowMemory = ();
}

my $file = shift() or die "Needs a filename";
open(my $fh, '<', $file) or die $!;

while (my $line = <$fh>) {
    chomp($line);
    if ($line =~ /^K: (\d+)$/) {
	my $newK = $1;
	if (defined($currentK) && $currentK != $newK) {
	    printRow();
	}
	$currentK = $newK;
    } elsif ($line =~ /^Bound: (\d+)$/) {
	$currentBound = $1;
    } elsif ($line =~ /^(\d+) ms$/) {
	$currentRowSpeed{$currentBound} = ceil($1 / 1000);
    } elsif ($line =~ /^(\d+) kB$/) {
	$currentRowMemory{$currentBound} = ceil($1 / 1024);
    }
}
defined($currentK) or die "K not defined";
printRow();
close($fh);
