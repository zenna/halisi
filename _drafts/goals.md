---
layout: classic
title:  "Goals"
date:   2014-03-31 20:30:18
categories: goals
---
<script type="text/javascript" src="http://cdn.mathjax.org/mathjax/latest/MathJax.js?config=TeX-AMS-MML_HTMLorMML"></script>

## Theory

- Understand whether we should be thinking in terms of sets of distributions or approximations of distributions
- Determine whether it is an abstract interpretation and define Galois connection
- Determine whether we should be working with global or local probability spaces
- Determine semantics of `condition`, especially whether a) the conditional distribution is dependent on the original distribution b) whether Armando's intuition that condition defines a new probability space is correct 

### Bounding and Quantifying Approximation
- Formalise way(s) to quantify error

## Evaluation
- Create test data-set and profiling framework
- Implement our examples on all existing probabilistic languages
- Summarise all existing probabilistic languages

## Deterministic Sigma - Pattern matching

- Handle loop/recur
- Handle and, or, and other short-cutting functions
- Make all rewrite rules order-independent
- Build test-suite for comparison of Clojure and Sigma

## Probabilistic operations and abstraction

__Implement abstract domains:__

- Discrete Large Conditional Probability Table
- Discrete Factored Conditional Probability Table
- Continuous Histogram Representation
- Continuous Spline Representation

__For each, where possible, implement__:

- `(rand-int)` and `(rand-real)`
- Primitive arithmetic `+` `-` `*` `/` on
- Primitive Logical: `if` `not` `and` `or`
- Others: `>=`

## Refinement

- Formulate what refinement means

## Decision Making Interpreter

- Build interface necessary to connect decision making interpreter