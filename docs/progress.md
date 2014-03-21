# Progress

Open questions are roughly divided between conceptual/formalism, and technical.
Techncial Questions are:
## Technical Questions ##

__What is required to have a universal, albeit likely slow, language__
- Define abstract domains for all random primitives
It's likely everything can be derived using flip, or rand.  For convenience let's try to cover at least the following three random primitives.
rand::[] -> Real
flip::[] -> Bool
rand-int::[] -> Integer

Sigma programs are functions and special forms.
Primitive functions: +, -, *, /, =, >, <
Logical functions: and, or, not, if
numerical conversion: ceil, floor, double
list operations: head, tail

We need to lift the functions to a higher domain.

- Lift primitive functions
- Ensure that all functions are lifted for range of lifted function

__Should Sigma rely on Clojure, or be fully self hosted.__
Clojure implements Sigma rules, and are also callable from them.
The first of these is necessary, any new language must initially be implemented in another. The latter, perhaps not.

__How to abstract non-uniform distributions?__

__Cousot says Markov chains can be described in this framework, does this mean Church can?__

## Conceptual Questions
- What are the semantics of values in a Sigma program
- Should I delineate between the pattern matching and the purely functional language?
- What does soundness really mean in this context?
- What precisely is the relationship between the pattern matching and the probabilistic interpretation

__What is a Sigma program in general.__
The measure theoretic definition I gave in the UAI paper claims a Sigma program is a random variable, i.e. a function from some sample space to a value.
Why is a sigma program a random variable - Well its purely functional it just maps some input to some output.  That's what a sigma function does.  But is that what a sigma program.  Just thinking of a Sigma function for hte moment.  Is that a random vaiable?  

The idea is that say you have S[(+ 3 4)] = 7. The real integer 7.  Perhaps ironically the formal definition of  an integer is in terms of syntax.]
What are the probabilistic semantics of S_p[(+ 3 9)].
You might argue that given that the entire sample space maps onto 4.
i.e. if my sample space is a coin heads -> 4, tails -> 4.
So in this sense a program, is a random variable defined on some probability space.
That makes sense I suppose.
This is a very accurate desciription of what something like (+ 1 (rand)) is.
But what of a program that is just a definition.
e.g. (defn [a] b)

## Primary Objectives ##

- 
- Implementing the decision making process.
- Demonstrate with physical reasoning problem

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