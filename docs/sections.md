#Sections
Here is an overview of all the different parts of Sigma.
Whereas progress will be in flux, this serves to be up to date as the state of the art


## Abstract Finite Discrete Domain
This domain is used to represent finite discrete distributions.

When we apply a function to some values `(f x1 x2 .. xn)` there are a number of possible scenarios:

1. __All the arguments are concrete.__  In this case the function acts normally
2. __There is a single abstract argument, i.e. `(f x)`__
3. __all n arguments are the same random variable__
3. __There are n independent rvs__:

### There is a single abstract argument, i.e. `(f x)`
For example:
```Clojure
(let [x (uniform 0 1)]
  (inc x))
```
In this case we contruct a new abo with `f` applied to its independent variable.  E.g.

| x |  p  |
|---|-----|
| 0 | 0.5 |
| 1 | 0.5 |

The value of `(inc x)` is:

| `x` | `(inc x)` |  P  |
|-----|-----------|-----|
|   0 |         1 | 0.5 |
|   1 |         2 | 0.5 |

### All n arguments are the same random variable
For example:
```Clojure
(let [x (uniform 0 1)]
  (+ x x))
```

| `x` | `(+ x x)` |  P  |
|-----|-----------|-----|
|   0 |         0 | 0.5 |
|   1 |         2 | 0.5 |

```Clojure
(let [x (uniform 0 1)]
  (+ (inc x) (inc x)))
```

| `x` | `(inc x)` | `(inc x)` | v |  P  |
|-----|-----------|-----------|---|-----|
|   0 |         1 |         1 | 2 | 0.5 |
|   1 |         2 |         2 | 4 | 0.5 |

Here we have observed that they both share the same dependent variable, and hence the addition is just applied to the indepndent variable.

```Clojure
(defn even-discrete [l u]
  (* 2 (uniform l (/ u 2))))

(even-discrete 0 4)
```

| `(uni..)` | `(* 2 (..))` |  P  |
|-----------|--------------|-----|
|         0 |            0 | 1/3 |
|         1 |            2 | 1/3 |
|         2 |            4 | 1/3 |

```Clojure
(defn triangular [l u]
  (+ (uniform l (/ u 2)) (uniform l (/ u 2))))

(triangular 0 4)
```

| `(uni..)` | `(uni..)` | `(/ (..) (..))` | P |
|-----------|-----------|-----------------|---|
|         0 |         0 |               0 |   |
|         1 |         0 |               1 |   |
|         2 |         0 |               2 |   |
|         0 |         1 |               1 |   |
|         1 |         1 |               2 |   |
|         2 |         1 |               3 |   |
|         0 |         2 |               2 |   |
|         1 |         2 |               3 |   |
|         2 |         2 |               4 |   |

### There are n independent rvs
For example:
```Clojure
(let [x (uniform 0 1)
      y (unifomrm 0 1)
      z (uniform 0 1)]
  (inc x))
```