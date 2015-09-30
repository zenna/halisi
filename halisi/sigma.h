# pragma once

#include <memory>
#include <cmsat/Solver.h>
#include "ibex/ibex.h"
#include "halisi/sat.h"
#include "halisi/refine.h"
#include "halisi/types.h"
#include "halisi/measure.h"

#include <random>
#include <tuple>
#include <limits>
#include <iostream>
#include <stdlib.h>
#include <stdexcept>

namespace halisi {

void print_literal_map(const LiteralMap &lmap) {
  for (const auto key_map : lmap) {
    std::cout << key_map.first << " -> " << key_map.second << std::endl;
  }
  std::cout << std::endl;  
}

// Creates a conjunction of constraints
std::vector<ibex::ExprCtr> conjoin_constraints(const LiteralMap &lmap, const BoolModel &model) {
  std::vector<ibex::ExprCtr> constraints;

  for (int i = 0; i<model.size(); ++i) {
    CMSat::Lit pos_lit(i,false);
    CMSat::Lit neg_lit(i,true);

    // Some literals do not have corresponding constraint, so check
    if ((lmap.count(pos_lit) == 1) && (model[i] == CMSat::l_True)) {
      // If model says A=0,B=1. Lookup constraints (inequalities) for !A and B
      constraints.push_back(lmap.at(pos_lit));
    }
    if ((lmap.count(neg_lit) == 1) && (model[i] == CMSat::l_False)) {
      constraints.push_back(lmap.at(neg_lit));
    }
    if (model[i] == CMSat::l_Undef) {
      throw std::domain_error("Undefined Boolean Value in Model");
    }
  }
  return constraints;
}

ibex::NormalizedSystem build_system(const ibex::ExprSymbol &omega,
                                    const std::vector<std::shared_ptr<ibex::ExprSymbol>> &aux_vars,
                                    const std::vector<ibex::ExprCtr> &constraints) {
  ibex::SystemFactory fac;
  // Add variables
  fac.add_var(omega);
  std::cout << "Got " << aux_vars.size() << " auxilariy variables" << std::endl;
  for (const auto &aux_var: aux_vars) {
    fac.add_var(*aux_var);
  }

  std::cout << "contraints are:" << std::endl;
  // Add onstraints
  for (const auto &c: constraints) {
    std::cout << c << std::endl;
    fac.add_ctr(c);
  }
  std::cout << std::endl;
  // Normalize
  return ibex::NormalizedSystem(ibex::System(fac));
}

// Get a model from cryptominisat and convert to BoolModel
BoolModel get_model(const CMSat::Solver &solver) {
  return to_stl_vec(solver.model);
}

tuple<Box, BoolModel, double, double>
get_box(const LiteralMap &lmap, const ibex::ExprSymbol &omega,
        const std::vector<std::shared_ptr<ibex::ExprSymbol>> &aux_vars, const Box &init_box,
        CMSat::Solver &solver, std::mt19937 &gen) {

  int ndimsomega = init_box.size() - aux_vars.size();
  assert(ndimsomega > 0);

  while (true) {
    // Gets boolean model (if exists) using CMSat, convert to conjunction of constraints 
    CMSat::lbool res = solver.solve();
    if (res == CMSat::l_False) {throw std::domain_error("Cannot condition on unsatisfiable condition");}
    if (res == CMSat::l_Undef) {throw std::runtime_error("SAT Solver failed");}

    BoolModel bool_model = get_model(solver);
    std::cout << "Got new model" << std::endl;
    print_bool_model(bool_model);
    std::vector<ibex::ExprCtr> constraints = conjoin_constraints(lmap, bool_model);
    
    // Build system
    ibex::NormalizedSystem sys = build_system(omega, aux_vars, constraints);
    sys.box = init_box;
    ibex::CtcHC4 hc4(sys);
    hc4.accumulate=true;
    ibex::CtcHC4 hc4_2(sys,0.1,true);
    ibex::CtcAcid acid(sys, hc4_2);
    // ibex::CtcNewton newton(sys.f, 5e+08, 0.01, 1e-04);
    ibex::LinearRelaxCombo linear_relax(sys,LinearRelaxCombo::COMPO);
    ibex::CtcPolytopeHull polytope(linear_relax,CtcPolytopeHull::ALL_BOX);
    ibex::CtcCompo polytope_hc4(polytope, hc4);
    ibex::CtcFixPoint fixpoint(polytope_hc4);
    ibex::CtcCompo compo(hc4,acid,fixpoint);

    ibex::SmearMaxRelative bsc(sys, 0.01);

    // Check constraints at theory level (may be logically SAT but UNSAT at theory level)
    auto sample = theory_sample(sys, compo, bsc, gen);
    CMSat::lbool theory_sat = get<0>(sample);
    ibex::IntervalVector box = get<1>(sample);
    double logq = get<2>(sample);

    // How full (as a fraction) is the box?
    double box_fullness = get<3>(sample);
    // Estimate of volume of feasible region within box = size(box) * fraction full
    double logp = logmeasure(box, ndimsomega) + log(box_fullness);

    if (theory_sat == CMSat::l_True) {
      std::cout << "Logmeasure " << logmeasure(box) << " log(box_fullness): " << log(box_fullness) << std::endl;
      return std::tuple<ibex::IntervalVector,BoolModel,double,double>{box, bool_model, logq, logp};
    }
    else if (theory_sat == CMSat::l_False) {
      // Unsat at theory level, add clause to avoid trying it again
      add_clause(solver, model_to_conflict(bool_model));
    }
    else {
      std::runtime_error("Theory Solver failed");
    }
  }
}

// @doc "Main loop to construct preimage samples" ->
std::vector<Box> pre_tlmh(const LiteralMap &lmap, const CNF &cnf,
                          const ibex::ExprSymbol &omega,
                          const std::vector<std::shared_ptr<ibex::ExprSymbol>> &aux_vars,
                          const Box &init_box,
                          int nsamples) {

  std::cout << "Literal Map" << std::endl;
  print_literal_map(lmap);

  // Add CNF to solver
  CMSat::Solver solver;
  new_vars(solver, max_var(cnf)+1);
  // solver.set_num_threads(1);
  add_clauses(solver, cnf);
  std::vector<Box> samples;
  samples.reserve(nsamples);

  // Setup randomness
  std::random_device rd;
  std::mt19937 gen(rd());
  std::uniform_real_distribution<> uniform(0, 1);

  // Try to find first satisfying box
  Box omega_box = init_box;
  auto sample = get_box(lmap, omega, aux_vars, omega_box, solver, gen);
  Box box = get<0>(sample);
  double logq = get<2>(sample);
  double logp = get<3>(sample);

  // First box is a valid sample
  samples.push_back(box);

  // Run Markov Chain
  while (samples.size() < nsamples) {
    std::cout << " \n \n Doing Markov Chain Loop Iteration " << samples.size() << " so far" << std::endl;
    auto sample = get_box(lmap, omega, aux_vars, omega_box, solver, gen);
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
    else {
      cout << "sticking" << endl;
    }
    std::cout << std::endl;
    samples.push_back(box);
  }

  return samples;
}

}