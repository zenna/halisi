(ns ^{:doc "Abstract Domain (Orthorope) abstractions"
                :author "Zenna Tavares"}
  sigma.domains.abstract)

(defprotocol dist
   "This is an interface for probabilstic abstract domains
    A domain should strive to implement means to

    1) sample from it
    2) condition it
    3) compute its expectation"
  (sample [dist randomness])
  (condition [dist pred?])
  (expectation [dist]))