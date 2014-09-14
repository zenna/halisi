using Sigma
immutable EnvVar{K,V}
  worlds::Dict{K,V}

  EnvVar() = new(Dict{K,V}())
  EnvVar(worlds::Dict{K,V}) = new(worlds)
end
EnvVar() = EnvVar{Any,Any}()
singleton{T}(x::T) = (s = Set{T}();push!(s,x);s)
const noconstraints = Set{Symbol}()

ConcreteValue = Union(Float64, Int64, Bool)

for op = (:+, :-, :*, :>, :>=, :<=, :<, :&, :|)
  @eval begin
    function ($op)(x::EnvVar, y::EnvVar)
      ret = EnvVar() #FIXME: MAKE TYPE STABLE
      for xworld in x.worlds
        xworldid = xworld[1]
        if haskey(y.worlds,xworldid)     # If we share the same world
          ret.worlds[xworldid] = ($op)(xworld[2],y.worlds[xworldid])
        else
          for yworld in y.worlds
            conjworld = union(xworldid,yworld[1])
            ret.worlds[conjworld] = ($op)(xworld[2],yworld[2])
          end
        end
      end
      ret
    end
  end

  @eval begin
    function ($op)(x::EnvVar, y::ConcreteValue)
      ret = EnvVar() #FIXME: MAKE TYPE STABLE
      for xworld in x.worlds
        xworldid = xworld[1]
        ret.worlds[xworldid] = ($op)(xworld[2],y)
      end
      ret
    end
  end

  @eval begin
    function ($op)(y::ConcreteValue, x::EnvVar)
      ret = EnvVar() #FIXME: MAKE TYPE STABLE
      for xworld in x.worlds
        xworldid = xworld[1]
        ret.worlds[xworldid] = ($op)(y,xworld[2])
      end
      ret
    end
  end
end

symbol(string(:(x>3)))
gensym(:(x>3))


function update_ret!(a::EnvVar,ret::EnvVar, worldids)
  if isa(a, EnvVar)
    for aworld in a.worlds
      ret.worlds[union(worldids, aworld[1],$idtrue)] = aworld[2]
      @show ret
    end
  else
    ret.worlds[union(world[1],$idtrue)] = a
  end
end

macro Iff(condition, conseq, alt)
  local idtrue = singleton(gensym())
  local idfalse = singleton(gensym())
  q =
  quote
  c =  $(esc(condition));
  local ret
  if isa(c, EnvVar)
    ret = EnvVar()
    for world in c.worlds
      if world[2] === T || world[2] === true
        ret.worlds[world[1]] = $(esc(conseq))
      elseif world[2] === F || world[2] === false
        ret.worlds[world[1]] = $(esc(alt))
      elseif world[2] === TF
        a = $(esc(conseq))
        @show a
        if isa(a, EnvVar)
          for aworld in a.worlds
            ret.worlds[union(world[1], aworld[1],$idtrue)] = aworld[2]
           @show ret
          end
        else
          ret.worlds[union(world[1],$idtrue)] = a
        end

        b = $(esc(alt))
        if isa(b, EnvVar)
          for bworld in b.worlds
            ret.worlds[union(world[1], bworld[1],$idfalse)] = bworld[2]
          end
          @show ret
        else
          ret.worlds[union(world[1],$idfalse)] = b
        end
      else
        println("error:", world[2])
        throw(DomainError())
      end
    end
  end
  ret
  end
  return q
end




typealias IntervalEnvDict Dict{Set{Symbol},Interval}
typealias IntervalEnvVar EnvVar{Set{Symbol}, Interval}

# Example
X = EnvVar{Set{Symbol},Interval}(IntervalEnvDict([noconstraints => Interval(3,5),
                            singleton(symbol("x>2")) => Interval(0,2)]))

Y = EnvVar{Set{Symbol},Interval}(IntervalEnvDict([noconstraints => Interval(0,1),
                                   singleton(symbol("y<2")) => Interval(0,4)]))



function intervalenvvar{T<:Real}(x::T,y::T)
  EnvVar{Set{Symbol},Interval}(IntervalEnvDict([noconstraints => Interval(x,y)]))
end

# Example
X = intervalenvvar(4.0,6.0)
Y = intervalenvvar(3.0,7.0)
# X = uniform(0,4,6)
# Y = uniform(1,3,7)


c = @Iff((X > 5), X, Y)
c
c

A,B = @Iff (X > 5) (X+4,Y+5) (X-5,Y-6)