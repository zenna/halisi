using Sigma

function tosatboxes(pre)
  sat =  Sigma.sat_tree_data(pre)
  map(x->convert(NDimBox,collect(values(x.intervals))),sat)
end

function tomixedboxes(pre)
  mixed =  Sigma.mixedsat_tree_data(pre)
  map(x->convert(NDimBox,collect(values(x.intervals))),mixed)
end

x = uniform(0,0,1)
y = uniform(1,0,1)

prob_deep((x>y) & (x <=y), max_depth = 20)

tree = pre_deepening(x>y+0.001, T, Omega(),max_depth = 4)
Sigma.mixedsat_tree_data(tree)

plot_2d_boxes(tosatboxes(tree))

# Coin Weight
coinweight = normal(0,0.5,0.1)
coinweight = uniform(1, 0.4, 0.6)
coin = flip(2,coinweight)

cond_prob_deep(coin, (coinweight >= 0) & (coinweight < 1))

xs = Float64[]
ys = Float64[]
for x in linspace(0.0,1.0,100)
  for y in linspace(0.0,1.0,100)
    if coin([x,y])
      push!(xs,x)
      push!(ys,y)
    end
  end
end
Gadfly.plot(x=xs,y=ys)

prob_deep(coin)
tree = pre_deepening(coin, T, Omega(),max_depth = 10)
# function convert{I}(::Type{Vector{Intervals}}, os::Vector{Omega})
#   okeys = keys()
map(x->convert(NDimBox,collect(values(x.intervals))),sat)

plot_2d_boxes(tosatboxes(tree))






prob_deep(flip(5,coinweight), max_depth=20)
d =cond_prob_deep(flip(1,coinweight),( coinweight > 0) & (coinweight < 1) )
d
flips = [flip(i,coinweight) for i=1:3]
cond_prob_deep(faircoin > 0.5, flips[1] & flips[2] & flips[3],max_depth = 10)

plot_psuedo_density(coinweight, 0.0, 1.0)

plot_cond_density(coinweight,
                  flips[1] & flips[2] & flips[3] &
                  (coinweight >= 0) & (coinweight

                                       <= 1),
                  0.0, 1.0, n_bars =20 , max_depth = 10)
