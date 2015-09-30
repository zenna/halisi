#include <cmsat/Solver.h>
#include "cmsat/DimacsParser.h"
#include <iostream>
#include "halisi/unigen.h"
#include <zlib.h>


using namespace halisi;

void readInAFile(const std::string& filename, Solver& solver)
{
    if (solver.conf.verbosity >= 1) {
        std::cout << "c Reading file '" << filename << "'" << std::endl;
    }
    #ifdef DISABLE_ZLIB
        FILE * in = fopen(filename.c_str(), "rb");
    #else
        gzFile in = gzopen(filename.c_str(), "rb");
    #endif // DISABLE_ZLIB

    if (in == NULL) {
        std::cout << "ERROR! Could not open file '" << filename << "' for reading" << std::endl;
        exit(1);
    }

    CMSat::DimacsParser parser(&solver, false, false, false);
    parser.parse_DIMACS(in);

    #ifdef DISABLE_ZLIB
        fclose(in);
    #else
        gzclose(in);
    #endif // DISABLE_ZLIB
}
int main() {
  CMSat::SolverConf conf;
  conf.verbosity = 0;
  CMSat::Solver solver(conf);
  readInAFile("formula.cnf", solver);
  std::cout << solver.solve() << std::endl;

  std::random_device rd;
  std::mt19937 gen(rd());

  std::vector<BoolVar> indep_vars = {212,109,7,19,331,50,318,338,101,287,66,224,183,77,140,133
                ,211,73,308,220,199,274,39,258,238,136,89,118,125,178,322,
                47,106,354,319,155,81,311,195,296,194,291,219,11,255,82,215,
                330,360,259,97,342,59,175,117,148,122,70,235,271,279,85,94,30,
                144,78,149,335,10,282,323,182,349,86,377,114,299,74,129,174,62,
                262,242,51,234,179,22,102,43,105,223,34,126,227,312,31,275,54,
                369,121,373,247,216,363,38,376,171,2,207,186,353,334,63,366,307
                ,304,239,15,35,167,254,295,263,204,278,187,231,230,156,137,42,270
                ,113,58,14,145,6,350,198,160,251,315,110,170,132,67,141,26,266,98,
                18,208,246,90,191,250,345,327,159,163,288,23,190,243,152,346,27,339,
                55,46,164,292,93,370,0,3,357,326,267};



  // loThresh 11, hiThresh 64, startIteration 23
  for (int i = 0; i < 10; ++i) {
    unigen(solver, 23, 11, 64, indep_vars, gen);
  }

  return 0;
}
