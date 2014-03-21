# Sigma #

Sigma is a probabilistic programming language.
In it we can specify probabilstic models and perform inference.
Sigma is based on abstract and approximate interpretation, pattern matching, and decision making.

## Basic Idea ##
There are a number of key ideas in Sigma

- Evaluation of the program is abstract - instead of computing with we compute with approximations of values. Consider the Clojure code:
```Clojure
(defn make-a-2d-path [n]
  (repeatedly n #(vector (rand)(rand))))
```

What does (make-a-2d-path 5) evaluate to? In Clojure it would evaluate to some _concrete_ path, a nested list such as
```
([0.652 0.815] [0.545 0.416] [0.852 0.263] [0.562 0.734])
```

- Approximate interpretation involves making decisions.  The act of interpretation is separated from the decision making involved.
Consider a complex distribution

- This separation is facilitated by formulating interpretation as pattern-matching and transformation.

### Evaluation as pattern matching
Evaluation of a program is a series of transformations.
In particular, we search for a pattern in a program, if the pattern matches, we may replace that pattern with a transformation.

