(ns kettler-control.time-to-value)


(defn seq-from-ints
  [int-pairs]
  (let [total (reduce + (map #(first %) int-pairs))
        start (first (first int-pairs))]
    (loop [t 0
           seq-pairs []
           [cur-p & rest] int-pairs]
      (let [new-t (+ t (first cur-p))
            cur-pow  (second cur-p)
            new-seq (cons [t cur-pow] seq-pairs)]
        (if (= 0 (count rest))
        new-seq
        (recur new-t new-seq  rest))))))

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
