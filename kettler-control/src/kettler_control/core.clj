(ns kettler-control.core
  (:require [kettler-control.time-to-value :as ttv]))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (def time-to-power (ttv/time-to-value ttv/power-seq))
  (time-to-power 1))
