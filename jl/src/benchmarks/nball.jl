# See how preimage refinement scales with increasing d
using Sigma
using Gadfly

function unit_n_ball(num_dims::Integer)
  rvs = [uniform(i,-1,1) for i in 1:num_dims]
  cond = sqrt(sum(map(sqr,rvs))) < 1
  prob_deep(cond,max_depth = 7)
end

# unit n-sphere volume (n-ball surface area)
S(n) = n == 0 ? 2 : 2π*V(n-1)

# unit n-ball volume
V(n) = n == 0 ? 1 : S(n-1) / n

# ratio of n_ball volume to enclosing unit_sphere
ratio_V(n) = V(n) / 2^n

# See error as dim increases
xs = 2:8
ys = [ratio_V(i) for i = 2:8]
errors = [unit_n_ball(i) for i = 2:8]
plot(x=xs, y=ys, ymin=map(x->x[1],errors), ymax=map(x->x[2],errors), Geom.point, Geom.errorbar)
