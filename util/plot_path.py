from sys import argv
import matplotlib.pyplot as plt
from matplotlib.path import Path
import matplotlib.patches as patches
import numpy as np

def cartesian(arrays, out=None):
    """
    Generate a cartesian product of input arrays.

    Parameters
    ----------
    arrays : list of array-like
        1-D arrays to form the cartesian product of.
    out : ndarray
        Array to place the cartesian product in.

    Returns
    -------
    out : ndarray
        2-D array of shape (M, len(arrays)) containing cartesian products
        formed of input arrays.

    Examples
    --------
    >>> cartesian(([1, 2, 3], [4, 5], [6, 7]))
    array([[1, 4, 6],
           [1, 4, 7],
           [1, 5, 6],
           [1, 5, 7],
           [2, 4, 6],
           [2, 4, 7],
           [2, 5, 6],
           [2, 5, 7],
           [3, 4, 6],
           [3, 4, 7],
           [3, 5, 6],
           [3, 5, 7]])

    """

    arrays = [np.asarray(x) for x in arrays]
    dtype = arrays[0].dtype

    n = np.prod([x.size for x in arrays])
    if out is None:
        out = np.zeros([n, len(arrays)], dtype=dtype)

    m = n / arrays[0].size
    out[:,0] = np.repeat(arrays[0], m)
    if arrays[1:]:
        cartesian(arrays[1:], out=out[0:m,1:])
        for j in xrange(1, arrays[0].size):
            out[j*m:(j+1)*m,1:] = out[0:m,1:]
    return out

# lines
def chunks(l, n):
    """ Yield successive n-sized chunks from l.
    """
    for i in xrange(0, len(l), n):
        yield l[i:i+n]

X = np.loadtxt(argv[1])
n_samples = np.shape(X)[0]
n_vars = np.shape(X)[1]
print n_samples, n_vars

fig = plt.figure()
ax = fig.add_subplot(111)
for i in range(3,4):
    verts = [x for x in chunks(X[i,:],2)]
    codes = [Path.MOVETO] + [Path.LINETO]*(n_vars/2 - 1)
    # print "verts", verts, "codes", codes
    path = Path(verts, codes)
    patch = patches.PathPatch(path, facecolor='none', lw=.2)
    ax.add_patch(patch)

# BOXES
codes = [Path.MOVETO,
         Path.LINETO,
         Path.LINETO,
         Path.LINETO,
         Path.CLOSEPOLY,
         ]
pos_delta = 0.1
targets = [[1, 1], [7, 6]]
#obstacles = [[[2, 5],[5,7]],[[5 ,8],[0 ,3]]]
# obstacles = [[[1.5, 4.5],[3, 7]] ,[[5, 8],[0, 3]]]    
# obstacles = [[[3, 5],[0, 3]], [[3, 5],[3.5, 9]], [[5.5, 7],[2, 4]]]
obstacles = [[[3, 5],[0, 3]], [[3, 5],[3.5, 9]], [[5.5, 7],[2, 4]]]

target_boxes = [[[x + pos_delta, x - pos_delta],[y + pos_delta, y-pos_delta]] for [x, y] in targets]

def box_to_vertices(box):
    return [[box[0][0], box[1][0]], [box[0][0], box[1][1]], [box[0][1], box[1][1]], [box[0][1], box[1][0]], [box[0][0], box[1][0]]]
    # return cartesian(box)

for box in obstacles:
    verts = box_to_vertices(box)
    # print "BOX VERTS", verts, box
    # verts = np.append(verts, [[0.0, 0.0]], axis = 0)
    # print "BOX VERTS", verts, box
    path = Path(verts, codes)
    patch = patches.PathPatch(path, facecolor='none', lw=2)
    ax.add_patch(patch)

for box in target_boxes:
    verts = box_to_vertices(box)
    # print "BOX VERTS", verts, box
    # verts = np.append(verts, [[0.0, 0.0]], axis = 0)
    # print "BOX VERTS", verts, box
    path = Path(verts, codes)
    patch = patches.PathPatch(path, facecolor='orange', lw=2)
    ax.add_patch(patch)

ax.set_xlim(-10,20)
ax.set_ylim(-10,20)
plt.show()