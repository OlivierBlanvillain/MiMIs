inInclusiveRange(Start, End, Start) :-
    Start =< End.
inInclusiveRange(Start, End, Result) :-
    Start < End,
    NewStart is Start + 1,
    inInclusiveRange(NewStart, End, Result).
until(Start, End, Result) :-
    NewEnd is End - 1,
    inInclusiveRange(Start, NewEnd, Result).
makeRiff(0, 0, _, _, riff_leaf) :- !.
makeRiff(1, DataSize, JiffLoss, AvChunks,
         riff_payload(DataSize, OutputDataSize, AvChunks)) :-
    DataSize > 0,
    AvChunks =< 1,
    JiffLossTimesFour is JiffLoss * 4,
    0 is mod(JiffLossTimesFour, DataSize),
    !,
    OutputDataSize is truncate(JiffLossTimesFour / DataSize).
makeRiff(Size, DataSize, JiffLoss, AvChunks,
         riff_node(DataSize, LeftTree, RightTree)) :-
    SizeMinusOne is Size - 1,
    until(0, SizeMinusOne, LeftSize),
    RightTreeSize is Size - LeftSize - 1,
    LeftAudioStart is max(0, AvChunks - (Size - LeftSize - 1)),
    LeftAudioEnd is min(LeftSize, AvChunks),
    inInclusiveRange(LeftAudioStart, LeftAudioEnd, LeftAudio),
    RightAvChunks is AvChunks - LeftAudio,
    DataSizeDivTwo is truncate(DataSize / 2),
    inInclusiveRange(0, DataSizeDivTwo, LeftDataSize),
    RightDataSize is DataSize - LeftDataSize,
    LeftJiffStart is max(0, JiffLoss - (DataSize - LeftDataSize)),
    LeftJiffEnd is min(LeftDataSize, JiffLoss),
    inInclusiveRange(LeftJiffStart, LeftJiffEnd, LeftJiff),
    RightJiffLoss is JiffLoss - LeftJiff,
    makeRiff(LeftSize, LeftDataSize, LeftJiff, LeftAudio, LeftTree),
    makeRiff(RightTreeSize, RightDataSize, RightJiffLoss, RightAvChunks, RightTree).
makeRiff(Size, Riff) :-
    JiffLoss is truncate((Size + 1) / 2),
    AvChunks is truncate(Size / 2),
    makeRiff(Size, Size, JiffLoss, AvChunks, Riff).
riff_main(Size, Riff) :-
    makeRiff(Size, Riff).
