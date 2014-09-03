using Sigma
using Gadfly
import Sigma.Omega
x = uniform(1,0,10)
y = x + normal(2,0,1)

type
  a::Array
end

function cons(x::RandomVariable, y::RandomVariable)
end


prob_deep(x > 0.5)
prob_deep(x > 0.5, max_depth = 10)
cond_prob_deep(x > 0.2, x < 0.1)

y = normal(1,0,2)
z = y * x
prob_deep((sqr(z) > 0.5) & (sqr(z) < 0.6))
y = uniform(0,0,1)
y
if_rv = @If z > 2 normal(2,0,3) z
cond_prob_deep(if_rv < 2, y > 0)

plot_psuedo_density(if_rv,0.,3.)

function the_formula(a)
  x,y = to_intervals(a)
  to_intervals(ndcube(-100.0,100.0,2))
  big_circle = sqrt(sqr(x) + sqr(y)) <= 5
  ear = sqrt(sqr(x+5) + sqr(y-2)) <= 3
  eye_one = sqrt(sqr(x-2) + sqr(y-2)) <= 1
  pupil_one = sqrt(sqr(x-2.5) + sqr(y-2.5)) <= .3
  eye_two = sqrt(sqr(x+2) + sqr(y-2)) <= 1
  pupil_two = sqrt(sqr(x+1.5) + sqr(y-2.5)) <= .3
  mouth = (sqrt(sqr(x) + sqr(y+1)) <= 1) & (y < -1)
  (big_circle & !eye_one & !eye_two & !mouth) | pupil_one | pupil_two
end

# import Sigma.num_dims

t = pre_deepening(the_formula, T, ndcube(-10.0,10.0,2), max_depth = 12)
boxes = filter(n->n.status==SAT, t.nodes)
plot_boxes(map(b->b.data,boxes))

# function plot_boxes(bs)
#   x_min = Float64[]
#   y_min = Float64[]
#   x_max = Float64[]
#   y_max = Float64[]

#   for b in bs
#     push!(x_min, b.intervals[1,1])
#     push!(x_max, b.intervals[2,1])
#     push!(y_min, b.intervals[1,2])
#     push!(y_max, b.intervals[2,2])
#   end

#   plot(x_min=x_min, x_max=x_max, y_min=y_min,color=rand(length(bs)), y_max=y_max,Geom.rectbin)
# end


# ## e.g. plot_psuedo_density((normal(0,0,1) + uniform(1,0,1))
# function moving_normal(num_steps::Integer)
#   num_steps = num_steps - 1
#   X = Array(Any, num_steps + 1)
#   X[1] = uniform(0,0,1)

#   for i = 1:num_steps
#     X[i+1] = @If (X[i] + 2 < 10) (X[i] + 2) X[i]
#   end
#   X
# end

# X = moving_normal(20)
# # t = pre_deepening(X[end] < 10, T, Omega(), max_depth = 50)
# # length(t.nodes)
# @profile prob_deep((X[end] > 8) & (X[end] < 8.2))
# Profile.print(format=:flat)
# using ProfileView
# plot_psuedo_density_2(X[end], 8., 12.,n_bars = 20)
