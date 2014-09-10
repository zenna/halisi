import Base: quantile, convert
import Distributions.pnormalize!

# =======
# Measure

measure{B<:Box}(bs::Vector{B}) = map(volume,bs)
logmeasure{B<:Box}(bs::Vector{B}) = map(x->exp(logvolume(x)),bs)
function measure(os::Vector{Omega})
  if isempty(os)
    0.0
  else
    measure(map(x->convert(NDimBox,collect(values(x.intervals))),os))
  end
end
logmeasure(os::Vector{Omega}) = logmeasure(map(x->convert(NDimBox,collect(values(x.intervals))),os))

# =======
# Queries

function prob(rv::RandomVariable; pre_T = (rv,y,X)->pre_bfs(rv, y, X; n_iters = 7))
  preimage = pre_T(rv, T, [Omega()])
  println("num in preimage", length(preimage))
  sum(measure(preimage))
end

function logprob(rv::RandomVariable; pre_T = (rv,y,X)->pre2(rv, y, X; n_iters = 4))
  preimage = pre_T(rv, T, [Omega()])
  println("num in preimage", length(preimage))
  logmeasure(preimage)
end

function prob_deep(rv::RandomVariable;  max_depth = 5, box_budget = 300000)
  tree = pre_deepening(rv, T, Omega(), max_depth = max_depth, box_budget = box_budget)
  under_pre, over_pre = sat_tree_data(tree), mixedsat_tree_data(tree)
  sum(measure(under_pre)), sum(measure(over_pre))
end

function cond_prob_deep(rv::RandomVariable, q::RandomVariable; box_budget = 300000, max_depth = 5)
  tree1 = pre_deepening(rv & q, T, Omega(), max_depth = max_depth, box_budget = box_budget)
  under_pre_cond, over_pre_cond = sat_tree_data(tree1), mixedsat_tree_data(tree1)

  tree2 = pre_deepening(q, T, Omega(), max_depth = max_depth, box_budget = box_budget)
  under_pre_query, over_pre_query =  sat_tree_data(tree2), mixedsat_tree_data(tree2)
  println(" under cond: ", sum(measure(under_pre_cond)),
          " under query: ", sum(measure(under_pre_query)),
          " over_pre_cond: ", sum(measure(over_pre_cond)),
          " over_pre_query: ", sum(measure(over_pre_query)))

  (sum(measure(under_pre_cond))) / (sum(measure(under_pre_query))), (sum(measure(over_pre_cond))) / (sum(measure(over_pre_query)))
end


## Convenience
prob_recursive(rv::RandomVariable) = prob(rv, pre_T = (rv,y,X)->pre_recursive2(rv, y, X;max_depth = 16, box_budget=3000))

random(i) = ω->ω[i]

function middle_split(o::Omega)
  ks = collect(keys(o.intervals))
  vs = collect(values(o.intervals))
  box = convert(NDimBox,vs)
  z = middle_split(box)
  map(x->Omega(Dict(ks,to_intervals(x))),z)
end

middle_split(os::Vector{Omega}) = map(middle_split, os)
## ========
## Sampling
function cond_sample(X::RandomVariable, Y::RandomVariable; max_depth = 10)
  tree = pre_deepening(Y, T, Omega(), max_depth = max_depth)
  over_pre_cond = mixedsat_tree_data(tree)
  measures = measure(over_pre_cond)
  pnormalize!(measures)
  c = Categorical(measures)
  omegas_as_boxes = convert(Vector{Box}, over_pre_cond)

  function(num_tries)
    for i = 1:num_tries
      box = omegas_as_boxes[rand(c)]
      sample = rand(box)
      println(sample)
      measure(over_pre_cond)
    end
  end
end

# methods(measure)

# cond_sample(uniform(0,0,1),uniform(0,0,1) > 0.5)




# import Sigma.mixedsat_tree_data
# tree = pre_deepening(flip(0,.7), T, Omega())
# over_pre_cond = mixedsat_tree_data(tree)
# measures = measure(over_pre_cond)


# x = uniform(0,0,1)
# y = uniform(1,0,1)
# cond_sample(x,x*y>2)

## =======================
## Primitive Distributions

quantile(d::Normal, X::RandomVariable) = ω->quantile(d, X(ω))
quantile(d::Normal, i::Interval) = Interval(quantile(d,i.l),quantile(d,i.u))
normal(i, mean, var) = quantile(Normal(mean, var), random(i))

flip(i) = 0.5 > random(i)
flip(i,p) = p > random(i)

function uniform(i,l,u)
  @assert u > l
  l + (u - l) * random(i)
end

uniformArray(l,u,x,y) = independentRandomArray(x->uniform(x,l,u),x,y)
