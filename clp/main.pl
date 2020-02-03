module(main, [], []).

use_module('binary_search_trees.pl',
           [bst_main/2],
           [bst]).
use_module('red_black_trees.pl',
           [rb_main/2],
           [rb_tree]).
use_module('heap_arrays.pl',
           [heap_main/2],
           [heap]).
use_module('b_trees.pl',
           [b_tree_main/2],
           [b_tree]).
use_module('riff_images.pl',
           [riff_main/2],
           [riff_chunk]).
use_module('java.pl',
           [java_main/2],
           [java_test]).

clausedef(yolo_UNSAFE_writeThing, [A], [A]).
yolo_UNSAFE_writeThing(A) :-
    write(A),
    nl.

clausedef(getAll, [], [relation([])]).
getAll(Thunk) :-
    call(Thunk),
    fail.
getAll(_).

clausedef(benchmarkBST, [], [int]).
benchmarkBST(Size) :-
    getAll(lambda([], bst_main(Size, _))).

clausedef(benchmarkRB, [], [int]).
benchmarkRB(Size) :-
    getAll(lambda([], rb_main(Size, _))).

clausedef(benchmarkHeap, [], [int]).
benchmarkHeap(Size) :-
    getAll(lambda([], heap_main(Size, _))).

clausedef(benchmarkBTree, [], [int]).
benchmarkBTree(Size) :-
    getAll(lambda([], b_tree_main(Size, _))).

clausedef(benchmarkRiff, [], [int]).
benchmarkRiff(Size) :-
    getAll(lambda([], riff_main(Size, _))).

clausedef(benchmarkJava, [], [int]).
benchmarkJava(Size) :-
    getAll(lambda([], java_main(Size, _))).

