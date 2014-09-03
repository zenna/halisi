import Base.quantile
using Base.Test
typealias RandomVariable typeof(+)

# Lift primitive operators to work on random variables
# A function applied to a random variable evaluates to
# a random variable
for op = (:+, :-, :*, :/, :./, :&, :|, :$, :>, :>=, :<=, :<)
  @eval begin
    function ($op)(a::RandomVariable, b::RandomVariable)
      f(ω) = ($op)(a(ω),b(ω))
    end
    function ($op)(a::Number, b::RandomVariable)
      f(ω) = ($op)(a,b(ω))
    end
    function ($op)(a::RandomVariable, b::Number)
      f(ω) = ($op)(a(ω),b)
    end
  end
end

## Array Operations
for op = (:*,:./)
  @eval begin
    function ($op){T <:Number}(a::RandomVariable, arr::Array{T})
      f(ω) = map(b->($op)(a(ω),b), arr)
    end
  end
end

for op = (:!,:sqrt,:sqr,:abs)
  @eval begin
    function ($op)(a::RandomVariable)
      f(ω) = ($op)(a(ω))
    end
  end
end

# ==========
# Probability

immutable Omega
  intervals::Dict{Any,Interval}
end
Omega() = Omega(Dict{Any,Interval}())

function getindex(o::Omega, key::Int64)
  if haskey(o.intervals,key)
    o.intervals[key]
  else
    i = Interval(0,1)
    o.intervals[key] = i
    i
  end
end

measure{B<:Box}(bs::Vector{B}) = sum(map(volume,bs))
logmeasure{B<:Box}(bs::Vector{B}) = sum(map(x->exp(logvolume(x)),bs))
function measure(os::Vector{Omega})
  if isempty(os)
    0.0
  else
    measure(map(x->convert(NDimBox,collect(values(x.intervals))),os))
  end
end
logmeasure(os::Vector{Omega}) = logmeasure(map(x->convert(NDimBox,collect(values(x.intervals))),os))

function prob(rv::RandomVariable; pre_T = (rv,y,X)->pre_bfs(rv, y, X; n_iters = 7))
  preimage = pre_T(rv, T, [Omega()])
  println("num in preimage", length(preimage))
  measure(preimage)
end

function logprob(rv::RandomVariable; pre_T = (rv,y,X)->pre2(rv, y, X; n_iters = 4))
  preimage = pre_T(rv, T, [Omega()])
  println("num in preimage", length(preimage))
  logmeasure(preimage)
end

function prob_deep(rv::RandomVariable;  max_depth = 5, box_budget = 300000)
  tree = pre_deepening(rv, T, Omega(), max_depth = max_depth, box_budget = box_budget)
  under_pre, over_pre = sat_tree_data(tree), mixedsat_tree_data(tree)
  measure(under_pre), measure(over_pre)
end

function cond_prob_deep(rv::RandomVariable, q::RandomVariable; max_depth = 5)
  tree1 = pre_deepening(rv & q, T, Omega(), max_depth = max_depth)
  under_pre_cond, over_pre_cond = sat_tree_data(tree1), mixedsat_tree_data(tree1)

  tree2 = pre_deepening(q, T, Omega(), max_depth = max_depth)
  under_pre_query, over_pre_query =  sat_tree_data(tree2), mixedsat_tree_data(tree2)
  measure(under_pre_cond) / measure(under_pre_query), measure(over_pre_cond) / measure(over_pre_query)
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
