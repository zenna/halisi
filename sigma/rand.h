#pragma once
#include "ibex.h"
#include <random>

namespace sigma {

// Generate a uniformly distributed point along an interval
double rand(const ibex::Interval &i, mt19937 &gen) { 
  uniform_real_distribution<double> dist(i.lb(), i.ub());
  return dist(gen);
}

ibex::Interval rand_interval(const ibex::Interval &i, mt19937 &gen) { 
  uniform_real_distribution<double> dist(i.lb(), i.ub());
  return ibex::Interval(rand(i,gen));
}

// Generate a uniformly distributed point along an interval
ibex::IntervalVector rand_interval(const ibex::IntervalVector &v, mt19937 &gen) { 
  ibex::IntervalVector degen_box(v.size()); 
  for (int i=0; i<v.size(); ++i) {
    degen_box[i] = rand(v[i], gen);
  }
  return degen_box;
}

// Generate a uniformly distributed point along an interval
std::vector<double> rand(const ibex::IntervalVector &v, mt19937 &gen) { 
  std::vector<double> res;
  for (int i=0; i<v.size(); ++i) {
    res.push_back(rand(v[i], gen));
  }
  return res;
}

// generate samples from box, conditiond on system being satisfied
vector<ibex::IntervalVector> rand(const ibex::IntervalVector &box, const ibex::System &sys, mt19937 &gen, int ntries) {
  ibex::NormalizedSystem normsys(sys);
  vector<ibex::IntervalVector> samples;

  // Do rejection sampling ntries times
  for (int i=0;i<ntries;++i) {
    // Generate random point and eval on all constraints
    ibex::IntervalVector point = rand_interval(box, gen);
    ibex::IntervalVector output = normsys.f.eval_vector(point);
    
    // cout << "Point" << point << endl << "after eval " << output << endl;
    // Is point valid for each constraint
    bool is_valid = true;
    for (int i = 0; i<output.size(); ++i) {
      is_valid &= (output[i].ub() <= 0.0);
      if (!is_valid) {break;};
    }

    // If so, se found a valid point!
    if (is_valid) {samples.push_back(point);}
  }
  return samples;
}

}

