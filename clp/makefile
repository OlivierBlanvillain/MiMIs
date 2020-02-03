SWIPL_VERSION_STRING=$(shell swipl --version)
SWIPL_FLAGS= 
PROBE= version 7

ifneq (,$(findstring $(PROBE),$(SWIPL_VERSION_STRING)))
  SWIPL_FLAGS += --traditional
endif

output: output.pl
	gplc --no-top-level output.pl

output.pl: compiler.pl inclusive.pl binary_search_trees.pl unsafe_math.pl red_black_trees.pl heap_arrays.pl b_trees.pl riff_images.pl unsafe_main.pl main.pl
	swipl $(SWIPL_FLAGS) -q -s compiler.pl -g "public_7_processFileForSwipl('main.pl', 'output.pl')." -t halt.
	cat unsafe_math.pl >> output.pl
	cat unsafe_main.pl >> output.pl

clean:
	rm output.pl output
