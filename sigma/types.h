#pragma once
#include <cmsat/Solver.h>
#include "ibex/ibex.h"

namespace sigma {
  using LiteralMap = std::map<CMSat::Lit, ibex::ExprCtr>;
  using Clause = std::vector<CMSat::Lit>;
  using CNF = std::vector<Clause>;
  using BoolModel = std::vector<CMSat::lbool>;  
  using Box = ibex::IntervalVector;
}