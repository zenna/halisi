#pragma once
#include <cryptominisat4/cryptominisat.h>
#include "ibex/ibex.h"

namespace sigma {
  using LiteralMap = std::vector<ibex::NumConstraint *>;
  using Clause = std::vector<CMSat::Lit>;
  using CNF = std::vector<Clause>;
  using BoolModel = std::vector<CMSat::lbool>;  
  using Box = ibex::IntervalVector;
}