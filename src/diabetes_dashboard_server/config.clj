(ns diabetes-dashboard-server.config
  (:require [cprop.core :refer [load-config]]))

(def config (atom nil))

(defn init-config!
  []
  (reset! config (load-config)))

(defn unset-config!
  []
  (reset! config nil))

