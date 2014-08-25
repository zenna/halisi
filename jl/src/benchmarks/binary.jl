# Miscellaneous binary examples
using Sigma

import Sigma.tolerant_eq
import Sigma.plot_2d_boxes
## Restrict to epislon thick circle
circle(radius, x, y) =  tolerant_eq(sqrt(sqr(x) + sqr(y)), radius, 0.01)
preimage = pre_recursive((x,y)->circle(5.0,x,y), T, [ndcube(-100.0,100.0,2)],max_depth = 15)
plot_2d_boxes(preimage)
