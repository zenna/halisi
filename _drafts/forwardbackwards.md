---
layout: classic
title:  "Going Forward And Backwards"
date:   2014-03-31 20:30:18
categories: inference
---
# Going forwards and backwards

When probabilistic models are programs, inference entails running them backwards to find sets of inputs which would *cause* an observed output.
When a program is a viewed as a function, this can is formalised as a pre-image computation.

Applying a function $$f:X \to Y$$, to each element of any $$X' \subseteq X$$ results in subset of $$Y$$: the *image* of $$X'$$ under $$f$$.
For any $$f$$, we can define an $$f^\rightarrow$$, which computes images:

<!-- The image of a function $$f:X \to Y$$, is the set of values it maps some input *set* onto:
 --><!-- \text{ where } X = X_1 \times \cdots \times X_n$$ -->

$$
f^\rightarrow:\mathcal{P}(X)\rightarrow\mathcal{P}(Y) \text{  with  } f^\rightarrow(A) = \{ f(a)\;|\; a \in A\}
$$

The inverse image, or pre-image is its inverse:

$$
f^\leftarrow:\mathcal{P}(Y)\rightarrow\mathcal{P}(X) \text{  with  } f^\leftarrow(B) = \{ a \in X \;|\; f(a) \in B\}
$$

Probabilistic programs are simply transformations of the sample space - $$X:\Omega \to E$$, i.e. random variables.
Probability queries involve computing the pre-image of a boolean valued random variable ($$X:\Omega \to \{True, False\}$$), under the codomain singleton set $$\{True\}$$, then measuring it.


$$
P:X \to \{True, False\} \to \mathbb{R}
$$

$$
P(X) = \mathbb{P}(X^\leftarrow(\{True\}))
$$

We shall discuss a number of ways of computing pre-images.  Rejection samplers, such as that found in simple implementations of Church sample from the pre-image by rejecting all those not in the target.


## Pre-images by abstract interpretation
So far, I am doing computation with interval arithmetic.  The idea is to compute preimages by computing images and seeing which coincide with our observation. The question then is how to compute images. Well then we need to compute with sets of values.  When our numbers are real values we can compute with intervals. but not all sets on the real line are intervals.  Consider two cases, when the function is unary, we can compose any subset from jsut intervals. When the function is not unary, then we n.

Non relational computation.
There's a kind of mismatch that only becomes apparent when we compute with sets of values.  A function f(x,y) is a function from the product space $$X \times Y$$, yet when we express $$X$$ and $$Y$$ as programs, we deal with individual variables x and y. How then, can we evaluate $$f$$ as a program un an arbitrary subset of the cartesian product, since it should be well defined.

The problem is when we have a function $$f$$ defined in terms of operations on its arguments, how do we define $$f^\rightarrow$$ in these terms, if at all.
Concretely, suppose

{% highlight clojure %}
(defn f
  (* 2 (+ x y)))
{% endhighlight %} 

What are x and y in in a f->.

In an imperative program we're looking for constraints on our input variables which would cause the constraint to be true.

There are really two questions to consider, somewhat orthogonal.
The first is what abstract domain we compute forward with.
When we use rectangles things are simple, but imprecise.
Different domains could be more precise.

The second question is when we reach our final constraint, are we looking for constraints on our random variables, or are we just seeking to evaluate g.

## Pre-images by forward computation and refinement

Here, we compute pre-images by finding sets whose image intersects with $$Y$$.

For any subset $$A \subseteq X^n$$ we compute:

$$
g(A) = \left\{
  \begin{array}{lr}
    a & : f^\rightarrow(A) \subseteq Y\\
    b & : f^\rightarrow(A) \subseteq Y \ge 0
  \end{array}
\right.
$$

When $$g(A) = a$$ we have found a valid subset of the preimage, and nothing more need be done with it.  When $$g(A) = b$$ this subset is entirely not within the preimage and it can be discarded.
When $$g(A) = c$$, there is a partial overlap, to improve our approximation we may split A and repeat this process on each subset.

### Testing intersection - computing $$g(A)$$

There are two main approaches to computing g(A).  The first is to perform an abstract interpretation, that is, evaluate our program with sets of values.
What are the constraints

## Pre-images by Approximating functions and propagating backwards

The idea is to compute pre-images by:
- Starting with some set of inputs
- At each "step" approximating the function from one set of values to another
- Intersecting $$y$$ with

- Approximate multi-variable functions
- Intersect those approximations with subsets(intervals)

## Pre-images by Computing Preimages directly

## Pre-images by Edging forward eagerly
In this approach we will approximate random variables explicitly.

A random variable is represented as a subset of a space of the form:

$$P \times V \times A_1 \times \cdot \times A_n$$

A value $V
Y = f(a,b,c). You would say A is an argument

There is an axis $$A_i$$ for every random variable it is a function of.

We construct

