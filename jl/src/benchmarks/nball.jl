# See how preimage refinement scales with increasing d
using Sigma

function unit_n_ball(num_dims::Integer)
  rvs = [uniform(i,-1.,1.) for i in 1:num_dims]
  cond = sqrt(sum(map(sqr,rvs))) < 1
  prob_deep(cond,max_depth = 7)
end

function unit_n_box(num_dims::Integer)
  rvs = [uniform(i,-1.,1.) for i in 1:num_dims]
  small_rvs  = map(x->(x>-0.1) & (x < 0.1), rvs)
  cond = apply(&, small_rvs)
  prob_deep(cond,max_depth = 7)
end

unit_n_box(4)

# unit n-sphere volume (n-ball surface area)
S(n) = n == 0 ? 2 : 2π*V(n-1)

# unit n-ball volume
V(n) = n == 0 ? 1 : S(n-1) / n

# ratio of n_ball volume to enclosing unit_sphere
ratio_V(n) = V(n) / 2^n

ratio_V(14)

# See error as dim increases
xs = 2:8
ys = [ratio_V(i) for i = 2:8]
prob_deep(unit_n_ball(15), max_depth = 30)
errors = [unit_n_ball(i) for i = 2:8]
plot(x=xs, y=ys, ymin=map(x->x[1],errors), ymax=map(x->x[2],errors), Geom.point, Geom.errorbar)
