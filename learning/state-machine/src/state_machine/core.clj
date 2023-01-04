(ns state-machine.core
  (:require [state-machine.mockup :as mu])
  (:gen-class))

(defn state-machine [transition-table initial-state]
  (ref initial-state :meta transition-table))

(defn find-first [pred s]
  (first (filter pred s)))

(defn- switch-state? [conds]
  (if (empty? conds)
    true
    (not (some false? (reduce #(conj %1 (if (fn? %2) (%2) %2)) [] conds)))))

(defn first-valid-transition [ts]
  (find-first #(= (second %) true)
              (map #(let [{conds :conditions 
                           transition :transition
                           on-success :on-success} %]
                      [transition (switch-state? conds) on-success]) ts)))

(defn update-state [state]
  (let [transition-list ((meta state) @state)
        [transition _ on-success] (first-valid-transition transition-list)]
    (if-not (nil? transition)
      (do 
        (if-not (nil? on-success)
          (on-success))
        (dosync (ref-set state transition))))))

(def sim-pars {
               :time-inc 0.1
               :relax-time 7
               :max-pulse 170
               :ctrl-treshold 10
               :pulse-perf-drift 0.08
               :max-drift 20
               :rest-pulse 70})

;; labels taken from serial :pulse :rpm :speed :dist :req-power :energy :time :power
(def kettler-state (atom {
                          :pulse 70
                          :pulse-target 145
                          :power 80
                          :req-power 80
                          :time 0 
                          :rpm 100
                          :speed 10
                          :dist 0
                          :energy 0}))

(defn kettler-control [pars]
  {:start [{:conditions [#(= (:mode @pars) "hr-target")]
            :transition :hr-target}
           {:conditions [#(= (:mode @pars) "pw-target")]
            :transition :pw-target}
           {:conditions []
            :on-success #(println "start")
            :transition :start}]
   
   :pw-target [{:conditions []
                :on-success #(do (println "pw-target")
                                 (println @kettler-state))
                :transition :update-power}]
   
   :hr-target [{:conditions []
                :on-success #(do (mu/update-kettler kettler-state sim-pars)
                                  (println "hr-target")
                                  (println @kettler-state))
                :transition :pid-power}]
   
   :pid-power [{:conditions []
                :on-success #(do (println "pid-power")
                                 (println @pars))
                                 
                :transition :start}]
   
   :update-power [{:conditions []
                   :on-success #(do (println "update-power")
                                    (println @pars))
                   :transition :start}]
   })

(defn long-thread
  [sm]
  (dotimes [_ 40]
    (Thread/sleep 1000)
    (update-state sm)))
  
(defn -main
  "demo loop for state-machine"
  [& args]
  
  (def demo-pars (ref 
    {
     :mode "hr-target"
     :pulse-target 135 
    }))
  (future (long-thread (state-machine  (kettler-control demo-pars) :start)))
  (Thread/sleep 20000) 
  (dosync (ref-set demo-pars {:mode "pw-target"  :pw-target 90})) 
 ) 
