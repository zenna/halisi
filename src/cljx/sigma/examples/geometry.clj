(ns ^{:doc "Generate Geometry with constraints"
      :author "Zenna Tavares"}
  sigma.examples.geometry)

(defn gen-box-constraints-overlap
  "Generate a set of n-boxes such that they all overlap (I think?)"
  [n-boxes]
  (let [vars
       (for [i (range n-boxes)]
          [(symbol (str "x" i)) (symbol (str "y" i)) (symbol (str "r" i))])]
    {:vars (reduce concat vars)
     :pred
    `(~'and
      ~@(reduce concat
        (for [[[ax ay ar][bx by br]] (unique-pairs vars)]
        `((~'< (~'+ ~ax (~'* -1 ~ar) (~'* -1 ~bx) (~'* -1 ~br)) 0)
          (~'> (~'+ ~ax ~ar (~'* -1 ~bx) ~br ) 0)
          (~'> (~'+ ~ay ~ar (~'* -1 ~by) ~br ) 0)
          (~'< (~'+ ~ay (~'* -1 ~ar) (~'* -1 ~by) (~'* -1 ~br)) 0)))))}))

(defn gen-box-non-overlap
  "Generate a set of n-boxes such that none overlap (I think?)"
  [n-boxes]
  (let [vars
       (for [i (range n-boxes)]
          [(symbol (str "x" i)) (symbol (str "y" i)) (symbol (str "r" i))])]
    {:vars (reduce concat vars)
     :pred
    `(~'and
      ~@(for [[[ax ay ar][bx by br]] (unique-pairs vars)]
        `(~'or 
          (~'> (~'+ ~ax (~'* -1 ~ar) (~'* -1 ~bx) (~'* -1 ~br)) 0)
          (~'< (~'+ ~ax ~ar (~'* -1 ~bx) ~br ) 0)
          (~'> (~'+ ~ay (~'* -1 ~ar) (~'* -1 ~by) (~'* -1 ~br)) 0)
          (~'< (~'+ ~ay ~ar (~'* -1 ~by) ~br ) 0))))}))

(defn gen-box-non-overlap-close
  "Generate a set of n-boxes within thres-distance of eachother"
  [n-boxes]
  (let [proximity-thresh 1.0 ; How close the boxes must be
        vars
       (for [i (range n-boxes)]
          [(symbol (str "x" i)) (symbol (str "y" i)) (symbol (str "r" i))])]
    {:vars (reduce concat vars)
     :pred
    `(~'and
      ~@(for [[[ax ay ar][bx by br]] (unique-pairs vars)]
        `(~'or 
          (~'and
            (~'> (~'+ ~ax (~'* -1 ~ar) (~'* -1 ~bx) (~'* -1 ~br)) 0)
            (~'< (~'+ ~ax (~'* -1 ~ar) (~'* -1 ~bx) (~'* -1 ~br)) ~proximity-thresh))

          (~'and
            (~'< (~'+ ~ax ~ar (~'* -1 ~bx) ~br ) 0)
            (~'> (~'+ ~ax ~ar (~'* -1 ~bx) ~br ) ~(* -1 proximity-thresh)))

          (~'and
            (~'> (~'+ ~ay (~'* -1 ~ar) (~'* -1 ~by) (~'* -1 ~br)) 0)
            (~'< (~'+ ~ay (~'* -1 ~ar) (~'* -1 ~by) (~'* -1 ~br)) ~proximity-thresh))

          (~'and
            (~'< (~'+ ~ay ~ar (~'* -1 ~by) ~br ) 0)
            (~'> (~'+ ~ay ~ar (~'* -1 ~by) ~br ) ~proximity-thresh)))))}))