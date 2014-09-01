using Sigma
# Friends and Smokers
a = flip(0)
b = flip(1)
c = flip(2)
prob(a & b & c, flip(@If(a==b) 1.0 0.3) & flip(@If(b == c) 1.0 0.3))