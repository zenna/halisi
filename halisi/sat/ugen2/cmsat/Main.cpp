/***************************************************************************************
Copyright (c) 2003-2006, Niklas Een, Niklas Sorensson
Copyright (c) 2007-2009, Niklas Sorensson
Copyright (c) 2009-2012, Mate Soos
Copyright (c) 2014, Supratik Chakraborty, Kuldeep S. Meel, Moshe Y. Vardi
Copyright (c) 2015, Supratik Chakraborty, Daniel J. Fremont, Kuldeep S. Meel, Sanjit A. Seshia, Moshe Y. Vardi
Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
associated documentation files (the "Software"), to deal in the Software without restriction,
including without limitation the rights to use, copy, modify, merge, publish, distribute,
sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or
substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT
OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 **************************************************************************************************/

/**
@mainpage UniGen2
@author Kuldeep S. Meel, Daniel J. Fremont
 */

#include <ctime>
#include <cstring>
#include <errno.h>
#include <string.h>


void Main::setDoublePrecision(const uint32_t verbosity) {
#if defined(_FPU_EXTENDED) && defined(_FPU_DOUBLE)
    fpu_control_t oldcw, newcw;
    #pragma omp critical
    {
        _FPU_GETCW(oldcw);
        newcw = (oldcw & ~_FPU_EXTENDED) | _FPU_DOUBLE;
        _FPU_SETCW(newcw);
    }

    if (verbosity >= 1) {
        printf("c WARNING: for repeatability, setting FPU to use double precision\n");
    }
#endif
}


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

bool Main::GenerateRandomBits(string &randomBits, uint32_t size, std::mt19937 &randomEngine) {
    std::uniform_int_distribution<int> uid{0, 2147483647};
    uint32_t i = 0;
    while (i < size) {
        i += 31;
        randomBits += binary(uid(randomEngine), 31);
    }
    return true;
}
int Main::GenerateRandomNum(int maxRange, std::mt19937 &randomEngine) {
    std::uniform_int_distribution<int> uid{0, maxRange};
    return uid(randomEngine);
}
/* Number of solutions to return from one invocation of UniGen2 */
uint32_t Main::SolutionsToReturn(uint32_t maxSolutions, uint32_t minSolutions, unsigned long currentSolutions) {
    if(conf.multisample)
        return minSolutions;
    else
        return 1;
}
bool Main::AddHash(uint32_t numClaus, Solver& solver, vec<Lit> &assumptions, std::mt19937 &randomEngine) {
    string randomBits;
    GenerateRandomBits(randomBits, (solver.independentSet.size() + 1) * numClaus, randomEngine);
    bool xorEqualFalse = false;
    Var activationVar;
    vec<Lit> lits;

    for (uint32_t i = 0; i < numClaus; i++) {
        lits.clear();
        activationVar = solver.newVar();
        assumptions.push(Lit(activationVar, true));
        lits.push(Lit(activationVar, false));
        xorEqualFalse = (randomBits[(solver.independentSet.size() + 1) * i] == 1);

        for (uint32_t j = 0; j < solver.independentSet.size(); j++) {

            if (randomBits[(solver.independentSet.size() + 1) * i + j] == '1') {
                lits.push(Lit(solver.independentSet[j], true));
            }
        }
        solver.addXorClause(lits, xorEqualFalse);
    }
    return true;
}

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
    while (current_nr_of_solutions < maxSolutions && ret == l_True) {
        ret = solver.solve(allSATAssumptions);
        current_nr_of_solutions++;
 
        if (ret == l_True && current_nr_of_solutions < maxSolutions) {
            vec<Lit> lits;
            lits.push(Lit(activationVar, false));
            model.clear();
            solver.model.copyTo(model);
            modelsSet.push_back(model);
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

SATCount Main::ApproxMC(Solver &solver, vector<FILE *> *resLog, std::mt19937 &randomEngine) {
    int32_t currentNumSolutions = 0;
    uint32_t  hashCount;
    std::list<int> numHashList, numCountList;
    vec<Lit> assumptions;
    SATCount solCount;
    solCount.cellSolCount = 0;
    solCount.hashCount = 0;
    double elapsedTime = 0;
    int repeatTry = 0;
    for (uint32_t j = 0; j < conf.tApproxMC; j++) {
        for (hashCount = 0; hashCount < solver.nVars(); hashCount++) {
            double currentTime = totalTime();
            elapsedTime = currentTime-startTime;
            if (elapsedTime > conf.totalTimeout - 3000){
                break;
            }
            double myTime = totalTime();
            currentNumSolutions = BoundedSATCount(conf.pivotApproxMC + 1, solver, assumptions);

            myTime = totalTime() - myTime;
            //printf("%f\n", myTime);
            //printf("%d %d\n",currentNumSolutions,conf.pivotApproxMC);
            if (conf.shouldLog) {
                fprintf((*resLog)[0], "ApproxMC:%d:%d:%f:%d:%d\n", j, hashCount, myTime,
                    (currentNumSolutions == (int32_t)(conf.pivotApproxMC + 1)),currentNumSolutions);
                fflush((*resLog)[0]);
            }
            if (currentNumSolutions <= 0){
                assumptions.clear();
                if (repeatTry < 2){     /* Retry up to twice more */
                    AddHash(hashCount,solver,assumptions,randomEngine);
                    hashCount --;
                    repeatTry += 1;
                }else{
                    AddHash(hashCount+1,solver,assumptions,randomEngine);
                    repeatTry = 0;
                }
                continue;
            }
            if (currentNumSolutions == conf.pivotApproxMC + 1) {
                AddHash(1,solver,assumptions,randomEngine);
            } else {
                break;
            }

        }
        assumptions.clear();
        if (elapsedTime > conf.totalTimeout - 3000){
            break;
        }
        numHashList.push_back(hashCount);
        numCountList.push_back(currentNumSolutions);
    }
    if (numHashList.size() == 0){
        return solCount;
    }
    int minHash = findMin(numHashList);
    for (std::list<int>::iterator it1 = numHashList.begin(), it2 = numCountList.begin();
        it1 != numHashList.end() && it2 != numCountList.end(); it1++, it2++) {
        (*it2) *= pow(2, (*it1) - minHash);
    }
    int medSolCount = findMedian(numCountList);
    solCount.cellSolCount = medSolCount;
    solCount.hashCount = minHash;
    return solCount;
}

/*
 * Returns the number of samples generated 
 */
uint32_t Main::UniGen(uint32_t samples, Solver &solver,
        FILE* res, vector<FILE* > *resLog, uint32_t sampleCounter, std::mt19937 &randomEngine, std::map<std::string, uint32_t> &solutionMap, uint32_t *lastSuccessfulHashOffset, double timeReference) {
    lbool ret = l_False;
    uint32_t i, solutionCount, currentHashCount, lastHashCount, currentHashOffset, hashOffsets[3];
    int hashDelta;
    vec<Lit> assumptions;
    double elapsedTime =0;
    #if defined(_OPENMP)
    int threadNum = omp_get_thread_num();
    #else
    int threadNum = 0;
    #endif
    int repeatTry = 0;
    for (i = 0; i < samples; i++) {
        sampleCounter ++;
        ret = l_False;

        hashOffsets[0] = *lastSuccessfulHashOffset;   /* Start at last successful hash offset */
        if(hashOffsets[0] == 0)     /* Starting at q-2; go to q-1 then q */
        {
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
            currentHashOffset = hashOffsets[j];
            currentHashCount = currentHashOffset + conf.startIteration;
            hashDelta = currentHashCount - lastHashCount;

            if(hashDelta > 0)   /* Add new hash functions */
                AddHash(hashDelta, solver, assumptions, randomEngine);
            else if(hashDelta < 0)    /* Remove hash functions */
            {
                assumptions.clear();
                AddHash(currentHashCount, solver, assumptions, randomEngine);
            }
            lastHashCount = currentHashCount;

            double currentTime = totalTime(); 
            elapsedTime = currentTime-startTime;
            if (elapsedTime > conf.totalTimeout - 3000){
                break;
            }
            uint32_t maxSolutions = (uint32_t) (1.41*(1+conf.kappa)*conf.pivotUniGen +2);
            uint32_t minSolutions = (uint32_t) (conf.pivotUniGen/(1.41*(1+conf.kappa)));
            ret = BoundedSAT(maxSolutions + 1, minSolutions, solver, assumptions, randomEngine, solutionMap, &solutionCount);
            if (conf.shouldLog) {
                fprintf((*resLog)[threadNum], "UniGen2:%d:%d:%f:%d:%d\n", sampleCounter, currentHashCount, totalTime() - timeReference, (ret == l_False ? 1 : (ret == l_True ? 0 : 2)), solutionCount);
                fflush((*resLog)[threadNum]);
            }
            if (ret == l_Undef)     /* Solver timed out; retry current hash count at most twice more */
            {
                assumptions.clear();    /* Throw out old hash functions */
                if (repeatTry < 2){     /* Retry current hash count with new hash functions */
                    AddHash(currentHashCount, solver, assumptions, randomEngine);
                    j--;
                    repeatTry += 1;
                }else{      /* Go on to next hash count */
                    lastHashCount = 0;
                    if((j == 0) && (currentHashOffset == 1))    /* At q-1, and need to pick next hash count */
                    {
                        /* Somewhat arbitrarily pick q-2 first; then q */
                        hashOffsets[1] = 0;
                        hashOffsets[2] = 2;
                    }
                    repeatTry = 0;
                }
                continue;
            }
            if (ret == l_True)      /* Number of solutions in correct range */
            {
                *lastSuccessfulHashOffset = currentHashOffset;
                break;
            }
            else    /* Number of solutions too small or too large */
            {
                if((j == 0) && (currentHashOffset == 1))  /* At q-1, and need to pick next hash count */
                {
                    if(solutionCount < minSolutions)
                    {
                        /* Go to q-2; next will be q */
                        hashOffsets[1] = 0;
                        hashOffsets[2] = 2;
                    }
                    else
                    {
                        /* Go to q; next will be q-2 */
                        hashOffsets[1] = 2;
                        hashOffsets[2] = 0;
                    }
                }
            }
        }
        if (ret != l_True){
            i --;
        }
        assumptions.clear();
        if (elapsedTime > conf.totalTimeout - 3000){
            break;
        }
    }
    return sampleCounter;
}

int Main::singleThreadUniGenCall(uint32_t samples, FILE* res, vector<FILE*> *resLog, uint32_t sampleCounter, std::map<std::string, uint32_t> &solutionMap, std::mt19937 &randomEngine, uint32_t *lastSuccessfulHashOffset, double timeReference) {
    Solver solver2(conf, gaussconfig);
    //solversToInterrupt[0] = &solver2;
    //need_clean_exit = true;

    int num;
    #if defined(_OPENMP)
    num = omp_get_thread_num();
    #else
    num = 0;
    #endif

    #pragma omp critical (solversToInterr)
    {
      //printf("%d\n",num);
      solversToInterrupt[num] = &solver2;
    }
    //SeedEngine(randomEngine);

    setDoublePrecision(conf.verbosity);
    parseInAllFiles(solver2);
    sampleCounter = UniGen(samples, solver2, res, resLog, sampleCounter, randomEngine, solutionMap, lastSuccessfulHashOffset, timeReference);
    return sampleCounter;
}

void Main::SeedEngine(std::mt19937 &randomEngine)
{
    /* Initialize PRNG with seed from random_device */
    std::random_device rd{};
    std::array<int, 10> seedArray;
    std::generate_n(seedArray.data(), seedArray.size(), std::ref(rd));
    std::seed_seq seed(std::begin(seedArray), std::end(seedArray));
    randomEngine.seed(seed);
}

int Main::singleThreadSolve() {
    /* Determine the number of sampling threads to use */
    #if defined(_OPENMP)
    if(numThreads > 0)      /* Number has been specified by the user */
        omp_set_num_threads(numThreads);
    else
    {
        /* Using system default number of threads (or env variable OMP_NUM_THREADS) */
        #pragma omp parallel
        {
            if(omp_get_thread_num() == 0)
                numThreads = omp_get_num_threads();
        }
    }
    #else
    numThreads = 1;
    #endif

    startTime = totalTime();
    mytimer = new timer_t[numThreads];
    timerSetFirstTime = new bool[numThreads];
    struct sigaction sa;
    sa.sa_flags = SA_SIGINFO;
    sa.sa_sigaction = SIGALARM_handler;
    sigemptyset(&sa.sa_mask);
    sigaction(SIGUSR1, &sa, NULL);
    for (int i = 0; i< numThreads; i++){
      timerSetFirstTime[i] = true;
    }
    Solver solver(conf, gaussconfig);
    solversToInterrupt.clear();
    solversToInterrupt[0] = &solver;
    need_clean_exit = true;
    printVersionInfo(conf.verbosity);
    setDoublePrecision(conf.verbosity);
    parseInAllFiles(solver);
    FILE* res = openOutputFile();
    vector<FILE*> *resLog = new vector<FILE*>(numThreads,NULL);
    openLogFile(resLog);
    lbool ret = l_True;
    if (conf.startIteration > solver.independentSet.size()){
        printf("Manually-specified startIteration is larger than the size of the independent set.\n");
        return 0;
    }
    if (conf.startIteration == 0){
        printf("Computing startIteration using ApproxMC\n");

        SATCount solCount;
        std::mt19937 randomEngine{};
        SeedEngine(randomEngine);
        solCount = ApproxMC(solver, resLog, randomEngine);
        double elapsedTime = totalTime() - startTime;
        printf("Completed ApproxMC at %f s", elapsedTime);
        if (elapsedTime > conf.totalTimeout - 3000){
            printf(" (TIMED OUT)\n");
            return 0;
        }
        printf("\n");
        //printf("Solution count estimate is %d * 2^%d\n", solCount.cellSolCount, solCount.hashCount);
        if (solCount.hashCount == 0 && solCount.cellSolCount == 0){
            printf("The input formula is unsatisfiable.");
            return 0;
        }
        conf.startIteration = round(solCount.hashCount + log2(solCount.cellSolCount) + 
            log2(1.8) - log2(conf.pivotUniGen))-2;
    }
    else
        ;//printf("Using manually-specified startIteration\n");
    
    solversToInterrupt.clear();
    uint32_t maxSolutions = (uint32_t) (1.41*(1+conf.kappa)*conf.pivotUniGen +2);
    uint32_t minSolutions = (uint32_t) (conf.pivotUniGen/(1.41*(1+conf.kappa)));
    uint32_t samplesPerCall = SolutionsToReturn(maxSolutions+1, minSolutions, minSolutions);
    uint32_t callsNeeded = (conf.samples + samplesPerCall - 1) / samplesPerCall;
    printf("loThresh %d, hiThresh %d, startIteration %d\n", minSolutions, maxSolutions, conf.startIteration);
    //printf("Outputting %d solutions from each UniGen2 call\n", samplesPerCall);
    uint32_t numCallsInOneLoop = 0;
    if(conf.callsPerSolver == 0)
    {
        numCallsInOneLoop = std::min(solver.nVars()/(conf.startIteration*14), callsNeeded/numThreads);
        if (numCallsInOneLoop == 0){
            numCallsInOneLoop = 1;
        }
    }
    else
    {
        numCallsInOneLoop = conf.callsPerSolver;
        printf("Using manually-specified callsPerSolver\n");
    }

    uint32_t numCallLoops = callsNeeded / numCallsInOneLoop;
    uint32_t remainingCalls = callsNeeded % numCallsInOneLoop;
    //printf("Making %d loops, %d calls per loop, %d remaining\n", numCallLoops, numCallsInOneLoop, remainingCalls);
    bool timedOut;
    uint32_t sampleCounter = 0;
    std::map<std::string, uint32_t> threadSolutionMap;
    double allThreadsTime = 0;
    uint32_t allThreadsSampleCount = 0;
    printf("Launching %d sampling thread(s)\n", numThreads);
    #pragma omp parallel private(timedOut) firstprivate(threadSolutionMap,sampleCounter)
    {
        int threadNum = 0;
        #if defined(_OPENMP)
        threadNum = omp_get_thread_num();
        //printf("hello from thread %d\n", threadNum);
        #else
        //printf("not using OpenMP\n");
        #endif

        timedOut = false;
        //sampleCounter = 0;

        double threadStartTime = totalTime();

        std::mt19937 randomEngine{};
        SeedEngine(randomEngine);

        uint32_t lastSuccessfulHashOffset = 0;

        /* Perform extra UniGen calls that don't fit into the loops */
        #pragma omp single nowait
        {
            if(remainingCalls > 0)
                sampleCounter = singleThreadUniGenCall(remainingCalls,res,resLog,sampleCounter,threadSolutionMap,randomEngine,&lastSuccessfulHashOffset,threadStartTime);
        }

        /* Perform main UniGen call loops */
        #pragma omp for schedule(dynamic) nowait
        for (uint32_t i = 0;i<numCallLoops;i++)
        {
            if (!timedOut)
            {
                sampleCounter = singleThreadUniGenCall(numCallsInOneLoop,res,resLog,sampleCounter,threadSolutionMap,randomEngine,&lastSuccessfulHashOffset,threadStartTime);
                if ((totalTime() - threadStartTime) > conf.totalTimeout - 3000)
                    timedOut = true;
            }
        }

        /* Aggregate thread-specific solution counts */
        #pragma omp critical
        {
            for (map<std::string, uint32_t>::iterator itt = threadSolutionMap.begin();
                itt != threadSolutionMap.end(); itt++)
            {
                std::string solution = itt->first;
                map<std::string, std::vector<uint32_t>>::iterator itg = globalSolutionMap.find(solution);
                if (itg == globalSolutionMap.end()) {
                    globalSolutionMap[solution] = std::vector<uint32_t>(numThreads, 0); 
                }
                globalSolutionMap[solution][threadNum] += itt->second;
                allThreadsSampleCount += itt->second;
            }

            double timeTaken = totalTime() - threadStartTime;
            allThreadsTime += timeTaken;
            printf("Total time for UniGen2 thread %d: %f s", threadNum, timeTaken);
            if(timedOut)
                printf(" (TIMED OUT)");
            printf("\n");
        }
    }
    if (printResult)
        printSolutions(res);

    printf("Total time for all UniGen2 calls: %f s\n", allThreadsTime);
    printf("Samples generated: %d\n", allThreadsSampleCount);

    if (conf.needToDumpOrig) {
        if (ret != l_False) {
            solver.addAllXorAsNorm();
        }
        if (ret == l_False && conf.origFilename == "stdout") {
            std::cout << "p cnf 0 1" << std::endl;
            std::cout << "0";
        } else if (ret == l_True && conf.origFilename == "stdout") {
            std::cout << "p cnf " << solver.model.size() << " " << solver.model.size() << std::endl;
            for (uint32_t i = 0; i < solver.model.size(); i++) {
                std::cout << (solver.model[i] == l_True ? "" : "-") << i + 1 << " 0" << std::endl;
            }
        } else {
            if (!solver.dumpOrigClauses(conf.origFilename)) {
                std::cout << "Error: Cannot open file '" << conf.origFilename << "' to write learnt clauses!" << std::endl;
                exit(-1);
            }
            if (conf.verbosity >= 1)
                std::cout << "c Simplified original clauses dumped to file '"
                    << conf.origFilename << "'" << std::endl;
        }
    }
    if (ret == l_Undef && conf.verbosity >= 1) {
        std::cout << "c Not finished running -- signal caught or maximum restart reached" << std::endl;
    }
    if (conf.verbosity >= 1) solver.printStats();

    // printResultFunc(solver, ret, res, current_nr_of_solutions == 1);

    return correctReturnValue(ret);
}

/**
@brief For correctly and gracefully exiting

It can happen that the user requests a dump of the learnt clauses. In this case,
the program must wait until it gets to a state where the learnt clauses are in
a correct state, then dump these and quit normally. This interrupt hander
is used to achieve this
 */

int main(int argc, char** argv) {
    Main main(argc, argv);
    main.parseCommandLine();
    signal(SIGINT, SIGINT_handler);
    //signal(SIGALRM, SIGALARM_handler);
    try{
        return main.singleThreadSolve();

    }

    catch(std::bad_alloc) {
        std::cerr << "Memory manager cannot handle the load. Sorry. Exiting." << std::endl;
        exit(-1);
    }

    catch(std::out_of_range oor) {
        std::cerr << oor.what() << std::endl;
        exit(-1);
    }

    catch(CMSat::DimacsParseError dpe) {
        std::cerr << "PARSE ERROR!" << dpe.what() << std::endl;
        exit(3);
    }
    return 0;
}
