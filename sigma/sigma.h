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
std::vector<ibex::ExprCtr> conjoin_constraints(const LiteralMap &lmap, const BoolModel &model) {
  std::vector<ibex::ExprCtr> constraints;

  for (int i = 0; i<model.size(); ++i) {
    CMSat::Lit pos_lit(i,false);
    CMSat::Lit neg_lit(i,true);

    // Some literals do not have corresponding constraint, so check
    if (lmap.count(pos_lit) == 1) {
      // If model says A=0,B=1. Lookup constraints (inequalities) for !A and B
      if (model[i] == CMSat::l_True) {
        constraints.push_back(lmap.at(pos_lit));
      }
      else if (model[i] == CMSat::l_False) {
        constraints.push_back(lmap.at(pos_lit));
      }
      else {
        throw std::domain_error("Undefined Boolean Value in Model");
      }
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

ibex::NormalizedSystem build_system(const ibex::ExprSymbol &omega, const std::vector<ibex::ExprCtr> &constraints) {
  ibex::SystemFactory fac;
  fac.add_var(omega);
  for (const auto &c: constraints) {
    fac.add_ctr(c);
  }
  return ibex::NormalizedSystem(ibex::System(fac));
}

tuple<Box, BoolModel, double, double>
get_box(const LiteralMap &lmap, const ibex::ExprSymbol &omega, const Box &init_box,
        CMSat::SATSolver &solver, std::mt19937 &gen) {
  while (true) {
    // Gets boolean model (if exists) using CMSat, convert to conjunction of constraints 
    CMSat::lbool res = solver.solve();
    if (res == CMSat::l_False) {throw std::domain_error("Cannot condition on unsatisfiable condition");}
    if (res == CMSat::l_Undef) {throw std::runtime_error("SAT Solver failed");}

    BoolModel bool_model = solver.get_model();
    std::cout << "Got new model" << std::endl;
    std::vector<ibex::ExprCtr> constraints = conjoin_constraints(lmap, bool_model);
    
    // Build system
    ibex::NormalizedSystem sys = build_system(omega, constraints);
    sys.box = init_box;
    ibex::CtcHC4 hc4(sys);
    hc4.accumulate=true;
    ibex::SmearMaxRelative bsc(sys, 0.01);

    // Check constraints at theory level (may be logically SAT but UNSAT at theory level)
    auto sample = theory_sample(sys, hc4, bsc, gen);
    CMSat::lbool theory_sat = get<0>(sample);
    ibex::IntervalVector box = get<1>(sample);
    double logq = get<2>(sample);
    double prevolfrac = get<3>(sample);
    double logp = logmeasure(box) + log(prevolfrac);

    if (theory_sat == CMSat::l_True) {
       return std::tuple<ibex::IntervalVector,BoolModel,double,double>{box, bool_model, logq, logp};
    }
    else if (theory_sat == CMSat::l_False) {
      solver.add_clause(model_to_conflict(bool_model));
    }
    else {
      std::runtime_error("Theory Solver failed");
    }
  }
}

// @doc "Main loop to construct preimage samples" ->
std::vector<Box> pre_tlmh(const LiteralMap &lmap, const CNF &cnf,
                          const ibex::ExprSymbol &omega, const Box &init_box,
                          int nsamples) {
  // Add CNF to solver
  CMSat::SATSolver solver;
  solver.new_vars(max_var(cnf)+1);
  solver.set_num_threads(1);
  add_clauses(solver, cnf);
  std::vector<Box> samples;
  samples.reserve(nsamples);

  // Setup randomness
  std::random_device rd;
  std::mt19937 gen(rd());
  std::uniform_real_distribution<> uniform(0, 1);

  // Try to find first satisfying box
  Box omega_box = init_box;
  auto sample = get_box(lmap, omega, omega_box, solver, gen);
  Box box = get<0>(sample);
  double logq = get<2>(sample);
  double logp = get<3>(sample);

  // First box is a valid sample
  samples.push_back(box);

  while (samples.size() < nsamples) {
    auto sample = get_box(lmap, omega, omega_box, solver, gen);
    Box nextbox = get<0>(sample);
    double nextlogq = get<2>(sample);
    double nextlogp = get<3>(sample);

    double loga = nextlogp + logq - logp - nextlogq;
    double a = exp(loga);
    
    std::cout << "old box is: " << box << std::endl;
    std::cout << "new box is:" << nextbox << std::endl;
    std::cout << "oldp: " << logp << " oldq: " << logq << std::endl;
    std::cout << "newp: " << nextlogp << "newq: " << nextlogq << std::endl;
    std::cout << "a value: " << a << std::endl;

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
    std::cout << std::endl;
    samples.push_back(box);
  }

  return samples;
}

}