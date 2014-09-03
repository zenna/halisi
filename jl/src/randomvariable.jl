typealias RandomVariable typeof(+)

# Lift primitive operators to work on random variables
# A function applied to a random variable evaluates to
# a random variable
for op = (:+, :-, :*, :/, :./, :&, :|, :$, :>, :>=, :<=, :<)
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

## Array Operations
# for op = (:*,:./)
#   @eval begin
#     function ($op){T <:Number}(a::RandomVariable, arr::Array{T})
#       f(ω) = map(b->($op)(a(ω),b), arr)
#     end
#   end
# end

for op = (:!,:sqrt,:sqr,:abs)
  @eval begin
    function ($op)(a::RandomVariable)
      f(ω) = ($op)(a(ω))
    end
  end
end

getindex(rv::RandomVariable, i::Integer, j::Integer) = ω->rv(ω)[i]
setindex!{T}(rv::RandomVariable,v::T,i::Integer)

function RandomArray(Any,sizex)


function cons(x::RandomVariable, y::RandomVariable)
  ω->[x(ω), y(ω)]
end

c = cons(uniform(0,0,1), uniform(1,0,1))
c[1] = 4.