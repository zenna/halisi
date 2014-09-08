immutable Omega
  intervals::Dict{Int64,Interval}
end
Omega() = Omega(Dict{Int64,Interval}())

function getindex(o::Omega, key::Int64)
  if haskey(o.intervals,key)
    o.intervals[key]
  else
    i = Interval(0,1)
    o.intervals[key] = i
    i
  end
end

function convert(::Type{Vector{Box}}, os::Vector{Omega})
  map(x->convert(NDimBox,collect(values(x.intervals))),os)
end

# Omega is a lazy sample space
# immutable Lazy_Omega
#   intervals::Vector{SplitRef}
# end

# ([0 => 0.3],2),([1 => 0.5],2)

# function getindex(o::Lazy_Omega, key::Int64)
#   if haskey(o.intervals,key)
#     o.intervals[key]
#   else
#     i = Interval(0,1)
#     o.intervals[key] = i
#     i
#   end
# end
