using Iterators
abstract Box <: Real
type NDimBox <: Box
  intervals::Array{Float64,2}
end

# Open Interval
immutable Interval <: Box
  l::Real
  u::Real
  Interval(l,u) =  if u > l new(l, u) else new(u,l) end
end

Interval(v::Vector) = Interval(v[1],v[2])
to_intervals(b::Box) = [Interval(b.intervals[:,i]) for i = 1:num_dims(b)]
gen_arguments(l::Float64, u::Float64, n_dim::Integer) = to_intervals(ndcube(l,u,n_dim))

# Constructors
unit_box(num_dims) = NDimBox([zeros(num_dims) ones(num_dims)]')
ndcube(l::Float64, u::Float64, num_dims) = NDimBox(repmat([l,u],1,num_dims))

subsumes(x::Interval, y::Interval) = y.l >= x.l && y.u <= x.u
overlap(x::Interval, y::Interval) = y.l < x.u && x.l < y.u

ConcreteReal = Union(Float64,Int64)

## ====================================
## Interval Arithmetic and Inequalities

>(x::Interval, y::Interval) = if overlap(x,y) TF elseif x.l > y.u T else F end
<(x::Interval, y::Interval) = if overlap(x,y) TF elseif x.u < y.l T else F end

>(x::Interval, y::ConcreteReal) = if x.l > y true elseif x.u > y >= x.l TF else false end
>(y::ConcreteReal, x::Interval) =  if x.u < y true elseif x.l < y <= x.u TF else false end

<(x::Interval, y::ConcreteReal) = y > x
<(y::ConcreteReal, x::Interval) = x > y

<=(x::Interval, y::Interval) = !(x > y)
>=(x::Interval, y::Interval) = !(x < y)
<=(x::Interval, y::ConcreteReal) = !(x > y)
<=(y::ConcreteReal, x::Interval) = !(y > x)

>=(x::Interval, y::ConcreteReal) = !(x < y)
>=(y::ConcreteReal, x::Interval) = !(x < y)
+(x::Interval, y::Interval) = Interval(x.l + y.l, x.u + y.u)
-(x::Interval, y::Interval) = Interval(x.l - y.u, x.u - y.l)
+(x::Interval, y::ConcreteReal) = Interval(x.l + y, x.u + y)
+(y::ConcreteReal, x::Interval) = x + y
-(x::Interval, y::ConcreteReal) = Interval(x.l - y, x.u - y)
-(y::ConcreteReal, x::Interval) = Interval(y - x.l, y - x.u)

*(x::Interval, y::ConcreteReal) = Interval(x.l * y, x.u * y)
*(y::ConcreteReal, x::Interval) = x * y

sqrt(x::Interval) = Interval(sqrt(x.l), sqrt(x.u))
function sqr(x::Interval)
  a,b,c,d = x.l * x.l, x.l * x.u, x.u * x.l, x.u * x.u
  Interval(max(min(a,b,c,d),0),max(a,b,c,d,0))
end

function *(x::Interval, y::Interval)
  a,b,c,d = x.l * y.l, x.l * y.u, x.u * y.l, x.u * y.u
  Interval(min(a,b,c,d),max(a,b,c,d))
end

function /(x::Interval, y::Interval)
  a,b,c,d = x.l / y.l, x.l / y.u, x.u / y.l, x.u / y.u
  Interval(min(a,b,c,d),max(a,b,c,d))
end

function /(x::Interval, y::ConcreteReal)
  Interval(x.l / y, x.u / y)
end


## =========
## Merging
function merge_interval(a::Interval, b::Interval)
  l = min(a.l,b.l)
  u = max(a.u, b.u)
  Interval(l,u)
end

function merge_interval(a::Interval, b::ConcreteReal)
  l = min(a.l,b)
  u = max(a.u,b)
  Interval(l,u)
end

## ===========
## Conversions

function convert(Type{NDimBox}, i::Vector{Interval})
  intervals = Array(Float64,2,length(i))
  for j in 1:length(i)
    intervals[:,j] = [i[j].l i[j].u]
  end
  NDimBox(intervals)
end

## ========
## WHATEVER

flip(x::Interval) = Interval(-x.l,-x.u)
lower_bound_at(x::Interval, lower_bound) = Interval(max(x.l,0), max(x.u,0))

function abs(x::Interval)
  if x.l >= 0.0 && x.u >= 0.0 x
  elseif x.u >= 0.0 Interval(0,max(abs(x.l), abs(x.u)))
  else lower_bound_at(flip(x),0.0)
  end
end

INTERVAL_DIM_ERR = "Interval has only one single dimension"
num_dims(i::Interval) = 1
num_dims(b::Box) = size(b.intervals,2)
nth_dim_interval(i::Interval, dim_n::Uint) = if dim_n == 1 i else error(INTERVAL_DIM_ERR) end

left(i::Interval) = i.l
right(i::Interval) = i.u

middle_point(i::Vector) = i[1] + (i[2] - i[1]) / 2
middle_point(b::Box) = [middle_point(b.intervals[:,i]) for i = 1:num_dims(b)]

function split_box(i::Interval, split_point::Float64)
  [Interval(left(i), split_point), Interval(right(i),split_point)]
end

split_box(i::Vector, split_point::Float64) = Array[[i[1],split_point],[split_point, i[2]]]
f = split_box([3.0,4.0], 3.4)

# Splits a box at a split-point along all its dimensions into n^d boxes
function split_box(b::Box, split_points::Vector{Float64})
  @assert(length(split_points) == num_dims(b))
  boxes = NDimBox[]
  splits = [split_box(b.intervals[:, i],split_points[i]) for i = 1:num_dims(b)]

  for subbox in apply(product, splits)
    z = Array(Float64,2,num_dims(b))
    for i = 1:size(z,2)
      z[:,i] = subbox[i]
    end
    push!(boxes, NDimBox(z))
  end
  boxes
end

# Split box into 2^d equally sized boxes by cutting down middle of each axis"
middle_split(b::Box) = split_box(b, middle_point(b))
middle_split(is::Vector{Interval}) = map(to_intervals,middle_split(convert(NDimBox, is,)))

volume(b::Box) = prod([b.intervals[2,i] - b.intervals[1,i] for i = 1:num_dims(b)])
logvolume(b::Box) = sum(map(log,[b.intervals[2,i] - b.intervals[1,i] for i = 1:num_dims(b)]))$

function split_many_boxes{T}(to_split::Vector{T})
  bs = T[]
  for b in to_split
    for bj in middle_split(b)
      push!(bs, bj)
    end
  end
  bs
end

## ========
## Sampling
rand_interval(a::Float64, b::Float64) = a + (b - a) * rand()
rand(b::Box) = [apply(rand_interval,b.intervals[:,i]) for i = 1:num_dims(b)]
