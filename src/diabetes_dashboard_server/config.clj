(ns diabetes-dashboard-server.config
  (:require [cprop.core :refer [load-config]]
            [mount.core :refer [defstate]]))

(defstate config :start (load-config))
