---
layout: classic
title:  "Semantics"
date:   2014-03-31 20:30:18
categories: Semantics
---
<script type="text/javascript" src="http://cdn.mathjax.org/mathjax/latest/MathJax.js?config=TeX-AMS-MML_HTMLorMML"></script>

Programming languages are specified by defining syntax and semantics.

#Sigma Semantics

A denotational semantics is defined by a deterministic semantic function from language terms to values in another meta-language.
The deterministic semantics of Sigma are identical to a subset of Clojure, so we shall focus on the probabilistic semantics.
We will describe two semantics which map exactly onto objects of measure-theoretic probability.
From these we shall construct an approximate semantics, which is what the  Sigma interpreter implements to perform inference.

We can describe Sigma semantics with an example.
First, we set up a model of four variables `a b c d e`:

{% highlight clojure %}
(def a (uniform 'a 0 3))
(def b (uniform 'b 0 3))

(def c (* a a))
(def d (- a 10))
(def e (* a b))
{% endhighlight %}

In Sigma there is a single, global, probability space.
To evaluate a random primitive means to define a random variable on that space.
Hence, `(uniform 'a 0 3)` returns a random variable uniformly distributed over $$[0, 3]$$.
The first argument `a`, its *identifier*, differentiates the random variable from others.
Specifically it ensures that it is independent of other random variables which neither share the same identifier, nor are functionally related.
This property is implicit in conventional mathematical notation, for instance it is implied that $$X \sim \ \mathcal{U}(0,3)$$ is independent of $$Y \sim \mathcal{U}(0,3)$$.
Furthermore, probabilistic languages with sampling semantics circumvent the problem by positing a i.i.d sequence of random bre ts.
Since `uniform` is a pure function which evaluates to a random variable, to yield different values it must take different arguments.
More formally then:

$$S[\text{(uniform 'a 0 3)}] = f : \Omega \to \mathbb{R}$$

Under the constraint:

$$\forall e_i, e_j.id(e_i) \neq id(e_j) \implies S[e_i] \bot S[e_j]$$

A random variable is a measurable function $$f : \Omega \to E$$.
It is said to be *defined on* some probability space $$(\Omega, \mathcal{F}, \mathbb{P})$$.
Here $$\mathcal{F} \subseteq \mathcal{P}(\Omega)$$ is a $$\sigma$$ algebra, and $$\mathbb{P}$$ is a probability measure.

## Geometric Semantics
The primary operations Sigma allows us to perform - applying functions to, conditioning, computing expectations of, and sampling from distributions - require efficient representations, and means of determining independence.
The measure theoretic semantic function $$S$$ is often intractable or not computable.  For example we can not explicitly represent the probability of all subsets of a infinite $$\Omega$$.  Even in finite $$\mathcal{F} = \mathcal{P}(\Omega)$$ will become intractably large for non-trivial problems.  Moreover, determining independence is a numerical calculation, whereas many independence properties can be derived from the program structure.

We will define a second semantics $$G$$ which will still be exact, incomputable, but will lend it self to support in defining more approximate, efficient representations.
In defining $$G$$ we will construct from $$S$$ a new type of object, and a measure of that object.
We'll represent a random variable $$X$$ as a tuple of __named conditional probability distributions__ $$\{X_1, X_2, ..., X_n\}$$, where each $$X_i$$ is a subset of a euclidean $$\mathbb{R}^n$$ *conditional product space* of the form:

$$F \times I \times D^1 \times \cdots \times D^n$$

Under the constraint that $F$ values are non-negative and X *fills the space* towards the $F$ axis, i.e.:

$$(f,i,d_1,..,d_n) \in X \implies \{(f_i,i,d_1,..,d_n) \vert \forall f_i \in [0,f)\} \subseteq X$$

The measure $$h$$
$$h$$ is connected to our probability.
If $$X$$ is a random variable with support $$A$$.
If we first define a distribution function on this random variable
$$Pr_X[A] = \mathbb{P}(X^{-1}(A))$$.
For all measurable subsets A of range(X)

$$Pr_X[A] = h(A)$$

Referring to our example:

$$G[\text{(uniform 'a 0 4)}] = (\{(d,x) \vert x \in [0,4] y \in [0,0.25]\}) $$

## Abstract Domain
The geometric semantics are in general not computable.
We are faced with the task of choosing an abstract domain, which will be a finite, computable, and hopefully efficient abstraction of the geometric domain.
In this example we shall choose an abstract domain of orthotopes.

## Functions of a Single Random Variable
A functional probabilistic language would be of little use without the ability to apply functions to random variables.
But what does this mean?
When random variables are regarded as free variables which sare sampled from distribution, arithmetic with random variables is no different from deterministic arithmetic.  Measure-theoretic probability uses the same notation, but regards it as implicit pointwise lifting (as in vector arithmetic).
If $$X$$ is an $$E$$-values random variable, given a function $$r : E \to F$$, we can define $$Y = r(X)$$ to mean:

$$Y : \Omega \to F$$

$$Y(\omega) = r(X(\omega))$$

If we define $$A$$ and $$B$$ to be any measurable subset of $$X$$ and $$Y$$ respectively, the following constraints should always hold:

$$Pr_X[A] = Pr_Y[f(A)]$$

$$Pr_Y[B] = Pr_X[f^{-1}(B)]$$

The result of applying a function to a distribution should yield a new distribution

The simplest kind of operation on random variables is to apply a unary function to it.
`(def c (* a a))`

# Queries

Expectation
Conditioning
Sampling

## Expectation Queries

Expectations are weighted sums.
Expectations are more general than more familiar *probability questions* (i.e. those )

(defn probability [random-variable predicate]
  (expectation (predicate random-variable)))

## Conditioning

Conditioning is the most complex query.
`condition` is a binary function which takes as argument a random variable to condition, and a (noisy predicate) i.e. a random variable ranging over 0.1.



This means, given a predicate on values which $$X$$ takes (more formally, values from its domain): $$\phi : E \to Bool$$, we want to compute the probability of the predicate on a random variable itself.
For concreteness we migt define $$\Phi : (\Omega \to E) \to \mathcal{P}(E)$$, where $$\Phi(X) = {e \in range(X) \vert \phi(e)}$$.

$$Pr : (\Omega \to E) \to Space \to (E \to Bool) \to Real$$

Notationally, typically the sample space is implicit.  The probability function is defined as follows:

$$Pr(X, S, \phi) = \mathbb{P} (X^{-1}(\phi(X)))$$

When it comes to question of how to represent a distribution, Sigma's philosophy is that there exists no single universally optimal solution.

If $$\Omega$$ (and by construction $$\mathcal{F}$$), and $$X$$ are discrete we could represent this an explicit mapping from all values of $$F$$ to $$[0,1]$$.
Of course, this would not be very efficient.
We could take advantange of the axiom of countable additivity - for events events $$E_1, E_2,$$
$$P(E_1 \cup E_2 \cup \cdots) = \sum_{i=1}^\infty P(E_i).$$ - we need only store the disjoint subsets, and can compute.  This is a probability mass function.

But this fails in the continuous case.

The second consideration is dependence.

We'll represent a random variable $$X$$ geometrically as subset of $$P \times X_I \times X_D^1 \times \dot \times X_D^n$$.
Here $$X_I$$ represents the independent variable, $$X_D^1 \times \dot \times X_D^n$$

