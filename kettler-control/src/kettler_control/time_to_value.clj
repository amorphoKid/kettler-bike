(ns kettler-control.time-to-value)

(def power-seq [[16 0] [14 100] [12 190] [2 160] [0 120]]) 

(defn choose-value-from-pairs
  [pair-seq x]
  (cond
    (< x 0) (second (last pair-seq))
    :else  
    (second 
     (first
      (filter #(>= x (first %) ) pair-seq)))))

(defn time-to-value
  [pair-seq]
  (partial #(choose-value-from-pairs pair-seq %)))
 
;(def time-to-power (time-to-value power-seq))
