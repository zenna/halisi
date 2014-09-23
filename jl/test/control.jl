x = Interval(-5,-2)
@While(x < 0,
  begin
    x = x + 1
  end)

@test x == Interval(0,3)
