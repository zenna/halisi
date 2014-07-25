using NLopt

import Base.intersect, Base.convert

# TODO
# Implement contructive approaches, e.g. when linear
# Do some smarter things with optimisation
# Implement multiple argument function
# Get example

typealias Point Vector

immutable Linear
  points::Vector{Point}
end

function linear_interpolate(p1::Point, p2::Point, n_points)
  delta_v = (1 / (1 + n_points)) * (p2 - p1)
  map(n->(delta_v * n) + p1, range(0, n_points + 2))
end

## ======================================
## Primitive Random Variable Constructors

function uniform_real(low,up,n)
  height = 1 / (up - low)
  [height low]
  points = linear_interpolate([height, low],[height, up], n)
  Linear(points)
end

a = function(a,b) b end

prob_axis = function(p::Point) p[1] end
i_axis = function(p::Point) p[end] end
var_axes = function(p::Point) p[2:end] end

# Euclidean Distance
function dist(a::Vector, b::Vector)
  tot = 0.0
  for i = range(1, length(a))
    d = a[i] - b[i]
    tot += d * d
  end
  sqrt(tot)
end

# Hausford Measure;
function measure(points::Vector) #FIXME Type is too generic?
  part_sums = Float64[]
  for i = range(1,length(points) - 1)
    a = points[i]
    b = points[i + 1]
    push!(part_sums, 0.5 * dist(a,b) * (prob_axis(a) + prob_axis(b)))
  end
  sum(part_sums)
end

# Find intersection of two intervals in R
# If they don't intersect, it will find the interval between htem
# With inverted dimensions
function intersect(a::Vector, b::Vector)
  [max([min(a), min(b)])
   min([max(a), max(b)])]
end

# Return interval where second value >= first
function order_interval(a::Vector)
  if a[2] >= a[1]
    a
  else
    [a[2], a[1]] # FIX: ambiguou return type slow?
  end
end

function interval_measure(points::Vector, interval::Vector, dim)
  tot = 0.0
  for i = range(1,length(points) - 1), j = i + 1
    proj_interval = order_interval([var_axes(points[i])[dim], var_axes(points[j])[dim]])
    isect = intersect(proj_interval, interval)

    if isect[2] > isect[1] # i.e. they intersect
      ratio = (isect[2] - isect[1]) / (proj_interval[2] - proj_interval[1])
      @assert ratio > 0 and ratio <= 1.0

      # FIXME: I have this silly thing becaue I dont know how to

      silly = Vector[]
      push!(silly, points[i])
      push!(silly, points[j])
      tot += measure(silly) * ratio
    end
  end
  tot
end

# root square difference
function rsq(x,y)
  sqrt(abs2(x-y))
end

# What is the cost on a single interval
function interval_cost(l::Linear,  intervals::Vector, image_intervals::Vector, interval_measures::Vector)
  # Measures of intervals on y
  ms = map(x->interval_measure(l.points, x,2), image_intervals)

  # Difference on independent axis
  ms = map((x,y)->sqrt(square(x-y)), ms, interval_measures)

  # Measures of intervals on y
  ms2 = map(x->interval_measure(l.points, x,1), intervals)

  # Difference on dependent axis
  ms2 = map((x,y)->sqrt(square(x-y)), ms2, interval_measures)

  m = mean(ms) + mean(ms2)
  m + rsq(measure(l.points), 1.0)
end

function lower_bound(p::Vector) min(p) end
function upper_bound(p::Vector) max(p) end
function bounds(p::Vector)
  [min(p), max(p)]
end

# Sample uniformly w/in interval [low, up]
function rand_in_interval(low, up)
  low + rand() * (up - low)
end

function rand_n_intervals(low, up, n)
  intervals = Vector[]
  for i = 1:n
    a = rand_in_interval(low, up)
    b = rand_in_interval(low, up)
    push!(intervals, order_interval([a,b]))
  end
  intervals
end

function interpolate_n_intervals(low, up, n)
  width = (up - low) / n
  l = low
  intervals = Vector[]
  for i = 1:n
    push!(intervals, [l, l+width])
    l = l+width
  end
  intervals
end

# Project a Linear down to a single axis
function project(l::Linear, dim)
  [ x[dim] for x in l.points ]
end

# Project a Linear down to its independet variable axis
function project_i (l::Linear)
  [ x[end] for x in l.points ]
end

immutable Interval <: Number
  low
  up
  function Interval(a,b)
    new(min(a,b),max(a,b))
  end
end

function Interval{T<:Number}(v::Vector{T})
  Interval(v[1],v[2])
end
convert(::Type{Vector}, x::Interval) = [x.low, x.up]

function *(a::Interval, b::Interval)
  Interval(a.low * a.low, b.up * b.up)
end

function dbl(x) x * 2 end

function *(i::Interval, n::Int64)
  Interval(i.low * n, i.up *n)
end

function structure_points(x::Vector, d)
  @assert length(x) % d == 0
  r = Vector[]
  i = 1
  while (i + d) <= length(x) + 1
    push!(r, x[i:i+2])
    i += d
  end
  r
end

function generate_objf(f, l::Linear, n_points, n_intervals)
  projection = project_i(l)
  intervals = interpolate_n_intervals(min(projection), max(projection), n_intervals)
  image_intervals = map(i->convert(Vector,f(Interval(i))), intervals)
  measures = map(x->interval_measure(l.points,x,1), intervals) #FIXME: HARDCODED IN DIM
  objf = function(x::Vector, grad::Vector)
     #FIXMEL WHY THREE?
    shape = Linear(structure_points(x, 3))
#     println("measure is", measure(shape.points), "shape is", shape)
    interval_cost(shape, intervals, image_intervals, measures)
#     (l::Linear, intervals::Vector, interval_measures::Vector)
  end
end

function *(l::Linear, n)
  n_points = 2
  f = function(x) x*n end
  objf = generate_objf(f, l, n_points, 10)
  opt = Opt(:LN_COBYLA, n_points*3)
  # lower_bounds!(opt, [-Inf, 0.])
  xtol_rel!(opt,1e-4)
  min_objective!(opt, objf)
  init_params = rand(n_points * 3)

  # Constraints
  #FIXME : FALSE LOWERBOUND OF ZERO ON PARAMS
  lower_bounds!(opt::Opt, zeros(init_params))

  # Create initial parameters
  projection = project_i(l)
  low = lower_bound(projection)
  up = upper_bound(projection)
  p = linear_interpolate([low],[up],n_points - 2)
  init_params = map(x->[1,x[1],f(x[1])],p)
  println("initial_params:", init_params)

  (minf,minx,ret) = optimize(opt, vcat(init_params...))
  println("got $minf at $minx after iterations (returned $ret)")
  Linear(structure_points(minx,3))
end

## Example
x = uniform_real(0,5,3)
a = x * 3
measure(a.points)
a.points

n_points = 2
f = function(x) x*3 end
objf = generate_objf(f, x, n_points, 20)
d = dist([5,0],[0,15])
h = 1/d

x_proj = project_i(x)
intervals = interpolate_n_intervals(lower_bound(x_proj), upper_bound(x_proj), 5)
map(i->interval_measure(x.points,i,1),intervals)
map(i->interval_measure(a.points, i,1),intervals)
map(i->interval_measure(a.points, convert(Vector, Interval(i)*3),2),intervals)

