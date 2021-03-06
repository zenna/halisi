#pragma once

#include "halisi/types.h"
#include "halisi/rand.h"
#include "halisi/sat.h"

#include <random>

namespace halisi {

std::string binary(int x, uint32_t length) {
  uint32_t logSize = (x == 0 ? 1 : log2(x) + 1);
  std::string s;
  do {
      s.push_back('0' + (x & 1));
  } while (x >>= 1);
  for (uint32_t i = logSize; i < (uint32_t) length; i++) {
      s.push_back('0');
  }
  std::reverse(s.begin(), s.end());

  return s;
}

std::string gen_random_bits(uint32_t size, std::mt19937 &gen) {
  std::string random_bits;
  std::uniform_int_distribution<int> uid{0, 2147483647};
  uint32_t i = 0;
  while (i < size) {
    i += 31;
    random_bits += binary(uid(gen), 31);
  }
  return random_bits;
}

// Adds an XOR hash to a function
void add_hash(uint32_t nclauses, Solver& solver, std::vector<Lit> &assumptions,
              std::mt19937 &gen, const std::vector<BoolVar> &independent_vars) {
  int nindependent_vars = independent_vars.size();
  std::string random_bits = gen_random_bits((nindependent_vars + 1) * nclauses, gen);
  BoolVar activation_var; // A literal added to XOR clause to turn it off or on
  std::vector<Lit> lits;

  for (uint32_t i = 0; i < nclauses; i++) {
    lits.clear();
    activation_var = solver.newVar();
    assumptions.push_back(Lit(activation_var, true));
    lits.push_back(Lit(activation_var, false));
    bool xor_is_false = (random_bits[(nindependent_vars + 1) * i] == 1);

    for (uint32_t j = 0; j < nindependent_vars; j++) {
      if (random_bits[(nindependent_vars + 1) * i + j] == '1') {
        lits.push_back(Lit(independent_vars[j], true));
      }
    }
  solver.addXorClause(lits, xor_is_false);
  }
}

// Sample models from within a region bounded by the assumptions
// This region will be contrained to the preimage of a bitstring under a hash
std::tuple<LBool, std::vector<BoolModel>>
bounded_sat(uint32_t max_sols, uint32_t min_sols, Solver &solver,
            const std::vector<BoolVar> &independent_vars,
            std::vector<Lit> assumptions, std::mt19937 &gen) {
  // Turns on or off xor constraint
  BoolVar activation_var = solver.newVar();
  assumptions.push_back(Lit(activation_var, true));

  std::vector<BoolModel> models;
  unsigned long num_sols = 0;
  LBool ret = l_True;

  // Sample max_sols unique solutions
  while (num_sols < max_sols && ret == l_True) {
    ret = solver.solve(to_cmsat_vec(assumptions));

    // Add conflict to not resample old ones
    if (ret == l_True && num_sols < max_sols) {
      num_sols++;
      std::vector<Lit> lits;
      BoolModel model = to_stl_vec(solver.model);
      models.push_back(model);
      LitVec conflict = model_to_conflict(model, independent_vars);
      conflict.push_back(Lit(activation_var, false));
      solver.addClause(conflict);
    }
  }

  // Remove this hash
  std::vector<Lit> cls_that_removes;
  cls_that_removes.push_back(Lit(activation_var, false));
  solver.addClause(cls_that_removes);

  // WIll return true with modelset if got reach here if we couldn't generate enough solutions
  return tuple<LBool, std::vector<BoolModel>>(ret, models);
}

// Generates a random boolean model
// Attempt F ∧ (hash(x) = {0,1}^m) where m is q, q +1, q + 2
// Two optimisations:
// Choose initial m which was successful before
// (because success begets success)
// Choose subsequent m based on whether num solutions found too big or small

// TODO: should return laast best hash size
tuple<LBool,BoolModel>
unigen(Solver &solver, uint32_t last_good_hash_offset, std::mt19937 &gen,
      int min_sols, int max_sols, const std::vector<BoolVar> &independent_vars) {
  // Assumptions used to 'turn on and off' hashing constraint
  std::vector<Lit> assumptions;

  // Hash size = + offset
  std::list<int> offsets = {0,1,2};
  int offset = last_good_hash_offset;
  offsets.remove(offset);

  // Try al three possible hash sixes
  for (int i = 0; i < 3; ++i) {
    // Take three attempts at each hash size
    for (int j=0; j < 3; j++) {
      add_hash(offset, solver, assumptions, gen, independent_vars);

      // TODO: Get ris of this weird + 1
      auto result = bounded_sat(max_sols + 1, min_sols, solver, independent_vars, assumptions, gen);
      LBool success = get<0>(result);
      std::vector<BoolModel> solutions = get<1>(result);
      int nsolutions = solutions.size();

      // Solving Failed
      if (success == l_False) {
        // Try again with same m but different hash
        assumptions.clear();
      }

      // If I have a choice, try larger cells (smaller m)
      else if ((success == l_True) && (nsolutions < min_sols)) {
        offset = offsets.front();
        offsets.pop_front();
        break;
      }

      // // If I have a choice, try smaller cells (larger m)
      // else if ((success == l_True) && (nsolutions > min_sols)) {
      //   m = ms.back();
      //   ms.pop_back();
      //   break;
      // }

      // solver returned correct number of solutions
      else if ((success == l_True)) {
        // Sample from solution set and done
        return std::tuple<LBool, BoolModel>(l_True, rand_select(solutions, gen));
      }
    }
  }

  // TODO:: Couldn't get a sample
  // return std::tuple<l_False, 
}


}