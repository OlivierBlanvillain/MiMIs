benchmarkBST(Size) :-
    private_7_benchmarkBST(Size).

benchmarkRB(Size) :-
    private_7_benchmarkRB(Size).

benchmarkHeap(Size) :-
    private_7_benchmarkHeap(Size).

benchmarkBTree(Size) :-
    private_7_benchmarkBTree(Size).

benchmarkRiff(Size) :-
    private_7_benchmarkRiff(Size).

benchmarkJava(Size) :-
    private_7_benchmarkJava(Size).

% ---BEGIN CODE FROM SENNI ET AL. FOR BSTS---
searchtree(T,S) :- searchtree(T,S,S).

searchtree(T,S,MaxNat) :-
	% Preamble
	varlist(S,Vs), Max #= MaxNat-1, fd_domain(Vs,0,Max), fd_all_different(Vs),
	% Structural Constraints
	lbt(T,S,Vs,_),
	ordered(T,0,MaxNat),
	% Instantiation
	fd_labeling(Vs,[]).	% useless for canonical trees

lbt(t(e,N,e),  S,[N|Rest],Rest) :- S#=1.
lbt(t(TL,N,e), S,[N|Set] ,Rest) :- S#>=2, NewS #= S-1, lbt(TL,NewS,Set,Rest).
lbt(t(e,N,TR), S,[N|Set] ,Rest) :- S#>=2, NewS #= S-1, lbt(TR,NewS,Set,Rest).
lbt(t(TL,N,TR),S,[N|Set] ,Rest) :- S#>=2, NewS #= S-1, SL#>=1, SR#>=1, fdsum(NewS,SL,SR),
                                   lbt(TL,SL,Set,Temp), lbt(TR,SR,Temp,Rest).

ordered(e,_,_).
ordered(t(TL,N,TR),Min,Max) :- Min #=< N, N #< Max, ordered(TL,Min,N), M #= N+1, ordered(TR,M,Max).

fdsum(Sum,X,Y) :- fd_domain(X,0,Sum), fd_labeling(X), Y#=Sum-X.

varlist(0,[]).
varlist(N,[_|L]) :- N>0, M is N-1, varlist(M,L).
% ---END CODE FROM SENNI ET AL. FOR BSTS---

benchmarkSenniBST(Size) :-
    searchtree(_, Size),
    fail.
benchmarkSenniBST(_).

% ---BEGIN CODE FROM SENNI ET AL. FOR RBTS---
rbtree(T,S) :- rbtree(T,S,S,S).

rbtree(T,MinSize,MaxSize,NumKeys) :-
    % Preamble
      MinSize#=<S, S#=<MaxSize, fd_labeling([S]),
      varlist(S,Keys), varlist(S,Colors), Max#=NumKeys-1,
      fd_domain(Keys,0,Max), fd_domain(Colors,0,1), 
    % Symbolic Definition
      sync(T,S,Keys,[],Colors,[],0,NumKeys,_),
    % Instantiation
      fd_labeling(Keys), fd_labeling(Colors).


sync(                           e,A,    B,B,    C,C,_,_,0) :- A#=0.
sync(t(A,B,         e,         e),C,[B|D],D,[A|E],E,F,G,H) :- C#=1, H#=A, F#=<B, B#<G.
sync(t(A,B,         e,t(C,D,E,F)),G,[B|H],I,[A|J],K,L,M,N) :-
         G#>=2, O#=G-1, A+C#>0, N#=A, L#=<B, B#<M, P#=B+1,
         sync(t(C,D,E,F),O,H,I,J,K,P,M,0).                       % replacement
sync(t(A,F,t(B,C,D,E),         e),G,[F|H],I,[A|J],K,L,M,N) :-
         G#>=2, O#=G-1, A+B#>0, N#=A, L#=<F, F#<M,
         sync(t(B,C,D,E),O,H,I,J,K,L,F,0).                       % replacement
sync(t(A,F,t(B,C,D,E),t(G,H,I,J)),K,[F|L],M,[A|N],O,P,Q,R) :-
         K#>=3, S#=K-1, T#>0, U#>0, A+B#>0, A+G#>0, V#>=0,
         R#=V+A, P#=<F, F#<Q, W#=F+1, fdsum(S,T,U),
         sync(t(B,C,D,E),T,L,X,N,Y,P,F,V),                       % replacement
         sync(t(G,H,I,J),U,X,M,Y,O,W,Q,V).                       % replacement
% ---END CODE FROM SENNI ET AL. FOR RBTS---

benchmarkSenniRB(Size) :-
    rbtree(_, Size),
    fail.
benchmarkSenniRB(_).

benchmarkThing(kyle_bst, Size) :-
    benchmarkBST(Size).
benchmarkThing(senni_bst, Size) :-
    benchmarkSenniBST(Size).
benchmarkThing(kyle_rb, Size) :-
    benchmarkRB(Size).
benchmarkThing(senni_rb, Size) :-
    benchmarkSenniRB(Size).
benchmarkThing(kyle_heap, Size) :-
    benchmarkHeap(Size).
benchmarkThing(kyle_btree, Size) :-
    benchmarkBTree(Size).
benchmarkThing(kyle_riff, Size) :-
    benchmarkRiff(Size).
benchmarkThing(kyle_java, Size) :-
    benchmarkJava(Size).

main :-
    argument_value(1, Thing),
    argument_value(2, SizeAtom),
    number_atom(Size, SizeAtom),
    benchmarkThing(Thing, Size).

:- initialization(main).
