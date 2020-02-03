module(inclusive,
       [inRange/2, until/3, drop/3, inInclusiveRange/3,
        downTo/3, inInclusiveRangeList/3],
       [inclusive]).

% represents inclusive range from first to second
datadef(inclusive, [], [inclusive(int, int)]).

clausedef(inRange, [], [inclusive, % input inclusive
                        int]).     % output integer
inRange(inclusive(Start, End), Result) :-
    inInclusiveRange(Start, End, Result).

clausedef(inInclusiveRange, [], [int,   % start of range
                                 int,   % end of range
                                 int]). % output
inInclusiveRange(Start, End, Start) :-
    Start =< End.
inInclusiveRange(Start, End, Result) :-
    Start < End,
    NewStart is Start + 1,
    inInclusiveRange(NewStart, End, Result).

clausedef(inInclusiveRangeList, [], [int,         % start
                                     int,         % end
                                     list(int)]). % list
inInclusiveRangeList(Start, End, []) :-
    Start > End.
inInclusiveRangeList(Start, End, [Start|Rest]) :-
    Start =< End,
    NewStart is Start + 1,
    inInclusiveRangeList(NewStart, End, Rest).

clausedef(until, [], [int,   % start
                      int,   % end
                      int]). % result
until(Start, End, Result) :-
    NewEnd is End - 1,
    inInclusiveRange(Start, NewEnd, Result).

% works like Scala's drop
clausedef(drop, [], [inclusive,
                     int,
                     inclusive]).
drop(inclusive(Start, End),
     DropAmount,
     inclusive(NewStart, End)) :-
    NewStart is min(DropAmount + Start, End).

clausedef(downTo, [], [int,   % start
                       int,   % end
                       int]). % value
downTo(Start, End, Start) :-
    Start >= End.
downTo(Start, End, Value) :-
    Start > End,
    NewStart is Start - 1,
    downTo(NewStart, End, Value).
