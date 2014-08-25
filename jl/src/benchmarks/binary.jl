# Miscellaneous binary examples
using Sigma

## Restrict to epsilon thick circle
circle(radius, x, y) =  tolerant_eq(sqrt(sqr(x) + sqr(y)), radius, 0.1)
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
  (big_circle & !eye_one & !eye_two & !mouth) | ear #| eye_two
end

# preimage1 = pre_recursive((x,y)->circle(5.0,x,y), T, [ndcube(-100.0,100.0,2)],max_depth = 15)
# preimage2 = pre((x,y)->two_circles(5.0,x,y), T, [ndcube(-100.0,100.0,2)],n_iters = 14)
preimage2 = pre(emilys_formula, T, [ndcube(-100.0,100.0,2)],n_iters = 14)
plot_2d_boxes(preimage2)



