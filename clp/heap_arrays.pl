module(heap_arrays,
       [heap_main/2],
       [heap]).

use_module('inclusive.pl',
           [until/3, downTo/3],
           []).

datadef(heap, [], [heap_leaf,
                   heap_node(heap, int, heap)]).

clausedef(makeHeap, [], [int,    % size
                         int,    % start range
                         int,    % end range
                         heap]). % output heap
makeHeap(Size, RangeStart, RangeEnd, _) :-
    RangeSize is RangeEnd - RangeStart + 1,
    Size > RangeSize,
    !,
    fail.
makeHeap(Size, _, _, heap_leaf) :-
    Size =< 0,
    !.
makeHeap(1, RangeStart, RangeEnd, heap_node(heap_leaf, Value, heap_leaf)) :-
    !,
    RangeSize is RangeEnd - RangeStart + 1,
    until(0, RangeSize, I),
    % start and ends are REVERSED in Scala because they are doing
    % a range that goes down
    Value is RangeEnd + I.
makeHeap(Size, RangeStart, RangeEnd, heap_node(Left, RootInd, Right)) :-
    RangeEnd >= RangeStart,
    !,
    SizeMinusOne is Size - 1,
    LeftSize is truncate(SizeMinusOne / 2),
    ActualLeftSize is SizeMinusOne - LeftSize,
    downTo(RangeEnd, RangeStart, RootInd),
    makeHeap(ActualLeftSize, 0, RootInd, Left),
    makeHeap(LeftSize, 0, RootInd, Right).

clausedef(heap_main, [], [int, heap]).
heap_main(Size, Heap) :-
    makeHeap(Size, 0, Size, Heap).
