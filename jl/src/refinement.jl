# Pre-image Computation:
# Given a function f:X->Y and Y' ⊆ Y -
# -- find X' ⊆ X such that f(x') ∈ Y'

# DEPRECATE
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

# Find the pre-image of a function on set y
function pre2{T}(f::Function, y, bs::Vector{T}; n_iters = 10)
  goodboxes = T[]
  for i = 1:n_iters
    println("\n\n iteration - ", i)
    to_split = T[]

    # For each box, find the image and test intersection with y
    #     println("BS Volumes", sum(map(volume,bs)))
    println("bs len- ", length(bs))
    for b in bs
      image = f(b)
      # Split box further iff image overlaps
      if subsumes(y,image)
        push!(goodboxes, b)
      elseif overlap(image,y)
        push!(to_split, b)
      end
    end
    println("to_split len- ", length(to_split))

    bs = split_many_boxes(to_split)
  end
  goodboxes
end

# Recursive preimage refinement
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

# Recursive preimage refinement
function pre_recursive2{T}(f::Function, y, bs::Vector{T}, depth = 1;
                           box_count = 0, max_depth = 4, box_budget =2000)
  goodboxes = T[]
#   println("Depth is", depth)
  if depth < max_depth
#     println("Found ",length(goodboxes) + box_count, "out of budget of ", box_budget)
    for b in bs
      if length(goodboxes) + box_count >= box_budget
        return goodboxes
      end

      image = f(b)
      if subsumes(y,image)
#         println("Good Box")
        push!(goodboxes, b)
      elseif overlap(image,y)
#         println("Will Recurse")
        boxes_from_deeper = pre_recursive2(f,y,middle_split(b),depth + 1,
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

## ===================================
## Iterative Deepening Preconditioning

immutable SatStatus
  status::Uint8
end

const UNSAT = SatStatus(0x0)
const SAT = SatStatus(0x1)
const MIXEDSAT = SatStatus(0x2)
const UNKNOWNSAT = SatStatus(0x3)

type Node{T}
  id::Uint64
  status::SatStatus
  data::T
end

type Tree
  id_counter::Uint64
  nodes::Vector{Node}
  children::Vector{Vector{Uint64}}
  Tree(n::Node,c::Vector{Vector{Uint64}}) = new(1,n,c)
  Tree(n,c) = new(1,n,c)
end

Tree(n::Node) = (t = Tree([],[]); add_node!(t,n); t)
root(t::Tree) = t.nodes[1]

function add_node!(t::Tree, n::Node)
  n.id = t.id_counter
  t.id_counter += 1
  push!(t.nodes,n)
  push!(t.children,[])
  n
end

function add_child!(t::Tree, n::Node, parent_id::Uint64)
  add_node!(t,n)
  push!(t.children[parent_id],n.id)
  n
end

has_children(t::Tree, n::Node) = !isempty(t.children[n.id])
node_from_id(t::Tree, node_id::Integer) = t.nodes[node_id]
children_ids(t::Tree, n::Node) = t.children[n.id]

function dls(f::Function, Y_sub, depth::Integer, depth_limit::Integer, t::Tree, node::Node; box_budget = 2000)
#   println("\nDepth = ", depth, " Node Status ", node.status, "limit is", depth_limit)
#   println("T is ", t)
  # Resolve SAT status is unknown
  if node.status == UNKNOWNSAT
#     println("node data is  ", node)
    image = f(node.data)
#     println("image is ", image)
    satstatus = if subsumes(Y_sub, image) SAT elseif overlap(image,Y_sub) MIXEDSAT else UNSAT end
#     println("sat statsis ", satstatus)
    node.status = satstatus
  end
#   println("Depth = ", depth, " Node Status ", node.status)

  if node.status == MIXEDSAT
    if has_children(t, node)
      for child_id in children_ids(t, node)
        child = node_from_id(t,child_id)
        if child.status == MIXEDSAT && depth + 1 < depth_limit
           t = dls(f, Y_sub, depth + 1, depth_limit, t, child; box_budget = box_budget)
        end
      end
    elseif depth + 1 < depth_limit
      children_data = middle_split(node.data)
      children_nodes = Array(typeof(node),length(children_data)) # DO THIS LAZILY
      for i = 1:length(children_data)
        new_node = Node(rand(Uint64), UNKNOWNSAT, children_data[i])
        children_nodes[i] = new_node
        new_child = add_child!(t, children_nodes[i], node.id)
        t = dls(f, Y_sub, depth + 1, depth_limit, t, new_child; box_budget = box_budget)
      end
    end
  elseif node.status == UNSAT || node.status == SAT
    t
  end
  t
end


function pre_deepening{T}(f::Function, Y_sub, X::T; box_budget = 2000, max_depth = 4)
  tree = Tree(Node(rand(Uint64), UNKNOWNSAT, X))
  for depth_limit = 1:max_depth
    println("Deepening Depth Limit", depth_limit)
    tree = dls(f, Y_sub, zero(Uint), depth_limit, tree, root(tree))
  end
  tree
end

tree_to_boxes(t::Tree) = map(n->n.data,filter(n->n.status==SAT,t.nodes))


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
