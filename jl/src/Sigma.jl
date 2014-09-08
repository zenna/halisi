module Sigma

using Distributions

import Base: sqrt, abs, convert, rand, getindex
import Base: show, print, showcompact
import Base: sum, dot, length

export
  RandomVariable,
  RandomArray,
  MakeRandomArray,
  independentRandomArray,
  Omega,
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

  # Probabilistic functions
  prob,
  prob_deep,

  setindex,

  # Distributions
  normal,
  uniform,
  flip,
  uniformArray,

  #utils
  tolerant_eq,

  #Plotting
  plot_2d_boxes,
  plot_psuedo_density,
  plot_cond_density,
  plot_volume_distribution


include("util.jl")
include("randomvariable.jl")
include("bool.jl")
include("box.jl")
include("omega.jl")
include("refinement.jl")
include("query.jl")
include("vis.jl")
end
