#include <cryptominisat4/cryptominisat.h>
#include <iostream>
#include "sigma/types.h"
#include "sigma/refine.h"
#include "ibex/ibex.h"


using std::vector;
using namespace sigma;
using namespace ibex;

Array<Ctc> convert(const Array<NumConstraint>& csp) {
    std::vector<Ctc*> vec;
    for (int i= 0; i<csp.size(); i++) {
        vec.push_back(new CtcFwdBwd(csp[i]));
    }
    return vec;
}

bool checkthashit (const Array<Ctc>& l)  {
    int i=1, n=l[0].nb_var;
    while (i<l.size() && l[i].nb_var==n) {
        i++;
    }
    return (i==l.size());
}

int main()
{
    // std::cout << "Hello" << std::endl;
      //Variable x;
    // Variable y;
    // NumConstraint c1(x,y,x*2*y>0.8);
    // NumConstraint c2(x,y,x>0.5);
    // CtcFwdBwd ctc1(c1);
    // CtcFwdBwd ctc2(c2);

    // const ExprSymbol& z=ExprSymbol::new_("z");
    const ExprSymbol& xx = ibex::ExprSymbol::new_("z");
    auto yy = xx = xx;

    SystemFactory fac;
    fac.add_var(x);
    fac.add_var(y);
    fac.add_ctr(c1);
    fac.add_ctr(c2);

    // System
    System sys(fac);
    CtcHC4 hc4(sys);
    hc4.accumulate=true;
    sys.box = unit_box(sys.nb_var);

    // Randomness
    std::random_device rd;
    std::mt19937 gen(rd());

    // Bisector
    SmearMaxRelative bsc(sys, 0.01);

    auto sample = theory_sample(sys, hc4, bsc, gen);
    cout << "FInal box is" << get<1>(sample) << endl;
    cout << "Log probability box is" << get<2>(sample) << endl;
    cout << "fraction full is" << get<3>(sample) << endl;


    // IntervalVector box = unit_box(2);
    //     std::cout << box << endl;
    // cout << "number of variables is" << hc4.nb_var << endl;
    // cout << "box after is" << box << endl;

    // std::cout << box << endl;
    // hc4.contract(box);

    //     std::cout << box << endl;
    // hc4.contract(box);

    //     std::cout << box << endl;
    // hc4.contract(box);

    // auto constraints = convert(a);
    // bool ok = checkthashit(constraints);
    // std::cout << "ok?" << ok << endl;
    // cout << constraints[0].nb_var << endl;
    // cout << constraints[1].nb_var << endl;

    // Formula A or B
    Clause clause;
    clause.push_back(Lit(0, false));
    clause.push_back(Lit(1, false));

    CNF cnf;
    cnf.push_back(clause);

    LiteralMap lmap;
    lmap[clause[0]] =  &c1;
    lmap[clause[1]] =  &c2;
    std::cout << clause[0].toInt() << std::endl;
    std::cout << clause[1].toInt() << std::endl;
    pre_tlmh(lmap, cnf, 100);

    return 0;
}
