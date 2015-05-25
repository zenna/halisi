#pragma once
#include "ibex/ibex.h"

namespace sigma {

double measure(const ibex::Interval &i) {return i.ub() - i.lb();}
double logmeasure(const ibex::Interval &i) {return log(i.ub() - i.lb());}
double logmeasure(const ibex::IntervalVector &box) {
  double logmeasure_ = 0.0;
  for (int i = 0; i<box.size(); ++i) {
    logmeasure_ += logmeasure(box[i]);
  }
  return logmeasure_;
}

}
