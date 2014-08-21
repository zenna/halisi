# ==========
# Probability
function probability(rv::Function, Ω, ℙ)
  ℙ(pre(rv, T, Ω))
end

function uniform(a,b)
  a + (b - a) * rand()
end
