(ns kettler-control.core
  (:require [kettler-control.time-to-value :as ttv]
             [kettler-control.statemachine :as sm]))

  (def time-to-power (ttv/time-to-value ttv/power-seq))
  (def demo-pars (ref 
                  {
                   :mode "pw-target"
                   :t 0
                   :t-start (System/currentTimeMillis)
                   :power-target 135
                   }))
(defn get-power []
  (ttv/choose-value-from-pairs ttv/power-seq (/ (- (:t @demo-pars) (:t-start @demo-pars)) 60000.)))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (future (sm/statemachine-thread sm/kettler-automaton demo-pars :init)))
