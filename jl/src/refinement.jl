# PreImage Computation

# Find the pre-image of a function on set y
function pre(f::Function, y, bs; n_iters = 10)
  goodboxes = Box[]
  for i = 1:n_iters
    output = Int[]
    to_split = Box[]

    # For each box, find the image and test intersection with y
    println("BS Volumes", sum(map(volume,bs)))
    for b in bs
      image = apply(f, to_intervals(b))

#       println(b.intervals)
#       println(image)
      # Split box further iff image overlaps
      if subsumes(y,image)
        push!(goodboxes, b)
#         println("GoodBox")
      elseif overlap(image,y)
        push!(to_split, b)
#         println("Splitting")
#       else
#         println("Leaving the box alone")
      end
#       println()
    end


    bs = Box[]
    for b in to_split
      for bj in middle_split(b)
        push!(bs, bj)
      end
    end
  end
#   println("Volumes", sum(map(volume,goodboxes)))
  goodboxes
end

## =============
## Visualisation

function plot_2d_boxes(bs::Vector{Box})
  x_min = Float64[]
  y_min = Float64[]
  x_max = Float64[]
  y_max = Float64[]

  for b in bs
    push!(x_min, b.intervals[1,1])
    push!(x_max, b.intervals[2,1])
    push!(y_min, b.intervals[1,2])
    push!(y_max, b.intervals[2,2])
  end

  plot(x_min=x_min, x_max=x_max, y_min=y_min,color=rand(length(bs)), y_max=y_max,Geom.rectbin)
end

# ========
# Examples
function testy(x, y)
  x*y
end

function testy2(x, y)
  x  > y + .1
end

function testy3(x,y)
  @If x > 3.5 x>y+0.1 y*y>x+-.1
end

gen_path(n) = repmat([Interval(0,10) Interval(0,10)],2,n)

function point_in_box(x::Real,y::Real, box_x_min::Real, box_x_max::Real, box_y_min::Real, box_y_max::Real)
  x >= x_box_min & x <= x_box_max & y >= y_box_min & y <= y_box_max
end


# This function takes as input a path
function valid_path(start, path::Vector)
  svx, svy = path[:,1]
  good_start = point_in_box(svx,svy,start_box)

  evx, evy = path[:,end]
  good_end = point_in_box(svx,svy,start_box)

  good_start & good_end
end

#     ; Points must be certain distane apart
#     ~@(reduce concat
#         (for [[[path-x0 path-y0] [path-x1 path-y1]] (partition 2 1 vars)]
#           `[(~'>= (~'+ ~path-x1 (~'* -1 ~path-x0)) 0)
#             (~'<= (~'+ ~path-x1 (~'* -1 ~path-x0)) ~max-step)
#             (~'>= (~'+ ~path-y1 (~'* -1 ~path-y0)) 0)
#             (~'<= (~'+ ~path-y1 (~'* -1 ~path-y0)) ~max-step)]))

#     ; Points must not be within obstacles
#     ~(:pred (lambda-points-avoid-poly-obs n-points obstacles)))}))


preimage = pre(valid_path, T, [ndcube(0.0,5.0,2)], n_iters = 10)
plot_2d_boxes(preimage)
