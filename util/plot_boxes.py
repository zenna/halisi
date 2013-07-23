from sys import argv
import matplotlib.pyplot as plt
from matplotlib.path import Path
import matplotlib.patches as patches
import numpy as np


def chunks(l, n):
    """ Yield successive n-sized chunks from l.
    """
    for i in xrange(0, len(l), n):
        yield l[i:i+n]

X = np.loadtxt(argv[1])
n_samples = np.shape(X)[0]
n_vars = np.shape(X)[1]

codes = [Path.MOVETO,
         Path.LINETO,
         Path.LINETO,
         Path.LINETO,
         Path.CLOSEPOLY,
         ]

# fig = plt.figure()
# ax = fig.add_subplot(111)

for i in range(0,1):
    fig = plt.figure()
    ax = fig.add_subplot(111)
    for sqr in chunks(X[i,:],3):
        x = sqr[0]
        y = sqr[1]
        r = sqr[2]
        verts = [
            (x-r, y-r), # left, bottom
            (x-r, y+r), # left, top
            (x+r, y+r), # right, top
            (x+r, y-r), # right, bottom
            (x-r, y-r), # ignored
            ]
        path = Path(verts, codes)
        patch = patches.PathPatch(path, facecolor='none', lw=2)
        ax.add_patch(patch)

ax.set_xlim(-10,20)
ax.set_ylim(-10,20)
plt.show()