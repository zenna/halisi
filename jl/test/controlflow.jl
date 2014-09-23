using Base.Test
import Sigma: @If, @While, Interval, TF, Omega, flip

# Should act like a normal if when cond is true
begin
  local x = 0
  local a = @If x > 0 false true
  @test a
  local b = @If x == 0 true false
  @test b
end

begin
  local x = TF
  local a = @If x false true
  @test a === TF
end

begin
  local x = flip(1,0.6)
  local a = @If x false true
  @test isa(a,Function)
  @test a(Omega()) === TF
  @test a([0.3]) == false
  @test a([0.7]) == true
end

begin
  local x = Interval(-5,-2)
  @While(x < 0,
    begin
      x = x + 1
    end)
  @test x == Interval(0,3)
end
