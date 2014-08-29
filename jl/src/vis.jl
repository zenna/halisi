## =============
## Visualisation
using Gadfly

function plot_2d_boxes(bs::Vector{Box})
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

function plot_volume_distribution(bs::Vector{Box})
  vols = map(volume,preimage)
  plot(x=vols, Geom.histogram)
end

## e.g. plot_psuedo_density((normal(0,0,1) + uniform(1,0,1))
function plot_psuedo_density(rv::RandomVariable, lower::Float64, upper::Float64; n_bars = 20)
  xs = Array(Float64,n_bars - 1)
  ys = Array(Float64,n_bars - 1)
  for i in 1:(n_bars - 1)
    j = i + 1
    l,u = linspace(lower,upper,n_bars)[i],linspace(lower,upper,n_bars)[j]
    ys[i] = prob((rv > l) & (rv < u))
    xs[i] = (u-l)/2 + l
  end
  plot(x=xs, y=ys, Scale.x_continuous(minvalue=lower, maxvalue=upper), Scale.y_continuous(minvalue=0))
end
