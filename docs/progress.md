# Progress

Open questions are roughly divided between conceptual/formalism, and technical.
Techncial Questions are:

## Technical Questions ##
__What is allowed in pred? of condition?__
Condition is done with a predicate `pred?`.  The role of `pred?` is to restrict the random variable to values which pass it.  More precisely, `pred?` describes a subset of the domain of the random variable, and we restrict the probability measure to be within that range.
```Clojure
(condition dist pred) 
```

But how, in terms of code, should we allow access to elements of the random variable.

- Option 1: Only allow access to the independent variable.
e.g.
```Clojure
(condition a-dist #(pos? %))
```
Then, the independent value of a variable within pred could be just the input to it as a function.  This would be useful for interop with existing code.

- Option 2: Specifically reference which variable of the rv one wishes to refer to.
```Clojure
(condition a-dist #(and (pos? (indep-var %) (neg? (first (dep-var %))))))
```
Advantages of this method are that I don't need to do  any explicit tracking, or anything fancy with variable names (as in the next example).  Problem is that it is verbose, it's not clear how one would select which independent variable one wants without keeping track of an ordering.

- Option 3: Allow access to dependent vars but only through bound variable names.
```Clojure
(let [x (uniform -1 1)
      y (+ x 3)]
  (condition y #(or (> 2 %) (neg? x))))
```
These seems most natural, and closer to normal semantics.
But it also seems a little bit confused.

__How to correctly apply functions to (in)dependent and conditionally dependent variables?__
Consider the following examples:
```Clojure
(let [x (rand)
      y (+ x x)]
  y)
```

```Clojure
(let [x0 (rand)
      x1 (rand)
      y (+ x0 x1)]
  y)
```

If this was a normal Clojure program, `y` would be sampling from different distributions in these two cases.  In the first case `y` would be sampled from a uniform on `[0 1]`, whereas in the second `y` is being sampled from the addition of two uniform distribution, which is a *triangular distribution*.

It seems like we need some more informationn in the semantics of Sigma to account for the difference here.  Something to capture the notion of two distributions being the same or not.  Otherwise both of these cases are identical. If we think of (rand) as purely evaluating to a value, then they are identical.  Suppose instead (rand) evaluated to the integer 7, there would be no difference.

Consider another example:
```Clojure
(let [x (rand)]
  (+ x (condtion x (fn [_] true))))
```
Here we've applied a ineffective condition on a random variable, and added it to the original random variable.
Should the result here be a triangular distribution or a uniform distribution?
Thinking in sampling terms may help but it doesn't quite fit: if condition took as input a sampler a produced a sampler, e.g.

```Clojure
(let [x rand] ; Note: unevaluated
  (+ (x) ((condtion x (fn [_] true)))))
```
We would have a triangular distribution, as we are sampling twice.


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

__What is a valid abstraction, and what is meant by valid?__
If our end goal is to draw exact samples, then what is a valid abstraction.
If the case where the prior distribution was a 

__How to abstract non-uniform distributions?__
No thoughts yet

__Cousot says Markov chains can be described in this framework, does this mean Church can?__
No thoughts yet

__Should Sigma rely on Clojure, or be fully self hosted.__
Clojure implements Sigma rules, and are also callable from them.
The first of these is necessary, any new language must initially be implemented in another. The latter, perhaps not.

## Conceptual Questions
__What are the semantics of values in a Sigma program?__
No coherent thoughts yet.

*What are the semantics of condition on a predicate with noise?*
No coherent thoughts yet.

__Should I delineate between the pattern matching and the purely functional language?__
No coherent thoughts yet.

__What does soundness really mean in this context?__
No coherent thoughts yet.

__What precisely is the relationship between the pattern matching and the probabilistic interpretation?__
No coherent thoughts yet.

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

__How to parameterise choices, and separate real choices from any old rule__
I have put the abstract interpretation choices on the same level as the normal evaluation choices.  If I frame the interpretation as a decision process where a rule is applied as an action to yield a new program, I will surely have a larger than necessary action space.  Is there a way to avoid making decision about irrelevant options, and focus only on the ones that matter (i.e. the approximations)


## Primary Objectives ##
- Complete implementation of Sigma
- Implementing the decision making process.
- Demonstrate with physical reasoning problem

### Subgoal: Evaluate normal clojure with rewrite rules ###
Here, a program is _executed_ by applying a series of transformations - it is transformed from the source code to a value.

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
- Make all rewrite rules order-independent
- Handle and
- Handle loop/recur

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

__Subgoal Status__:
I have begun an implementation of discrete domains on the integers.
I need to decide what the main operations I shall support for all random variables should be: 




__TODO Implement:__
- Abstracting a random primitive
- Consistency checking and elimination
- Redundancy checking and elimination 
- Domain conversion, interval <-> convex polyhedra
- Refinement

### Subgoal: Implement Simple Decision Making Interpreter ###
One motivation for separating the act of interpreting from the decision making was so that we could learn good decisions.
This problem could be framed in a number of different ways