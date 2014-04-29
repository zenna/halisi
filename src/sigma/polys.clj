;1. TODO move this to a separate library
;2. Move nelder mead to separate library
;3. 

(defn img-cost-f
  [param-values]
  (let [points (subvec param-values 0 -1)
        rendered-img (render-poly params)
        sigma (last params)
        blurred-img (blur-img)
        error (blurred-img data)
        mse (sum (square error))]
    mse))

(defn render-poly)

(defn noisy-compare
  ""
  []
  (nelder-mead img-cost-f))