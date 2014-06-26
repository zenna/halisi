---
layout: classic
title:  "Bugs"
date:   2014-03-31 20:30:18
categories: bugs
---
<script type="text/javascript" src="http://cdn.mathjax.org/mathjax/latest/MathJax.js?config=TeX-AMS-MML_HTMLorMML"></script>

## Bugs ##

__Erroneous Reduction:__
There's a bug with the rewrite rules that needs fixing, I need to figure out how to handle if.  That should affect variable binding.  In general

{% highlight clojure %}
dbg: (transform p1__265#) = (+ 3 ((fn [coll] (/ (reduce + coll) (count coll))) [1 2 a]))
dbg: (transform p1__265#) = (+ 3 (/ (reduce + [1 2 a]) (count [1 2 a])))
dbg: (transform p1__265#) = (+ 3 (/ a (count [1 2 a])))
dbg: (transform p1__265#) = (+ 3 (/ a 3))
)
{% endhighlight%}

Why is reduce + `[1 2 a]` getting reduced to a. That should throw an error.

__Vector (e.g. in let) to list mis-conversion when rewriting:__

Rewriting a term can cause the encapsulating collection to be converted from a vector into a list

Example program:

{% highlight clojure %}
(let [x (uniform-int 'x 0 3)
      y (uniform-int 'y 0 3)] (+ x y)) eager-transformer)
{% endhighlight %}

The first rewrite is on uniform-int of x.

{% highlight clojure %}
(let
 (x
  #sigma.domains.cpt.Cpt{:var-names [(quote x)],
                         :entries [[1/4 0] [1/4 1] [1/4 2] [1/4 3]],
                         :primitives {(quote x) [[1/4 0]
                                                 [1/4 1]
                                                 [1/4 2]
                                                 [1/4 3]]}}
  y
  (uniform-int (quote y) 0 3))
 (+ x y))
{% endhighlight %}

__Cryptic error message when non-rule is supplied to rewrite:__

You can pass an arbitrary function in to rewrite as a rule, we should prevent this with types!
