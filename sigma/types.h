#pragma once
#include <cmsat/Solver.h>

namespace sigma {
  using BoolVar =  CMSat::Var;
  using LBool = CMSat::lbool;
  using Lit = CMSat::Lit;
  template <typename T> 
  using vec = CMSat::vec<T>;
  using Clause = std::vector<CMSat::Lit>;
  using LitVec = std::vector<CMSat::Lit>;
  using CNF = std::vector<Clause>;
  using BoolModel = std::vector<CMSat::lbool>;
  using Solver = CMSat::Solver;

  const LBool l_True = CMSat::l_True;
  const LBool l_False = CMSat::l_False;


}