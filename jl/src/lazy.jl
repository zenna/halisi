immutable Var{T}
  id::T
  σ::Vector{Dict{T, IntervalDisj}}
end

newid() = gensym()

function Var(i::IntervalDisj)
  id = newid()
  Var(id,[[id => i]])
end

function vars(is::Vector{IntervalDisj})
  vs = Array(Var, length(is))
  ids = [newid() for i in is]
  env = Dict{Symbol, IntervalDisj}()
  for i = 1:length(is)
    env[ids[i]] = is[i]
  end
  [Var(ids[i],[env]) for i = 1:length(is)]
end

for op = (:+, :-, :*, :/, :>, :>=, :<=, :<)
  @eval begin
    function ($op)(x::Var, y::Var)
      id = symbol(string(x.id, $op, y.id))

      # Some operations e.g division may create disjunctions
      # In this case I will need to create a newenv
      newenvs = Dict[]
      for env in x.σ
        xi = env[x.id]
        yi = env[y.id]
        res = ($op)(xi,yi)
        @show numelems(res)
        if numelems(res) > 1
          # Don't make more than necessary copies,
          # modify original
          env[id] = nthinterval(res,1)
          for i = 2:numelems(res)
            newenv = copy(env)
            newenv[id] = nthinterval(res,i)
            push!(newenvs,newenv)
          end
        else
          env[id] = ($op)(xi,yi)
        end
      end

      # Push newenvs to xs (and ys as a result)
      for newenv in newenvs
        push!(x.σ, newenv)
      end
      println("id is", id)
      Var(id,x.σ)
    end
  end
end

using Sigma

# Example
X, Y = Sigma.vars([IntervalDisj(0,3), IntervalDisj(5,10)])
X * Y
X

function string(x::Var)
  s = String[]
  for env in x.σ
    for entry in env
      entrystring = string(entry[1],"=>",entry[2],"\n")
      push!(s,entrystring)
    end
    push!(s,"\n|n")
  end
  s
end

# Printing
print(io::IO, x::Var) = print(io, string(x))
show(io::IO, x::Var) = print(io, string(x))
showcompact(io::IO, x::Var) = print(io, string(x))
