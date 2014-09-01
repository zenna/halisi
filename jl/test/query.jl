using Sigma
x = uniform(1,0,1)
y = normal(1,0,1)
z = normal(2,0,1)
a = x * y
prob_deep(a > 0.3;  max_depth = 10)

# Conditional probability query
cond_prob_deep(uniform(0,0,2) > 1.5, uniform(0,0,2) > 1, max_depth = 6)

## ===============
## Burglary

earthquake = flip(1,0.001)
burglary = flip(2,0.01)
alarm = earthquake | burglary
phone_working = @If earthquake flip(3,0.6) flip(4,0.6)
mary_wakes = @If (earthquake & alarm) flip(4,0.8) flip(5,0.6)
called = mary_wakes & phone_working
p_called = prob_deep(called)

println("Prob yo called is", p_called)
# @time prob_recursive((a > 0.5) | (a < 0.3))
# @test tolerant_eq(prob(uniform(0,5) > 2.5), 0.5)
# @test tolerant_eq(prob(normal(0,1) > 0), 0.5)'
