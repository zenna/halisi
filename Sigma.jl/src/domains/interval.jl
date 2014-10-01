# Optiso 1. Make Interval and abstract type
# Option 2. Make open/closed boundaries a property
# Make just separate types

# Interval
# LInterval
# RInterval
# LRInterval

immutable Interval <: Box
  l::Real
  u::Real
  Interval(l,u) =  if u > l new(l, u) else new(u,l) end
end

# # CODEREVIEW - DO I NEED OPEN/CLOSED INTERVAL
# immutable Interval{T <: Real} <: Domain{T}
#   l::T
#   u::T
#   Interval{T <: Real}(l::T,u::T) =  if u > l new{T}(l, u) else {T}new(u,l) end
# end
# Interval{T}(l::T,u::T) = Interval{T}(u,l)

# # Lower bound is open
# immutable LInterval{T <: Real} <: Domain{T}
#   l::T
#   u::T
#   LInterval{T <: Real}(l::T,u::T) =  if u > l new(l, u) else new(u,l) end
# end
# LInterval{T <: Real}(l::T,u::T) =  if u > l LInterval{T}(l, u) else LInterval{T}(u,l) end

# # Upper bound is open
# immutable UInterval{T <: Real} <: Domain{T}
#   l::T
#   u::T
#   Interval(l::T,u::T) =  if u > l new(l, u) else new(u,l) end
# end

# # Both bounds open
# immutable LUInterval{T <: Real} <: Domain{T}
#   l::T
#   u::T
#   Interval(l::T,u::T) =  if u > l new(l, u) else new(u,l) end
# end

# # >(x::Interval, y::Interval) = if overlap(x,y) TF elseif x.l > y.u T else F end



# CODEREVIEW: SHOULD WE ASSERT SIZE?
Interval(v::Vector) = Interval(v[1],v[2])

# CODEREVIEW: SHOULDN'T THIS OVERLOAD CONVERT
to_intervals(b::Box) = [Interval(b.intervals[:,i]) for i = 1:num_dims(b)]
unitinterval(::Type{Interval}) = Interval(0.,1.)

# FIX THESE ERROS
INTERVAL_DIM_ERR = "Interval has only one single dimension"
num_dims(i::Interval) = 1
nth_dim_interval(i::Interval, dim_n::Uint) = if dim_n == 1 i else error(INTERVAL_DIM_ERR) end

# CODEREVIEW: TESTME
subsumes(x::Interval, y::Interval) = y.l >= x.l && y.u <= x.u

# CODEREVIEW: TESTME
overlap(x::Interval, y::Interval) = y.l < x.u && x.l < y.u

promote_rule{T<:ConcreteReal}(::Type{T}, ::Type{Interval}) = Interval
# A concrete number can be concerted to an interval with no width
convert(::Type{Interval}, c::ConcreteReal) = Interval(c, c)

## ====================================
## Interval Arithmetic and Inequalities

# CODEREVIEW - TEST ALL OF THESE
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

# CODEREVIEW: Generalise to even powers
function sqr(x::Interval)
  a,b,c,d = x.l * x.l, x.l * x.u, x.u * x.l, x.u * x.u
  Interval(max(min(a,b,c,d),0),max(a,b,c,d,0))
end

function *(x::Interval, y::Interval)
  a,b,c,d = x.l * y.l, x.l * y.u, x.u * y.l, x.u * y.u
  Interval(min(a,b,c,d),max(a,b,c,d))
end

# is c inside the interval
in(c::ConcreteReal, y::Interval) = y.l <= c <= y.u

# CODEREVIEW
inv(x::Interval) = Interval(1/x.u,1/x.l)

 # Ratz Interval Division
function /(x::Interval, y::Interval)
  a,b,c,d = x.l,x.u,y.l,y.u
  if !(0 ∈ y)
    x * inv(y)
  elseif (0 ∈ x)
    Interval(-Inf,Inf)
  elseif b < 0 && c < d == 0
    Interval(b/c,Inf)
  elseif b < 0 && c < 0 < d
    Interval(-Inf,Inf)
  elseif b < 0 && 0 == c < d
    Interval(-Inf,b/d)
  elseif 0 < a && c < d == 0
    Interval(-Inf,a/c)
  elseif 0 < a && c < 0 < d
    Interval(-Inf,Inf)
  elseif 0 < a && 0 == c < d
    Interval(a/d, Inf)
  else
    Inf
  end
end

flip(x::Interval) = Interval(-x.l,-x.u)
lower_bound_at(x::Interval, lower_bound) = Interval(max(x.l,0), max(x.u,0))

function abs(x::Interval)
  if x.l >= 0.0 && x.u >= 0.0 x
  elseif x.u >= 0.0 Interval(0,max(abs(x.l), abs(x.u)))
  else lower_bound_at(flip(x),0.0)
  end
end

/(c::ConcreteReal, x::Interval) = convert(Interval,c) / x
/(x::Interval, c::ConcreteReal) = x / convert(Interval,c)

## =========
## Merging
function ⊔(a::Interval, b::Interval)
#   IntervalDisj(a,b)
  l = min(a.l,b.l)
  u = max(a.u, b.u)
  Interval(l,u)
end

function ⊔(a::Interval, b::ConcreteReal)
#   IntervalDisj(a,b)
  l = min(a.l,b)
  u = max(a.u,b)
  Interval(l,u)
end

⊔(x::Interval) = x
# ⊔(a::ConcreteReal, b::ConcreteReal) = Interval(a,b)
# ⊔{R<:ConcreteReal}(a::Vector{R}, b::Vector{R}) = [⊔(a[i],b[i]) for i = 1:length(a)]
# ⊔{T,R}(a::Array{T}, b::Array{R}) = map((a,b)->⊔(a,b),a,b)

## ===================
## Arrays of intervals
# FIXME:$ PERFORMANCE
*(x::Interval, y::Array{Float64}) = map(e->x*e,y)

## ===========
## Conversions

function convert(::Type{NDimBox}, i::Vector{Interval})
  intervals = Array(Float64,2,length(i))
  for j in 1:length(i)
    intervals[:,j] = [i[j].l i[j].u]
  end
  NDimBox(intervals)
end

## ========
## Splitting
function split_box(i::Interval, split_point::Float64)
  [Interval(i.l, split_point), Interval(i.u,split_point)]
end
middle_split(is::Vector{Interval}) = map(to_intervals,middle_split(convert(NDimBox, is,)))
measure(i::Interval) = i.u - i.l

## ========
## Sampling
rand_interval(a::Float64, b::Float64) = a + (b - a) * rand()
rand(b::Box) = [apply(rand_interval,b.intervals[:,i]) for i = 1:num_dims(b)]
