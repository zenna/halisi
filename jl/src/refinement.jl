# PreImage Computation

function split_many_boxes{T}(to_split::Vector{T})
  bs = T[]
  for b in to_split
    for bj in middle_split(b)
      push!(bs, bj)
    end
  end
  bs
end

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
    bs = split_many_boxes(to_split)
#   println("Volumes", sum(map(volume,goodboxes)))
  end
  goodboxes
end



function pre_recursive(f::Function, y, bs, depth = 1; box_count = 0, max_depth = 4, box_budget =2000)
  goodboxes = Box[]
  println("Depth is", depth)
  if depth < max_depth
    println("Found ",length(goodboxes) + box_count, "out of budget of ", box_budget)
    for b in bs
      if length(goodboxes) + box_count >= box_budget
        return goodboxes
      end

      image = apply(f,to_intervals(b))
      if subsumes(y,image)
#         println("Good Box")
        push!(goodboxes, b)
      elseif overlap(image,y)
#         println("Will Recurse")
        boxes_from_deeper = pre_recursive(f,y,middle_split(b),depth + 1,
                                          box_count = length(goodboxes) + box_count,
                                          max_depth = max_depth,
                                          box_budget = box_budget)
        goodboxes = vcat(goodboxes, boxes_from_deeper)
#       else
#         println("Wasteman Box")
      end
    end
  end
  goodboxes
end

function pre_greedy(f::Function, y, bs, depth = 1)
  @label start_again
  println("starting again,", length(bs), sum(map(volume,bs)))
  i = 1
  for b in bs
    image = f(to_intervals(b))
    println(i, image)
    i += 1
    if subsumes(y,image)
      return b
    elseif overlap(image,y)
      bs = middle_split(b)
      println("Going back")
      @goto start_again
    end
  end
  println("Got to the end somehow")
end
