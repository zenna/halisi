(ns ^{:doc "Linear programming wrapper around lpsolve"
	  :author "Zenna Tavares"}
	relax.linprog
  (:import (lpsolve AbortListener BbListener LogListener MsgListener
            LpSolveException LpSolve)))

(defn conv-ineq
  "Convert inequality symbol to form used by java-wrapper of lpsolve"
  [symb]
  ({'>= LpSolve/GE '> LpSolve/GE 
    '<= LpSolve/LE '< LpSolve/LE} symb))

(defn bounding-box-lp
  "Find bounding box around system of linear inequalities by making and 
   solving 2 * n-vars linear programming problems.
   Constraint in form [[coeffs var-is rhs] .. [..]]"
  [constraint vars]
  (let [n-vars (count vars)
        lp (LpSolve/makeLp 0 n-vars)]
    (println "LINPROG: find bounding box, NUM VARS :" n-vars)
    (.setAddRowmode lp true)
    (.setVerbose lp LpSolve/IMPORTANT)
  
    ; Create the linear program
    (doall
      (for [[coeffs var-is le-ge rhs] constraint]
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
                (let [ret (.solve lp)
                      pvar (println "SOLVE VAL IS" ret)]
                      (if (zero? ret)
                          (do
                            (.getVariables lp results)
                            (nth results i))
                          nil)))))]
      (.deleteLp lp)
      bounds)))