(ns kettler-control.statemachine
  (:require [kettler-control.time-to-value :as ttv]))
;; implementation taken from nakkaya.com/2010/06/22/finite-state-machine-implementation-in-clojure/ 
;; used with permission

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

(defn update-pars [pars key val] 
  (dosync (ref-set pars (update @pars key (fn [x] val)))))

(defn wait-some [ms]
        (Thread/sleep ms))

(defn get-minutes [pars]
  (/ (- (:t @pars) (:t0 @pars)) 60000.))


(defn get-power-by-minutes [pars]
  (ttv/choose-value-from-pairs (:seq @pars) (get-minutes pars)))

(defn seq-finished [pars]
  (<= (first (first (:seq @pars))) (get-minutes pars)))

(defn power-changed [pars]
  (not= (:power-target @pars) (get-power-by-minutes pars)))

(defn kettler-automaton [pars]
  {:init [ {:conditions [(= "pw-target" (:mode @pars))]
           :on-success #(update-pars pars :t (System/currentTimeMillis))
           :transition :start-pw-target}
          
          {:conditions []
            :on-success #(update-pars pars :t (System/currentTimeMillis))
            :transition :wait}]

   :start-pw-target [{:conditions []
            :transition :check-power}]
   
   :check-power [{:conditions [(power-changed pars)]
                  :on-success #(update-pars pars :power-target (get-power-by-minutes pars))
                  :transition :update-power}
                 
                 {:conditions []
                  :transition :wait}]
   
   :update-power  [{:conditions []
            :transition :wait}]

   :wait  [{:conditions []
            :on-success #(wait-some 500)
            :transition :init}]})

(defn statemachine-thread
  [automaton pars start-state]
  (let [sm (state-machine (automaton pars) start-state)]
    (while (not (or (seq-finished pars) (= (:mode @pars) "exit")))
      (do
        (Thread/sleep 100)
        (update-state sm)))
  (println "exit...")))
