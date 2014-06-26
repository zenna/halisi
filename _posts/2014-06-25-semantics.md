---
layout: classic
title:  "Semantics"
date:   2014-03-31 20:30:18
categories: Semantics
---
# The Sigma Language

Programming languages are defined by specifying syntax and semantics.
Syntactically, Sigma is a lisp dialect - expressions are mostly of the form `(f x y ..)`.
More formally, Sigma expressions are __forms__ in the following the grammar:

{% highlight haskell %}
form: literal | list | vector | map

list: ( form* )

vector: [ form* ]

map: { (form form)* }

literal
    : STRING | NUMBER  | nil | BOOLEAN | KEYWORD | SYMBOL
{% endhighlight %}

## Semantics
The semantics of a programming language maps each of its programs onto an object which represents its meaning.
Sigma programs are pure functions: unlike procedures which compute through changes in state, a Sigma program is a mathematical function which given some input always return the same output.

To specify the semantics of Sigma we define semantic functions which map expressions in the language to values in another meta-language.
Semantic functions are conventionally shown with double square brackets, where the argument is *syntactic* and devoid of any inherent meaning.
If $$\mathcal{L_{\sigma}}$$ is the Sigma language - the set of all valid Sigma expressions, and $$P \in \mathcal{L_{\sigma}}$$ a Sigma program, its *concrete semantics* $$S\llbracket \texttt{P}\rrbracket$$ is an element of the semantic domain $$\mathcal{D}$$, a set equipped with a partial order relation $$\subseteq$$.
For instance both $$S\llbracket \texttt{(+ 3 4)} \rrbracket$$ and $$S\llbracket \texttt{(+ 4 3)} \rrbracket$$ denote the integer $$7$$.
Later we contrast this with its *abstract* counterpart.

The semantics of deterministic expressions are identical to a pure subset of Clojure, so we focus here on the probabilistic semantics.
Such that the article is fully selfcontained. which denotes the functional application $$f(x,y,..)$$ when \texttt{f} is a function.
Larger programs are formed by nesting expressions within each other, for example \texttt{(+ (/ 3 2) (* 2 4))}, and the execution, or \textit{evaluation} of a program then consists of reducing nested expressions.
\texttt{f}  may denote something other than a function, i.e. one of many \textit{special forms}.
The majority of these are inherited from lisp and adopt the same semantics.
In particular \texttt{(def name value)} globally binds a name to a value to loosely mirror the mathematical $$:=$$ operator;
\texttt{(let [name1 value1 name2 value2 ..] body)} binds names seen in the context of an expression \texttt{body}, to a corresponding set of values;
\texttt{(fn [args] body)} creates a function.

\[
\mathcal{M}form: literal | list | vector | map

list: ( form* )

vector: [ form* ]

map: { (form form)* }

literal
    : STRING | NUMBER  | nil | BOOLEAN | KEYWORD | SYMBOL
\]



Three special forms set Sigma apart from other probabilstic languages: \texttt{expectation}, \texttt{condition}, \texttt{sample} and \texttt{rule}.
Put briefly, these correspond to performing inference, defining evaluation rules of the language, and sampling from distributions.
We shall describe in detail the semantics each of these in the following sections.

We will describe two semantics which map exactly onto objects of measure-theoretic probability.
From these we shall construct an approximate semantics, which is what the  Sigma interpreter implements to perform inference.

### Probabilistic Semantics
Many Sigma expressions map onto objects of measure-theoretic probability.
For clarity let us briefly recap some of these concepts.
The primary object of interest is a measurable space $$(\Omega, \mathcal{F})$$, where the sample space $$\Omega$$ is the set of all possible outcomes of a random experiment.
An event $$A \subseteq \Omega$$ is a subset of the sample space, and the set of events $$\mathcal{F}$$ is a $$\sigma$$-algebra.
Uncertainty is represented as a probability measure $$\mathbb{P}:\mathcal{F} \rightarrow [0,1]$$, which assigns probabilities to events.
These compose to form a probability space $$(\Omega, \mathcal{F}, \mathbb{P})$$, a model of a random process.

An $$E$$-valued random variable $$X:\Omega \rightarrow E$$ maps the sample space to an arbitrary set $$E$$.
The \textit{distribution} of a random variable $$X$$ with respect to a probability space assigns probabilities to elements of its domain $$P_X(E) = \mathbb{P}(X^{-1}(E))$$.

There are three main terms whose evaluation we need to describe: primitive distribution constructors, expectation, condition, and sample.
The denotational semantics are summarised in figure \ref{semantics}.
Here, we find it more illustrative to demonstrate with an example.
First, we set up a model of four variables `a b c d e`:


### Primitive Random Variable Constructors

{% highlight clojure %}
(def a (uniform 'a 0 3))
(def b (uniform 'b 0 3))

(def c (* a a))
(def d (- a 10))
(def e (* a b))
{% endhighlight %}

In Sigma there is a single, global, probability space.
Sigma's analogue of random operators found in most languages are \textit{primitive distribution constructors}.
These are pure functions which construct primitive random variables the global probability space.
Hence, \texttt{(uniform 'a 0 3)} returns a random variable uniformly distributed over $$[0, 3]$$.

The first argument \texttt{a}, its \textit{identifier}, differentiates the random variable from others.
Specifically it ensures that it is independent of other primitive random variables which do not share the same identifier.
This property is implicit in conventional mathematical notation, for instance it is implied that $$X \sim \ \mathcal{U}(0,3)$$ is independent of $$Y \sim \mathcal{U}(0,3)$$.
Furthermore, probabilistic languages with sampling semantics circumvent the problem by positing a i.i.d sequence of random bre ts.
Since \textsc{uniform} is a pure function, to yield different random primitive variables it must be applied to different arguments.
More formally then:

\[
S \llbracket \texttt{(uniform 'a 0 3)} \rrbracket &=& X : \Omega \to \mathbb{R}\\
\]
Under the constraint that if $$\mathcal{R}_{\sigma} \subset \mathcal{L}$$ is the set of valid primitive random variable constructors expressions, $$id:\mathcal{R}_{\sigma} \to \mathcal{L}_{\sigma}$$ extracts the identifier and $$\bot$$ denotes the independence relation:
\[
\forall r_i, r_j \in \mathcal{R}_{\sigma}.id(r_i) \neq id(r_j) \implies S[r_i] \bot S[r_j]
\]
Where if $$\lambda^*$$ denotes the Lebesgue measure, for any subset of $$A \subseteq [0, 3]$$
\[
P_X(A) &=& Ω f dP
\]

#Hausdorff Conditional Semantics
The sematic function $$S$$ is often intractable or not computable.  For example we can not explicitly represent the probability of all subsets of a infinite $$\Omega$$.  Even in finite $$\mathcal{F} = \mathcal{P}(\Omega)$$ will become intractably large for non-trivial problems.  Moreover, determining independence is a numerical calculation, whereas many independence properties can be derived from the program structure.

We will define a second semantics $$G$$ which will still be exact, incomputable, but from which a number of approximate, efficient semantics will be derived.
In defining $$G$$ we will construct from $$S$$ a new type of object, and a measure of that object.
We'll represent a random variable $$X$$ as a tuple of \textit{named conditional probability distributions} $$\{X_1, X_2, ..., X_n\}$$, where each $$X_i$$ is a subset of a euclidean $$\mathbb{R}^n$$ \textit{conditional product space} of the form:

\[
F \times I \times D_1 \times \cdots \times D_n
\]

Under the constraint that $$F$$ values are non-negative and X *fills the space* towards the $$F$$ axis, i.e.:

\[
(f,i,d_1,..,d_n) \in X \implies \{(f_i,i,d_1,..,d_n) \vert \forall f_i \in [0,f)\} \subseteq X
\]

$$\nu$$ is constrained by $$P$$.
If $$X$$ is a random variable with associated measure $$P$$, we define a representation oF $$X$$ as $$X' \subset \chi$$, such that
For all measurable subsets $$E$$ of $$range(X)$$.

\[
P_X(E) = nu(\{x' \in X' \vert I(x) \in E\})
\]

The measure $$\nu$$ wi

Referring to our example:

\[G[\texttt{(uniform 'a 0 4)}] = (\{(d,x) \vert x \in [0,4] y \in [0,0.25]\})\]

#Abstract Domain}
The geoetric semantics are in general not computable.
We are faced with the task of choosing an abstract domain, which will be a finite, computable, and hopefully efficient abstraction of the geometric domain.

In this example we shall choose an abstract domain of orthotopes.


#Functions of a Single Random Variable

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

#Queries

Expectation
Conditioning
Sampling

\subsubection{Expectation Queries}

Expectations are weighted sums.
Expectations are more general than more familiar *probability questions* (i.e. those )

(defn probability [random-variable predicate]
  (expectation (predicate random-variable)))

### Conditioning

Conditional distribution queries ask how one random variables output influences the distribution of another.

Conditioning is an operation on probability spaces (as opposed to on)

Toronto defines condition as

{% highlight clojure %}
(defn image
  [f A]

(defn mapping
  "Convert X which may be a lambda to a mapping with domain Omega
   Mappings are hash tables"
  [f A]
  (image (fn [x] [x (f x)]) A))

(defn condition [Y y [Omega p]]
  (let [Omega-new (preimage (mapping Y Omega) (set y))
        p-new (fn [w-new] (/ (p w) (sum p Omega-new)))]
    [Omega-new p-new]))
{% endhighlight %}

This returns a probability space with $$\Omega$$ restricted to the subset where random variable $$Y$$ returns $$y$$.

Condition returns a new measure. It's as simple as that.

Conditioning is the most complex query.
`condition` is a binary function which takes as argument a random variable to condition, and a (noisy predicate) i.e. a random variable ranging over 0.1.

This means, given a predicate on values which $$X$$ takes (more formally, values from its domain): $$\phi : E \to Bool$$, we want to compute the probability of the predicate on a random variable itself.
For concreteness we migt define $$\Phi : (\Omega \to E) \to \mathcal{P}(E)$$, where $$\Phi(X) = {e \in range(X) \vert \phi(e)}$$.

$$Pr : (\Omega \to E) \to Space \to (E \to Bool) \to Real$$

Notationally, typically the sample space is implicit.  The probability function is defined as follows:

$$Pr(X, S, \phi) = \mathbb{P} (X^{-1}(\phi(X)))$$

% If $$f:X \rightarrow Y$$, the pre-image measure defines distributions over subsets of $$Y$$ - $$\mathbb{P}(B) = P(f^{-1}(B))$$.
% Where the pre-image $$f^-1(B) = \{w \vert w \in X, f(w) \in B \}$$.

Primarily, we want to infer properties of distributions defined by probabilistic programs, which amounts to conditioning.
If $$\mathbb{P}(B) > 0$$ the conditional probability of $$B' \subseteq Y$$ given $$B \subseteq Y$$ is
\[
\mathbb{P}(B' \vert B) = \frac{\mathbb{P}(B \cap B')}{\mathbb{P}(B)}
\]

As in \cite{Cousot:2012uka}, we can define probabilistic semantics in terms of measure theoretic objects.
$$S_p\llbracket \texttt{P} \rrbracket \in \mathcal{D}_p = \Omega \rightarrow \mathcal{D}$$.
In words, when an $$\omega \in \Omega$$ is
sampled according to $$\mathbb{P}$$, the evaluation of the program \texttt{P} yields the deterministic semantics $$S_p\llbracket \texttt{P} \rrbracket \in \mathcal{D}$$.
That is, $$\omega$$ embodies all the possible random choices a program will make during its evaluation.
A $$Sigma$$ program is then viewed simply as a mapping from a random source to output, i.e., a random variable specifying a distribution.
% Probabilistic inference is primarily interested in the computing conditional probabilities $$B \subseteq Y$$ given some condition set $$B \subseteq Y$$, sampling from the conditional distribution or computing the expectation.
% The probability measure on the output set $$B \subseteq Y$$ should be defined by preimages of $$B$$ under $$f$$ and the probability measure on $$X$$.

Distributions are composed of random primitives and functions to yield more complex distributions.
The evaluation of random primitives, e.g. \texttt{(rand)}, \texttt{(rand-int)}, \texttt{(flip p)} denote random variables defined on a probability spaces; respectively the continuous uniform, discrete uniform, and Bernoulli distributions.
For instance the semantics of \texttt{(flip)}, $$S\llbracket \texttt{(flip)} \rrbracket = X:\Omega \rightarrow \{0, 1\}$$ is a Bernoulli distributed random variable, i.e., $$P_X(0) = 0.5$$ and $$P_X(1) = 0.5$$.
That is, unlike Church programs where evaluation of a random primitive yields a sample, evaluation of a Sigma random primitive yields a distribution, or more precisely an approximation of a distribution.

To support conditioning, Sigma tracks dependencies between random variables.
Fortunately, in functional languages these are explicit: $$x$$ is dependent on $$y$$ if $$x$$ is a function of $$y$$.
If \texttt{f} denotes a function and \texttt{x1},\texttt{x2},..,\texttt{xn} are random variables with respective ranges $$E_1,E_2,..,E_n$$, then \texttt{(f x1 x2 .. xn)} evaluates to a new random variable \texttt{rv'} which \textit{encapsulates} its dependent variables.
That is, \texttt{rv'} captures the joint conditional distribution; it ranges over the range of \texttt{f} as an independent variable, and the product of the ranges of \texttt{f}'s arguments as dependent variables: $$E_1 \times E_2 \times \cdots \times E_n$$.
In the discrete case, we can think of these as conditional probability tables as seen in Bayesian graphical models.
For instance, if \texttt{x} is bound to \texttt{(flip)} and \texttt{y} is bound to \texttt{(flip)}, \texttt{(* x y)} evaluates to some representation of the following table:

\begin{center}
    \begin{tabular}{| l | l | l | l |}
    \hline
    \texttt{x} & \texttt{y} & \texttt{(* x y)} & \texttt{P((* x y))} \\ \hline
    0 & 0 & 0 & 0.25 \\
    0 & 1 & 0 & 0.25 \\
    1 & 0 & 0 & 0.25 \\
    1 & 1 & 1 & 0.25 \\
    \hline
    \end{tabular}
\end{center}

In \texttt{Sigma}, \texttt{condition} enables probabilistic inference, and maps directly onto the measure theoretic definition.
If \texttt{pred?} is a predicate on a random variable \texttt{rv}, \texttt{(condition rv pred?)} evaluates to a new random variable \texttt{rv'} under a new probability measure $$\mathbb{P}'$$.
$$\mathbb{P}'$$ is \textit{restricted} by \texttt{pred?}$$:E \rightarrow \{0,1\}$$, such that if we define:

\[
G(\omega) = \begin{cases}
  \mathbb{P}(\omega) & \mbox{ if } pred?(rv(\omega)) \\
  0 & \mbox{ otherwise }
\end{cases}
\]

$$\mathbb{P}'$$ is a found by normalising $$G$$.

Algorithmically, \texttt{condition} applies \texttt{pred?} to \texttt{rv} to yield an intermediate boolean random variable, which is then filtered to only true values.
\texttt{rv} is extracted, then renormalised.

Extending the example above, if we define a predicate (i.e. $$f(x)=x>0)$$ \texttt{positive?}:
{% highlight clojure %}
(def positive?
  (fn [value]
    (> value 0)))
{% endhighlight %}

\texttt{(condition (+ x y) positive?)} evaluates \texttt{positive?} yielding an intermediate distribution.

\begin{center}
    \begin{tabular}{| l | l | l | l | l |}
    \hline
    \texttt{x} & \texttt{y} & \texttt{(* x y)} & \texttt{(positive? (* x y))} & \texttt{(P(v))} \\ \hline
    0 & 0 & 0 & false & 0.25 \\
    0 & 1 & 0 & false & 0.25 \\
    1 & 0 & 0 & false & 0.25 \\
    1 & 1 & 1 & true & 0.25 \\
    \hline
    \end{tabular}
\end{center}

\texttt{condition} filters out all rows where the predicate is false, resulting in a distribution with all its mass on \texttt{x}$$=$$\texttt{y}$$=1$$.



<!-- 

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
Hence, `(uniform 'a 0 3)` returns a random variable uniformly distributed over $$$$[0, 3]$$$$.
The first argument `a`, its *identifier*, differentiates the random variable from others.
Specifically it ensures that it is independent of other random variables which neither share the same identifier, nor are functionally related.
This property is implicit in conventional mathematical notation, for instance it is implied that $$$$X \sim \ \mathcal{U}(0,3)$$$$ is independent of $$$$Y \sim \mathcal{U}(0,3)$$$$.
Furthermore, probabilistic languages with sampling semantics circumvent the problem by positing a i.i.d sequence of random bre ts.
Since `uniform` is a pure function which evaluates to a random variable, to yield different values it must take different arguments.
More formally then:

$$$$S[\text{(uniform 'a 0 3)}] = f : \Omega \to \mathbb{R}$$$$

Under the constraint:

$$$$\forall e_i, e_j.id(e_i) \neq id(e_j) \implies S[e_i] \bot S[e_j]$$$$

A random variable is a measurable function $$$$f : \Omega \to E$$$$.
It is said to be *defined on* some probability space $$$$(\Omega, \mathcal{F}, \mathbb{P})$$$$.
Here $$$$\mathcal{F} \subseteq \mathcal{P}(\Omega)$$$$ is a $$$$\sigma$$$$ algebra, and $$$$\mathbb{P}$$$$ is a probability measure.

## Geometric Semantics
The primary operations Sigma allows us to perform - applying functions to, conditioning, computing expectations of, and sampling from distributions - require efficient representations, and means of determining independence.
The measure theoretic semantic function $$$$S$$$$ is often intractable or not computable.  For example we can not explicitly represent the probability of all subsets of a infinite $$$$\Omega$$$$.  Even in finite $$$$\mathcal{F} = \mathcal{P}(\Omega)$$$$ will become intractably large for non-trivial problems.  Moreover, determining independence is a numerical calculation, whereas many independence properties can be derived from the program structure.

We will define a second semantics $$$$G$$$$ which will still be exact, incomputable, but will lend it self to support in defining more approximate, efficient representations.
In defining $$$$G$$$$ we will construct from $$$$S$$$$ a new type of object, and a measure of that object.
We'll represent a random variable $$$$X$$$$ as a tuple of __named conditional probability distributions__ $$$$\{X_1, X_2, ..., X_n\}$$$$, where each $$$$X_i$$$$ is a subset of a euclidean $$$$\mathbb{R}^n$$$$ *conditional product space* of the form:

$$$$F \times I \times D^1 \times \cdots \times D^n$$$$

Under the constraint that $$F$$ values are non-negative and X *fills the space* towards the $$F$$ axis, i.e.:

$$$$(f,i,d_1,..,d_n) \in X \implies \{(f_i,i,d_1,..,d_n) \vert \forall f_i \in [0,f)\} \subseteq X$$$$

The measure $$$$h$$$$
$$$$h$$$$ is connected to our probability.
If $$$$X$$$$ is a random variable with support $$$$A$$$$.
If we first define a distribution function on this random variable
$$$$Pr_X[A] = \mathbb{P}(X^{-1}(A))$$$$.
For all measurable subsets A of range(X)

$$$$Pr_X[A] = h(A)$$$$

Referring to our example:

$$$$G[\text{(uniform 'a 0 4)}] = (\{(d,x) \vert x \in [0,4] y \in [0,0.25]\}) $$$$

## Abstract Domain
The geometric semantics are in general not computable.
We are faced with the task of choosing an abstract domain, which will be a finite, computable, and hopefully efficient abstraction of the geometric domain.
In this example we shall choose an abstract domain of orthotopes.

## Functions of a Single Random Variable
A functional probabilistic language would be of little use without the ability to apply functions to random variables.
But what does this mean?
When random variables are regarded as free variables which sare sampled from distribution, arithmetic with random variables is no different from deterministic arithmetic.  Measure-theoretic probability uses the same notation, but regards it as implicit pointwise lifting (as in vector arithmetic).
If $$$$X$$$$ is an $$$$E$$$$-values random variable, given a function $$$$r : E \to F$$$$, we can define $$$$Y = r(X)$$$$ to mean:

$$$$Y : \Omega \to F$$$$

$$$$Y(\omega) = r(X(\omega))$$$$

If we define $$$$A$$$$ and $$$$B$$$$ to be any measurable subset of $$$$X$$$$ and $$$$Y$$$$ respectively, the following constraints should always hold:

$$$$Pr_X[A] = Pr_Y[f(A)]$$$$

$$$$Pr_Y[B] = Pr_X[f^{-1}(B)]$$$$

The result of applying a function to a distribution should yield a new distribution

The simplest kind of operation on random variables is to apply a unary function to it.
`(def c (* a a))`

# Queries

Expectation
Conditioning
Sampling

## Conditioning

Conditioning is the most complex query.
`condition` is a binary function which takes as argument a random variable to condition, and a (noisy predicate) i.e. a random variable ranging over 0.1.



This means, given a predicate on values which $$$$X$$$$ takes (more formally, values from its domain): $$$$\phi : E \to Bool$$$$, we want to compute the probability of the predicate on a random variable itself.
For concreteness we migt define $$$$\Phi : (\Omega \to E) \to \mathcal{P}(E)$$$$, where $$$$\Phi(X) = {e \in range(X) \vert \phi(e)}$$$$.

$$$$Pr : (\Omega \to E) \to Space \to (E \to Bool) \to Real$$$$

Notationally, typically the sample space is implicit.  The probability function is defined as follows:

$$$$Pr(X, S, \phi) = \mathbb{P} (X^{-1}(\phi(X)))$$$$

When it comes to question of how to represent a distribution, Sigma's philosophy is that there exists no single universally optimal solution.

If $$$$\Omega$$$$ (and by construction $$$$\mathcal{F}$$$$), and $$$$X$$$$ are discrete we could represent this an explicit mapping from all values of $$$$F$$$$ to $$$$[0,1]$$$$.
Of course, this would not be very efficient.
We could take advantange of the axiom of countable additivity - for events events $$$$E_1, E_2,$$$$
$$$$P(E_1 \cup E_2 \cup \cdots) = \sum_{i=1}^\infty P(E_i).$$$$ - we need only store the disjoint subsets, and can compute.  This is a probability mass function.

But this fails in the continuous case.

The second consideration is dependence.

We'll represent a random variable $$$$X$$$$ geometrically as subset of $$$$P \times X_I \times X_D^1 \times \dot \times X_D^n$$$$.
Here $$$$X_I$$$$ represents the independent variable, $$$$X_D^1 \times \dot \times X_D^n$$$$

 -->