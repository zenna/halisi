#pragma once

#include "util.h"
#include "halisi/types.h"

// Functions for dealing with (crpytomini) sat

namespace halisi {

void print_bool_model(const BoolModel &bool_model) {
  for (const auto lit : bool_model) {
    std::cout << lit;
  }
  std::cout << std::endl;  
}
// Conversion Between CMSat and STL Vector
template <typename T>
std::vector<T> to_stl_vec(const CMSat::vec<T> &input) {
  std::vector<T> output(input.size());
  for (int i = 0;i<input.size();++i) {
    output[i] = input[i];
  }
  return output;
}


template <typename T>
CMSat::vec<T> to_cmsat_vec(const std::vector<T> &input) {
  CMSat::vec<T> output(input.size());
  for (int i = 0;i<input.size();++i) {
    output[i] = input[i];
  }
  return output;
}

// Highest variable number in cnf
int max_var(const CNF &cnf) {
  int maxvar = 0;
  for (Clause const &clause : cnf) {
    for (CMSat::Lit const &lit : clause) {
      if (lit.var() > maxvar) {maxvar = lit.var();}
    }
  }
  return maxvar;
}

void add_clause(CMSat::Solver &solver, const Clause &clause) {
  CMSat::vec<CMSat::Lit> cmsat_clause = to_cmsat_vec(clause);
  solver.addClause(cmsat_clause);
}

// Add all the clauses
void add_clauses(CMSat::Solver &solver, const CNF &cnf) {
  for (auto &clause : cnf) {
    add_clause(solver, clause);
  }
}

// Convert a Boolean model to a clause, such that the model is now unsatisfiable
LitVec model_to_conflict(const BoolModel &model) {
  LitVec conflict;
  for (int i = 0; i < model.size(); ++i) {
    // Why would it ever == CMSat::l_Undef?
    if (model[i] != CMSat::l_Undef) {
      // If model i A=1,B=0, conflict cause is !A or B
      CMSat::Lit conflict_lit = model[i] == CMSat::l_True ? CMSat::Lit(i,true) : CMSat::Lit(i,false); 
      conflict.push_back(conflict_lit);
    }
  } 
  return conflict;
}

LitVec model_to_conflict(const BoolModel &model,
                                          const std::vector<BoolVar> &vars) {
  LitVec conflict;
  for (const BoolVar &bool_var : vars) {
    if (model[bool_var] != CMSat::l_Undef) {
      // If model i A=1,B=0, conflict cause is !A or B
      CMSat::Lit conflict_lit = model[bool_var] == CMSat::l_True ? CMSat::Lit(bool_var,true) : CMSat::Lit(bool_var,false); 
      conflict.push_back(conflict_lit);
    }
  } 
  return conflict;
}

// Create N new boolean vars
void new_vars(CMSat::Solver &solver, int n) {
  for (int i =0; i<n; ++i) {
    solver.newVar();
  }
}

}