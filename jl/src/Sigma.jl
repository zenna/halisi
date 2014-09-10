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
  SampleOmega,
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
  cond_sample,
  setindex,

  # Distributions
  normal,
  uniform,
  flip,
  uniformArray,

  #utils
  tolerant_eq,
  rand_select,

  #Plotting
  plot_2d_boxes,
  plot_psuedo_density,
  plot_cond_density,
  plot_volume_distribution,
  plot_performance,
  plot_sat_distribution,
  distinguished_colors,
  rand_color,

  # Benchmarking
  parse_output,
  run_church,
  stat_line_layer,
  stat_ribbon_layer,
  stat_errorbar_layer,
  plot_cond_performance,
  plot_prob_performance,
  add_KL!,
  add_KL_church!


include("util.jl")
include("randomvariable.jl")
include("bool.jl")
include("box.jl")
include("omega.jl")
include("refinement.jl")
include("query.jl")
include("benchmarks/benchmark.jl")
include("vis.jl")
end
