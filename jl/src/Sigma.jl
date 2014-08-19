module Sigma

using Distributions
using Gadfly

include("bool.jl")
include("box.jl")
include("refinement.jl")

# Machines
# export Collection, Source, Worker
# export Delta, Socket
# export run!, push!, hard, socket_id, no_sockets
end
