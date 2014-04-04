---
layout: post
title:  "ANOVA"
date:   2014-03-31 20:30:18
categories: statistics tests
---
<script type="text/javascript" src="http://cdn.mathjax.org/mathjax/latest/MathJax.js?config=TeX-AMS-MML_HTMLorMML"></script>

# Analysis of Variance

Analysis of variance is a statistical test method to test the difference between a means.
ANOVA tests the hypothesis that all means are the same, this is referred to as the non-specific null hypothesis, or the *omnibus hypothesis* *(comment: why is it called these?)*.

## Why?
Probability theory provides tools to reason with - i.e. infer properties of, do calculations with - values which are uncertain.
Uncertainty can stem from randomness; by definition, it is impossible to say with certainty what will result from a truly random physical process.
Probability is not limited to true randomness however, it allows us to model processes which may be deterministic but for all intents and purposes behave randomly, such as rolling a die.
Probability theory also allows us to model processes which we may not think of as random at all, but over which there may still be uncertainty due to our limited access to information, or limited resources to compute.

## Basic Definitions
It can be difficult for non-mathematicians to get to grips with probability theory.
While some of this difficulty surely stems from concepts which are counter intuitive or irreducibly difficult to grasp, more often confusing naming,  notation and an unspecified encroachment of interpretation are to blame.

Fortunately, we can eliminate many of these problems by depending upon a language which is formal to the extent it must satisfy a computer 

###
We must first define some objects which will allow us to model a random process.
We'll say we run a random experiment to get some outcome outcome $$\omega$$.
$$\Omega$$, the sample space, is the set of all possible outcomes.

Example
```Haskell
let omega = [1, 2, 3, 4, 5, 6]
```

Often we will want to reason about sets of possible outcomes, for instance - how likely it is that a thrown die lands on an even number.
Hence it will be useful to define a set of all subsets of omega, i.e. it's power-set. 

```
events :: Num t => [[t]]
let events = power-set omega
```

A probability measure is a measurable function which assigns probabilities to events
```
measure :: Fractional a => a -> a
measure x = 1 / x
```

An E-valued random variable $$X$$ is a function from the sample space to $$E$$.
$$X:\Omega \rightarrow E$$.
Random variables are typically given uppercase characters, and are often seen in confusing notation.
As stated, a random variable is a function, not a variable.

```
num- :: Omega -> 
```

##What is a distribution, exactly?
There are all kinds of distributions - uniform distributions, gaussian distributions, bernoulli distributions, and so on.
But when we talk about *a* distribution, what is that thing exactly, and is it different to something which *follows* some distribution, e.g. the grades were uniformly distributed.

A probability distribution is simply the probability measure which assigns probabilities to events.