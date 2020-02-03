#!/usr/bin/perl -w

use strict;

my $baseScalaCommand = '/usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java -Xms2048m -Xmx30000m -XX:+UseConcMarkSweepGC -classpath /home/kyle/SciFe/target/scala-2.11/test-classes:/home/kyle/SciFe/target/scala-2.11/classes:/home/kyle/.ivy2/cache/org.scala-lang/scala-compiler/jars/scala-compiler-2.11.4.jar:/home/kyle/.ivy2/cache/org.scala-lang/scala-library/jars/scala-library-2.11.4.jar:/home/kyle/.ivy2/cache/org.scala-lang/scala-reflect/jars/scala-reflect-2.11.4.jar:/home/kyle/.ivy2/cache/org.scala-lang.modules/scala-xml_2.11/bundles/scala-xml_2.11-1.0.2.jar:/home/kyle/.ivy2/cache/org.scala-lang.modules/scala-parser-combinators_2.11/bundles/scala-parser-combinators_2.11-1.0.2.jar:/home/kyle/.ivy2/cache/com.typesafe.scala-logging/scala-logging_2.11/jars/scala-logging_2.11-3.1.0.jar:/home/kyle/.ivy2/cache/org.slf4j/slf4j-api/jars/slf4j-api-1.7.7.jar:/home/kyle/.ivy2/cache/org.apache.logging.log4j/log4j-api/jars/log4j-api-2.0.2.jar:/home/kyle/.ivy2/cache/org.apache.logging.log4j/log4j-core/jars/log4j-core-2.0.2.jar:/home/kyle/.ivy2/cache/org.apache.logging.log4j/log4j-slf4j-impl/jars/log4j-slf4j-impl-2.0.2.jar:/home/kyle/.ivy2/cache/com.storm-enroute/scalameter_2.11/jars/scalameter_2.11-0.6.jar:/home/kyle/.ivy2/cache/com.storm-enroute/scalameter-core_2.11/jars/scalameter-core_2.11-0.6.jar:/home/kyle/.ivy2/cache/org.apache.commons/commons-math3/jars/commons-math3-3.4.1.jar:/home/kyle/.ivy2/cache/com.github.wookietreiber/scala-chart_2.11/jars/scala-chart_2.11-0.4.2.jar:/home/kyle/.ivy2/cache/org.jfree/jfreechart/jars/jfreechart-1.0.17.jar:/home/kyle/.ivy2/cache/org.jfree/jcommon/jars/jcommon-1.0.21.jar:/home/kyle/.ivy2/cache/xml-apis/xml-apis/jars/xml-apis-1.3.04.jar:/home/kyle/.ivy2/cache/org.scala-lang.modules/scala-swing_2.11/bundles/scala-swing_2.11-1.0.1.jar:/home/kyle/.ivy2/cache/org.scala-tools.testing/test-interface/jars/test-interface-0.5.jar:/home/kyle/.ivy2/cache/com.googlecode.kiama/kiama_2.11/jars/kiama_2.11-1.7.0.jar:/home/kyle/.ivy2/cache/com.google.code.findbugs/jsr305/jars/jsr305-2.0.3.jar:/home/kyle/.ivy2/cache/com.google.guava/guava/bundles/guava-17.0.jar:/home/kyle/.ivy2/cache/org.bitbucket.inkytonik.dsinfo/dsinfo_2.11/jars/dsinfo_2.11-0.4.0.jar:/home/kyle/.ivy2/cache/org.bitbucket.inkytonik.dsprofile/dsprofile_2.11/jars/dsprofile_2.11-0.4.0.jar:/home/kyle/.ivy2/cache/org.rogach/scallop_2.11/jars/scallop_2.11-0.9.5.jar:/home/kyle/.ivy2/cache/jline/jline/jars/jline-2.12.jar:/home/kyle/.ivy2/cache/com.googlecode.combinatoricslib/combinatoricslib/jars/combinatoricslib-2.1.jar:/home/kyle/.ivy2/cache/org.jgrapht/jgrapht-core/jars/jgrapht-core-0.9.0.jar:/home/kyle/.ivy2/cache/com.madhukaraphatak/java-sizeof_2.11/jars/java-sizeof_2.11-0.1.jar:/home/kyle/.ivy2/cache/org.scalacheck/scalacheck_2.11/jars/scalacheck_2.11-1.12.1.jar:/home/kyle/.ivy2/cache/org.scala-sbt/test-interface/jars/test-interface-1.0.jar:/home/kyle/.ivy2/cache/org.scalatest/scalatest_2.11/bundles/scalatest_2.11-2.2.4.jar:/home/kyle/.ivy2/cache/junit/junit/jars/junit-4.11.jar:/home/kyle/.ivy2/cache/org.hamcrest/hamcrest-core/jars/hamcrest-core-1.3.jar:/home/kyle/.ivy2/cache/com.novocode/junit-interface/jars/junit-interface-0.11.jar:/home/kyle/.ivy2/cache/org.specs2/specs2-core_2.11/jars/specs2-core_2.11-3.0.jar:/home/kyle/.ivy2/cache/org.specs2/specs2-matcher_2.11/jars/specs2-matcher_2.11-3.0.jar:/home/kyle/.ivy2/cache/org.specs2/specs2-common_2.11/jars/specs2-common_2.11-3.0.jar:/home/kyle/.ivy2/cache/org.scalaz/scalaz-core_2.11/bundles/scalaz-core_2.11-7.1.1.jar:/home/kyle/.ivy2/cache/org.scalaz/scalaz-concurrent_2.11/bundles/scalaz-concurrent_2.11-7.1.1.jar:/home/kyle/.ivy2/cache/org.scalaz/scalaz-effect_2.11/bundles/scalaz-effect_2.11-7.1.1.jar:/home/kyle/.ivy2/cache/org.scalaz.stream/scalaz-stream_2.11/bundles/scalaz-stream_2.11-0.6a.jar:/home/kyle/.ivy2/cache/org.typelevel/scodec-bits_2.11/bundles/scodec-bits_2.11-1.0.4.jar:/home/kyle/.ivy2/cache/com.chuusai/shapeless_2.11/bundles/shapeless_2.11-2.0.0.jar:/home/kyle/.ivy2/cache/org.scoverage/scalac-scoverage-runtime_2.11/jars/scalac-scoverage-runtime_2.11-1.0.4.jar:/home/kyle/.ivy2/cache/org.scoverage/scalac-scoverage-plugin_2.11/jars/scalac-scoverage-plugin_2.11-1.0.4.jar scife.enumeration.benchmarks.iterators.';

# Benchmark
# {
#    sciFeClassName
#    mimiClassName
#    streamClassName
#    kyleClpName
#    senniClpName
#    startBound
#    endBound
# }

my @benchmarks = (
    { sciFeClassName => 'BSTSciFeEvaluate',
      mimiClassName => 'BSTMiMIEvaluate',
      streamClassName => 'BSTStreamEvaluate',
      kyleClpName => 'kyle_bst',
      senniClpName => 'senni_bst',
      startBound => 12,
      endBound => 50
    },
    { sciFeClassName => 'RBTSciFeEvaluate',
      mimiClassName => 'RBTMiMIEvaluate',
      streamClassName => 'RBTStreamEvaluate',
      kyleClpName => 'kyle_rb',
      senniClpName => 'senni_rb',
      startBound => 14,
      endBound => 50
    },
    { sciFeClassName => 'HeapSciFeEvaluate',
      mimiClassName => 'HeapMiMIEvaluate',
      streamClassName => 'HeapStreamEvaluate',
      kyleClpName => 'kyle_heap',
      senniClpName => undef,
      startBound => 9,
      endBound => 50
    },
    { sciFeClassName => 'BTreeSciFeEvaluate',
      mimiClassName => 'BTreeMiMIEvaluate',
      streamClassName => 'BTreeStreamEvaluate',
      kyleClpName => 'kyle_btree',
      senniClpName => undef,
      startBound => 28,
      endBound => 50
    },
    { sciFeClassName => 'RiffSciFeEvaluate',
      mimiClassName => 'RiffMiMIEvaluate',
      streamClassName => 'RiffStreamEvaluate',
      kyleClpName => 'kyle_riff',
      senniClpName => undef,
      startBound => 12,
      endBound => 50
    },
    { sciFeClassName => 'JavaSciFeEvaluate',
      mimiClassName => 'JavaMiMIEvaluate',
      streamClassName => 'JavaStreamEvaluate',
      kyleClpName => undef,
      senniClpName => undef,
      startBound => 0,
      endBound => 10
    }
    );
    
# Takes:
# -Name of main class to run
# -Bound
# -K value (optional)
# Returns 1 if it terminated normally, else undef
sub runSingleScalaBenchmark($$@) {
    my ($className, $bound, @ks) = @_;
    scalar(@ks) <= 1 or die;
    my $k = (scalar(@ks) == 1) ? $ks[0] : '';
    
    my @output = `$baseScalaCommand$className $bound $k 2>&1`;
    for my $line (@output) {
	print $line;
    }
    if (scalar(@output) == 4) {
	return 1;
    } else {
	print "Error detected\n";
	return undef;
    }
}

# Takes:
# -Name of the CLP thing to run
# -Bound
# Returns 1 if terminated normally, else undef
sub runSingleClpBenchmark($$) {
    my ($thingName, $bound) = @_;
    print "$thingName; CLP; $bound\n";
    my @output = `./clp/run_benchmark.sh $thingName $bound 2>&1`;
    for my $line (@output) {
	print $line;
    }
    if (scalar(@output) == 2) {
	return 1;
    } else {
	print "Error detected\n";
	return undef;
    }
}

# Takes:
# -Reference to benchmark
sub runBenchmark($) {
    my $benchmarkRef = shift();
    my $runSciFe = 1;
    my $runMimi = 1;
    my $runStream = 1;
    my $runKyleClp = 1;
    my $runSenniClp = 1;
    
    for (my $bound = $benchmarkRef->{startBound};
	 $bound <= $benchmarkRef->{endBound};
	 $bound++) {
	if (defined($runSciFe)) {
	    $runSciFe = runSingleScalaBenchmark($benchmarkRef->{sciFeClassName}, $bound);
	} else {
	    print "Skipping bound $bound for SciFe due to previously-detected error\n";
	}
	
	if (defined($runMimi)) {
	    $runMimi = runSingleScalaBenchmark($benchmarkRef->{mimiClassName}, $bound);
	} else {
	    print "Skipping bound $bound for MiMIs due to previously-detected error\n";
	}

	if (defined($runStream)) {
	    $runStream = runSingleScalaBenchmark($benchmarkRef->{streamClassName}, $bound);
	} else {
	    print "Skipping bound $bound for Streams due to previously-detected error\n";
	}

	if (defined($benchmarkRef->{kyleClpName})) {
	    if (defined($runKyleClp)) {
		$runKyleClp = runSingleClpBenchmark($benchmarkRef->{kyleClpName}, $bound);
	    } else {
		print "Skipping bound $bound for Kyle CLP due to previously-detected error\n";
	    }
	}

	if (defined($benchmarkRef->{senniClpName})) {
	    if (defined($runSenniClp)) {
		$runSenniClp = runSingleClpBenchmark($benchmarkRef->{senniClpName}, $bound);
	    } else {
		print "Skipping bound $bound for Senni CLP due to previously-detected error\n";
	    }
	}
    }
} # runBenchmark

# Takes:
# -Reference to benchmark to run
# -Start bound
# -End bound
# -Values of k to run for
sub runKBenchmark($$$@) {
    my ($benchmarkRef, $startBound, $endBound, @ks) = @_;
    foreach my $k (@ks) {
	for (my $bound = $startBound; $bound <= $endBound; $bound++) {
	print "K: $k\n";
	print "Bound: $bound\n";
	runSingleScalaBenchmark($benchmarkRef->{mimiClassName},
				$bound,
				$k);
	}
    }
}

sub usage() {
    print "Needs either 'normal' or 'k' to indicate the kind of benchmarking run\n";
}

# ---BEGIN MAIN---
if (scalar(@ARGV) != 1) {
    usage();
} elsif ($ARGV[0] eq 'normal') {
    for my $benchmarkRef (@benchmarks) {
	runBenchmark($benchmarkRef);
    }
} elsif ($ARGV[0] eq 'k') {
    runKBenchmark($benchmarks[0],
		  15,
		  17,
		  1000,
		  10000,
		  100000,
		  1000000,
		  10000000,
		  100000000);
} else {
    usage();
}
