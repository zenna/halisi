---
layout: classic
title:  "Sandbox"
date:   2014-03-31 20:30:18
categories: sandbox
---

## Sandbox
__How to describe physical reasoning inference questions in Sigma?__

Suppose we have some physical reasoning simulation described by some transition function `(update-state state)`. Reasoning questions we might like to ask are 1) Is it possible for some event to occur over the simulation, 2) What's the distribution over some final states 3) What geometry would cause these observations.  For instance, is it possible for a ball contained within an unsealable container to escape.  How can we phrase these in a probabilstic program?  The first question is the matter of time; we may interpret the previous question as

- Within some time limit, e.g. 10 seconds, is it possible for the ball to escape

However this may be inadequate since we have introduced extra information not in the original question.  We might instead ask

- Within an infinite amount of simulation, will the ball leave the container?

The question then becomes one of how to represent this infinity.  There seem to be at least two plausible candidates

- THe maximum time is a non-determinisitc value from 0 to infinity
- The maximum time is a probabilstic value with infinte range
- The function itself takes no maximum-time, it is a non-terminating function.

Sigma does not support nondeterminism, so the first is out.  The second adds extraneous probabilistic information. The last could be problematic because non-terminating functions are traditionally undefined, or considered errors.

Let's first consider the specific version of the problem as described above.

{% highlight clojure %}
(let [max-time 10
      dt 0.01]
  (pos? (probability (run-simulation init-state max-time dt) #(ball-escaped? %))))
{% endhighlight %}


Here `run-simulation` is a recursive function looking something like:

{% highlight clojure %}
(defn run-simulation [init-state t max-time dt]
  (if (> t max-time)
      state
      (run-simulation (update-state state) (+ t dt) max-time dt)))
{% endhighlight %}


The simplest way Sigma could answer this question is to compute with the distrubtion and restrict the result distribution to that which satisfied the predicate.  And return the ratio of the mass.

So let's create a simpler non-linear example: a ray bouncing around some environment.

The first operations that need to be handled will be seen when we find the vector between two points.

{% highlight clojure %}
(defn points-to-vec
  "convert pair of points to vector"
  [[x1 y1] [x2 y2]]
  [(- x2 x1) (- y2 y1)])
{% endhighlight %}

__*Requirement 1: Here all the arguments wil be uniform distributions, and so must support subtraction on these.*__

{% highlight clojure %}
(defn intersection-point
  "Find the intersection point of two 2D vectors.
   Each vector is defined as a pair of points, [a b].
   returns t parameter on line [a0 a1].
   fraction of distane from a0 to a1."
  [[a0 a1 :as v] [b0 b1 :as q]]
  {:pre [(not (parallel? v q))]}
  (let [[u1 u2] (points-to-vec a0 a1)
        [v1 v2] (points-to-vec b0 b1)
        [w1 w2] (points-to-vec b0 a0)
        denom (- (* v1 u2) (* v2 u1))]
    [(/ (- (* v2 w1) (* v1 w2)) denom)
     (/ (- (* u1 w2) (* u2 w1)) (- denom))]))
{% endhighlight %}

Then `u1 u2 v1 v2 w1 w2` will all be bound to values of this form (subtraction of uniform distributions).  These distributions are not independent.

Each of these will create a uniform difference distribution

__*Requirement 3: Computing the denominator `(- (* v1 u2) (* v2 u1))` requires the multiplication of these uniform difference distributions.*__

__*Requirement 4: Whatever the resulting distribution of Computing the denominator `(- (* v1 u2) (* v2 u1))` requires the multiplication of these uniform difference distributions, and then subtraction*__

__*Requirement 5: The denominator is subject to negation `(- denom)`__

__*Requirement 6: These distributions are divided by one another*__

Generally we have all the arithmetic operations `+ - / *` on distributiosn which are functions of uniform distributions.  They are not independent.

## Transforming Random Variables Using Derivative Information:

Given a set $$X$$ we wish to compute $$f(X) = \{y : y = f(x) . \forall x \in X\}$$.
Suppose we can compute $$f'$$, $$sup(X)$$, and $$inf(X)$$, we calculate a lower and upper bound the derivative: $$l = f'(inf(X))$$ $$u = f'(inf(X))$$