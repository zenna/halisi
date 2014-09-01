using Sigma
# Friends and Smokers
a = flip(0)
b = flip(1)
c = flip(2)
cond_prob_deep(a & b & c, flip(3, @If (a==b) 1.0 0.3) & flip(4, @If (a==b) 1.0 0.3) , max_depth = 7)
