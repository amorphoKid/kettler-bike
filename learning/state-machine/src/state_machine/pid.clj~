(ns pid.core
  (:gen-class))

(defn pid-control
  [buf-ref kp ki kd target current]
  (let [dev (- target current)
        dev-sum (:dev-sum @buf-ref)
        prev-dev (:prev-dev @buf-ref)
        diff (- prev-dev dev)
        new-sum (+ dev-sum dev)]
    (dosync
     (ref-set buf-ref (update @buf-ref :dev-sum (fn [x] new-sum)))
     (ref-set buf-ref (update @buf-ref :prev-dev (fn [x] dev))))
    (+ (* kp target) (* ki dev-sum) (* kd diff))))
        

(defn get-pid-control
  [buf-ref kp ki kd]
  (partial pid-control buf-ref kp ki kd))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (def my-buf (ref {:dev-sum 0 :prev-dev 0})) 
  (def my-pid (get-pid-control my-buf 1 1 1))
  (my-pid 2 0))
