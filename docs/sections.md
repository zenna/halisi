#Sections
Here is an overview of all the different parts of Sigma.
Whereas progress will be in flux, this serves to be up to date as the state of the art


## Abstract Finite Discrete Domain
This domain is used to represent finite discrete distributions.

When we apply a function to some values `(f x1 x2 .. xn)` there are a number of possible scenarios:

1. __All the arguments are concrete.__  In this case the function acts normally
2. __There is a single abstract argument, i.e. `(f x)`__
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

| x | (inc x) |  P  |
|---|---------|-----|
| 0 |       1 | 0.5 |
| 1 |       2 | 0.5 |