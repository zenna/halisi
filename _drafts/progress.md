---
layout: post
title:  "Progress"
date:   2014-03-31 20:30:18
categories: progress
---
<script type="text/javascript" src="http://cdn.mathjax.org/mathjax/latest/MathJax.js?config=TeX-AMS-MML_HTMLorMML"></script>
###What is the motivation for the semantic domain G?

The semantic domain G maps terms onto objects onto elements of $$(X, \nu)$$

$$G : \mathcal{R}_{\sigma} \to $$

It:

- Does not represent $$\Omega$$ explicitly, instead it represents the set dual of the distribution function of a random variable $$P_X$$.
- With $$S$$ the independence of two random variables is a propety determined only by checking whether all subsets of their events are indepedent.
- Here we encode independence constraints from the program into the values themselves
- Maps onto the notion of a density 

The purpose in defining it is:
- Avoid representations of $$\Omega$$

### If X = f(X_1, X_2, .., X_n) is a function of multiple random variables, what can we say about X?

### What are the primitive operations of Sigma

### How to go backwards from the condition

### How to sample

## Approximation

### What is the relationship between the approximating semantics and the probabilistic semantics


## Pattern matching

__Should Sigma rely on Clojure, or be fully self hosted.__

Clojure implements Sigma rules, and are also callable from them.
The first of these is necessary, any new language must initially be implemented in another. The latter, perhaps not.


__What is allowed in pred? of condition?__

Bool or RV Bool


__Should I delineate between the pattern matching and the purely functional language?__

No coherent thoughts yet.

__What does soundness really mean in this context?__

No coherent thoughts yet.

__What precisely is the relationship between the pattern matching and the probabilistic interpretation?__

No coherent thoughts yet.


__What are the semantics of evaluating a random primitive, and functions on these values?__
One thing Sigma must be able to do is apply functions to values which may be dependent.
Fortunately, dependencies in a functional language are explicit, and need not be inferred; a value has the potential to be dependent on another value if it is a function of that value.
I use the term 'possibly' because some dependencies may be meaningless,
For instance: `(def x ((fn [_] x) y))` suggests `x` is a function of `y`, but in reality it is not - i.e. even though there exists a syntactic functional relationship, semantically there is none.

When applying a function to discrete distributions with dependencies I noticed the following: it seems like we needed some way to give identities to distributions.
Consider the program
```Clojure
(let [x (uniform 0 1)
      y (uniform 0 1)
      a (+ x y)
      b (* x y)]
  (- a b))
```

| x |  p  |
|---|-----|
| 0 | 0.5 |
| 1 | 0.5 |

`y` has an identical table to `x`, whereas `a` and `b` are dependent on `x` and `y`:

| `x` | `y` | `a` |  P   |
|-----|-----|-----|------|
|   0 |   0 |   0 | 0.25 |
|   0 |   1 |   1 | 0.25 |
|   1 |   0 |   1 | 0.25 |
|   1 |   1 |   2 | 0.25 |

| `x` | `y` | `b` |  P   |
|-----|-----|-----|------|
|   0 |   0 |   0 | 0.25 |
|   0 |   1 |   0 | 0.25 |
|   1 |   0 |   0 | 0.25 |
|   1 |   1 |   1 | 0.25 |

The output of this function `(- a b)` is of course dependent on `a` and `b`, which are in turn dependent on `x` and `y`:

| `x` | `y` | `b` | `a` | `(- a b)` |  P   |
|-----|-----|-----|-----|-----------|------|
|   0 |   0 |   0 |   0 |         0 | 0.25 |
|   0 |   1 |   0 |   1 |         1 | 0.25 |
|   1 |   0 |   0 |   1 |         1 | 0.25 |
|   1 |   1 |   1 |   2 |         1 | 0.25 |

Note the difference between when I evaluated `(+ x y)` and `(* x y)` compared to `(- a b)`.  In the former case the number of rows in the table doubled, whereas in the latter case no rows were added.
Intuitively in the former case I need to add more rows to account for all the possible ways `x` could interact with `y`, whereas in the latter case, all these possible interactions are already accounted for.
But here's the problem, if `x`

But formally, why is this? How do I know they share the same dependent variables? Why is this the correct thing to do? What if they only share some of the same variables?

An analogy in mathematical notation might be something like
let $$X = \mathcal{U}(0, 1)$$ and $$Y= \mathcal{U}(0,1)$$ be two standard uniform distributions, the sum $X+Y$ is ...
evaluating a random primitive e.g. `(uniform 0 1)`, seemed to require that we give it a unique identifier,

What does `(uniform-int)` mean?  Previously I defined the semantics of the evaluation of a random primitive as a random variable.
But this conspicuously leaves the meaning of an unevaluated random primitive undefined, and brings about the following conceptual problems:

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

It seems like we need some more information in the semantics of Sigma to account for the difference here.  Something to capture the notion of two distributions being the same or not.  Otherwise both of these cases are identical. If we think of (rand) as purely evaluating to a value, then they are identical.  Suppose instead (rand) evaluated to the integer 7, there would be no difference.

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

I think part of the confusion stems from having this mis-mash of a purely functional language and pattern matching.
If `rand` is a function it should take some argument.
Actually for clarity I should stop using the notation `rand`, it is poor naming because 1) it does not define the distribution 2) it appeals to the idea of randomness, none of which exists in its semantics.
So instead we lets say `uniform-int`, and in a purely functional setting we might say `(uniform omega)`.
Looking back at the previous two examples:
```Clojure
(let [x (uniform omega)
      y (+ x x)]
  y)
```

```Clojure
(let [x0 (uniform omega)
      x1 (uniform omega)
      y (+ x0 x1)]
  y)
```

__*What are the semantics of condition on a predicate with noise?*__

No coherent thoughts yet.

__How to parameterise choices, and separate real choices from any old rule__
I have put the abstract interpretation choices on the same level as the normal evaluation choices.  If I frame the interpretation as a decision process where a rule is applied as an action to yield a new program, I will surely have a larger than necessary action space.  Is there a way to avoid making decision about irrelevant options, and focus only on the ones that matter (i.e. the approximations)

__Discrete Example__

{% highlight clojure %}
(def A (flip))
(def B (flip))
(def B (flip))
(def C (+ A B))
(def D (- B A))
(def E (+ D (flip)))
{% endhighlight %}

  *A*

  | A |  P  |
  |===|=====|
  | 0 | 0.5 |
  | 1 | 0.5 |

  - Demonstrates: a single random variable with no dependents

  *B*

  | B |  P  |
  |===|=====|
  | 0 | 0.5 |
  | 1 | 0.5 |

  - Demonstrates: a single random variable with no dependents

  *C*

  | A | B | C |  P   |
  |===|===|===|======|
  | 0 | 0 | 0 | 0.25 |
  | 0 | 1 | 1 | 0.25 |
  | 1 | 0 | 1 | 0.25 |
  | 1 | 1 | 2 | 0.25 |

  - Demonstrates: a function of two independent random variables
  - Found joint by doing cross product of A and B and
  - multiplying probabilities

{% highlight clojure %}
(def X (flip 0.7))
(def Y (flip 0.4))
(def Z (* X Y))
(+ Z C)
{% endhighlight %}

  | X | Y | Z |  P   |
  |===|===|===|======|
  | 0 | 0 | 0 | 0.18 |
  | 0 | 1 | 1 | 0.12 |
  | 1 | 0 | 1 | 0.42 |
  | 1 | 1 | 2 | 0.28 |  

  | X | Y | Z | A | B | C |   P   |
  |===|===|===|===|===|===|=======|
  | 0 | 0 | 0 | 0 | 0 | 0 | 0.045 |
  | 0 | 1 | 1 | 0 | 0 | 0 | 0.045 |
  | 1 | 0 | 1 | 0 | 0 | 0 | 0.045 |
  | 1 | 1 | 2 | 0 | 0 | 0 | 0.045 |
  | 0 | 0 | 0 | 0 | 1 | 1 |  0.03 |
  | 0 | 1 | 1 | 0 | 1 | 1 |  0.03 |
  | 1 | 0 | 1 | 0 | 1 | 1 |  0.03 |
  | 1 | 1 | 2 | 0 | 1 | 1 |  0.03 |
  | 0 | 0 | 0 | 1 | 0 | 1 | 0.105 |
  | 0 | 1 | 1 | 1 | 0 | 1 | 0.105 |
  | 1 | 0 | 1 | 1 | 0 | 1 | 0.105 |
  | 1 | 1 | 2 | 1 | 0 | 1 | 0.105 |
  | 0 | 0 | 0 | 1 | 1 | 2 |  0.07 |
  | 0 | 1 | 1 | 1 | 1 | 2 |  0.07 |
  | 1 | 0 | 1 | 1 | 1 | 2 |  0.07 |
  | 1 | 1 | 2 | 1 | 1 | 2 |  0.07 |

- Its not the case that we have to find the cartesian product for every new varible
- For each random variable find the non-shared variables and find the cross product of these *sets*

  *D*

  | A | B | D  |  P   |
  |===|===|====|======|
  | 0 | 0 |  0 | 0.25 |
  | 0 | 1 |  1 | 0.25 |
  | 1 | 0 | -1 | 0.25 |
  | 1 | 1 |  0 | 0.25 |

(/ D C)

  | A | B | D  | C | (/ D C) |  P   |
  |===|===|====|===|=========|======|
  | 0 | 0 |  0 | 0 | undef   | 0.25 |
  | 0 | 1 |  1 | 1 | 1       | 0.25 |
  | 1 | 0 | -1 | 1 | -1      | 0.25 |
  | 1 | 1 |  0 | 2 | 0       | 0.25 |

  - The number of rows did not grow.  This is because all the dependent variables were shared

  
  *E (+ D (flip))*

  | A | B | D  | flip | E  |   P   |
  |===|===|====|======|====|=======|
  | 0 | 0 |  0 |    0 |  0 | 0.125 |
  | 0 | 0 |  0 |    1 |  0 | 0.125 |
  | 0 | 1 |  1 |    0 |  1 | 0.125 |
  | 0 | 1 |  1 |    1 |  2 | 0.125 |
  | 1 | 0 | -1 |    0 | -1 | 0.125 |
  | 1 | 0 | -1 |    1 |  0 | 0.125 |
  | 1 | 1 |  0 |    0 |  0 | 0.125 |
  | 1 | 1 |  0 |    1 |  1 | 0.125 |

  - New variable not in list, only had independent vvariable
  - found cross product with existing table

(* E A)

  | A | B | D  | flip | E  | (* E A) |   P   |
  |===|===|====|======|====|=========|=======|
  | 0 | 0 |  0 |    0 |  0 |       0 | 0.125 |
  | 0 | 0 |  0 |    1 |  0 |       0 | 0.125 |
  | 0 | 1 |  1 |    0 |  1 |       0 | 0.125 |
  | 0 | 1 |  1 |    1 |  2 |       0 | 0.125 |
  | 1 | 0 | -1 |    0 | -1 |      -1 | 0.125 |
  | 1 | 0 | -1 |    1 |  0 |       0 | 0.125 |
  | 1 | 1 |  0 |    0 |  0 |       0 | 0.125 |
  | 1 | 1 |  0 |    1 |  1 |       1 | 0.125 |

- All variables were in table already, did not need
- to add any more columns, just more rows

(* E (double A))

  | A | B | D  | flip | E  | (double A) | (* E (double A)) |   P   |
  |===|===|====|======|====|============|==================|=======|
  | 0 | 0 |  0 |    0 |  0 |          0 |                0 | 0.125 |
  | 0 | 0 |  0 |    1 |  0 |          0 |                0 | 0.125 |
  | 0 | 1 |  1 |    0 |  1 |          0 |                0 | 0.125 |
  | 0 | 1 |  1 |    1 |  2 |          0 |                0 | 0.125 |
  | 1 | 0 | -1 |    0 | -1 |          2 |               -2 | 0.125 |
  | 1 | 0 | -1 |    1 |  0 |          2 |                0 | 0.125 |
  | 1 | 1 |  0 |    0 |  0 |          2 |                0 | 0.125 |
  | 1 | 1 |  0 |    1 |  1 |          2 |                2 | 0.125 |

- (+ D (flip 0.9))

  | D | (flip 0.9) | (/ D (flip 0.9)) |  P   |
  |===|============|==================|======|
  | 0 |          0 |                ⊥ | 0.05 |
  | 0 |          1 |                0 | 0.45 |
  | 1 |          0 |                ⊥ | 0.05 |
  | 1 |          1 |                1 | 0.45 |

(+ (* E (double A))
   (+ D (flip 0.9)))

  | A | B | D  | flip | E  | (double A) | (* E (dbl A)) | flip 0.9 | /D | P |
  |===|===|====|======|====|============|===============|==========|====|===|
  | 0 | 0 |  0 |    0 |  0 |          0 |             0 |        0 | ⊥  |   |
  | 0 | 0 |  0 |    1 |  0 |          0 |             0 |        0 | ⊥  |   |
  | 0 | 1 |  1 |    0 |  1 |          0 |             0 |        0 | ⊥  |   |
  | 0 | 1 |  1 |    1 |  2 |          0 |             0 |        0 | ⊥  |   |
  | 1 | 0 | -1 |    0 | -1 |          2 |            -2 |        0 | ⊥  |   |
  | 1 | 0 | -1 |    1 |  0 |          2 |             0 |        0 | ⊥  |   |
  | 1 | 1 |  0 |    0 |  0 |          2 |             0 |        0 | ⊥  |   |
  | 1 | 1 |  0 |    1 |  1 |          2 |             2 |        0 | ⊥  |   |
  | 0 | 0 |  0 |    0 |  0 |          0 |             0 |        1 | 0  |   |
  | 0 | 0 |  0 |    1 |  0 |          0 |             0 |        1 | 0  |   |
  | 0 | 1 |  1 |    0 |  1 |          0 |             0 |        1 | 1  |   |
  | 0 | 1 |  1 |    1 |  2 |          0 |             0 |        1 | 1  |   |
  | 1 | 0 | -1 |    0 | -1 |          2 |            -2 |        1 | -1 |   |
  | 1 | 0 | -1 |    1 |  0 |          2 |             0 |        1 | -1 |   |
  | 1 | 1 |  0 |    0 |  0 |          2 |             0 |        1 | 0  |   |
  | 1 | 1 |  0 |    1 |  1 |          2 |             2 |        1 | 0  |   |

-- Algorithm
- Every random primitive has a name

{% highlight python %}
# Given a set of random variables and a variable name
# return the row of values of that variable name
def values(var-name rvs)

def apply-binary-f(f, rv-a, rv-b):
    # Find variable names which overlap
    same = intersection(var-names(rv-a), var-names(rv-b))
    diff = symmetric-difference(var-names(rv-a), var-names(rv-b))

    # Get all the values for all the diff variables
    all-values = map(values, diff)

    # Find their cartesian product
    product = cart-product(values))

{% endhighlight %}

(def x
  (let [a (normal 10 1)
        b (normal a )]
    (condition a #(= b 4))))

(sample x 1000)