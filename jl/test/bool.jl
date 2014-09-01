using Sigma
using Base.Test
import Sigma.flip, Sigma.abs

@test T & F === F
@test TF & F === F
@test T & T === T
@test TF & TF === TF
@test (F == T) === false
@test !T === F
@test !TF === TF
@test !F === T
@test TF | T === T
@test TF | F === TF
@test F | T === T
@test T | TF === T

x = Interval(-5,-2)
@While(x < 0,
  begin
    x = x + 1
  end)

@test x == Interval(0,3)
