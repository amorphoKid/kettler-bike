(ns kettler-control.core
  (:require [kettler-control.time-to-value :as ttv]
            [kettler-control.statemachine :as sm]
            [serial-port :as sp]
            [kettler-control.serial :as sr]))


(use 'serial-port)
;(def power-seq '([16 0] [14 100] [12 190] [2 160] [0 120])) 
(def power-seq '([5 0] [3 100] [2 50] [1 30] [0 10])) 
(def power-ints '([3 0] [2 120] [10 160] [2 190] [2 100] [0 0])) 


(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (def demo-pars (ref
                  {
                    :port nil 
                    :mode "pw-target"
                    :seq power-seq
                    :t (System/currentTimeMillis)
                    :t0 (System/currentTimeMillis)
                    :power-target 0
                   }))
  
  (def port (open "/dev/ttyUSB0" 9600))
  (on-byte port (fn [x] (sr/add-new-char x)))
  ;(sr/send-command port sr/b-reset)
  (sm/update-pars demo-pars :port port)  
  (Thread/sleep 1000)
  (future (sm/statemachine-thread sm/kettler-automaton demo-pars :init)))
