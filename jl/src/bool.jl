# Abstract Boolean Types

#CODEREVIEW - AbstractBool Should be related in type hierarchy to Bool
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

# REVIEW: Tests for equality. Also is overriding equality even the right thing to do.
(==)(x::AbstractBool, y::AbstractBool) = x === TF || y === TF ? TF : x === T && y === T || x === F && y === F
(==)(x::AbstractBool, y::Bool) = x == convert(AbstractBool, y)
(==)(y::Bool, x::AbstractBool) = x == y

function (|)(x::AbstractBool, y::AbstractBool)
  if x === T || y === T T
  elseif x === TF || y === TF TF
  else F
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

# REVIEW: Tests for overlap
overlap(x::AbstractBool, y::AbstractBool) = !((x === T && y === F) || (x === F && y === T))
overlap(x::Bool,y::Bool) = x == y
overlap(x::AbstractBool, y::Bool) = overlap(x,convert(AbstractBool, y))
overlap(x::Bool, y::AbstractBool) = overlap(convert(AbstractBool, x),y)

# Review: Tests for Meet
⊔(a::AbstractBool) = a
⊔(a::AbstractBool, b::AbstractBool) = a === b ? a : TF
⊔(a::Bool, b::AbstractBool) = ⊔(convert(AbstractBool,a),b)
⊔(a::AbstractBool, b::Bool) = ⊔(a,convert(AbstractBool,b))
⊔(a::Bool, b::Bool) = a == b ? a : TF

## ============
## Control Flow

# REVIEW: Make this a type dispatch
make_rv(v, ω) = isa(v,RandomVariable) ? v(ω)  : v

# REVIEW SEPARATE THIS OUT INTO FUNCTIONS
macro If(condition, conseq, alt)
  local idtrue = singleton(gensym())
  local idfalse = singleton(gensym())
  q =
  quote
  c =  $(esc(condition));
#   println("enterin$$g the if",$idtrue)
  if isa(c, RandomVariable)
    (ω)->begin
          d = c(ω)
          if isa(d, EnvVar)
            ret = EnvVar()
            for world in d.worlds
              if world[2] === T || world[2] === true
                ret.worlds[world[1]] = make_rv($(esc(conseq)),ω)
              elseif world[2] === F || world[2] === false
                ret.worlds[world[1]] = make_rv($(esc(alt)),ω)
              elseif world[2] === TF
                a = make_rv($(esc(conseq)),ω)
                constraintstrue = union(world[1],$idtrue)
                update_ret!(a,ret, constraintstrue)

                b = make_rv($(esc(alt)),ω)
                constraintsfalse = union(world[1],$idfalse)
                update_ret!(b,ret, constraintsfalse)

              else
                println("error:", world[2])
                throw(DomainError())
              end
            end
          elseif isa(d, Bool)
            d ? make_rv($(esc(conseq)),ω) : make_rv($(esc(alt)),ω)
          elseif d === T
            make_rv($(esc(conseq)),ω)
          elseif d === F
            make_rv($(esc(alt)),ω)
          elseif d === TF
            a = make_rv($(esc(conseq)),ω)
            b = make_rv($(esc(alt)),ω)
            ⊔(a,b)
          else
            println("condition is " , d)
            throw(DomainError())
          end
        end

  elseif isa(c, EnvVar)
    ret = EnvVar()
    for world in c.worlds
#       @show world[2]
#       println("length=", length(c.worlds))
      if world[2] === T || world[2] === true
        v = $(esc(conseq))
        update_ret!(v,ret, world[1])

#                 println("DID I FIND YOU BITCH?")
#         ret.worlds[world[1]] = $(esc(conseq))
#                     println("NOO YOU BITCH?")

      elseif world[2] === F || world[2] === false
#             println("DID I FIND YOU BITCH?")
        v = $(esc(alt))
        update_ret!(v,ret, world[1])

#         println("LETS SEE BITCH")
#         @show v
#         ret.worlds[world[1]] = v
#         println("NO BITCH?")
      elseif world[2] === TF
        a = $(esc(conseq))
        constraintstrue = union(world[1],$idtrue)
        update_ret!(a,ret, constraintstrue)

        b = $(esc(alt))
        constraintsfalse = union(world[1],$idfalse)
        update_ret!(b,ret, constraintsfalse)
#         println("leaving")
      else
        println("error:", world[2])
        throw(DomainError())
      end
    end
  elseif isa(c, Bool)
    c ? $(esc(conseq)) : $(esc(alt))
  elseif c === T
    $(esc(conseq))
  elseif c === F
    $(esc(alt))
  elseif c === TF
    a = $(esc(conseq))
    b = $(esc(alt))
    ⊔(a,b)
  else
    throw(DomainError())
  end
#   println("leaving the if", $idtrue)
  end
  return q
end

# REVIEW: IS THIS STILL RELEVANT? TEST OR REMOVE
macro While(c, todo)
  quote
    while $(esc(c)) === true || $(esc(c)) === T || $(esc(c)) === TF
      $(esc(todo));
    end
  end
end

## Printing
# REVIEW: Make this based on list, what is relevance of showcompact, show
string(x::AbstractBool) = [0x0 => "{T}", 0x1 => "{F}", 0x2 => "{T,F}"][x.v]
print(io::IO, x::AbstractBool) = print(io, string(x))
show(io::IO, x::AbstractBool) = print(io, string(x))
showcompact(io::IO, x::AbstractBool) = print(io, string(x))
