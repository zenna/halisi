#pragma once
// Uniform Generation of Samples

#include "halisi/util.h"
namespace halisi {

struct SATCount {
  uint32_t hashCount;
  uint32_t cellSolCount;
};

int32_t Main::BoundedSATCount(uint32_t maxSolutions, Solver &solver, vec<Lit> &assumptions) {
  unsigned long current_nr_of_solutions = 0;
  lbool ret = l_True;
  Var activationVar = solver.newVar();
  vec<Lit> allSATAssumptions;
  if (!assumptions.empty()) {
    assumptions.copyTo(allSATAssumptions);
  }
  allSATAssumptions.push(Lit(activationVar, true));
  //signal(SIGALRM, SIGALARM_handler);
  start_timer(conf.loopTimeout);
  while (current_nr_of_solutions < maxSolutions && ret == l_True) {
    ret = solver.solve(allSATAssumptions);
    current_nr_of_solutions++;
    if (ret == l_True && current_nr_of_solutions < maxSolutions) {
      vec<Lit> lits;
      lits.push(Lit(activationVar, false));
      for (uint32_t j = 0; j < solver.independentSet.size(); j++) {
          Var var = solver.independentSet[j];
          if (solver.model[var] != l_Undef) {
              lits.push(Lit(var, (solver.model[var] == l_True) ? true : false));
          }
      }
      solver.addClause(lits);
    }
  }
  vec<Lit> cls_that_removes;
  cls_that_removes.push(Lit(activationVar, false));
  solver.addClause(cls_that_removes);
  if (ret == l_Undef){
    solver.needToInterrupt = false;
    return -1*current_nr_of_solutions;
  }
  return current_nr_of_solutions;
}

lbool Main::BoundedSAT(uint32_t maxSolutions, uint32_t minSolutions, Solver &solver, vec<Lit> &assumptions, std::mt19937 &randomEngine, std::map<std::string, uint32_t> &solutionMap, uint32_t *solutionCount) {
  unsigned long current_nr_of_solutions = 0;
  lbool ret = l_True;
  Var activationVar = solver.newVar();
  vec<Lit> allSATAssumptions;
  if (!assumptions.empty()) {
      assumptions.copyTo(allSATAssumptions);
  }
  allSATAssumptions.push(Lit(activationVar, true));

  std::vector<vec<lbool>> modelsSet;
  vec<lbool> model;
  //signal(SIGALRM, SIGALARM_handler);
  start_timer(conf.loopTimeout);

  // Generate min(max_solutions, total number of possible solutions)
  while (current_nr_of_solutions < maxSolutions && ret == l_True) {
      ret = solver.solve(allSATAssumptions);
      current_nr_of_solutions++;

      if (ret == l_True && current_nr_of_solutions < maxSolutions) {
        vec<Lit> lits;
        // We need to add the activation
        lits.push(Lit(activationVar, false));
        model.clear();
        solver.model.copyTo(model);
        modelsSet.push_back(model);
        // Generate a conflict for this model such that we never generate it again
        for (uint32_t j = 0; j < solver.independentSet.size(); j++) {
            Var var = solver.independentSet[j];
            if (solver.model[var] != l_Undef) {
                lits.push(Lit(var, (solver.model[var] == l_True) ? true : false));
            }
        }
        solver.addClause(lits);
    }
  }
  *solutionCount = modelsSet.size();
  //std::cout<<current_nr_of_solutions<<std::endl;
  
  // Deactive Sample Solutions
  vec<Lit> cls_that_removes;
  cls_that_removes.push(Lit(activationVar, false));
  solver.addClause(cls_that_removes);
  if (ret == l_Undef){
      solver.needToInterrupt = false;
      
      return ret;
  }

  // Sample Solutions
  if (current_nr_of_solutions < maxSolutions && current_nr_of_solutions > minSolutions) {
      std::vector<int> modelIndices;
      for (uint32_t i = 0; i < modelsSet.size(); i++)
          modelIndices.push_back(i);
      std::shuffle(modelIndices.begin(), modelIndices.end(), randomEngine);
      Var var;
      uint32_t numSolutionsToReturn = SolutionsToReturn(maxSolutions, minSolutions, modelsSet.size());
      for (uint32_t i = 0; i < numSolutionsToReturn; i++) {
          vec<lbool> model = modelsSet.at(modelIndices.at(i));
          string solution ("v");
          for (uint32_t j = 0; j < solver.independentSet.size(); j++) {
              var = solver.independentSet[j];
              if (model[var] != l_Undef) {
                  if (model[var] != l_True) {
                      solution += "-";
                  }
                  solution += std::to_string(var+1);   
                  solution += " ";
              }
          }
          solution += "0";

          map<std::string, uint32_t>::iterator it = solutionMap.find(solution);
          if (it == solutionMap.end()) {
              solutionMap[solution] = 0; 
          }
          solutionMap[solution] += 1;
      }
      return l_True;
  
  }

  return l_False;
}

/*
 * Returns the number of samples generated 
 */
uint32_t UniGen(uint32_t samples, Solver &solver, uint32_t sampleCounter,
                std::mt19937 &gen, 
                uint32_t *lastSuccessfulHashOffset,
                double timeReference) {
  lbool ret = l_False;
  uint32_t i, solutionCount, currentHashCount, lastHashCount;
  uint32_t currentHashOffset, hashOffsets[3];

  int hashDelta;
  vec<Lit> assumptions;
  double elapsedTime =0;
  int repeatTry = 0;
  for (i = 0; i < samples; i++) {
    sampleCounter ++;
    ret = l_False;

    hashOffsets[0] = *lastSuccessfulHashOffset;   /* Start at last successful hash offset */
    if(hashOffsets[0] == 0) {     /* Starting at q-2; go to q-1 then q */
      hashOffsets[1] = 1; 
      hashOffsets[2] = 2;
    }
    else if(hashOffsets[0] == 2)    /* Starting at q; go to q-1 then q-2 */
    {
      hashOffsets[1] = 1;
      hashOffsets[2] = 0;
    }
    repeatTry = 0;
    lastHashCount = 0;
    for(uint32_t j = 0; j < 3; j++) {
      // j=0, hashoffset = 1
      // j=0, hashoffset = 1
      // j=1, currhashoffset = 0
      currentHashOffset = hashOffsets[j];
      // startit = 5, currenthashcount = 6
      // startit = 5, currenthashcount = 6
      // startit = 5, currenthashcount = 5
      currentHashCount = currentHashOffset + conf.startIteration;

      // hasDelta = 6 - 0 = 6
      // hasDelta = 6 - 6 = 0 (no hashes added or removed)
      // hasDelta = 5 - 0 = 5
      hashDelta = currentHashCount - lastHashCount;

      if(hashDelta > 0) {   /* Add new hash functions */
        AddHash(hashDelta, solver, assumptions, gen);
      }
      else if(hashDelta < 0) {    /* Remove hash functions */ 
        assumptions.clear();
        AddHash(currentHashCount, solver, assumptions, gen);
      }
      // = 6
      // = 6
      lastHashCount = currentHashCount;

      ret = BoundedSAT(maxSolutions + 1, minSolutions, solver, assumptions, gen, solutionMap, &solutionCount);
      if (ret == l_Undef) {     /* Solver timed out; retry current hash count at most twice more */
        assumptions.clear();    /* Throw out old hash functions */
        if (repeatTry < 2){     /* Retry current hash count with new hash functions */
          // 6
          AddHash(currentHashCount, solver, assumptions, gen);
          j--;
          repeatTry += 1;
        }
        else{/* Go on to next hash count */
          lastHashCount = 0;
          if((j == 0) && (currentHashOffset == 1)) {   /* At q-1, and need to pick next hash count */
            /* Somewhat arbitrarily pick q-2 first; then q */
            hashOffsets[1] = 0;
            hashOffsets[2] = 2;
          }
          repeatTry = 0;
        }
        continue;
      }
        else if (ret == l_True) { /* Number of solutions in correct range */
        *lastSuccessfulHashOffset = currentHashOffset;
        break;
      }
      else {    /* Number of solutions too small or too large */
        if((j == 0) && (currentHashOffset == 1)) { /* At q-1, and need to pick next hash count */
          // In bounded SAT we couldnt find enough soutions so lets try a
          // smalller cells,
          // if j != 0, second failure and no choice of what to try next
          // if currentHashOffset != 1, then the next one ill try is already
          // The smallest one
          if(solutionCount < minSolutions) {
            /* Go to q-2; next will be q */
            hashOffsets[1] = 0;
            hashOffsets[2] = 2;
          }
          else {
            /* Go to q; next will be q-2 */
            hashOffsets[1] = 2;
            hashOffsets[2] = 0;
          }
        }
      }
    }
    if (ret != l_True) {
      i --;
    }
    assumptions.clear();
    if (elapsedTime > conf.totalTimeout - 3000){
      break;
    }
  }
  return sampleCounter;
}


// // MAKE  Solver
// SATCount ApproxMC(Solver &solver, vector<FILE *> *resLog, std::mt19937 &gen) {
//   int32_t currentNumSolutions = 0;
//   uint32_t hashCount;
//   std::list<int> numHashList, numCountList;
//   vec<Lit> assumptions;
//   SATCount solCount;
//   solCount.cellSolCount = 0;
//   solCount.hashCount = 0;
//   double elapsedTime = 0;
//   int repeatTry = 0;
//   for (uint32_t j = 0; j < conf.tApproxMC; j++) {
//     for (hashCount = 0; hashCount < solver.nVars(); hashCount++) {
//       double currentTime = totalTime();
//       elapsedTime = currentTime-startTime;
//       if (elapsedTime > conf.totalTimeout - 3000) {break;}
//       double myTime = totalTime();
//       currentNumSolutions = BoundedSATCount(conf.pivotApproxMC + 1, solver, assumptions);
//       myTime = totalTime() - myTime;
//         //printf("%f\n", myTime);
//         //printf("%d %d\n",currentNumSolutions,conf.pivotApproxMC);
//       if (currentNumSolutions <= 0){
//         assumptions.clear();
//         if (repeatTry < 2){     /* Retry up to twice more */
//           AddHash(hashCount,solver,assumptions,randomEngine);
//             hashCount --;
//             repeatTry += 1;
//         }
//         else {
//           AddHash(hashCount+1,solver,assumptions,randomEngine);
//           repeatTry = 0;
//         }
//         continue;
//       }
//       if (currentNumSolutions == conf.pivotApproxMC + 1) {
//         AddHash(1,solver,assumptions,randomEngine);
//       }
//       else {break; }
//     }
//     assumptions.clear();
//     if (elapsedTime > conf.totalTimeout - 3000){
//       break;
//     }
//     numHashList.push_back(hashCount);
//     numCountList.push_back(currentNumSolutions);
//   }
//   if (numHashList.size() == 0){
//       return solCount;
//   }
//   int minHash = findMin(numHashList);
//   for (std::list<int>::iterator it1 = numHashList.begin(), it2 = numCountList.begin();
//       it1 != numHashList.end() && it2 != numCountList.end(); it1++, it2++) {
//       (*it2) *= pow(2, (*it1) - minHash);
//   }
//   int medSolCount = findMedian(numCountList);
//   solCount.cellSolCount = medSolCount;
//   solCount.hashCount = minHash;
//   return solCount;
// }

// Parameters
// ==========

compute_parameters() {
  uint32_t maxSolutions = (uint32_t) (1.41*(1+conf.kappa)*conf.pivotUniGen +2);
  uint32_t minSolutions = (uint32_t) (conf.pivotUniGen/(1.41*(1+conf.kappa)));
  uint32_t samplesPerCall = SolutionsToReturn(maxSolutions+1, minSolutions, minSolutions);
  uint32_t callsNeeded = (conf.samples + samplesPerCall - 1) / samplesPerCall;
  printf("loThresh %d, hiThresh %d, startIteration %d\n", minSolutions, maxSolutions, conf.startIteration);
}

// "kappa: computed from desired tolerance as in Algorithm 1 of the TACAS-15 paper (default: 0.638, corresponding to epsilon=16)\n"
kappa() {
  return 0.638;
}

// “pivot” represent the expected size of a “small” cell
double pivot(double kappa) {
  pivotUniGen = math.ceil(4.03*(1+1/kappa)*(1+1/kappa));
}

uint32_t maxSolutions(dobule kappa, double pivot) {
  (uint32_t) (1.41*(1+kappa)*pivotUniGen +2);
}

uint32_t minSolutions(double ) {
  (uint32_t) (pivotUniGen/(1.41*(1+kappa)));
}


calls_per_loop(int nvars, int start_iteration, int callsNeeded, int numThreads) {
  uint32_t numCallsInOneLoop = std::min(solver.nVars()/(conf.startIteration*14), callsNeeded/numThreads);
  if (numCallsInOneLoop == 0){numCallsInOneLoop = 1;}
  return numCallsInOneLoop;
}

// Computes upper (?)  bound on range of values for m
// m is the string in the range of the hash function 
uint32_t start_iteration(pivotUniGen, std::mt19937 &gen) {
  SATCount solCount = ApproxMC(solver, resLog, gen);
  if (solCount.hashCount == 0 && solCount.cellSolCount == 0){
    throw std::domain_error("Cannot condition on unsatisfiable condition");
  }
  uint32_t q = round(solCount.hashCount + log2(solCount.cellSolCount) + 
               log2(1.8) - log2(pivotUniGen))-2;
  return q;
}

// Initialize SAT solver with 
void init(const CNF &cnf, const std::vector<BoolVar> &independentSet, uint32_t startIteration) {
  CMSat::Solver solver(conf, gaussconfig);
  add_clauses(solver, cnf);
  if (startIteration > solver.independentSet.size()){
    printf("Manually-specified startIteration is larger than the size of the independent set.\n");
    return 0;
  }

  uint32_t numCallLoops = callsNeeded / numCallsInOneLoop;
  uint32_t remainingCalls = callsNeeded % numCallsInOneLoop;

  bool timedOut;
  uint32_t sampleCounter = 0;
  std::map<std::string, uint32_t> threadSolutionMap;
  double allThreadsTime = 0;
  uint32_t allThreadsSampleCount = 0;
  printf("Launching %d sampling thread(s)\n", numThreads);
  
  // Per thread
  int threadNum = 0;
  timedOut = false;
  double threadStartTime = totalTime();
  std::mt19937 randomEngine{};
  SeedEngine(randomEngine);
  uint32_t lastSuccessfulHashOffset = 0;


  if(remainingCalls > 0)
    sampleCounter = singleThreadUniGenCall(remainingCalls,res,resLog,sampleCounter,threadSolutionMap,randomEngine,&lastSuccessfulHashOffset,threadStartTime);

  sampleCounter = singleThreadUniGenCall(numCallsInOneLoop,res,resLog,sampleCounter,threadSolutionMap,randomEngine,&lastSuccessfulHashOffset,threadStartTime);
  if ((totalTime() - threadStartTime) > conf.totalTimeout - 3000) timedOut = true;
}

void init(const CNF &cnf) {
  return init(start_iteration(pivotUniGen));
}
}