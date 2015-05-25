# pragma once

#include <cryptominisat4/cryptominisat.h>
#include "ibex/ibex.h"
#include "sigma/refine.h"
#include "sigma/types.h"
#include "sigma/measure.h"

#include <random>
#include <tuple>
#include <limits>
#include <iostream>
#include <stdlib.h>
#include <stdexcept>

using namespace std;

namespace sigma {

// @doc "Creates a conjunction of constraints" ->
std::vector<ibex::NumConstraint *> conjoin_constraints(const LiteralMap &lmap, const BoolModel &model) {
  std::vector<ibex::NumConstraint *> constraints;

  // TODO CHECK UNSEFCASE
  for (int i = 0; i<model.size(); ++i) {
    // If model says A=0,B=1. Lookup constraints (inequalities) for !A and B
    if (model[i] == CMSat::l_True) {
      constraints.push_back(lmap[CMSat::Lit(i,false).toInt()]);
      // constraints[i] =  lmap[CMSat::Lit(i,false).toInt()];
    }
    else {
      constraints.push_back(lmap[CMSat::Lit(i,true).toInt()]);
      // constraints[i] =  lmap[CMSat::Lit(i,true).toInt()];

    }
  }
  return constraints;
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

// Add all the clauses
void add_clauses(CMSat::SATSolver &solver, const CNF &cnf) {
  for (auto &clause : cnf) {
    solver.add_clause(clause);
  }
}

// Convert a Boolean model to a clause, such that the model is now unsatisfiable
inline std::vector<CMSat::Lit> model_to_conflict(const BoolModel &model) {
  std::vector<CMSat::Lit> conflict;
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

// @doc "Main loop to construct preimage samples" ->
std::vector<ibex::IntervalVector> pre_tlmh(const LiteralMap &lmap, const CNF &cnf, int nsamples) {
  CMSat::SATSolver solver;
  solver.new_vars(max_var(cnf)+1);
  solver.set_num_threads(1);
  add_clauses(solver, cnf);
  std::vector<ibex::IntervalVector> samples;
  samples.reserve(nsamples);

  // Setup randomness
  std::random_device rd;
  std::mt19937 gen(rd());
  std::uniform_real_distribution<> uniform(0, 1);

  // Log probability/proposal probability of "current" box in markov chain
  // Set initially to -Inf, such that first proposed box will always be accepted - log(0) = -Inf.
  double logp = -numeric_limits<double>::infinity();
  double logq = -numeric_limits<double>::infinity();
  ibex::IntervalVector box(1);

  // Logic <-> theory loop
  while (samples.size() < nsamples) {
    std::cout << samples.size() << std::endl;
    // Gets boolean model (if exists) using CMSat, convert to conjunction of constraints 
    CMSat::lbool res = solver.solve();
    if (res == CMSat::l_False) {throw std::domain_error("Cannot condition on unsatisfiable condition");}
    if (res == CMSat::l_Undef) {throw std::runtime_error("SAT Solver failed");}

    BoolModel bool_model = solver.get_model();
    std::vector<ibex::NumConstraint *> constraints = conjoin_constraints(lmap, bool_model);
    
    // Check constraints at theory level (may be logically SAT but UNSAT at theory level)
    auto sample = theory_sample(constraints, gen);
    CMSat::lbool theory_sat = get<0>(sample);

    // If we get back a valid box, then do MH step
    if (theory_sat == CMSat::l_True) {
      ibex::IntervalVector nextbox = get<1>(sample);
      double nextlogq = get<2>(sample);
      double prevolfrac = get<3>(sample);

      double nextlogp = logmeasure(nextbox) + log(prevolfrac);
      double loga = nextlogp + logq - logp - nextlogq;
      double a = exp(loga);

      // MH accept/reject step
      if (a >= 1 || uniform(gen) < a) {
        cout << "switching" << endl;
        box = nextbox;
        logp = nextlogp;
        logq = nextlogq;
      }
      else{
        cout << "sticking" << endl;
      }
      samples.push_back(box);
    }
    else if (theory_sat == CMSat::l_False) {
      solver.add_clause(model_to_conflict(bool_model));
    }
    else {
      std::runtime_error("Theory Solver failed");
    }
  }
  return samples;
}

}