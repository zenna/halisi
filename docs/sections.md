#Sections
Here is an overview of all the different parts of Sigma.
Whereas [progress](docs/progress.md) is in flux, this serves reflect current implementaion choices.

## Abstract Domains

### Finite Discrete Domain.
This domain is used to represent finite discrete distributions.

Current method:
1. Represent a discrete finite domain as a conditional probability table, which contains columns for independent variables, dependent variables and the associated probability
2. Any function of cpts `(f x1 x2 .. xn)` involves first finding the joint distribution.  The joint distribution is the distribution of the conjunction of *all combinations of values* of all variables involved.
3. Find the joint distribution by considering all combinations of values of deendent variables of operands, multiplying probabilities
4. Then for each combination, find `xi` find row in corresponding cpt which matches variable values in combination, and `xi` is the value of the independent variable. 

Questions:
- Should the object be a single CPT or a set of CPTs.

<!-- 
When we apply a function to some values `(f x1 x2 .. xn)` there are a number of possible scenarios:

For all random variables, find all colums which are not shared by all the random variables.
find c

1. __All the arguments are concrete.__  In this case the function acts normally
2. __There is a single abstract argument, i.e. `(f x)`__
3. __all n arguments are the same random variable__
3. __There are n independent rvs__:
 -->
### Examples
There is a single abstract argument, i.e. `(f x)`:
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

Example - All n arguments are the same random variable:
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

Example - There are n independent rvs:
```Clojure
(let [x (uniform 0 1)
      y (unifomrm 0 1)
      z (uniform 0 1)]
  (inc x))
```