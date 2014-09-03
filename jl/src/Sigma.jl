module Sigma

using Distributions

include("util.jl")
include("randomvariable.jl")
include("bool.jl")
include("box.jl")
include("refinement.jl")
include("query.jl")
include("vis.jl")

export
  RandomVariable,
  Interval,
  NDimBox,
  AbstractBool,
  T, F, TF,
  @If,
  @While,

  # Preimages
  pre_bfs,
  pre_recursive,
  pre_greedy,
  pre_deepening,
  cond_prob_deep,

  ndcube,
  sqr,
  sqrt,

  # Probabilistic functions
  prob,
  prob_deep,

  # Distributions
  normal,
  uniform,
  flip,

  #utils
  tolerant_eq,

  #Plotting
  plot_2d_boxes,
  plot_psuedo_density,
  plot_volume_distribution
end
