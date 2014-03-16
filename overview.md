# Construct #

Construct is a probabilistic programming language.
In it we can specify probabilstic models and perform inference.
Construct is based on abstract and approximate interpretation, pattern matching, and decision making.

## Basic Idea ##
There are a number of key ideas in construct

- Evaluation of the program is abstract - instead of computing with we compute with approximations of values. Consider the Clojure code:
```Clojure
(defn make-a-2d-path [n]
  (repeatedly n #(vector (rand)(rand))))
```
What does (make-a-2d-path 5) evaluate to? In Clojure it would evaluate to some _concrete_ path, a nested list such as
```
([0.652 0.815] [0.545 0.416] [0.852 0.263] [0.562 0.734])
```

- Approximate interpretation involves making decisions.  The act of interpretation is separated from the decision making involved.
Consider a complex distribution

- This separation is facilitated by formulating interpretation as pattern-matching and transformation.

### Evaluation as pattern matching
Evaluation of a program is a series of transformations.
In particular, we search for a pattern in a program, if the pattern matches, we may replace that pattern with a transformation.

## Objectives ##
Where do I want to be: I want to be at the point where I have a problem.
I want to be at the point where I have some cool examples.
This means implementing the itnerpreter as rewrite rules
Implementing some subet of the standard library
Implementing the abstractions as rewrite rules
Implementing the examples
Implementing the decision making process.


### Subgoal: Evaluate normal clojure with rewrite rules ###
Here, a program is _executed_ by applying a series of transformations - it is transformed from the source code to a value.
There are many ways to interpret a lisp program, but they can all be viewed as transformations to equivalent programs, i.e. tree rewrites.
Typically an eager interpreter will do a depth first traversal of the tree, applying transformations recursively.
A lazy interpreter will take a different route

When interpreting approximately, there are## many choices made.
Unlike a purely functional lisp, these choices will result in different values, i.e. different approximations.
Hence I deemed it useful to separate the _act_ of interpreting from the _decision making_.
The model of computation used for these transformations is based on pattern matching and term-rewriting.

__Subgoal Status:__

There's a bug with the rewrite rules that needs fixing, I need to figure out how to handle if.  That should affect variable binding.  In general
```Clojure
dbg: (transform p1__265#) = (+ 3 ((fn [coll] (/ (reduce + coll) (count coll))) [1 2 a]))
dbg: (transform p1__265#) = (+ 3 (/ (reduce + [1 2 a]) (count [1 2 a])))
dbg: (transform p1__265#) = (+ 3 (/ a (count [1 2 a])))
dbg: (transform p1__265#) = (+ 3 (/ a 3))
)
```
Why is reduce + [1 2 a] getting reduced to a. That should throw an error.


__TODO rewrite rules:__

- Rewrite rules, figure out the orderings
- Handle let
- Handle if
- Handle and
- Handle loop/recur

With an if we only want to evaluate the consequent before we do anything
in any of the branches.
-- How to enforce this
--- a) in the iterators, e.g. if subtree iterator hits an if
(+ 3 2 (if (> x 2) 'alpha 'beta))
--- b) in the condition

__TODO examples:__

- Example non-linear planning
- Examples for 2D inverse graphics
- Mesh generation

__TODO thinking:
__
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

__TODO Implement:__
- Abstracting a random primitive
- Consistency checking and elimination
- Redundancy checking and elimination 
- Domain conversion, interval <-> convex polyhedra
- Refinement

### Subgoal: Implement Simple Decision Making Interpreter ###
One motivation for separating the act of interpreting from the decision making was so that we could learn good decisions.
This problem could be framed in a number of different ways