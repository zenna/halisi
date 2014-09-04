using Sigma

## e.g. plot_psuedo_density((normal(0,0,1) + uniform(1,0,1))
function moving_normal(num_steps::Integer)
  num_steps = num_steps - 1
  X = Array(Any, num_steps + 1)
  X[1] = normal(0,0,6)

  for i = 1:num_steps
    X[i+1] = @If (X[i]< 10) (X[i] + 2) X[i]
  end
  X
end

X = moving_normal(10)
X
# t = pre_deepening(X[end] < 10, T, Omega(), max_depth = 50)
# length(t.nodes)
@profile prob_deep((X[end] > 8) & (X[end] < 8.2))
Profile.print(format=:flat)
using ProfileView
plot_psuedo_density(X[2], 0., 15.,n_bars = 500)
