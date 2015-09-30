#pragma once
#include <list>
#include <random>

namespace halisi {

// Uniformly and randomly select an element from a vector of those elements
template <typename T>
T rand_select(const std::vector<T> &x, std::mt19937 &gen) {
  assert(x.size()>0);
  std::uniform_int_distribution<int> dist(0, x.size()-1);
  int rand_index = dist(gen);
  std::cout << "Taking sample " << rand_index << " from vec of size " << x.size() << std::endl;
  return x[rand_index];
}

double findMean(std::list<int> numList) {
    double sum = 0;
    for (std::list<int>::iterator it = numList.begin(); it != numList.end(); it++) {
        sum += *it;
    }
    return (sum * 1.0 / numList.size());
}

double findMedian(std::list<int> numList) {
    numList.sort();
    int medIndex = int((numList.size() + 1) / 2);
    std::list<int>::iterator it = numList.begin();
    if (medIndex >= (int) numList.size()) {
        std::advance(it, numList.size() - 1);
        return double(*it);
    }
    std::advance(it, medIndex);
    return double(*it);
}

int findMin(std::list<int> numList) {
    int min = INT_MAX;
    for (std::list<int>::iterator it = numList.begin(); it != numList.end(); it++) {
        if ((*it) < min) {
            min = *it;
        }
    }
    return min;
}

}