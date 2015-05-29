#pragma once
#include "ibex/ibex.h"
#include "sigma/rand.h"
#include "sigma/types.h"
#include <tuple>
#include <limits>

namespace sigma {

Box unit_box(int n) {
  Box box(n);
  for (int i = 0; i<n; ++i) {
    box[i] = ibex::Interval(0.0,1.0);
  }
  return box;
}

bool is_valid_box(const Box &box, const ibex::NormalizedSystem &normsys) {
  // For each normalised constraints, e.g. x - y < 0
  // Eval box and check whether constraint holds
  Box output = normsys.f.eval_vector(box);
  bool is_valid = true;
  for (int i = 0; i<output.size(); ++i) {
    is_valid &= (output[i].ub() <= 0.0);
    if (!is_valid) {return false;}
  }
  return true;
}

// Is the box fully SAT?
bool is_valid_box(const Box &box, const ibex::System &sys) {
  return is_valid_box(box, ibex::NormalizedSystem(sys));  
}

// Can we accurately draw samples from this box?
bool is_sampleable(const Box &box, const ibex::System &sys, mt19937 &gen) {
  int ntries = 10;
  return rand(box, sys, gen, ntries).size() > 0;
}

// Fullness is estimate by ration of uniform samples which ard good over ntries
double estimate_box_fullness(const Box &box, const ibex::System &sys, mt19937 &gen) {
  int ntries = 100;
  return double(rand(box, sys, gen, ntries).size()) / double(ntries);
}

bool too_small(const ibex::Bsc &bsc, const Box &box) {
  for (int i=0; i<box.size(); ++i) {
    if (!bsc.too_small(box, i)) {return false;} // std::logic_error("TODO");
  }
  return true;
}

// @doc "Does pruning and stuff" ->
tuple<CMSat::lbool, Box, double, double>
theory_sample(const ibex::System &sys, ibex::Ctc & ctc, ibex::Bsc &bsc, mt19937 &gen) {
  Box box = sys.box;            // Initial box
  int depth = 0;                // Current depth
  double logq = 0.0;            // proposal probability == log(1.0)
  int last_bisected_var = -1;   // For round robin
  stack<Box> to_visit;          // Stack of boxes to visit (found through splits)
  bool box_is_empty;            // 

  cout << "Initial box is" << box << endl;
  cout << "System is" << sys << endl;
  while (true) {
    box_is_empty = false;
    std::cout << "Trying Contraction" << std::endl;
    try {ctc.contract(box, last_bisected_var);}
    catch (...) {
      std::cout << "Contraction reduced box to empty, exception caught" << std::endl;
      box_is_empty = true;
    }
    cout << "box is " << box << endl;
    cout << "last bisected var is " << last_bisected_var << endl;
    
    // 6 possible scenarios after contraction
    if (box_is_empty || box.is_empty()) {
      cout << "THE BOX IS EMPTY" << endl;

      // 1. All boxes (and hence problem) are UNSAT
      if (to_visit.size() == 0) {
        cout << "Unsat" << endl;
        // When unsat return false and magic values for others 
        double minus_inf = -numeric_limits<double>::infinity();
        return make_tuple(CMSat::l_False, box, minus_inf, 0);
      }

      // 2. Backtrack - current box UNSAT but boxes left to visit
      else {
        cout << "back tracking" << endl;
        logq += log(0.5);
        box = to_visit.top();
        to_visit.pop();
      }
    }

    // 3. Current box is SAT
    else if (is_valid_box(box, sys)) {
      cout << "valid box" << endl;
      // fraction_full = 1.0 when box subset of feasible region
      return make_tuple(CMSat::l_True, box, logq, 1.0);
    }

    // 4. Current box is partially SAT
    else if (is_sampleable(box, sys, gen)) {
      cout << "sampleable" << endl;
      double fraction_full = estimate_box_fullness(box, sys, gen);
      return make_tuple(CMSat::l_True, box, logq, fraction_full);
    }

    // 5. Current box is smaller than precision
    else if (too_small(bsc, box)) {
      cout << "small box" << endl;
      return make_tuple(CMSat::l_True, box, logq, 1.0);
    }

    // 6. Otherwise we can split it further and continue
    else {
      cout << "splittable" << endl;
      auto children = bsc.bisect(box);
      discrete_distribution<int> categorical{0.5, 0.5};
      logq += log(0.5);
      int rand_index = categorical(gen);
      box = rand_index == 0 ? get<0>(children) : get<1>(children);
      to_visit.push(rand_index == 0 ? get<1>(children) : get<0>(children));
    }
  }
}

}