(ns serial-killa.statemachine
(:gen-class)
)

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


(defn update-hr [pars val]
  (dosync (ref-set pars {:mode (:mode @pars) :hr-target (:hr-target @pars)
                         :hr-actual (+ val (:hr-actual @pars))}))) 

(defn kettler-control [pars]
   (let [start-trans {:conditions []
                      :on-success #(println @pars)
                      :transition :start}]
     {:start [{:conditions [#(= (:mode @pars) "hr-target")]
               :transition :hr-target}]
      :hr-target [{:conditions [#(< (:hr-actual @pars) (:hr-target @pars))]
                   :transition :increase-pw}
                  {:conditions [#(> (:hr-actual @pars) (:hr-target @pars))]
                   :transition :decrease-pw}
                  start-trans]
     :increase-pw [{:conditions []
                    :on-success #(do (println @pars)
                                     (update-hr pars 1))
                    :transition :start}]
     :decrease-pw [{:conditions []
                    :on-success #(do (println @pars)
                                     (update-hr pars -1))
                    :transition :start}]
      }))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]

  
  (let [pars (ref {:mode "hr-target" :hr-target 135 :hr-actual 144})
        sm (state-machine  (kettler-control pars) :start)]
    (dotimes [_ 40]
            (update-state sm)))
                  
  )
