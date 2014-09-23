# Control flow functions/operators
# REVIEW: Make this a type dispatch
make_rv(v, ω) = v
make_rv(v::RandomVariable, ω) = v(ω)

# REVIEW SEPARATE THIS OUT INTO FUNCTIONS
macro If(condition, conseq, alt)
  local idtrue = singleton(gensym())
  local idfalse = singleton(gensym())
  q =
  quote
  c =  $(esc(condition));
#   println("enterin$$g the if",$idtrue)
  if isa(c, RandomVariable)
    (ω)->begin
          d = c(ω)
          if isa(d, EnvVar)
            ret = EnvVar()
            for world in d.worlds
              if world[2] === T || world[2] === true
                ret.worlds[world[1]] = make_rv($(esc(conseq)),ω)
              elseif world[2] === F || world[2] === false
                ret.worlds[world[1]] = make_rv($(esc(alt)),ω)
              elseif world[2] === TF
                a = make_rv($(esc(conseq)),ω)
                constraintstrue = union(world[1],$idtrue)
                update_ret!(a,ret, constraintstrue)

                b = make_rv($(esc(alt)),ω)
                constraintsfalse = union(world[1],$idfalse)
                update_ret!(b,ret, constraintsfalse)

              else
                println("error:", world[2])
                throw(DomainError())
              end
            end
          elseif isa(d, Bool)
            d ? make_rv($(esc(conseq)),ω) : make_rv($(esc(alt)),ω)
          elseif d === T
            make_rv($(esc(conseq)),ω)
          elseif d === F
            make_rv($(esc(alt)),ω)
          elseif d === TF
            a = make_rv($(esc(conseq)),ω)
            b = make_rv($(esc(alt)),ω)
            ⊔(a,b)
          else
            println("condition is " , d)
            throw(DomainError())
          end
        end

  elseif isa(c, EnvVar)
    ret = EnvVar()
    for world in c.worlds
#       @show world[2]
#       println("length=", length(c.worlds))
      if world[2] === T || world[2] === true
        v = $(esc(conseq))
        update_ret!(v,ret, world[1])

#                 println("DID I FIND YOU BITCH?")
#         ret.worlds[world[1]] = $(esc(conseq))
#                     println("NOO YOU BITCH?")

      elseif world[2] === F || world[2] === false
#             println("DID I FIND YOU BITCH?")
        v = $(esc(alt))
        update_ret!(v,ret, world[1])

#         println("LETS SEE BITCH")
#         @show v
#         ret.worlds[world[1]] = v
#         println("NO BITCH?")
      elseif world[2] === TF
        a = $(esc(conseq))
        constraintstrue = union(world[1],$idtrue)
        update_ret!(a,ret, constraintstrue)

        b = $(esc(alt))
        constraintsfalse = union(world[1],$idfalse)
        update_ret!(b,ret, constraintsfalse)
#         println("leaving")
      else
        println("error:", world[2])
        throw(DomainError())
      end
    end
  elseif isa(c, Bool)
    c ? $(esc(conseq)) : $(esc(alt))
  elseif c === T
    $(esc(conseq))
  elseif c === F
    $(esc(alt))
  elseif c === TF
    a = $(esc(conseq))
    b = $(esc(alt))
    ⊔(a,b)
  else
    throw(DomainError())
  end
#   println("leaving the if", $idtrue)
  end
  return q
end

# REVIEW: IS THIS STILL RELEVANT? TEST OR REMOVE
macro While(c, todo)
  quote
    while $(esc(c)) === true || $(esc(c)) === T || $(esc(c)) === TF
      $(esc(todo));
    end
  end
end