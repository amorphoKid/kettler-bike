(ns rest-api.core
  (:require [org.httpkit.server :as server]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer :all]
            [clojure.pprint :as pp]
            [clojure.string :as str]
            [clojure.data.json :as json])
  (:gen-class))

;; Simple Body Page
(defn simple-body-page [req] ;(3)
  {:status  200
   :headers {"Content-Type" "text/html"}
   :body    "Hello World"})

(def dummy-status (atom  
  {:pulse "000",
   :rpm "000",
   :speed "000",
   :dist "000",
   :req-power "025",
   :energy "0000",
   :time "00:00",
   :power "000"}
  ))

;;dummy 
(defn kettler-status [req] ;(3)
  {:status  200
   :headers {"Content-Type" "text/html"}
   :body   (str @dummy-status) })

; Helper to get the parameter specified by pname from :params object in req
(defn getparameter [req pname] (get (:params req) pname))
; Our main routes

(defn set-power [req]
  {:status  200
   :headers {"Content-Type" "text/html"}
   :body (str "set power to: " (getparameter req :power))})

(defroutes app-routes
  (GET "/" [] simple-body-page)
  (GET "/kettler" [] kettler-status)
  (GET "/kettler/power" [] set-power) 
  (route/not-found "Error, page not found!"))

; Our main entry function
(defn -main
  "This is our main entry point"
  [& args]
  (let [port (Integer/parseInt (or (System/getenv "PORT") "3000"))]
    ; Run the server with Ring.defaults middleware
    (server/run-server (wrap-defaults #'app-routes site-defaults) {:port port})
    ; Run the server without ring defaults
    ;(server/run-server #'app-routes {:ip 192.168.178.145 :port port})
    (println (str "Running webserver at http:/191.168.178.145:" port "/"))))
