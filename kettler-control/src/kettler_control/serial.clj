(ns kettler-control.serial
  (:require [serial-port :as sp]
            )
  (:gen-class))


(use 'serial-port)
(def kettler (atom {:msg-status "ack" :status-str ""}))
(def buffer (atom []))
  
(defn parse-int [s]
  (Integer. (re-find  #"\d+" s )))

(defn get-time [s]
  (let [time-ar (map parse-int (clojure.string/split s #":"))]
    (+ (* 60 (first time-ar)) (second time-ar))))

(defn status-from-string [str]
  (let [stat (clojure.string/split str #"\t")]
    {
     :pulse (parse-int (nth stat 0))
     :rpm (parse-int (nth stat 1))
     :speed (parse-int (nth stat 2))
     :dist (parse-int (nth stat 3))
     :req-power (parse-int (nth stat 4))
     :energy (parse-int (nth stat 5))
     :time (get-time (nth stat 6))
     :power (parse-int (nth stat 7))}
    )
  )

(defn get-status-str [buf]
    (clojure.string/join (map str (map char (pop buf)))))

(defn add-new-char [char]
  (let [buf @buffer]
    (if (and (= 13 (last buf)) (= 10 char))
      (do
        (swap! buffer (fn [state] []))
        (swap! kettler (fn [state] {:msg-status "ack" :status-str (get-status-str buf)})))       
      (swap! buffer (fn [state] (conj buf char))))))

    
(def b-reset (byte-array [(byte 0x52) (byte 0x53) (byte 0x0a) (byte 0x0d)]))

(def b-pw110 (byte-array [(byte 0x50) (byte 0x57) (byte 0x20) (byte 0x31) (byte 0x31) (byte 0x30) (byte 0x0a) (byte 0x0d)]))

(def b-cm (byte-array [(byte 0x43) (byte 0x4d) (byte 0x0a) (byte 0x0d)]))

(def b-status (byte-array [(byte 0x53) (byte 0x54) (byte 0x0a) (byte 0x0d)]))

(defn byte-array-from-string [s]
  (byte-array (map byte (char-array s))))

(defn send-command [port byte-arr]
  ;;(while (not= (:msg-status @kettler) "ack"))
  (swap! kettler (fn [state] (assoc-in @kettler [:msg-status] "new"))) 
  (write port byte-arr)
  (while (= (:msg-status @kettler) "new")))

(defn set-power [port pw]
  (send-command port (byte-array-from-string (clojure.string/join ["PW " (str pw) "\n\r"])))
  (get-status port))

(defn get-status [port]
    (send-command port b-status)
    (status-from-string (:status-str @kettler)))


(defn update-power [port delta]
  (let [cur_pow_setpoint (:req-power (get-status port))
        new_pow_setpoint (+ cur_pow_setpoint delta)]
    (set-power port new_pow_setpoint)))


(defn test-tty
  "I don't do a whole lot ... yet."
  [& args]
  (def port (open "/dev/ttyUSB0" 9600))
  (on-byte port (fn [x] (add-new-char x)))
  (Thread/sleep 1000)
  (send-command port b-reset)
  (send-command port b-cm)
  (send-command port b-status)
  (set-power port 25)
  (dotimes [_ 10]
    ;;(Thread/sleep 1000)
    (update-power port 5)
    (println (status-from-string (:status-str @kettler))))
  (close port)
  )

