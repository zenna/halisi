import Base.convert
using Iterators
abstract Box <: Number
type NDimBox <: Box
  intervals::Array{Float64,2}
end

unit_box(num_dims) = NDimBox([zeros(num_dims) ones(num_dims)]')
ndcube(l::Float64, u::Float64, num_dims) = NDimBox(repmat([l,u],1,num_dims))

# Open Interval
immutable Interval <: Box
  l::Real
  u::Real
  Interval(l,u) =  if u > l new(l, u) else new(u,l) end
end

Interval(v::Vector) = Interval(v[1],v[2])

to_intervals(b::Box) = [Interval(b.intervals[:,i]) for i = 1:num_dims(b)]

#FIX ME: Handle End Points properly
subsumes(x::Interval, y::Interval) = y.l >= x.l && y.u <= x.u
overlap(x::Interval, y::Interval) = y.l < x.u && x.l < y.u

## ============
## Interval Arithmetic and Inequalities

>(x::Interval, y::Interval) = if overlap(x,y) TF elseif x.l > y.u T else F end
<(x::Interval, y::Interval) = if overlap(x,y) TF elseif x.u < y.l T else F end

>(x::Interval, y::Real) = if x.l > y true elseif x.u > y >= x.l TF else false end
>(y::Real, x::Interval) =  if x.u < y true elseif x.l < y <= x.u TF else false end

<(x::Interval, y::Real) = y > x
<(y::Real, x::Interval) = x > y

<=(x::Interval, y::Interval) = !(x > y)
>=(x::Interval, y::Interval) = !(x < y)
<=(x::Interval, y::Real) = !(x > y)
<=(y::Real, x::Interval) = !(y > x)

>=(x::Interval, y::Real) = !(x < y)
>=(y::Real, x::Interval) = !(x < y)
+(x::Interval, y::Interval) = Interval(x.l + y.l, x.u + y.u)
-(x::Interval, y::Interval) = Interval(x.l - y.l, x.u - y.u)
+(x::Interval, y::Real) = Interval(x.l + y, x.u + y)
+(y::Real, x::Interval) = y + x
-(x::Interval, y::Real) = Interval(x.l - y, x.u - y)
-(y::Real, x::Interval) = Interval(y - x.l, y - x.u)

*(x::Interval, y::Real) = Interval(x.l * y, x.u * y)
*(y::Real, x::Interval) = x * y

function *(x::Interval, y::Interval)
  a,b,c,d = x.l * y.l, x.l * y.u, x.u * y.l, x.u * y.u
  Interval(min(a,b,c,d),max(a,b,c,d))
end

function /(x::Interval, y::Interval)
  a,b,c,d = x.l / y.l, x.l / y.u, x.u / y.l, x.u / y.u
  Interval(min(a,b,c,d),max(a,b,c,d))
end

## =========
## Intervals


function merge_interval(a::Interval, b::Interval)
  l = min(a.l,b.l)
  u = max(a.u, b.u)
  Interval(l,u)
end

function merge_interval(a::Interval, b::Real)
  l = min(a.l,b)
  u = max(a.u,b)
  Interval(l,u)
end

## ========
## WHATEVER

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
volume(b::Box) = prod([b.intervals[2,i] - b.intervals[1,i] for i = 1:num_dims(b)])
