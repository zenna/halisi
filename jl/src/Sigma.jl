module Sigma

using Distributions

include("bool.jl")
include("box.jl")
include("refinement.jl")

export
  Interval,
  NDimBox,
  AbstractBool,
  T, F, TF,
  @If,
  pre,
  pre_recursive,
  pre_greedy,
  ndcube,
  sqr,
  sqrt
end
