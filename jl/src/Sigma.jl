module Sigma

using Distributions

include("util.jl")
include("bool.jl")
include("box.jl")
include("refinement.jl")
include("query.jl")
include("vis.jl")

export
  Interval,
  NDimBox,
  AbstractBool,
  T, F, TF,
  @If,
  @While,
  pre,
  pre_recursive,
  pre_greedy,
  ndcube,
  sqr,
  sqrt,
  plot_2d_boxes,

  # Probabilistic functions
  prob

  #utils
  tolerant_eq
end
