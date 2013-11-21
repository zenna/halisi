#!/usr/bin/env python
from sys import argv
import matplotlib.pyplot as plt
import numpy as np
from StringIO import StringIO

legends = []

bucket_file = open(argv[1])
for line in bucket_file:
  line = line.strip().split(",")

  x_vals_str = line[2].strip("[").strip("]")
  y_vals_str = line[3].strip("[").strip("]")
  x_vals = np.loadtxt(StringIO(x_vals_str))
  y_vals = np.loadtxt(StringIO(y_vals_str))

  legends.append(line[0] + line[1])
  plt.plot(x_vals, y_vals)
  print "x", x_vals, "y", y_vals

plt.legend(legends , loc = 'upper right')
plt.title('Bucket Scaling Plot')
plt.show()