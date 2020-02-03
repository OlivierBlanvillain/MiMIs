until(Start, End, Result) :-
    NewEnd is End - 1,
    inInclusiveRange(Start, NewEnd, Result).
downTo(Start, End, Start) :-
    Start >= End.
downTo(Start, End, Value) :-
    Start > End,
    NewStart is Start - 1,
    downTo(NewStart, End, Value).
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
heap_main(Size, Heap) :-
    makeHeap(Size, 0, Size, Heap).
