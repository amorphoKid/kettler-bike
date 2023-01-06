(ns mockup)

(defn state-string
  [state-map]
  (clojure.string/join ","
        (map str
             (map #(% state-map)
                  [:pulse :pulse-target :power :req-power :time :rpm :speed :dist :energy]))))  

(defn pid
  [dev prev-dev sum-dev [Kp Ki Kd]]
  (let [new-sum (+ sum-dev dev)
        delta-dev (- dev prev-dev)]
    {:response (+
                 (* Kp dev)
                 (* Ki new-sum)
                 (* Kd delta-dev))
     :sum new-sum
     :prev-dev dev}))

(defn get-equi-pulse
  "get quasi equilibrium heart rate"
  [power energy pars]
  (let [max-watt 220
        pulse-slope (/ (- (:max-pulse pars) (:rest-pulse pars)) max-watt)
        drift-val (min
                   (:max-drift pars)
                   (* energy (:pulse-perf-drift pars)))]
    (min (:max-pulse pars)
         (+ (:rest-pulse pars) drift-val (* pulse-slope power)))))

(defn update-kettler
  "update pulse by PT1 Model"
  [state pars]
  (let [cur-power (:power @state)
        cur-energy (:energy @state)
        cur-pulse (:pulse @state)
        cur-time (:time @state)
        cur-dist (:dist @state)
        eq-pulse (get-equi-pulse cur-power cur-energy pars)
        pulse-inc (* (- eq-pulse cur-pulse)
                  (/ (:time-inc pars)(:relax-time pars)))
        energy-inc (* cur-power (:time-inc pars) 4 0.239 0.001)
        dist-inc (* (:speed @state)
                    (:time-inc pars))]
    (swap! state assoc-in [:pulse] (+ cur-pulse pulse-inc))
    (swap! state assoc-in [:dist] (+ cur-dist dist-inc))
    (swap! state assoc-in [:energy] (+ cur-energy energy-inc))
    (swap! state assoc-in [:time] (+ cur-time (:time-inc pars)))))

(defn set-power
  "changes power"
  [state power]
  (let [p (max power 0)]
    (swap! state assoc-in [:req-power] p)
    (swap! state assoc-in [:power] p)))

(defn set-pulse-target
  "changes target-pulse"
  [state hr]
  (swap! state assoc-in [:pulse-target] hr))

(defn change-power
  "changes power by dw"
  [state dw]
  (let [cur-power (:power @state)]
    (set-power state (+ cur-power dw))))


(defn update-power-pid
  [state pid-resp]
  (let [prev-dev (:prev-dev pid-resp)
        sum (:sum pid-resp)
        cur-power (:power @state)
        cur-energy (:energy @state) 
        cur-pulse (:pulse @state)
        target-pulse (:pulse-target @state)
        dev (- target-pulse cur-pulse)
        pid-val (pid dev prev-dev sum [1.70 0.03 -1.4])]
    (set-power state (:response pid-val))
    pid-val))
    

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  
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
  (loop [n 0]
    (update-kettler kettler-state sim-pars)
    (spit "hr1.csv" (clojure.string/join [(state-string @kettler-state) "\n"]) :append true)
    (if-not (< n 2500)
      (println (state-string @kettler-state))
      (recur (inc n))))
  
  (loop [n 0
         buff {:sum 0 :prev-dev 0 :response 0}]
    (update-kettler kettler-state sim-pars)
    (spit "hr1.csv" (clojure.string/join [(state-string @kettler-state) "\n"]) :append true)
    (if-not (< n 2500)
      (println (state-string @kettler-state))
      (recur (inc n) (update-power-pid kettler-state buff))))

  (set-power kettler-state 90)

  (loop [n 0]
    (update-kettler kettler-state sim-pars)
    (spit "hr1.csv" (clojure.string/join [(state-string @kettler-state) "\n"]) :append true)
    (if-not (< n 2500)
      (println (state-string @kettler-state))
      (recur (inc n)))))
