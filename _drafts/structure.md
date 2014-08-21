---
layout: classic
title:  "Exploiting Structure"
date:   2014-03-31 20:30:18
categories: goals
---
<script type="text/javascript" src="http://cdn.mathjax.org/mathjax/latest/MathJax.js?config=TeX-AMS-MML_HTMLorMML"></script>

# Exploiting Structure

Many domains we wish to model exhibit forms of structure - informative, and often relatively simple patterns such as order, repetition and symmetry.
Programming languages, in particular modern programming languages provide means to capture this structure succinctly.
There's reason to believe that exploiting structure is key to scaling probabilistic programs to larger problems.
There's also reason to believe that the program structure itself, can lead the way to understanding structure.

## Dimensionality Reducation
What if, instead of computing $$f^\leftarrow(A)$$ we compute an easier $$l^\leftarrow(A)$$.  Easier in the sense that the domain of $$f$$ is of higher dimension than that of $$l$$.

We can consider two cases. Assuming for the moment that $$f:\mathbb{R}^m \to Y$$, in the first case we consider having a reduction $$g:\mathbb{R}^m \to \mathbb{R}^n$$, where $$m > n$$.
Then we are looking for a subset of the lower dimensionality space, which when projected to the higher dimensions though $$g$$, would be the preimage of $$A$$:

$$A' \subseteq \mathbb{R^n} \text{ such that } f^\rightarrow(g^\leftarrow(A')) = A$$

Mechanically, we can use the same refinement algorithm of computing pre-images through images

1. Take a subset $$A' \subseteq \mathbb{R^n}$$
2. Find its pre-image (g^\leftarrow(A')
3. 

Unfortunately, step 2 could be just as hard, if not harder than the original problem of $$f^\leftarrow(A)$$.  Perhaps one possibility is to choose a $$g$$ where its preimage can be easily computed, or perhaps computed offline.  The former of these may be difficult if a any notion of a good $$g$$ was dependent on $$f$$.

### Find pre-images in the lower dimensionality space
I thought it may be possible to avoid computing $$g^\leftarrow(A')$$ at all, and instead project $$f$$ to a lower dimension.
It seems however, that finding a lower dimensionality $$f$$ will require computing pre-images of $$g$$.
For instance suppose that $$f(x,y) = x + y$$. To find its lower dimensioned analogue $$f'(z)$$ we would need to reduce the $$+$$ operation, to $$+'(a) = apply(+,(g^\leftarrow(a))$$.

The main other issue is how would we fine $$g$$ in the first place.

### Find (possibly multiple) expansions, not reductions 
Instead of finding a reduction $$g:\mathbb{R^m} \to g:\mathbb{R^n}$$, we could instead find a lifted *expansion* $$h:\mathcal{P}(\mathbb{R^n}) \to \mathcal{P}(\mathbb{R^m})$$. 