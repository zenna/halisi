# Abstract Boolean Types
immutable AbstractBool
  x::Set{Bool}
end

const T = AbstractBool(Set(true))
const F = AbstractBool(Set(false))
const TF = AbstractBool(Set(true, false))

function !(b::AbstractBool)
  if b == T
    F
  elseif b == F
    T
  elseif b == TF
    TF
  end
end

## ==================
## Boolean Arithmetic
(==)(x::AbstractBool, y::AbstractBool) = x === TF || y === TF ? TF : x === T && y === T || x === F && y === F
(==)(x::AbstractBool, y::Bool) = x == convert(AbstractBool, y)
(==)(y::Bool, x::AbstractBool) = x == y

|(x::AbstractBool, y::AbstractBool) = if x === TF || y === TF TF elseif x === T && y === T T else F end
|(x::AbstractBool, y::Bool) = |(x,convert(AbstractBool,y))
|(y::Bool, x::AbstractBool) = |(convert(AbstractBool,y), x)

function (&)(x::AbstractBool, y::AbstractBool)
  if x === F || y === F F
  elseif x === TF || y === TF TF
  else T
  end
end

(&)(x::AbstractBool, y::Bool) = x & convert(AbstractBool, y)
(&)(y::Bool, x::AbstractBool) = convert(AbstractBool, y) & x

subsumes(x::AbstractBool, y::AbstractBool) = x == TF || x == y
subsumes(x::AbstractBool, y::Bool) = subsumes(x,convert(AbstractBool, y))

promote_rule(::Type{Bool}, ::Type{AbstractBool}) = AbstractBool
convert(::Type{AbstractBool}, b::Bool) = if b T else F end
overlap(x::AbstractBool, y::AbstractBool) = !((x == T && y == F) || (x == F && y == T))
overlap(x::Bool,y::Bool) = x == y
overlap(x::AbstractBool, y::Bool) = overlap(x,convert(AbstractBool, y))
overlap(x::Bool, y::AbstractBool) = overlap(convert(AbstractBool, x),y)


merge_interval(a::AbstractBool, b::AbstractBool) = a == b ? a : TF
merge_interval(a::Bool, b::AbstractBool) = merge_interval(convert(AbstractBool,a),b)
merge_interval(a::AbstractBool, b::Bool) = merge_interval(a,convert(AbstractBool,b))
merge_interval(a::Bool, b::Bool) = a == b ? a : TF

## =================
## Logical Operators

macro If(condition, conseq, alt)
  e = :(c = $condition;
        if isa(c, Bool)
          c ? $conseq : $alt
        elseif c == T || c == F
          convert(Bool,c) ? $conseq : $alt
        elseif c == TF
          a = $conseq
          b = $alt
          merge_interval(a,b)
        else
          error
        end)
  return e
end
