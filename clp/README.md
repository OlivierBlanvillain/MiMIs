# Compiling and Running Benchmarks #

### Step 1 ###

Install **both** [SWI Prolog](http://www.swi-prolog.org/) and [GNU Prolog](http://www.gprolog.org/).
Binary packages are usually available for both (I can confirm both on Ubuntu and Debian).
SWI Prolog is used to compile the benchmarks (written in [Typed-Prolog](https://github.com/kyledewey/typed-prolog)) to regular Prolog code (specifically, in `output.pl`).
This regular Prolog code is then compiled to machine code via GNU Prolog (producing `output`).

### Step 2 ###

Run `make`:

```console
make
/usr/bin/ld: Warning: alignment 8 of symbol `init_stream_supp' in /usr/lib/gprolog-iso/libbips_pl.a(stream_supp.o) is smaller than 16 in /usr/lib/gprolog-iso/libengine_pl.a(engine.o)
/usr/bin/ld: Warning: alignment 8 of symbol `fd_reset_solver' in /usr/lib/gprolog-iso/libengine_fd.a(fd_inst.o) is smaller than 16 in /usr/lib/gprolog-iso/libengine_pl.a(if_no_fd.o)
```

The warnings are normal; this is probably something not setup quite right.

### Step 3 ###

Run executable, along with benchmarks of desire:

```console
./output
GNU Prolog 1.3.0
By Daniel Diaz
Copyright (C) 1999-2007 Daniel Diaz
| ?- benchmarkBST(11).

(52 ms) yes
| ?- 
```

There are multiple benchmarks available, and all are of interest.
These are listed below:

- `benchmarkBST`: binary search trees
- `benchmarkRB`: red/black trees
- `benchmarkHeap`: heaps
- `benchmarkBTree`: B-trees
- `benchmarkRiff`: Riff images

Each takes a single integer parameter, representing a bound to use.
Generally, the larger the integer the bigger the problem.

GNU Prolog shows how long a query takes, as long as the query takes more than `1 ms`.
I'm not sure how to see memory usage information.

