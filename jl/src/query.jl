# ==========
# Probability
function prob(rv::Function, Ω, ℙ)
  ℙ(pre(rv, T, Ω))
end

function uniform(x,y)
  global count += 1
  Interval(x,y)
end
