#include <iostream>
#include "halisi/types.h"
#include "halisi/refine.h"
#include "ibex/ibex.h"


using std::vector;
using namespace halisi;

// Array<Ctc> convert(const Array<NumConstraint>& csp) {
//     std::vector<Ctc*> vec;
//     for (int i= 0; i<csp.size(); i++) {
//         vec.push_back(new CtcFwdBwd(csp[i]));
//     }
//     return vec;
// }

// bool checkthashit (const Array<Ctc>& l)  {
//     int i=1, n=l[0].nb_var;
//     while (i<l.size() && l[i].nb_var==n) {
//         i++;
//     }
//     return (i==l.size());
// }

int main() {

    // A ∨ B ∧ C ∨ !A
    // auto colvec = ibex::Dim::col_vec(1);
    // const ibex::ExprSymbol& omega = ibex::ExprSymbol::new_(colvec);
    // auto a = omega[1] > 0.5;
    // auto nota = omega[1] <= 0.5;
    // auto b = omega[1] < 0.8;
    // auto notb = omega[1] >= 0.8;
    // auto c = omega[1] > 0.6;
    // auto notc = omega[1] <= 0.6;

    // LiteralMap lmap;
    // Lit A(0, false);
    // Lit B(1, false);
    // Lit C(2, false);
    // lmap[A] = a;
    // lmap[B] = b;
    // lmap[C] = c;

    // lmap[~A] = nota;
    // lmap[~B] = notb;
    // lmap[~C] = notc;

    // Clause clause1{A,B};
    // Clause clause2{C,!A};
    // CNF cnf;
    // cnf.push_back(clause1);
    // cnf.push_back(clause2);

    // std::vector<std::shared_ptr<ibex::ExprSymbol>> aux_vars

    // Box box(1);
    // box[0] = ibex::Interval(0,1);

    // pre_tlmh(lmap, cnf, omega, aux_vars, box);
    
    // // const ExprSymbol& z=ExprSymbol::new_("z");
    // auto yy = xx = xx;

    // SystemFactory fac;
    // fac.add_var(x);
    // fac.add_var(y);
    // fac.add_ctr(c1);
    // fac.add_ctr(c2);

    // // System
    // System sys(fac);
    // CtcHC4 hc4(sys);
    // hc4.accumulate=true;
    // sys.box = unit_box(sys.nb_var);

    // // Randomness
    // std::random_device rd;
    // std::mt19937 gen(rd());

    // // Bisector
    // SmearMaxRelative bsc(sys, 0.01);

    // auto sample = theory_sample(sys, hc4, bsc, gen);
    // cout << "FInal box is" << get<1>(sample) << endl;
    // cout << "Log probability box is" << get<2>(sample) << endl;
    // cout << "fraction full is" << get<3>(sample) << endl;


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
    // Clause clause;
    // clause.push_back(Lit(0, false));
    // clause.push_back(Lit(1, false));

    // CNF cnf;
    // cnf.push_back(clause);

    // LiteralMap lmap;
    // lmap[clause[0]] =  &c1;
    // lmap[clause[1]] =  &c2;
    // std::cout << clause[0].toInt() << std::endl;
    // std::cout << clause[1].toInt() << std::endl;
    // pre_tlmh(lmap, cnf, 100);

    return 0;
}
