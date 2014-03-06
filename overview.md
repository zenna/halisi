# Construct #

Construct is a probabilistic programming language.
The goals are to
- Allow people to specify probabilistic models using a general programming language
- Abstract out and/or automate inference algorithms

## Outline ##

## Objectives ##

Objectives:
- Implement as a standalone interpreter

### Subgoal: Completely evaluate normal clojure with rewrite rules ###
Here, a program is _executed_ by applying a series of transformations - it is transformed from the source code to a value.
There are many ways to interpret a lisp program, but they can all be viewed as transformations to equivalent programs, i.e. tree rewrites.
Typically an eager interpreter will do a depth first traversal of the tree, applying transformations recursively.
A lazy interpreter will take a different route

When interpreting approximately, there are many choices made.
Unlike a purely functional lisp, these choices will result in different values, i.e. different approximations.
Hence I deemed it useful to separate the _act_ of interpreting from the _decision making_.
The model of computation used for these transformations is based on pattern matching and term-rewriting.

TODO rewrite rules:
- Rewrite rules, figure out the orderings
- Handle let
- Handle if
- Handle and
- Handle loop/recur

TODO examples:
- Example non-linear planning
- Examples for 2D inverse graphics
- Mesh generation

TODO thinking:
- Figure out the easiest way to test non trivial examples, which call standard library function etc. Obvious options 1) Write a full interpreter, problem with this is that I would have to deal with namespaces and macros etc 2) Some kind of symbolic execution, override all primitives to handle symbolic values
- Abstractions for discrete data structures
- Non-uniform distributions

Questions:
- I have put the abstract interpretation choices on the same level as the normal evaluation choices.  If I frame the interpretation as a decision process where a rule is applied as an action to yield a new program, I will surely have a larger than necessary action space.  Is there a way to avoid making decision about irrelevant options, and focus only on the ones that matter (i.e. the approximations)

### Subgoal: Implement abstract operations as rewrite rules ###
As stated above the goal is to phrase the abstract operations as rewrite rules.
I may discover when I get here, that my language for rewriting is not expressive enough, which is fine, I will extend it.

The main abstract operations I have thus far considered are:
- Abstracting a random primitive, e.g. (rand 0 10) -> [0 10]
- Consistency checking: (and a b c d), if we know any one of these are false we can save time.
- Redundancy checking
- Domain conversion, e.g. set-of-boxes -> convex-hull(set-of-boxes)
- Refinement - using analytical or numerical methods to get a better approximation
- Approximating Implicit Growth

TODO Implement:
- Abstracting a random primitive
- Consistency checking and elimination
- Redundancy checking and elimination 
- Domain conversion, interval <-> convex polyhedra
- Refinement

### Subgoal: Implement Simple Decision Making Interpreter ###
One motivation for separating the act of interpreting from the decision making was so that we could learn good decisions.
This problem could be framed in a number of different ways