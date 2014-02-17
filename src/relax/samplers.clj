(ns ^{:docs "Different Samplers"
      :authoer "Zenna Tavares"}
  relax.samplers)

(defn overlapping-sampler [n-samples]
  (let [box1 {:name 'box1 :internals [[0 20][0 20]]}
        box2 {:name 'box2 :internals [[18 22][18 22]]}
        ; box3 {:name 'box3 :internals [[0 10][0 12]]}
        vars '[x0 x1]
        boxes [box2 box1]
        formula-maker
        (fn [box]
          `(~'and
            ~@(reduce concat
                (for [[low upp i] (map #(conj %1 %2) box (range (count box)))]
              `((~'> ~(symbol (str "x" i)) ~low)
                (~'< ~(symbol (str "x" i)) ~upp))))))
        boxes (map #(assoc % :formula
                              (make-lambda-args (formula-maker (:internals %)) vars))
                    boxes)
        volumes (map volume boxes)]
    (loop [samples [] cache (zipmap boxes []) n-samples n-samples]
      (println cache "\n")
      (cond
        (zero? n-samples)
        samples

        :else
        (let [box (categorical boxes volumes)
              cache-item (peek (cache box))]
          ; Do I have a cache?
          (if (nil? cache-item)
              ; If not proceed normally
              (let [sample (interval-sample (:internals box))
                    all-boxes (map #(apply (:formula %) sample) boxes)
                    pos-dominant (max-pred-index true? all-boxes)
                    pos-box (max-pred-index #(= box %) boxes)]
                    (if (= pos-dominant pos-box)
                        (recur (conj samples sample) cache (dec n-samples))
                        (recur samples cache n-samples)))
                        ; (recur samples
                        ;        (update-in cache [(nth boxes pos-dominant)]
                        ;                   #(conj % {:from box :sample sample}))
                        ;        n-samples)))

              ; Otherwise with prob p=ratio of overlapping region/box, take sample
              (if (flip (/ (volume (overlap box (:from cache-item))) (volume box)))
                  (recur (conj samples (:sample cache-item)) ; I should take the sample
                         (update-in cache [box]
                                    #(pop %))
                         (dec n-samples))
                  ; and 1-p generate sample in non-overlapping region
                  ; Treat as normal sample
                  (let [sample (gen-until #(interval-sample (:internals box))
                                    #(and (apply (:formula box) %)
                                          (not (apply (:formula (:from cache-item)) %))))]
                        ; all-boxes (map #(apply (:formula %) sample) boxes)
                        ; pos-dominant (max-pred-index true? all-boxes)
                        ; pos-box (max-pred-index #(= box %) boxes)]
                        ; (if (= pos-dominant pos-box)
                            (recur (conj samples sample) cache (dec n-samples))))))))))

(defn -main[]
  (let [{vars :vars pred :pred} (gen-box-non-overlap-close 3)
        intervals (vec (reduce concat (map #(vector `(~'> ~% 0) `(~'< ~% 1)) vars)))
        new-model (construct
                    (vec vars)
                    intervals
                    pred)
        data (repeatedly 1 new-model)
        samples (extract data :sample)
        n-sampled (sum (extract data :n-sampled))
        n-rejected (sum (extract data :n-rejected))]
  (println "N-SAMPLES:" n-sampled " n-rejected: " n-rejected " ratio:" (double (/ n-rejected n-sampled)))
  (samples-to-file "opx" samples)
  samples))


; (defn -main[]
;   (let [{vars :vars pred :pred} (gen-box-non-overlap-close 3)
;         intervals (vec (reduce concat (map #(vector `(~'> ~% 0) `(~'< ~% 1)) vars)))
;         new-model (construct
;                     (vec vars)
;                     intervals
;                     pred)
;         data (repeatedly 1 new-model)
;         samples (extract data :sample)
;         n-sampled (sum (extract data :n-sampled))
;         n-rejected (sum (extract data :n-rejected))]
;   (println "N-SAMPLES:" n-sampled " n-rejected: " n-rejected " ratio:" (double (/ n-rejected n-sampled)))
;   (samples-to-file "opx" samples)
;   samples))

()
                            ; (recur samples
                            ;        (update-in cache [(nth boxes pos-dominant)]
                            ;                   #(conj % {:from box :sample sample}))
                            ;        n-samples))))))))))

; (defn -main []
;   (samples-to-file "kidding2" (overlapping-sampler 20000)))