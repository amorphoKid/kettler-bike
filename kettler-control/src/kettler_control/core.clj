(ns kettler-control.core
  (:require [kettler-control.time-to-value :as ttv]
             [kettler-control.statemachine :as sm]))

(defn get-power [pars]
  (ttv/choose-value-from-pairs ttv/power-seq (/ (- (:t @pars) (:t0 @pars)) 60000.)))

;;(def power-seq '([16 0] [14 100] [12 190] [2 160] [0 120])) 
(def power-seq '([0.2 10] [0 120])) 
(def power-ints '([2 120] [10 160] [2 190] [2 100] [0 0])) 

(def demo-pars (ref 
                {
                :mode "pw-target"
                :seq power-seq
                :t (System/currentTimeMillis)
                :t0 (System/currentTimeMillis)
                :power-target 0
                }))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (future (sm/statemachine-thread sm/kettler-automaton demo-pars :init)))
