#!/usr/bin/env python
from sys import argv
import matplotlib.pyplot as plt
import numpy as np
from StringIO import StringIO
import ast

legends = []

bucket_file = open(argv[1])
axis_legends = ast.literal_eval(bucket_file.readline())
print "LEGENDS", axis_legends
for line in bucket_file:
  line = line.strip().split(",")
  bucket_name = line[0]
  bucket_option = int(line[1])
  inspect = line[1]

  x_vals_str = line[2].strip("[").strip("]")
  y_vals_str = line[3].strip("[").strip("]")
  x_vals = np.loadtxt(StringIO(x_vals_str))
  y_vals = np.loadtxt(StringIO(y_vals_str))

  bucket_human_name = bucket_name
  bucket_human_option = bucket_option

  # Get sampler type and option
  if bucket_name in axis_legends[":bucket-legend"]:
    if ":name" in axis_legends[":bucket-legend"][bucket_name]:
      bucket_human_name = axis_legends[":bucket-legend"][bucket_name][':name']
    if (":options" in axis_legends[":bucket-legend"][bucket_name]) and (bucket_option in axis_legends[":bucket-legend"][bucket_name][':options']):
      bucket_human_option =  axis_legends[":bucket-legend"][bucket_name][':options'][bucket_option]

  if ":inspect-legend" in axis_legends and inspect in axis_legends[':inspect-legend']:
    inspect = axis_legends[":inspect-legend"][inspect]

  legends.append(bucket_human_name + "=" + str(bucket_human_option) + ", " + str(inspect))
  plt.plot(np.sort(x_vals), y_vals)
  print "x", np.sort(x_vals), "y", y_vals

if ":title" in axis_legends:
  plt.title(axis_legends[':title'])
else:
  plt.title('Bucket Scaling Plot')

# y label
if ":y-label" in axis_legends:
  plt.ylabel(axis_legends[':y-label'])

# x label
if ":x-label" in axis_legends:
  plt.xlabel(axis_legends[':x-label'])

plt.legend(legends , loc = 'upper right')
plt.show()