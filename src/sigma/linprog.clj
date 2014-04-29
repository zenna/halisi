(ns ^{:doc "Linear programming wrapper around lpsolve"
	    :author "Zenna Tavares"}
	sigma.linprog
  (:import (lpsolve AbortListener BbListener LogListener MsgListener
            LpSolveException LpSolve)))

(defn conv-ineq
  "Convert inequality symbol to form used by java-wrapper of lpsolve"
  [symb]
  ({'>= LpSolve/GE '> LpSolve/GE 
    '<= LpSolve/LE '< LpSolve/LE} symb))

(defn classify-ineqs
  "What kind of polyhedron is a system of linear inequalities?
   Options are 'feasible 'unbounded 'infeasible"
  [constraints vars]
  (let [n-vars (count vars)
        lp (LpSolve/makeLp 0 n-vars)]
    ; (println "LINPROG: find bounding box, NUM VARS :" n-vars)
    (.setAddRowmode lp true)
    (.setVerbose lp LpSolve/IMPORTANT)
  
    ; Create the linear program
    (doall
      (for [[coeffs var-is le-ge rhs] constraints]
        (.addConstraintex lp
          n-vars
          (double-array coeffs)
          (int-array var-is)
          (conv-ineq le-ge) rhs)))

    (.setAddRowmode lp false)
    
    ; Make arbitrary linear program: minimise all coefs 1.0
    ; We're just trying to find if constraints are feasible 
    ; (could prob do a better way)
    (.setMinim lp)
    (.setObjFnex lp
                  n-vars
                  (double-array (vec (repeat n-vars 1.0)))
                  (int-array (range 1 (inc n-vars))))
    (try
      (condp = (.solve lp)
        0 'optimal
        1 'suboptimal
        2 'infeasible
        3 'unbounded
        4 'degenerate
        :else 'other
        )
      (finally
        (.deleteLp lp)))))

(defn arbitrary-point-lp
  "Get an arbitrary solution from linear program.
   Currently arbitrary means a function to minimise has all on
   coefficients"
  [constraints vars]
  (let [n-vars (count vars)
        lp (LpSolve/makeLp 0 n-vars)]
    (.setAddRowmode lp true)
    (.setVerbose lp LpSolve/IMPORTANT)
    
    ; Create the linear program
    (doall
      (for [[coeffs var-is le-ge rhs] constraints]
        (.addConstraintex lp
          n-vars
          (double-array coeffs)
          (int-array var-is)
          (conv-ineq le-ge) rhs)))

    (.setAddRowmode lp false)

    ; Make linear program with coefficients all 1
    (let [results (double-array (vec (repeat n-vars 0.0)))
          new-results
          (do
            (.setMinim lp) 
            (.setObjFnex lp
              n-vars
              (double-array (vec (repeat n-vars 1.0)))
              (int-array (range 1 (inc n-vars))))
            (let [ret (.solve lp)]
                  (if (zero? ret)
                      (do
                        (.getVariables lp results)
                        (vec results))
                      nil)))]
      (.deleteLp lp)
      new-results)))

(defn bounding-box-lp
  "Find bounding box around system of linear inequalities by making and 
   solving 2 * n-vars linear programming problems.
   constraints in form [[coeffs var-is rhs] .. [..]]

   Returns nil if cannot solve"
  [constraints vars]
  (let [n-vars (count vars)
        lp (LpSolve/makeLp 0 n-vars)]
    ; (println "LINPROG: find bounding box, NUM VARS :" n-vars)
    (.setAddRowmode lp true)
    (.setVerbose lp LpSolve/IMPORTANT)
  
    ; Create the linear program
    (doall
      (for [[coeffs var-is le-ge rhs] constraints]
        (.addConstraintex lp
          n-vars
          (double-array coeffs)
          (int-array var-is)
          (conv-ineq le-ge) rhs)))

    (.setAddRowmode lp false)

    ; For each variable compute max and min
    (let [results (double-array (vec (repeat n-vars 0.0)))
          bounds
          (doall
            (for [i (range n-vars)
                  max-mim [#(.setMinim lp) #(.setMaxim lp)]]
              (do
                (max-mim) 
                (.setObjFnex lp
                  n-vars
                  (double-array (assoc (vec (repeat n-vars 0.0)) i 1.0))
                  (int-array (range 1 (inc n-vars))))
                (let [ret (.solve lp)]
                      (if (zero? ret)
                          (do
                            (.getVariables lp results)
                            (nth results i))
                          nil)))))]
      (.deleteLp lp)
      bounds)))