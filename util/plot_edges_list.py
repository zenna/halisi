from sys import argv
import matplotlib.pyplot as plt
from matplotlib.path import Path
import matplotlib.patches as patches
import numpy as np
import ast

## Input is a set of edge files

fig = plt.figure()
ax = fig.add_subplot(111)

for fname in argv[1:]:
  edges_file = open(fname)
  color = np.random.rand(3,1)
  for line in edges_file:
    line = line.strip().split(",")
    a = ast.literal_eval(line[0].replace(" ", ","))
    b = ast.literal_eval(line[1].replace(" ", ","))
    verts = [a, b]
    codes = [Path.MOVETO, Path.LINETO]
    path = Path(verts,codes)
    patch = patches.PathPatch(path, edgecolor=color, facecolor='none', lw=1)
    ax.add_patch(patch)

# ax.set_xlim(0,10)
# ax.set_ylim(0,10)
ax.autoscale()
plt.show()