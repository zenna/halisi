#!/usr/bin/env python
from pylab import *
import numpy as np

X = np.loadtxt("op")
scatter(X[1],X[0])

show()

