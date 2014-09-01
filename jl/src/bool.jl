import Base.abs, Base.show, Base.print, Base.showcompact

# Abstract Boolean Types
immutable AbstractBool
  v::Uint8
end

const T = AbstractBool(0x0)
const F = AbstractBool(0x1)
const TF = AbstractBool(0x2)

function !(b::AbstractBool)
  if b === T
    F
  elseif b === F
    T
  elseif b === TF
    TF
  end
end

## ==================
## Boolean Arithmetic

(==)(x::AbstractBool, y::AbstractBool) = x === TF || y === TF ? TF : x === T && y === T || x === F && y === F
(==)(x::AbstractBool, y::Bool) = x == convert(AbstractBool, y)
(==)(y::Bool, x::AbstractBool) = x == y

function (|)(x::AbstractBool, y::AbstractBool)
  if x === T || y === T T
  elseif x === TF || y === TF TF
  elseif x === T && y === T T else F
  end
end
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

subsumes(x::AbstractBool, y::AbstractBool) = x === TF || x === y
subsumes(x::AbstractBool, y::Bool) = subsumes(x,convert(AbstractBool, y))

promote_rule(::Type{Bool}, ::Type{AbstractBool}) = AbstractBool
convert(::Type{AbstractBool}, b::Bool) = if b T else F end
overlap(x::AbstractBool, y::AbstractBool) = !((x === T && y === F) || (x === F && y === T))
overlap(x::Bool,y::Bool) = x == y
overlap(x::AbstractBool, y::Bool) = overlap(x,convert(AbstractBool, y))
overlap(x::Bool, y::AbstractBool) = overlap(convert(AbstractBool, x),y)

merge_interval(a::AbstractBool, b::AbstractBool) = a === b ? a : TF
merge_interval(a::Bool, b::AbstractBool) = merge_interval(convert(AbstractBool,a),b)
merge_interval(a::AbstractBool, b::Bool) = merge_interval(a,convert(AbstractBool,b))
merge_interval(a::Bool, b::Bool) = a == b ? a : TF

# Flip interval around 0 axis,
# TODO make this parametric on symmetry point

## ============
## Control Flow

make_rv(v, ω) = isa(v,RandomVariable) ? v(ω)  : v

macro If(condition, conseq, alt)
  q =
  quote
  c =  $(esc(condition));
  if isa(c, RandomVariable)
    (ω)->begin
          d = c(ω)
          if isa(d, Bool)
            d ? make_rv($(esc(conseq)), ω) : make_rv($(esc(alt)), ω)
          elseif d === T
            make_rv($(esc(conseq)), ω)
          elseif d === F
            make_rv($(esc(alt)), ω)
          elseif d === TF
            a = make_rv($(esc(conseq)), ω)
            b = make_rv($(esc(alt)), ω)
            merge_interval(a,b)
          else
            error
          end
        end
  elseif isa(c, Bool)
    c ? $(esc(conseq)) : $(esc(alt))
  elseif c === T || c === F
    convert(Bool,c) ? $(esc(conseq)) : $(esc(alt))
  elseif c === TF
    a = $(esc(conseq))
    b = $(esc(alt))
    merge_interval(a,b)
  else
    error
  end
  end
  return q
end

macro While(c, todo)
  quote
    while $(esc(c)) === true || $(esc(c)) === T || $(esc(c)) === TF
      $(esc(todo));
    end
  end
end

## Printing
string(x::AbstractBool) = [0x0 => "{T}", 0x1 => "{F}", 0x2 => "{T,F}"][x.v]
print(io::IO, x::AbstractBool) = print(io, string(x))
show(io::IO, x::AbstractBool) = print(io, string(x))
showcompact(io::IO, x::AbstractBool) = print(io, string(x))
