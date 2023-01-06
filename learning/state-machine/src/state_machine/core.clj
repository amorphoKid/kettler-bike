(ns state-machine.core
  (:require [mockup :as mu])
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


(def demo-pars (ref 
                {
                 :mode "hr-target"
                 :pulse-target 135
                 :buf {:sum 0 :prev-dev 0 :response 0}
                 }))

(defn update-pars [pars key val] 
  (dosync (ref-set pars (update @pars key (fn [x] val))))) 

(defn update-power [state pars]
  (let [pid (mu/update-power-pid state (:buf @pars))]
    (update-pars pars :buf pid)))

(defn kettler-control [pars]
  {:start [{:conditions [#(= (:mode @pars) "hr-target")]
            :transition :hr-target}
           {:conditions [#(= (:mode @pars) "pw-target")]
            :transition :pw-target}
           {:conditions []
            :on-success #(println "start")
            :transition :start}]
   
   :pw-target [{:conditions []
                :transition :update-power}]
   
   :hr-target [{:conditions []
                :on-success #(mu/update-kettler kettler-state sim-pars)
                :transition :pid-power}]
   
   :pid-power [{:conditions []
                :on-success #(update-power kettler-state pars)
                :transition :start}]
   
   :update-power [{:conditions []
                   :transition :start}]
   })
(defn statemachine-thread
  [control pars start-state]
  (let [sm (state-machine (control pars) start-state)]
    (while (not (= "exit" (:mode @pars)))
      (do
        (Thread/sleep 100)
        (update-state sm)))
  (println "exit...")))
  
(defn set-pw-mode [power]
  (dosync (ref-set demo-pars {:mode "pw-target"  :pw-target power})))

(defn set-hr-mode [hr-target]
  (mu/set-pulse-target kettler-state hr-target)
  (dosync (ref-set demo-pars 
                   {
                    :mode "hr-target"
                    :pulse-target hr-target
                    :buf {:sum 0 :prev-dev 0 :response 0}
                    })))

(defn stop-machine []
  (dosync (ref-set demo-pars {:mode "exit"  :pw-target 90})))


(defn -main
  "demo loop for state-machine"
  [& args]
  (set-hr-mode 135)
  (future (statemachine-thread kettler-control demo-pars :start))
 ) 
