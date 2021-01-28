(ns diabetes-dashboard-server.app
  (:require [diabetes-dashboard-server.handler :refer [handler]]
            [mount.core :as mount :refer [defstate]]
            [diabetes-dashboard-server.config :refer [config]]
            [ring.adapter.jetty9 :refer [run-jetty stop-server]]
            [diabetes-dashboard-server.db :refer [rollback-db migrate-db]]))
            
(defstate app :start (run-jetty handler {:port (get-in config [:server :port])
                                         :join? false})
  :stop (stop-server app)) 

(defn -main
  [& args]
  (mount/start))
