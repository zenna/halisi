# Miscellaneous binary examples
using Sigma
import Sigma.gen_arguments
using Gadfly

plot_2d_boxes(ineq_pre)

## Restrict to epsilon thick circle
circle(radius, x, y) =  tolerant_eq(sqrt(sqr(x+0.001) + sqr(y-0.001)), radius, 0.1)
circle_pre = pre_recursive((x,y)->circle(5.0,x,y), T, [ndcube(-100.0,100.0,2)],max_depth = 15,box_budget = typemax(Int64))
plot_2d_boxes(circle_pre)

two_halves(radius,x,y) = @If x > 0 circle(radius, x, y) circle(radius/2, x, y)
two_circles(radius,x,y) = circle(radius, x, y) | circle(radius/2, x, y)

function emilys_formula(x,y)
  big_circle = sqrt(sqr(x) + sqr(y)) <= 5
  ear = sqrt(sqr(x+5) + sqr(y-2)) <= 3
  eye_one = sqrt(sqr(x-2) + sqr(y-2)) <= 1
  pupil_one = sqrt(sqr(x-2.5) + sqr(y-2.5)) <= .3
  eye_two = sqrt(sqr(x+2) + sqr(y-2)) <= 1
  pupil_two = sqrt(sqr(x+1.5) + sqr(y-2.5)) <= .3
  mouth = (sqrt(sqr(x) + sqr(y+1)) <= 1) & (y < -1)
  (big_circle & !eye_one & !eye_two & !mouth) | pupil_one | pupil_two
end

# preimage2 = pre((x,y)->two_circles(5.0,x,y), T, [ndcube(-100.0,100.0,2)],n_iters = 14)
preimage2 = pre_recursive(emilys_formula, T, [ndcube(-100.0,100.0,2)],max_depth = 15)
# preimage2 = pre(heart, T, [ndcube(-100.0,100.0,2)],n_iters = 14)

preimage_emily_deep = pre_deepening(emilys_formula,T, ndcube(-100.0,100.0,2), max_depth = 11)
preimage_emily_deep = Sigma.tree_to_boxes(preimage_emily_deep)
plot_2d_boxes(preimage_emily_deep)

function plot_2d_boxes(bs::Vector)
  x_min = Float64[]
  y_min = Float64[]
  x_max = Float64[]
  y_max = Float64[]

  for b in bs
    push!(x_min, b.intervals[1,1])
    push!(x_max, b.intervals[2,1])
    push!(y_min, b.intervals[1,2])
    push!(y_max, b.intervals[2,2])
  end

  plot(x_min=x_min, x_max=x_max, y_min=y_min,color=rand(length(bs)), y_max=y_max,Geom.rectbin)
end
