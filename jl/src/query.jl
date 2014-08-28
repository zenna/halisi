import Base.quantile
using Base.Test
typealias RandomVariable typeof(+)

# Lift primitive operators to work on random variables
# A function applied to a random variable evaluates to
# a random variable
for op = (:+, :*, :&, :|, :$, :>, :>=, :<=, :<)
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
measure(os::Vector{Omega}) = measure(map(x->convert(NDimBox,collect(values(x.intervals))),os))

function prob(rv::RandomVariable)
  preimage = pre2(rv, T, [Omega()], n_iters = 15)
  measure(preimage)
end

random(i) = ω->ω[i]

flip() = 0.5 > random()
flip(p) = p > random()

function uniform(i,l,u)
  @assert u > l
  l + (u - l) * random(i)
end

function middle_split(o::Omega)
  ks = collect(keys(o.intervals))
  vs = collect(values(o.intervals))
  box = convert(NDimBox,vs)
  z = middle_split(box)
  map(x->Omega(Dict(ks,to_intervals(x))),z)
end

# Find the pre-image of a function on set y
function pre2{T}(f::Function, y, bs::Vector{T}; n_iters = 10)
  goodboxes = T[]
  for i = 1:n_iters
    println("\n\n iteration - ", i)
    to_split = T[]

    # For each box, find the image and test intersection with y
    #     println("BS Volumes", sum(map(volume,bs)))
    println("bs len- ", length(bs))
    for b in bs
      image = f(b)
      # Split box further iff image overlaps
      if subsumes(y,image)
        push!(goodboxes, b)
      elseif overlap(image,y)
        push!(to_split, b)
      end
    end
    println("to_split len- ", length(to_split))

    bs = split_many_boxes(to_split)
  end
  goodboxes
end

## =======
## pre2()

quantile(d::Normal, X::RandomVariable) = ω->quantile(d, X(ω))
quantile(d::Normal, i::Interval) = Interval(quantile(d,i.l),quantile(d,i.u))
normal(i, mean, var) = quantile(Normal(mean, var), random(i))


## ========
## Examples
n_bars = 20
xs = Array(Float64,n_bars - 1)
ys = Array(Float64,n_bars - 1)
for i in 1:(n_bars - 1)
  j = i + 1
  l,u = linspace(0,2,n_bars)[i],linspace(0,2,n_bars)[j]
  println("LINES",l,u)
  ys[i] = prob((normal(0,0,1) + uniform(1,0,1) > l) & (normal(0,0,1) + uniform(1,0,1) < u))
  xs[i] = (u-l)/2 + l
end
# xs
# ys
plot(x=xs, y=ys, Scale.x_continuous(minvalue=0, maxvalue=3), Scale.y_continuous(minvalue=0, maxvalue=.05))
# plot(x = xs, y = ys, Geom.point, Geom.line)


# prob(uniform(1,0,1) + uniform(1,0,1) > 1.5)
