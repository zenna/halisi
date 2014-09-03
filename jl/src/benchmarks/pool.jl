# Pool like simulation/inference
using Sigma

points_to_parametric(p1,p2) = [p1 points_to_vec(p2,p1)]
points_to_parametric(edge) = points_to_parametric(edge[:,1], edge[:,2])
points_to_vec(p1, p2) = [p1[1] - p2[1], p1[2] - p2[2]]
points_to_vec(edge) = points_to_vec(edge[:,1], edge[:,2])

# A parametric line is composed of a origin and a direction vector
# Each is a column vector

# Where if anywhere, along p does it interect segment
function intersect_segments(p, q)
  w = p[:,1] - q[:,1]
  u = p[:,2]
  v = q[:,2]
  (v[2] * w[1] - v[1] * w[2]) / (v[1] * u[2] - v[2] * u[1])
end

parametric_to_point(p, s) = p[:,1] + s * p[:,2]
dotty(a,b) = a[1]*b[1] + a[2]*b[2]
perp(v) = [-v[2],v[1]]
normalise(v) = v / 5.
reflect(v,q) = (n = perp(normalise(q)); v = normalise(v); v - 2 * (dotty(v,n) * n))

function smallest(a,ss)
  issmallest = true
  for s in ss
    issmallest = issmallest & (a < s)
  end
  issmallest
end

function bounce(p,s,o)
  v = p[:,2]
  reflection = reflect(v,o)
  new_pos = p[:,1] + p[:,2] * s
  [new_pos reflection]
end

# function simulate(num_steps::Integer, obs)
obstacles = Array[[2.01 3.01; 1.02 9],
                  [0.5 3.08; 2.02 9.04],
                  [1.5 9.99; 7.02 5.04]]

num_steps = 2
obs = map(points_to_parametric, obstacles)
num_steps = num_steps - 1
start_pos = [1, 1]
dir = [uniform(1,-1,1),uniform(2,-1,1)]
pos_parametric = Array(Any,num_steps + 1)
pos_parametric[1] = [start_pos dir]

for i = 1:num_steps
  i = 1
  p = pos_parametric[i]
  ss = Array(Any, length(obs))
  for j = 1:length(obs)
    d = obs[j]
    println("d is", p)
    ss[j] = intersect_segments(p, obs[j])
  end

  #     ss = [intersect_segments(p, o) for o in obs]
  # For each obstacle ask whether or not this s is smaller than all others
  # for each obstacle that this is yes, do it again

  pos_parametric[i+1] = @If(smallest(ss[1],ss), bounce(p,ss[1],obs[2]),
                            @If(smallest(ss[2],ss),bounce(p,ss[2],obs[2]),
                                bounce(p,ss[3],obs[3])))
end

# simulate(3, obstacles)
pos_parametric[2]([0.1,0.2,0.3])
# ## ===
# ## Vis
# using PyPlot

# x = linspace(0,2*pi,1000); y = sin(3*x + 4*cos(2*x));
# plot(x, y, color="red", linewidth=2.0, linestyle="--")
# title("A sinusoidally modulated sinusoid")
# show()

# function line_layer(b::Box)
#   path_box  = rand(b)
#   path = reshape(path_box,2,int(length(path_box)/2))
#   layer(x=path[1,:], y=path[2,:], Geom.line)
# end
